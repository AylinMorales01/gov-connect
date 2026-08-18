package com.govconnect.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Servicio para generación y validación de tokens JWT.
 * <p>
 * Usa HMAC-SHA256 (HS256). La clave secreta se obtiene de
 * {@code jwt.secret} en {@code application.yaml}, inyectada
 * desde la variable de entorno {@code JWT_SECRET}.
 * </p>
 * <p>
 * <b>Access Token:</b> corta duración (15 min), enviado en header Authorization.
 * Contiene {@code sub}, {@code role}, {@code iat}, {@code exp}.
 * </p>
 * <p>
 * <b>Refresh Token:</b> larga duración (7 días), enviado en cookie HttpOnly.
 * Contiene {@code sub}, {@code tokenVersion}, {@code type}, {@code iat}, {@code exp}.
 * Incluye {@code tokenVersion} para invalidación server-side en logout.
 * </p>
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationSeconds;
    private final long refreshExpirationSeconds;

    /**
     * Tamaño mínimo de clave para HMAC-SHA256 (HS256):
     * 256 bits = 32 bytes según RFC 7518.
     */
    private static final int MIN_KEY_BYTES = 32;

    /** Claim que identifica el tipo de token: "access" o "refresh". */
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_VERSION = "tokenVersion";

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationSeconds,
            @Value("${jwt.refresh-expiration}") long refreshExpirationSeconds
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET no está definido. Debe configurarse una clave de al menos "
                    + (MIN_KEY_BYTES * 8) + " bits (" + MIN_KEY_BYTES + " caracteres)."
            );
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET es demasiado corta: " + keyBytes.length
                    + " bytes (" + (keyBytes.length * 8) + " bits). "
                    + "HMAC-SHA256 requiere al menos " + MIN_KEY_BYTES
                    + " bytes (" + (MIN_KEY_BYTES * 8) + " bits)."
            );
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    // ── Access Token ──────────────────────────────────────

    /**
     * Genera un access token JWT firmado para el usuario autenticado.
     * <p>
     * Claims incluidos: {@code sub} (username), {@code role} (rol),
     * {@code type} ("access"), {@code iat} (emitido), {@code exp} (expiración).
     * </p>
     *
     * @param userDetails detalles del usuario.
     * @return access token JWT como String.
     */
    public String generateAccessToken(UserDetails userDetails) {
        long now = System.currentTimeMillis();

        // User.getRole() ya devuelve el rol sin prefijo "ROLE_" (ej. "ADMIN", "USER")
        String role = (userDetails instanceof com.govconnect.auth.entity.User u)
                ? u.getRole()
                : userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("USER");

        String token = Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationSeconds * 1000))
                .signWith(secretKey)
                .compact();

        log.debug("Access token generado para usuario '{}' con rol '{}'", userDetails.getUsername(), role);
        return token;
    }

    // ── Refresh Token ─────────────────────────────────────

    /**
     * Genera un refresh token JWT firmado.
     * <p>
     * Claims incluidos: {@code sub} (username), {@code tokenVersion},
     * {@code type} ("refresh"), {@code iat}, {@code exp}.
     * </p>
     * <p>
     * El claim {@code tokenVersion} permite invalidar todos los refresh tokens
     * de un usuario incrementando {@code User.tokenVersion} en el logout.
     * </p>
     *
     * @param userDetails  detalles del usuario.
     * @param tokenVersion versión actual del token del usuario.
     * @return refresh token JWT como String.
     */
    public String generateRefreshToken(UserDetails userDetails, int tokenVersion) {
        long now = System.currentTimeMillis();

        String token = Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshExpirationSeconds * 1000))
                .signWith(secretKey)
                .compact();

        log.debug("Refresh token generado para usuario '{}' (v{})", userDetails.getUsername(), tokenVersion);
        return token;
    }

    /**
     * Valida un refresh token: verifica firma, tipo, expiración,
     * pertenencia al usuario y versión del token.
     *
     * @param token              refresh token JWT.
     * @param userDetails        detalles del usuario.
     * @param expectedTokenVersion versión esperada (de la BD).
     * @return {@code true} si el token es válido y no ha sido revocado.
     */
    public boolean isRefreshTokenValid(String token, UserDetails userDetails, int expectedTokenVersion) {
        try {
            String type = extractClaim(token, claims -> claims.get(CLAIM_TYPE, String.class));
            if (!TOKEN_TYPE_REFRESH.equals(type)) {
                log.debug("Token de tipo incorrecto: '{}' (esperado 'refresh')", type);
                return false;
            }
            int tokenVersion = extractTokenVersion(token);
            boolean versionMatch = tokenVersion == expectedTokenVersion;
            boolean usernameMatch = extractUsername(token).equals(userDetails.getUsername());
            boolean notExpired = !isTokenExpired(token);

            if (!versionMatch) {
                log.debug("Refresh token revocado: tokenVersion={}, esperado={}", tokenVersion, expectedTokenVersion);
            }

            return usernameMatch && notExpired && versionMatch;
        } catch (Exception e) {
            log.debug("Error al validar refresh token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrae la versión del token del claim {@code tokenVersion}.
     */
    public int extractTokenVersion(String token) {
        Integer version = extractClaim(token, claims -> claims.get(CLAIM_TOKEN_VERSION, Integer.class));
        return version != null ? version : 0;
    }

    // ── Métodos comunes ──────────────────────────────────

    /**
     * Extrae el username (subject) del token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae el rol del claim {@code role} del token.
     *
     * @return el rol (ej. "ADMIN", "USER") o {@code null} si no existe.
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_ROLE, String.class));
    }

    /**
     * Valida que el access token pertenezca al usuario y no esté expirado.
     *
     * @param token       JWT a validar.
     * @param userDetails detalles del usuario a comparar.
     * @return {@code true} si el token es válido para ese usuario.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            boolean valid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
            if (!valid) {
                log.debug("Token inválido para usuario '{}'", userDetails.getUsername());
            }
            return valid;
        } catch (Exception e) {
            log.debug("Error al validar token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Devuelve los segundos de expiración del access token.
     */
    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    /**
     * Devuelve los segundos de expiración del refresh token.
     */
    public long getRefreshExpirationSeconds() {
        return refreshExpirationSeconds;
    }

    // ── Métodos privados ──────────────────────────────

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }
}
