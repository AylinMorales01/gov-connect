package com.govconnect.auth.service;

import com.govconnect.auth.entity.User;
import com.govconnect.auth.repository.UserRepository;
import com.govconnect.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link AuthService}.
 * Verifica login, refresh y logout con mocking de dependencias.
 */
@DisplayName("AuthService — autenticación, refresh y logout")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User user;

    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "secret123";
    private static final String ACCESS_TOKEN = "access.token.here";
    private static final String REFRESH_TOKEN = "refresh.token.here";
    private static final String NEW_ACCESS_TOKEN = "new.access.token";
    private static final String NEW_REFRESH_TOKEN = "new.refresh.token";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username(USERNAME)
                .email("test@test.com")
                .passwordHash("$2a$10$hashedpassword")
                .fullName("Test User")
                .role("USER")
                .active(true)
                .tokenVersion(0)
                .build();
    }

    // ── Login ───────────────────────────────────────────

    @Test
    @DisplayName("Login exitoso: debe retornar access + refresh tokens")
    void shouldAuthenticateSuccessfully() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(user, 0)).thenReturn(REFRESH_TOKEN);
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthService.AuthResult result = authService.authenticate(USERNAME, PASSWORD);

        assertThat(result).isNotNull();
        assertThat(result.authResponse().token()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.authResponse().role()).isEqualTo("USER");
        assertThat(result.authResponse().expiresIn()).isEqualTo(3600L);
        assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("Login fallido: usuario inexistente lanza BadCredentialsException")
    void shouldFailWhenUserNotFound() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate(USERNAME, PASSWORD))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Credenciales inválidas");
    }

    @Test
    @DisplayName("Login fallido: contraseña incorrecta lanza BadCredentialsException")
    void shouldFailWhenPasswordWrong() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(USERNAME, PASSWORD))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Credenciales inválidas");
    }

    // ── Refresh ─────────────────────────────────────────

    @Test
    @DisplayName("Refresh exitoso: debe rotar ambos tokens")
    void shouldRefreshTokensSuccessfully() {
        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(USERNAME);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(jwtService.isRefreshTokenValid(REFRESH_TOKEN, user, 0)).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn(NEW_ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(user, 0)).thenReturn(NEW_REFRESH_TOKEN);
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthService.AuthResult result = authService.refreshAccessToken(REFRESH_TOKEN);

        assertThat(result).isNotNull();
        assertThat(result.authResponse().token()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("Refresh fallido: refresh token inválido lanza BadCredentialsException")
    void shouldFailRefreshWithInvalidToken() {
        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(USERNAME);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(jwtService.isRefreshTokenValid(REFRESH_TOKEN, user, 0)).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshAccessToken(REFRESH_TOKEN))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token inválido");
    }

    @Test
    @DisplayName("Refresh fallido: usuario inactivo lanza BadCredentialsException")
    void shouldFailRefreshForInactiveUser() {
        user.setActive(false);

        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(USERNAME);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refreshAccessToken(REFRESH_TOKEN))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Usuario inactivo");
    }

    @Test
    @DisplayName("Refresh fallido: tokenVersion no coincide (token revocado por logout)")
    void shouldFailRefreshWhenTokenVersionChanged() {
        // Después de un logout, tokenVersion es 1 pero el refresh token tiene v0
        user.setTokenVersion(1);

        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(USERNAME);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(jwtService.isRefreshTokenValid(REFRESH_TOKEN, user, 1)).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshAccessToken(REFRESH_TOKEN))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Refresh fallido: usuario no encontrado")
    void shouldFailRefreshWhenUserNotFound() {
        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(USERNAME);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshAccessToken(REFRESH_TOKEN))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // ── Logout ──────────────────────────────────────────

    @Test
    @DisplayName("Logout exitoso: debe incrementar tokenVersion")
    void shouldIncrementTokenVersionOnLogout() {
        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(USERNAME);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        authService.logout(REFRESH_TOKEN);

        assertThat(user.getTokenVersion()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Logout con token no parseable: no debe lanzar excepción")
    void shouldNotFailOnMalformedTokenDuringLogout() {
        when(jwtService.extractUsername(anyString())).thenThrow(new RuntimeException("malformed"));

        // No debe lanzar excepción
        authService.logout("malformed.token.here");
    }
}
