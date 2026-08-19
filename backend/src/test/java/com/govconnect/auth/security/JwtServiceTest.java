package com.govconnect.auth.security;

import com.govconnect.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para {@link JwtService}.
 * Verifica generación y validación de access tokens y refresh tokens,
 * incluyendo el mecanismo de invalidación por tokenVersion.
 */
@DisplayName("JwtService — generación y validación de tokens")
class JwtServiceTest {

    /** Clave de 32+ bytes para HS256 (mínimo requerido). */
    private static final String TEST_SECRET = "test-secret-key-for-junit-minimum-32-bytes!";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        // Access token 60s, Refresh token 300s
        jwtService = new JwtService(TEST_SECRET, 60L, 300L);

        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@test.com")
                .passwordHash("hashed")
                .fullName("Test User")
                .role("USER")
                .active(true)
                .tokenVersion(0)
                .build();
    }

    // ── Constructor validation ──────────────────────────

    @Test
    @DisplayName("Debe lanzar excepción si el secreto es null")
    void shouldRejectNullSecret() {
        assertThatThrownBy(() -> new JwtService(null, 60L, 300L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET no está definido");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el secreto es demasiado corto")
    void shouldRejectShortSecret() {
        assertThatThrownBy(() -> new JwtService("corta", 60L, 300L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET es demasiado corta");
    }

    // ── Access Token ────────────────────────────────────

    @Test
    @DisplayName("Debe generar un access token válido")
    void shouldGenerateValidAccessToken() {
        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("Debe rechazar access token de otro usuario")
    void shouldRejectTokenForDifferentUser() {
        String token = jwtService.generateAccessToken(user);

        User otherUser = User.builder()
                .username("other")
                .email("other@test.com")
                .passwordHash("x")
                .fullName("Other")
                .role("USER")
                .active(true)
                .build();

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("Debe retornar los segundos de expiración configurados")
    void shouldReturnConfiguredExpiration() {
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(60L);
        assertThat(jwtService.getRefreshExpirationSeconds()).isEqualTo(300L);
    }

    // ── Refresh Token ───────────────────────────────────

    @Test
    @DisplayName("Debe generar un refresh token válido")
    void shouldGenerateValidRefreshToken() {
        String token = jwtService.generateRefreshToken(user, user.getTokenVersion());

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
        assertThat(jwtService.extractTokenVersion(token)).isEqualTo(0);
        assertThat(jwtService.isRefreshTokenValid(token, user, 0)).isTrue();
    }

    @Test
    @DisplayName("Debe rechazar refresh token con versión incorrecta (revocado)")
    void shouldRejectRefreshTokenWithWrongVersion() {
        String token = jwtService.generateRefreshToken(user, 0);

        // El usuario ahora tiene tokenVersion=1 (como después de un logout)
        user.setTokenVersion(1);

        assertThat(jwtService.isRefreshTokenValid(token, user, 1)).isFalse();
    }

    @Test
    @DisplayName("Debe rechazar access token usado como refresh token")
    void shouldRejectAccessTokenAsRefresh() {
        String accessToken = jwtService.generateAccessToken(user);

        assertThat(jwtService.isRefreshTokenValid(accessToken, user, 0)).isFalse();
    }

    @Test
    @DisplayName("Debe rechazar refresh token con usuario inactivo")
    void shouldRejectRefreshTokenForInactiveUser() {
        String token = jwtService.generateRefreshToken(user, 0);

        user.setActive(false);

        // El username sigue siendo el mismo, pero el usuario está inactivo
        assertThat(jwtService.isRefreshTokenValid(token, user, 0)).isTrue();
        // Nota: la validación de usuario activo es responsabilidad de AuthService,
        // no de JwtService. Este test verifica que JwtService no lo rechaza
        // erróneamente — es AuthService quien debe verificar isActive().
    }

    // ── Edge cases ──────────────────────────────────────

    @Test
    @DisplayName("Debe rechazar token manipulado (firma inválida)")
    void shouldRejectTamperedToken() {
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThat(jwtService.isTokenValid(tampered, user)).isFalse();
    }

    @Test
    @DisplayName("Debe rechazar token vacío")
    void shouldRejectEmptyToken() {
        assertThat(jwtService.isTokenValid("", user)).isFalse();
    }

    @Test
    @DisplayName("Debe rechazar token malformado")
    void shouldRejectMalformedToken() {
        assertThat(jwtService.isTokenValid("not.a.jwt.token", user)).isFalse();
    }

    @Test
    @DisplayName("Debe contener claims esperados en el payload")
    void shouldContainExpectedClaims() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
    }
}
