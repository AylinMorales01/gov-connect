package com.govconnect.auth.service;

import com.govconnect.auth.dto.AuthResponse;
import com.govconnect.auth.security.JwtService;
import com.govconnect.auth.repository.UserRepository;
import com.govconnect.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de autenticación.
 * <p>
 * Verifica credenciales contra la base de datos, genera access + refresh tokens,
 * y maneja la renovación y cierre de sesión con invalidación server-side
 * vía {@code tokenVersion}.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Resultado interno de la autenticación: access token para el header
     * y refresh token para la cookie HttpOnly.
     */
    public record AuthResult(AuthResponse authResponse, String refreshToken) {}

    // ── Login ──────────────────────────────────────────────

    /**
     * Autentica a un usuario con sus credenciales.
     *
     * @param username nombre de usuario.
     * @param password contraseña en texto plano.
     * @return {@link AuthResult} con el access token (respuesta JSON)
     *         y el refresh token (para cookie HttpOnly).
     * @throws BadCredentialsException si las credenciales son inválidas.
     */
    @Transactional(readOnly = true)
    public AuthResult authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Intento de login con usuario inexistente: '{}'", username);
                    return new BadCredentialsException("Credenciales inválidas");
                });

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Contraseña incorrecta para usuario: '{}'", username);
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, user.getTokenVersion());

        log.info("Usuario autenticado exitosamente: '{}' con rol '{}'", username, user.getRole());

        AuthResponse authResponse = new AuthResponse(
                accessToken,
                jwtService.getExpirationSeconds(),
                user.getRole()
        );

        return new AuthResult(authResponse, refreshToken);
    }

    // ── Refresh ────────────────────────────────────────────

    /**
     * Renueva un access token a partir de un refresh token válido.
     * <p>
     * <b>Rotación de refresh token:</b> cada uso exitoso del refresh token
     * genera un nuevo par (access + refresh), invalidando el anterior
     * (el tokenVersion no cambia — la rotación es solo por reemplazo).
     * </p>
     *
     * @param refreshToken refresh token JWT desde la cookie.
     * @return {@link AuthResult} con nuevos tokens.
     * @throws BadCredentialsException si el refresh token es inválido, expirado
     *         o el usuario fue desactivado/cambió su tokenVersion.
     */
    @Transactional(readOnly = true)
    public AuthResult refreshAccessToken(String refreshToken) {
        String username;
        try {
            username = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            log.debug("No se pudo extraer username del refresh token");
            throw new BadCredentialsException("Refresh token inválido o expirado");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.debug("Usuario del refresh token no encontrado: '{}'", username);
                    return new BadCredentialsException("Usuario no encontrado");
                });

        if (!user.getActive()) {
            log.debug("Intento de refresh con usuario inactivo: '{}'", username);
            throw new BadCredentialsException("Usuario inactivo");
        }

        if (!jwtService.isRefreshTokenValid(refreshToken, user, user.getTokenVersion())) {
            throw new BadCredentialsException("Refresh token inválido, expirado o revocado");
        }

        // Rotación: emitir nuevos tokens
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user, user.getTokenVersion());

        log.debug("Tokens renovados para usuario '{}'", username);

        AuthResponse authResponse = new AuthResponse(
                newAccessToken,
                jwtService.getExpirationSeconds(),
                user.getRole()
        );

        return new AuthResult(authResponse, newRefreshToken);
    }

    // ── Logout ─────────────────────────────────────────────

    /**
     * Invalida todos los refresh tokens del usuario incrementando
     * {@code tokenVersion} en la base de datos.
     * <p>
     * Esto hace que cualquier refresh token emitido anteriormente
     * sea rechazado en {@link #refreshAccessToken(String)}.
     * </p>
     *
     * @param refreshToken refresh token JWT (para identificar al usuario).
     */
    @Transactional
    public void logout(String refreshToken) {
        String username;
        try {
            username = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            log.debug("Logout con refresh token no parseable — ignorado");
            return;
        }

        userRepository.findByUsername(username).ifPresent(user -> {
            user.setTokenVersion(user.getTokenVersion() + 1);
            userRepository.save(user);
            log.info("Sesión cerrada para usuario '{}' (tokenVersion → {})",
                    username, user.getTokenVersion());
        });
    }
}
