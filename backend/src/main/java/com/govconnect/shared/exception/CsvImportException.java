package com.govconnect.shared.exception;

/**
 * Excepción de negocio para errores de ingesta de archivos CSV
 * (archivo vacío, columnas requeridas faltantes o CSV malformado).
 * <p>
 * Se traduce a HTTP 400 en {@link GlobalExceptionHandler}.
 * </p>
 */
public class CsvImportException extends RuntimeException {

    public CsvImportException(String message) {
        super(message);
    }

    public CsvImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
