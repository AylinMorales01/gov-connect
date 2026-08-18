package com.govconnect.shared.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Envoltorio uniforme para todas las respuestas de la API REST.
 * <p>
 * El frontend desenvuelve {@code response.data.data} para acceder
 * a la carga útil con tipado fuerte.
 * </p>
 *
 * @param <T> tipo de la carga útil (data).
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        LocalDateTime timestamp,
        T data
) {

    private static final Logger log = LoggerFactory.getLogger(ApiResponse.class);

    /**
     * Crea una respuesta exitosa con mensaje y datos.
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                true,
                message,
                LocalDateTime.now(),
                data
        );
    }

    /**
     * Crea una respuesta de error con mensaje descriptivo.
     * <p>
     * Registra el mensaje en el log del servidor para trazabilidad,
     * ya que los {@code GlobalExceptionHandler} pueden no cubrir
     * todos los usos directos de este método.
     * </p>
     */
    public static <T> ApiResponse<T> error(String message) {
        log.warn("ApiResponse.error: {}", message);
        return new ApiResponse<>(
                false,
                message,
                LocalDateTime.now(),
                null
        );
    }
}