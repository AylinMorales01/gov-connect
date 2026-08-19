package com.govconnect.analytics.etl;

import com.govconnect.analytics.config.AnalyticalDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Carga los CSV exportados desde SQL Server hacia DuckDB.
 * <p>
 * Cada método obtiene y cierra su propia {@link Connection} del
 * {@link DataSource}, garantizando thread-safety en ejecuciones
 * concurrentes del ETL.
 * </p>
 * <p>
 * Las tablas se recrean con {@code CREATE OR REPLACE TABLE}, lo que
 * hace que cada importación individual sea atómica a nivel de tabla
 * en DuckDB.
 * </p>
 */
@Service
@Slf4j
public class ImportService {

    private final DataSource duckDbDataSource;

    // Constructor explícito: la anotación @AnalyticalDataSource evita ambigüedad
    // en la inyección sin depender del nombre del bean ni del comportamiento de Lombok.
    public ImportService(@AnalyticalDataSource DataSource duckDbDataSource) {
        this.duckDbDataSource = duckDbDataSource;
    }

    /** Directorio de CSVs, configurable vía {@code etl.export-dir}. */
    @Value("${etl.export-dir:exports}")
    private String csvBasePath;

    /**
     * Carga {@code collections.csv} en la tabla {@code collections} de DuckDB.
     */
    public void loadCollectionsCsv() throws SQLException {
        loadCsvToDuckDb("collections");
    }

    /**
     * Carga {@code departments.csv} en la tabla {@code departments} de DuckDB.
     */
    public void loadDepartmentsCsv() throws SQLException {
        loadCsvToDuckDb("departments");
    }

    /**
     * Carga {@code budgets.csv} en la tabla {@code budgets} de DuckDB.
     */
    public void loadBudgetsCsv() throws SQLException {
        loadCsvToDuckDb("budgets");
    }

    // ── Plantilla genérica ──────────────────────────────────

    /**
     * Carga un CSV en DuckDB usando {@code CREATE OR REPLACE TABLE … AS SELECT * FROM read_csv_auto}.
     * <p>
     * Mide el tiempo de carga y cuenta las filas resultantes para logging.
     * </p>
     *
     * @param tableName nombre de la tabla (y del archivo CSV, sin extensión).
     * @throws SQLException si la carga o el conteo fallan.
     */
    private void loadCsvToDuckDb(String tableName) throws SQLException {
        String csvPath = csvBasePath + "/" + tableName + ".csv";
        long start = System.currentTimeMillis();

        String loadSql = "CREATE OR REPLACE TABLE " + tableName
                + " AS SELECT * FROM read_csv_auto('" + csvPath + "');";

        try (Connection conn = duckDbDataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(loadSql);

            // Contar filas cargadas
            int rowCount = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
                if (rs.next()) {
                    rowCount = rs.getInt(1);
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info("ETL import: {} → {} filas ({} ms)", tableName, rowCount, elapsed);
        }
    }
}
