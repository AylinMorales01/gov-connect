package com.govconnect.auth.controller;

import com.govconnect.auth.dto.AuthResponse;
import com.govconnect.auth.dto.LoginRequest;
import com.govconnect.auth.security.LoginRateLimiter;
import com.govconnect.auth.service.AuthService;
import com.govconnect.auth.service.AuthService.AuthResult;
import com.govconnect.shared.constants.ApiMessages;
import com.govconnect.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de autenticación.
 * <p>
 * Expone los endpoints de login, renovación (refresh) y cierre de sesión.
 * El access token se entrega en el cuerpo JSON; el refresh token viaja
 * exclusivamente en una cookie HttpOnly para mitigar XSS.
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

    /** Nombre de la cookie que contiene el refresh token. */
    private static final String REFRESH_COOKIE = "refresh_token";

    /** Path de la cookie: solo se envía en peticiones a /api/v1/auth. */
    private static final String COOKIE_PATH = "/api/v1/auth";

    // ── Login ──────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica al usuario con username y contraseña. Devuelve un access token JWT en el cuerpo y establece un refresh token en cookie HttpOnly."
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
                .header("Set-Cookie", buildRefreshCookie(result.refreshToken(), result.authResponse().expiresIn()).toString())
                .body(ApiResponse.success(ApiMessages.AUTH_SUCCESS, result.authResponse()));
    }

    // ── Refresh ────────────────────────────────────────────

    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar access token",
            description = "Usa el refresh token de la cookie HttpOnly para emitir un nuevo access token (y rota el refresh token)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tokens renovados exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado o revocado")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(ApiMessages.AUTH_UNAUTHORIZED));
        }

        AuthResult result = authService.refreshAccessToken(refreshToken);

        return ResponseEntity.ok()
                .header("Set-Cookie", buildRefreshCookie(result.refreshToken(), result.authResponse().expiresIn()).toString())
                .body(ApiResponse.success(ApiMessages.AUTH_SUCCESS, result.authResponse()));
    }

    // ── Logout ─────────────────────────────────────────────

    @PostMapping("/logout")
    @Operation(
            summary = "Cerrar sesión",
            description = "Invalida el refresh token incrementando la versión de token del usuario y elimina la cookie."
    )
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        // Cookie con maxAge=0 para que el navegador la elimine
        ResponseCookie expiredCookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", expiredCookie.toString())
                .body(ApiResponse.success("Sesión cerrada correctamente", null));
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
     * Construye la cookie HttpOnly para el refresh token.
     * <p>
     * {@code SameSite=Lax} permite el envío en navegaciones top-level (GET)
     * y en POST desde el mismo sitio, bloqueando CSRF cross-site.
     * En producción debe usarse {@code Secure=true} con HTTPS.
     * </p>
     */
    private ResponseCookie buildRefreshCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(false)      // true en producción con HTTPS
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }
}
