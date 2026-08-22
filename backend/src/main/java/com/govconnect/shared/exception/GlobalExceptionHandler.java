package com.govconnect.shared.exception;

import com.govconnect.shared.constants.ApiMessages;
import com.govconnect.shared.response.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para toda la API REST.
 * <p>
 * Traduce las excepciones del sistema en respuestas JSON estructuradas
 * usando el contrato {@link ApiResponse}, sin exponer detalles internos
 * al cliente.
 * </p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 401 Unauthorized ──────────────────────────────────

    /**
     * Credenciales inválidas (usuario no existe o contraseña incorrecta).
     * <p>
     * Si el response ya fue commiteado por el {@code SecurityConfig}
     * (ruta de filtro), Spring MVC ignora la escritura y solo registra
     * un warning en DEBUG.
     * </p>
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex, HttpServletResponse response) {
        log.warn("Intento de autenticación fallido: {}", ex.getMessage());
        if (response.isCommitted()) {
            log.debug("Response ya commiteado — omitiendo escritura de BadCredentialsException");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ApiMessages.AUTH_BAD_CREDENTIALS));
    }

    /**
     * Fallback para cualquier otra excepción de autenticación.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException ex, HttpServletResponse response) {
        log.warn("Error de autenticación [{}]: {}",
                ex.getClass().getSimpleName(), ex.getMessage());
        if (response.isCommitted()) {
            log.debug("Response ya commiteado — omitiendo escritura de AuthenticationException");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ApiMessages.AUTH_UNAUTHORIZED));
    }

    // ── 403 Forbidden ─────────────────────────────────────

    /**
     * Acceso denegado por falta de permisos (rol insuficiente).
     * <p>
     * Si el response ya fue commiteado por el {@code SecurityConfig}
     * (ruta de filtro), Spring MVC ignora la escritura.
     * </p>
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, HttpServletResponse response) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        if (response.isCommitted()) {
            log.debug("Response ya commiteado — omitiendo escritura de AccessDeniedException");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ApiMessages.AUTH_FORBIDDEN));
    }

    // ── 404 Not Found ──────────────────────────────────────

    /**
     * Captura {@code getSingleResult()} sin filas en consultas nativas.
     */
    @ExceptionHandler(NoResultException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResult(NoResultException ex) {
        log.warn("Consulta sin resultados: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ApiMessages.ERROR_NOT_FOUND));
    }

    /**
     * Captura referencias a entidades JPA inexistentes.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entidad no encontrada: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ApiMessages.ERROR_ENTITY_NOT_FOUND));
    }

    // ── 400 Bad Request ────────────────────────────────────

    /**
     * Captura argumentos inválidos en la capa de negocio.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ApiMessages.ERROR_BAD_REQUEST));
    }

    /**
     * Captura errores de ingesta de CSV (archivo vacío, columnas faltantes o CSV malformado).
     */
    @ExceptionHandler(CsvImportException.class)
    public ResponseEntity<ApiResponse<Void>> handleCsvImport(CsvImportException ex) {
        log.warn("Error de ingesta CSV: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Captura errores de validación de Beans ({@code @Valid} / {@code @Validated}).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String detalles = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Error de validación en [{}]: {}",
                ex.getParameter().getParameterType().getSimpleName(), detalles);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ApiMessages.ERROR_VALIDATION
                        + " (" + detalles + ")"));
    }

    /**
     * Captura cuerpos HTTP mal formados (JSON inválido, tipo incorrecto, etc.).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedBody(HttpMessageNotReadableException ex) {
        log.warn("Cuerpo HTTP no legible: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ApiMessages.ERROR_BAD_REQUEST
                        + ": el cuerpo de la solicitud no es válido"));
    }

    // ── 500 Internal Server Error ──────────────────────────

    /**
     * Captura errores de base de datos vía JDBC (módulo analytics).
     * <p>
     * <b>Importante:</b> solo se expone un mensaje genérico al cliente;
     * los detalles reales del error se registran en el log del servidor.
     * </p>
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ApiResponse<Void>> handleSqlException(SQLException ex) {
        log.error("Error SQL [{}] — {}", ex.getSQLState(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ApiMessages.ERROR_DATABASE));
    }

    /**
     * Captura errores de acceso a datos de Spring (módulo dashboard / JPA).
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException ex) {
        log.error("Error de acceso a datos: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ApiMessages.ERROR_DATABASE));
    }

    // ── Fallback genérico ──────────────────────────────────

    /**
     * Último nivel de defensa para cualquier excepción no controlada.
     * Garantiza que el cliente siempre reciba JSON estructurado.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Error inesperado [{}]: {}",
                ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ApiMessages.ERROR_INTERNAL));
    }
}
