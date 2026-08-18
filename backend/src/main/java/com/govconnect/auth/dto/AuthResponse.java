package com.govconnect.auth.dto;

/**
 * DTO para la respuesta de autenticación exitosa.
 * Contiene el JWT, metadatos y el rol del usuario.
 * <p>
 * Los componentes del record se serializan en orden de declaración
 * (nativo en Java 21), sin necesidad de {@code @JsonPropertyOrder}.
 * </p>
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        String role
) {

    /** Tipo de token según RFC 6750 (Bearer). */
    private static final String TOKEN_TYPE_BEARER = "Bearer";

    /**
     * Constructor de conveniencia: asigna {@link #TOKEN_TYPE_BEARER} automáticamente.
     */
    public AuthResponse(String token, long expiresIn, String role) {
        this(token, TOKEN_TYPE_BEARER, expiresIn, role);
    }
}
