package com.govconnect.analytics.etl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.sql.SQLException;

/**
 * Orquesta el proceso ETL completo: SQL Server → CSV → DuckDB.
 * <p>
 * <b>Idempotencia:</b> cada ejecución sobrescribe los CSV y recrea las tablas
 * en DuckDB ({@code CREATE OR REPLACE TABLE}), por lo que es seguro re-ejecutar
 * el ETL ante cualquier falla.
 * </p>
 * <p>
 * <b>Atomicidad:</b> al cruzar tres sistemas (SQL Server, filesystem, DuckDB)
 * no es posible una transacción distribuida. La estrategia es:
 * <ol>
 *   <li>Limpiar CSVs de ejecuciones anteriores.</li>
 *   <li>Exportar desde SQL Server → CSV (3 tablas).</li>
 *   <li>Importar CSV → DuckDB (3 tablas).</li>
 *   <li>Si algún paso falla, se registra exactamente dónde y se recomienda
 *       re-ejecutar el ETL completo.</li>
 * </ol>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EtlService {

    private final ExportService exportService;
    private final ImportService importService;

    /** Directorio de CSVs, configurable vía {@code etl.export-dir}. */
    @Value("${etl.export-dir:exports}")
    private String exportDir;

    /**
     * Ejecuta el ciclo ETL completo.
     * <p>
     * Fases:
     * <ol>
     *   <li>Limpieza de CSVs anteriores</li>
     *   <li>Exportación SQL Server → CSV (collections, departments, budgets)</li>
     *   <li>Importación CSV → DuckDB (collections, departments, budgets)</li>
     * </ol>
     *
     * @throws RuntimeException si alguna fase falla (el mensaje indica cuál
     *         y recomienda re-ejecutar el ETL).
     */
    public void runFullEtl() {
        long start = System.currentTimeMillis();
        log.info("═══ ETL iniciado ═══");

        // ── Fase 0: limpiar CSVs anteriores ──
        cleanCsvFiles();

        // ── Fase 1: Exportar SQL Server → CSV ──
        try {
            log.info("ETL fase 1/2: exportando SQL Server → CSV...");
            exportService.exportCollectionsToCsv();
            exportService.exportDepartmentsToCsv();
            exportService.exportBudgetsToCsv();
            log.info("ETL fase 1/2: exportación completada");
        } catch (SQLException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("ETL falló en fase de exportación ({} ms): {}", elapsed, e.getMessage(), e);
            throw new RuntimeException(
                    "ETL falló durante la exportación SQL Server → CSV. "
                    + "Los datos en SQL Server no fueron modificados. "
                    + "Re-ejecute el ETL para reintentar.",
                    e
            );
        }

        // ── Fase 2: Importar CSV → DuckDB ──
        try {
            log.info("ETL fase 2/2: importando CSV → DuckDB...");
            importService.loadCollectionsCsv();
            importService.loadDepartmentsCsv();
            importService.loadBudgetsCsv();
            log.info("ETL fase 2/2: importación completada");
        } catch (SQLException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("ETL falló en fase de importación ({} ms): {}", elapsed, e.getMessage(), e);
            throw new RuntimeException(
                    "ETL falló durante la importación CSV → DuckDB. "
                    + "Los CSV exportados están íntegros en '" + exportDir + "/'. "
                    + "Re-ejecute el ETL para reintentar (las tablas DuckDB se recrean).",
                    e
            );
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("═══ ETL completado exitosamente en {} ms ═══", elapsed);
    }

    /**
     * Elimina los CSV de ejecuciones anteriores para garantizar
     * que una ejecución fallida no deje archivos parciales.
     */
    private void cleanCsvFiles() {
        File dir = new File(exportDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] csvFiles = dir.listFiles((d, name) -> name.endsWith(".csv"));
        if (csvFiles == null) {
            return;
        }
        for (File csv : csvFiles) {
            if (csv.delete()) {
                log.debug("CSV eliminado: {}", csv.getName());
            } else {
                log.warn("No se pudo eliminar CSV anterior: {}", csv.getName());
            }
        }
    }
}
