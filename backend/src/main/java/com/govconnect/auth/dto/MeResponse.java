package com.govconnect.auth.dto;

/**
 * DTO para el endpoint {@code GET /api/v1/auth/me}.
 * <p>
 * Permite al frontend restaurar el estado de sesión tras una recarga de
 * página sin necesidad de leer la cookie HttpOnly (inaccesible desde JS).
 * </p>
 *
 * @param username  nombre del usuario autenticado.
 * @param role      rol del usuario (ej. {@code ADMIN}, {@code USER}).
 * @param expiresIn segundos restantes hasta la expiración del access token.
 */
public record MeResponse(
        String username,
        String role,
        long expiresIn
) {}
