package com.govconnect.ingestion.service;

import com.govconnect.shared.exception.CsvImportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Copia los CSV subidos a un archivo temporal propio.
 * <p>
 * El almacenamiento temporal de un {@code MultipartFile} se libera al terminar
 * la petición HTTP, así que una importación asíncrona no puede leerlo desde su
 * hilo. La copia se hace con {@code transferTo} (streaming) y no con
 * {@code getBytes()}, que dejaría el archivo entero en heap durante toda la
 * importación.
 * </p>
 * <p>
 * Se usa el directorio temporal del sistema para no depender del working
 * directory del proceso, a diferencia de {@code exports/} o del fichero DuckDB.
 * </p>
 */
@Component
@Slf4j
public class IngestionFileStore {

    /**
     * Vuelca el archivo subido a un temporal y devuelve su ruta.
     *
     * @throws CsvImportException si el archivo está vacío o no se puede copiar.
     */
    public Path stage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CsvImportException("El archivo CSV está vacío");
        }
        try {
            Path target = Files.createTempFile("govconnect-ingestion-", ".csv");
            file.transferTo(target);
            return target;
        } catch (IOException e) {
            throw new CsvImportException("No se pudo preparar el archivo CSV para su importación", e);
        }
    }

    /**
     * Borra el temporal sin propagar errores: un fallo al limpiar no debe
     * enmascarar el resultado de la importación.
     */
    public void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("No se pudo borrar el archivo temporal de ingesta {}: {}", path, e.getMessage());
        }
    }
}
