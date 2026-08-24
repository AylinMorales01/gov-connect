package com.govconnect.ingestion.service;

import com.govconnect.ingestion.dto.ImportSummaryResponse;
import com.govconnect.ingestion.repository.IngestionRepository;
import com.govconnect.shared.csv.CsvParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio de ingesta de datos operacionales desde archivos CSV.
 * <p>
 * Cada método parsea el CSV, normaliza los valores (estado, fechas, montos,
 * dependencia), escribe en SQL Server mediante {@link IngestionRepository} y
 * acumula los errores por fila sin abortar la importación.
 * </p>
 * <p>
 * Recibe el CSV como {@link Path} y no como {@code MultipartFile}: la ingesta
 * corre en un hilo aparte y el temporal del multipart se libera al terminar la
 * petición HTTP (ver {@code IngestionFileStore}).
 * </p>
 * <p>
 * El disparo del ETL (para refrescar DuckDB) queda a cargo del servicio
 * asíncrono, fuera de la transacción, para evitar la condición de carrera entre
 * el commit y el hilo del ETL.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final CsvParser csvParser;
    private final IngestionRepository repository;
    private final SecopColumnMapper secopColumnMapper;

    private static final Set<String> BUDGET_HEADERS = Set.of(
            "dependencia", "anio", "asignado", "ejecutado");

    private static final Set<String> COLLECTION_HEADERS = Set.of(
            "fecha", "concepto", "contribuyente", "monto", "medio_pago", "dependencia");

    /** Código del departamento de respaldo para entidades no reconocidas. */
    private static final String FALLBACK_DEPARTMENT_CODE = "SIN";

    /**
     * Tope de errores acumulados en la respuesta. Un export de SECOP II puede
     * dejar miles de filas omitidas y serializarlas todas hincha la respuesta
     * sin aportar nada: el total real lo da {@code skipped}.
     */
    private static final int MAX_ERRORS = 500;

    // Límites de las columnas de `contracts` en SQL Server.
    private static final int MAX_CONTRACT_NUMBER_LENGTH = 50;
    private static final int MAX_CONTRACTOR_LENGTH = 150;
    private static final int MAX_OBJECT_LENGTH = 500;

    // ── Contratos ─────────────────────────────────────────

    @Transactional
    public ImportSummaryResponse importContracts(Path file) {
        List<Map<String, String>> rows = csvParser.parse(
                file, secopColumnMapper.requiredHeaders(), secopColumnMapper.aliases());
        Map<String, Long> departments = loadDepartments();
        Long fallbackDepartment = resolveFallbackDepartment(departments);
        // Una sola consulta en vez de un SELECT COUNT(*) por fila. El set se
        // actualiza sobre la marcha, así que un número repetido dentro del
        // mismo archivo se resuelve como actualización.
        Set<String> existingContracts = repository.findAllContractNumbers();

        int imported = 0, updated = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        int rowNumber = 1;
        for (Map<String, String> row : rows) {
            rowNumber++;
            try {
                String contractNumber = requireWithinLength(
                        row, "numero_contrato", MAX_CONTRACT_NUMBER_LENGTH);
                String contractor = truncate(require(row, "contratista"), MAX_CONTRACTOR_LENGTH);
                String object = truncate(require(row, "objeto"), MAX_OBJECT_LENGTH);
                BigDecimal value = parseAmount(require(row, "valor"));
                // El export puede traer la fecha de inicio vacía en contratos
                // que aún no arrancan; la fecha de firma sirve de respaldo.
                LocalDate start = parseDate(requireAny(row, "fecha_inicio", "fecha_firma"));
                LocalDate end = parseDate(require(row, "fecha_fin"));

                String status = normalizeStatus(row.get("estado"), errors, rowNumber);
                if (status == null) {
                    skipped++;
                    continue;
                }
                Long departmentId = resolveDepartment(
                        departments, fallbackDepartment, row.get("dependencia"), errors, rowNumber);
                if (departmentId == null) {
                    skipped++;
                    continue;
                }

                if (existingContracts.contains(contractNumber)) {
                    repository.updateContract(contractNumber, contractor, object, value, start, end, status, departmentId);
                    updated++;
                } else {
                    repository.insertContract(contractNumber, contractor, object, value, start, end, status, departmentId);
                    existingContracts.add(contractNumber);
                    imported++;
                }
            } catch (IllegalArgumentException e) {
                skipped++;
                addError(errors, rowNumber, e.getMessage());
            }
        }

        log.info("Importación de contratos: {} importados, {} actualizados, {} omitidos",
                imported, updated, skipped);
        return new ImportSummaryResponse(rows.size(), imported, updated, skipped, errors, null);
    }

    // ── Presupuestos ──────────────────────────────────────

    @Transactional
    public ImportSummaryResponse importBudgets(Path file) {
        List<Map<String, String>> rows = csvParser.parse(file, BUDGET_HEADERS);
        Map<String, Long> departments = loadDepartments();

        int imported = 0, updated = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        int rowNumber = 1;
        for (Map<String, String> row : rows) {
            rowNumber++;
            try {
                int fiscalYear = parseYear(require(row, "anio"));
                BigDecimal assigned = parseAmount(require(row, "asignado"));
                BigDecimal executed = parseAmount(require(row, "ejecutado"));
                BigDecimal available = parseAvailable(row.get("disponible"), assigned, executed);

                Long departmentId = resolveDepartment(departments, null, row.get("dependencia"), errors, rowNumber);
                if (departmentId == null) {
                    skipped++;
                    continue;
                }

                if (repository.budgetExists(departmentId, fiscalYear)) {
                    repository.updateBudget(departmentId, fiscalYear, assigned, executed, available);
                    updated++;
                } else {
                    repository.insertBudget(departmentId, fiscalYear, assigned, executed, available);
                    imported++;
                }
            } catch (IllegalArgumentException e) {
                skipped++;
                addError(errors, rowNumber, e.getMessage());
            }
        }

        log.info("Importación de presupuestos: {} importados, {} actualizados, {} omitidos",
                imported, updated, skipped);
        return new ImportSummaryResponse(rows.size(), imported, updated, skipped, errors, null);
    }

    // ── Recaudos ──────────────────────────────────────────

    @Transactional
    public ImportSummaryResponse importCollections(Path file) {
        List<Map<String, String>> rows = csvParser.parse(file, COLLECTION_HEADERS);
        Map<String, Long> departments = loadDepartments();

        int imported = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        int rowNumber = 1;
        for (Map<String, String> row : rows) {
            rowNumber++;
            try {
                LocalDate date = parseDate(require(row, "fecha"));
                String concept = require(row, "concepto");
                BigDecimal amount = parseAmount(require(row, "monto"));
                String taxpayer = optional(row, "contribuyente");
                String paymentMethod = optional(row, "medio_pago");

                Long departmentId = resolveDepartment(departments, null, row.get("dependencia"), errors, rowNumber);
                if (departmentId == null) {
                    skipped++;
                    continue;
                }

                repository.insertCollection(date, concept, taxpayer, amount, paymentMethod, departmentId);
                imported++;
            } catch (IllegalArgumentException e) {
                skipped++;
                addError(errors, rowNumber, e.getMessage());
            }
        }

        log.info("Importación de recaudos: {} importados, {} omitidos", imported, skipped);
        return new ImportSummaryResponse(rows.size(), imported, 0, skipped, errors, null);
    }

    // ── Helpers de normalización ──────────────────────────

    private Map<String, Long> loadDepartments() {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : repository.findAllDepartments()) {
            Long id = ((Number) row[0]).longValue();
            putKey(map, (String) row[1], id);
            putKey(map, (String) row[2], id);
        }
        return map;
    }

    /**
     * Resuelve el id del departamento de respaldo ("Sin asignar"). Si aún no
     * existe (seed no aplicado en una base antigua), lo crea para que la
     * importación no dependa de un paso manual de preparación.
     */
    private Long resolveFallbackDepartment(Map<String, Long> departments) {
        Long byCode = repository.findDepartmentIdByCode(FALLBACK_DEPARTMENT_CODE);
        if (byCode != null) {
            return byCode;
        }
        Long byName = departments.get(StatusNormalizer.normalizeText(FALLBACK_DEPARTMENT_CODE));
        if (byName != null) {
            return byName;
        }
        repository.insertDepartment(FALLBACK_DEPARTMENT_CODE, "Sin asignar",
                "Respaldo para entidades del export no reconocidas");
        return repository.findDepartmentIdByCode(FALLBACK_DEPARTMENT_CODE);
    }

    private void putKey(Map<String, Long> map, String key, Long id) {
        if (key != null && !key.isBlank()) {
            map.putIfAbsent(StatusNormalizer.normalizeText(key), id);
        }
    }

    /**
     * Resuelve el departamento de una fila. Para contratos, una entidad no
     * reconocida cae al departamento de respaldo en lugar de omitir la fila;
     * para presupuestos/recaudos se mantiene el comportamiento estricto
     * (dependencia obligatoria y reconocida, si no se omite).
     */
    private Long resolveDepartment(Map<String, Long> departments, Long fallbackDepartment,
                                   String raw, List<String> errors, int rowNumber) {
        if (raw == null || raw.isBlank()) {
            if (fallbackDepartment != null) {
                return fallbackDepartment;
            }
            addError(errors, rowNumber, "columna 'dependencia' es obligatoria");
            return null;
        }
        Long id = departments.get(StatusNormalizer.normalizeText(raw));
        if (id == null && fallbackDepartment != null) {
            return fallbackDepartment;
        }
        if (id == null) {
            addError(errors, rowNumber, "dependencia no reconocida '" + raw + "'");
        }
        return id;
    }

    private String normalizeStatus(String raw, List<String> errors, int rowNumber) {
        if (StatusNormalizer.isSkipped(raw)) {
            addError(errors, rowNumber, "estado '" + raw + "' omitido (pre-ejecución)");
            return null;
        }
        Optional<String> status = StatusNormalizer.normalize(raw);
        if (status.isEmpty()) {
            addError(errors, rowNumber, "estado no reconocido '" + raw + "'");
            return null;
        }
        return status.get();
    }

    /**
     * Acumula un error por fila respetando {@link #MAX_ERRORS}. Las filas
     * omitidas se siguen contando en {@code skipped} aunque su mensaje ya no
     * se registre.
     */
    private void addError(List<String> errors, int rowNumber, String message) {
        if (errors.size() < MAX_ERRORS) {
            errors.add("fila " + rowNumber + ": " + message);
        }
    }

    private String require(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("columna '" + key + "' es obligatoria");
        }
        return value.trim();
    }

    /**
     * Devuelve el valor de {@code key} y falla si excede el ancho de la columna
     * en base de datos. A diferencia de los campos de texto libre, un número de
     * contrato truncado dejaría de identificar al contrato, así que la fila se omite.
     */
    private String requireWithinLength(Map<String, String> row, String key, int maxLength) {
        String value = require(row, key);
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(
                    "columna '" + key + "' excede " + maxLength + " caracteres: '" + value + "'");
        }
        return value;
    }

    /**
     * Devuelve el primer valor no vacío entre dos columnas; falla indicando la
     * principal si ninguna trae dato.
     */
    private String requireAny(Map<String, String> row, String key, String fallbackKey) {
        String value = optional(row, key);
        if (value == null) {
            value = optional(row, fallbackKey);
        }
        if (value == null) {
            throw new IllegalArgumentException("columna '" + key + "' es obligatoria");
        }
        return value;
    }

    private String optional(Map<String, String> row, String key) {
        String value = row.get(key);
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /** Recorta un texto al ancho de su columna en base de datos. */
    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private int parseYear(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("año inválido: '" + raw + "'");
        }
    }

    /** Monto con separador de miles y sin decimales (p. ej. {@code $15.921.852}). */
    private static final Pattern THOUSANDS_SEPARATED = Pattern.compile("-?\\d{1,3}(\\.\\d{3})+");

    /**
     * Parsea un monto tolerando separadores colombianos (p. ej. "1.200.000,50"
     * y "$15.921.852") y notación estándar ("1200000.50").
     */
    private BigDecimal parseAmount(String raw) {
        String value = raw.trim()
                .replace("$", "")
                .replace("\u00A0", "")  // espacio duro, habitual en exports de Excel
                .replace(" ", "");
        if (value.contains(",")) {
            // Coma decimal: los puntos son separador de miles.
            value = value.replace(".", "").replace(",", ".");
        } else if (THOUSANDS_SEPARATED.matcher(value).matches()) {
            // Sin coma, los puntos solo pueden ser separador de miles. El patrón
            // exige grupos de tres dígitos, así que "1000.00" no entra aquí.
            value = value.replace(".", "");
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("monto inválido: '" + raw + "'");
        }
    }

    private static final DateTimeFormatter MONTH_FIRST = DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final DateTimeFormatter DAY_FIRST = DateTimeFormatter.ofPattern("d/M/yyyy");
    private static final Pattern SLASH_DATE = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");

    /**
     * Parsea una fecha en ISO ({@code yyyy-MM-dd}) o con barras.
     * <p>
     * El export de SECOP II usa {@code MM/dd/yyyy} (trae valores como
     * {@code 12/31/2025}), así que ese es el patrón por defecto; si el primer
     * componente es mayor que 12 no puede ser un mes y se interpreta como
     * {@code dd/MM/yyyy}. La ambigüedad es inevitable: un export en formato
     * colombiano con día menor o igual a 12 se leería con mes y día invertidos.
     * </p>
     */
    private LocalDate parseDate(String raw) {
        String value = raw.trim();
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException iso) {
            Matcher matcher = SLASH_DATE.matcher(value);
            if (matcher.matches()) {
                DateTimeFormatter formatter =
                        Integer.parseInt(matcher.group(1)) > 12 ? DAY_FIRST : MONTH_FIRST;
                try {
                    return LocalDate.parse(value, formatter);
                } catch (DateTimeParseException slash) {
                    // Cae al error común de más abajo.
                }
            }
            throw new IllegalArgumentException("fecha inválida: '" + raw
                    + "' (formato esperado yyyy-MM-dd, MM/dd/yyyy o dd/MM/yyyy)");
        }
    }

    private BigDecimal parseAvailable(String raw, BigDecimal assigned, BigDecimal executed) {
        if (raw != null && !raw.isBlank()) {
            return parseAmount(raw);
        }
        return assigned.subtract(executed);
    }
}
