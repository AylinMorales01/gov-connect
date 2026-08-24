package com.govconnect.shared.csv;

import com.govconnect.shared.exception.CsvImportException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parser de archivos CSV para la ingesta de datos operacionales.
 * <p>
 * Usa Apache Commons CSV para soportar campos entre comillas (los objetos de
 * contrato suelen contener comas). Devuelve cada fila como un mapa
 * {@code columna -> valor} con los nombres de cabecera normalizados (trim).
 * </p>
 * <p>
 * La lectura es en streaming sobre el {@code InputStream}: un export de SECOP II
 * puede pesar decenas de MB y materializarlo antes como {@code String} duplicaba
 * el archivo en memoria.
 * </p>
 * <p>
 * Errores estructurales (archivo vacío, cabecera ausente, columnas requeridas
 * faltantes o CSV malformado) lanzan {@link CsvImportException}.
 * </p>
 */
@Component
public class CsvParser {

    /**
     * Parsea un archivo CSV y valida que contenga las columnas requeridas.
     *
     * @param file            archivo multipart con el CSV (nunca {@code null}).
     * @param requiredHeaders nombres exactos de columnas que deben existir.
     * @return lista de filas como mapas {@code columna -> valor} (puede estar vacía).
     * @throws CsvImportException si el archivo está vacío o el CSV es inválido.
     */
    public List<Map<String, String>> parse(MultipartFile file, Set<String> requiredHeaders) {
        return parse(file, requiredHeaders, Map.of());
    }

    /**
     * Parsea un CSV mapeando cabeceras alternativas a sus nombres canónicos.
     * <p>
     * {@code headerAliases} es un mapa {@code alias normalizado -> nombre canónico};
     * se usa para aceptar export con nombres de columna distintos a los internos
     * (p. ej. SECOP II). Las cabeceras que no estén en el mapa se conservan tal
     * cual, y la validación de columnas requeridas se hace contra los nombres
     * canónicos resultantes.
     * </p>
     */
    public List<Map<String, String>> parse(MultipartFile file, Set<String> requiredHeaders,
                                           Map<String, String> headerAliases) {
        if (file == null || file.isEmpty()) {
            throw new CsvImportException("El archivo CSV está vacío");
        }
        try (InputStream input = file.getInputStream()) {
            return parse(input, requiredHeaders, headerAliases);
        } catch (IOException e) {
            throw new CsvImportException("No se pudo leer el archivo CSV", e);
        }
    }

    /**
     * Parsea un CSV ya volcado a disco. Es la vía que usa la ingesta asíncrona,
     * que copia el multipart a un temporal antes de responder la petición.
     */
    public List<Map<String, String>> parse(Path path, Set<String> requiredHeaders) {
        return parse(path, requiredHeaders, Map.of());
    }

    /**
     * Parsea un CSV en disco mapeando cabeceras alternativas a nombres canónicos.
     */
    public List<Map<String, String>> parse(Path path, Set<String> requiredHeaders,
                                           Map<String, String> headerAliases) {
        try (InputStream input = Files.newInputStream(path)) {
            return parse(input, requiredHeaders, headerAliases);
        } catch (IOException e) {
            throw new CsvImportException("No se pudo leer el archivo CSV", e);
        }
    }

    /**
     * Núcleo del parseo: lee el CSV del stream, resuelve los alias de cabecera y
     * valida las columnas requeridas. No cierra el stream recibido.
     */
    public List<Map<String, String>> parse(InputStream input, Set<String> requiredHeaders,
                                           Map<String, String> headerAliases) {
        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build()
                .parse(bomAwareReader(input))) {

            Map<String, Integer> headerMap = parser.getHeaderMap();
            if (headerMap == null || headerMap.isEmpty()) {
                throw new CsvImportException("El CSV no contiene una fila de cabecera");
            }

            Set<String> headers = headerMap.keySet().stream()
                    .map(h -> canonicalHeader(h, headerAliases))
                    .collect(Collectors.toSet());

            Set<String> missing = requiredHeaders.stream()
                    .filter(h -> !headers.contains(h))
                    .collect(Collectors.toSet());
            if (!missing.isEmpty()) {
                throw new CsvImportException("Columnas requeridas faltantes: " + String.join(", ", missing));
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, String> row = new LinkedHashMap<>();
                for (String header : headerMap.keySet()) {
                    // Una fila con menos columnas que la cabecera no debe abortar
                    // la importación completa: se trata como valor ausente.
                    String canonical = canonicalHeader(header, headerAliases);
                    String value = record.isSet(header) ? record.get(header) : "";

                    // Dos columnas del export pueden resolverse al mismo nombre
                    // canónico (p. ej. "Fecha de Fin del Contrato" y "Fecha Fin
                    // Liquidacion"). Como las columnas se recorren en orden de
                    // cabecera, una columna vacía posterior no debe pisar un valor
                    // ya presente: gana el primer valor no vacío.
                    String existing = row.get(canonical);
                    if (existing == null || (existing.isBlank() && !value.isBlank())) {
                        row.put(canonical, value);
                    }
                }
                rows.add(row);
            }
            return rows;

        } catch (IOException | IllegalArgumentException e) {
            throw new CsvImportException("Error al procesar el CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Envuelve el stream en un lector UTF-8 saltando el BOM si está presente.
     * Se comprueba el primer carácter y se rebobina cuando no lo es, para no
     * tener que materializar el archivo entero como {@code String}.
     */
    private static Reader bomAwareReader(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        reader.mark(1);
        if (reader.read() != 0xFEFF) {
            reader.reset();
        }
        return reader;
    }

    /**
     * Resuelve el nombre canónico de una cabecera: si coincide con un alias
     * normalizado devuelve el nombre canónico; si no, devuelve la cabecera
     * normalizada original. La normalización es la misma que aplica
     * {@code SecopColumnMapper} (sin tildes, minúsculas, guiones bajos a espacio)
     * para que el mapeo de alias sea independiente del caso/acentos.
     */
    private String canonicalHeader(String header, Map<String, String> headerAliases) {
        String normalized = normalizeForAlias(header);
        return headerAliases.getOrDefault(normalized, normalized);
    }

    /**
     * Normaliza una cabecera para el matching de alias: quita tildes, pasa a
     * minúsculas, recorta espacios y colapsa guiones bajos a espacio.
     */
    private static String normalizeForAlias(String header) {
        if (header == null) {
            return "";
        }
        return java.text.Normalizer.normalize(header, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('_', ' ')
                .trim()
                .toLowerCase(java.util.Locale.ROOT);
    }
}
