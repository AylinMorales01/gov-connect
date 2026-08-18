package com.govconnect.auth.dto;

/**
 * DTO para la respuesta de autenticación exitosa (login y refresh).
 * <p>
 * El access token ya <b>no</b> viaja en el cuerpo: se entrega en una
 * cookie HttpOnly. El cuerpo solo expone el rol y la expiración en
 * segundos (información no sensible) para la UX del frontend
 * (badge de rol y advertencia de expiración de sesión).
 * </p>
 */
public record AuthResponse(
        String role,
        long expiresIn
) {}
