package com.govconnect.auth.controller;

import com.govconnect.auth.dto.AuthResponse;
import com.govconnect.auth.dto.LoginRequest;
import com.govconnect.auth.dto.MeResponse;
import com.govconnect.auth.security.JwtService;
import com.govconnect.auth.security.LoginRateLimiter;
import com.govconnect.auth.service.AuthService;
import com.govconnect.auth.service.AuthService.AuthResult;
import com.govconnect.shared.constants.ApiMessages;
import com.govconnect.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de autenticación.
 * <p>
 * Ambos tokens viajan en cookies HttpOnly:
 * <ul>
 *   <li><b>access_token</b> — corta duración, path {@code /api/v1}, se envía
 *       automáticamente en cada petición a la API.</li>
 *   <li><b>refresh_token</b> — larga duración, path {@code /api/v1/auth}, usado
 *       solo para renovar el access token.</li>
 * </ul>
 * El cuerpo de las respuestas no contiene ningún token, de modo que un XSS
 * no puede robarlos.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de inicio de sesión, renovación y cierre de sesión")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter rateLimiter;
    private final JwtService jwtService;

    /** Nombre de la cookie que transporta el access token. */
    private static final String ACCESS_COOKIE = "access_token";
    /** Nombre de la cookie que transporta el refresh token. */
    private static final String REFRESH_COOKIE = "refresh_token";

    /** Path de la cookie de acceso: se envía en cualquier petición bajo /api/v1. */
    private static final String ACCESS_COOKIE_PATH = "/api/v1";
    /** Path de la cookie de refresh: solo en peticiones a /api/v1/auth. */
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    /**
     * Si las cookies deben marcarse {@code Secure} (solo HTTPS).
     * Debe ser {@code true} en producción.
     */
    @Value("${app.cookie-secure:false}")
    private boolean cookieSecure;

    // ── Login ──────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica al usuario y establece access y refresh tokens en cookies HttpOnly."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = getClientIp(httpRequest);

        // Verificar rate limit antes de consultar la base de datos
        if (!rateLimiter.tryConsume(clientIp)) {
            log.warn("Login bloqueado por rate limit para IP '{}' (usuario: '{}')",
                    clientIp, request.username());
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Demasiados intentos. Intente nuevamente en un minuto."));
        }

        log.info("Solicitud de autenticación para usuario: '{}'", request.username());

        AuthResult result = authService.authenticate(request.username(), request.password());

        return ResponseEntity.ok()
                .header("Set-Cookie", buildAccessCookie(
                        result.accessToken(), result.authResponse().expiresIn()).toString())
                .header("Set-Cookie", buildRefreshCookie(
                        result.refreshToken(), result.authResponse().expiresIn()).toString())
                .body(ApiResponse.success(ApiMessages.AUTH_SUCCESS, result.authResponse()));
    }

    // ── Refresh ────────────────────────────────────────────

    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar access token",
            description = "Usa el refresh token de la cookie HttpOnly para emitir un nuevo par de tokens."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(ApiMessages.AUTH_UNAUTHORIZED));
        }

        AuthResult result = authService.refreshAccessToken(refreshToken);

        return ResponseEntity.ok()
                .header("Set-Cookie", buildAccessCookie(
                        result.accessToken(), result.authResponse().expiresIn()).toString())
                .header("Set-Cookie", buildRefreshCookie(
                        result.refreshToken(), result.authResponse().expiresIn()).toString())
                .body(ApiResponse.success(ApiMessages.AUTH_SUCCESS, result.authResponse()));
    }

    // ── Logout ─────────────────────────────────────────────

    @PostMapping("/logout")
    @Operation(
            summary = "Cerrar sesión",
            description = "Invalida el refresh token en el servidor y elimina ambas cookies."
    )
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        return ResponseEntity.ok()
                .header("Set-Cookie", expireCookie(ACCESS_COOKIE, ACCESS_COOKIE_PATH).toString())
                .header("Set-Cookie", expireCookie(REFRESH_COOKIE, REFRESH_COOKIE_PATH).toString())
                .body(ApiResponse.success("Sesión cerrada correctamente", null));
    }

    // ── Me (sesión actual) ─────────────────────────────────

    @GetMapping("/me")
    @Operation(
            summary = "Usuario autenticado",
            description = "Devuelve el usuario autenticado actual a partir de la cookie HttpOnly."
    )
    public ResponseEntity<ApiResponse<MeResponse>> me(
            @CookieValue(name = ACCESS_COOKIE, required = false) String accessToken,
            Authentication authentication
    ) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(a -> a.startsWith("ROLE_") ? a.substring("ROLE_".length()) : a)
                .orElse(null);
        long expiresIn = (accessToken != null) ? jwtService.getRemainingSeconds(accessToken) : 0;

        return ResponseEntity.ok(
                ApiResponse.success(ApiMessages.AUTH_ME_SUCCESS, new MeResponse(username, role, expiresIn))
        );
    }

    // ── Helpers ────────────────────────────────────────────

    /**
     * Extrae la IP del cliente considerando proxies (X-Forwarded-For).
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Construye la cookie HttpOnly del access token.
     */
    private ResponseCookie buildAccessCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(ACCESS_COOKIE, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(ACCESS_COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }

    /**
     * Construye la cookie HttpOnly del refresh token.
     * <p>
     * {@code SameSite=Lax} bloquea el envío en peticiones cross-site,
     * mitigando CSRF sin necesidad de tokens CSRF.
     * </p>
     */
    private ResponseCookie buildRefreshCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }

    /**
     * Construye una cookie expirada ({@code maxAge=0}) para eliminarla del navegador.
     */
    private ResponseCookie expireCookie(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(path)
                .maxAge(0)
                .build();
    }
}
