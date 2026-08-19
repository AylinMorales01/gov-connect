package com.govconnect.analytics.etl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Exporta tablas de SQL Server a archivos CSV para el ETL.
 * <p>
 * Cada método público representa una tabla exportable. La lógica común
 * (conexión, escritura de cabecera, iteración de filas, manejo de errores)
 * está centralizada en {@link #exportToCsv(String, String, String, CsvRowMapper)}.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final DataSource dataSource;

    /** Directorio de salida de CSVs, configurable vía {@code etl.export-dir}. */
    @Value("${etl.export-dir:exports}")
    private String exportDir;

    // ── Tablas exportables ──────────────────────────────────

    /**
     * Exporta la tabla {@code collections} (recaudos) a CSV.
     */
    public void exportCollectionsToCsv() throws SQLException {
        String sql = """
                SELECT
                    collection_date,
                    concept,
                    taxpayer,
                    amount,
                    payment_method,
                    department_id
                FROM collections
                ORDER BY collection_date
                """;

        exportToCsv(
                "collections.csv",
                sql,
                "collection_date,concept,taxpayer,amount,payment_method,department_id",
                rs -> rs.getDate("collection_date") + "," +
                        clean(rs.getString("concept")) + "," +
                        clean(rs.getString("taxpayer")) + "," +
                        rs.getBigDecimal("amount") + "," +
                        clean(rs.getString("payment_method")) + "," +
                        rs.getLong("department_id")
        );
    }

    /**
     * Exporta la tabla {@code departments} (dependencias) a CSV.
     */
    public void exportDepartmentsToCsv() throws SQLException {
        String sql = """
                SELECT
                    id,
                    name
                FROM departments
                ORDER BY id
                """;

        exportToCsv(
                "departments.csv",
                sql,
                "id,name",
                rs -> rs.getLong("id") + "," +
                        clean(rs.getString("name"))
        );
    }

    /**
     * Exporta la tabla {@code budgets} con el porcentaje de ejecución
     * pre-calculado para el año fiscal más reciente de cada dependencia.
     */
    public void exportBudgetsToCsv() throws SQLException {
        String sql = """
                SELECT
                    department_id,
                    CASE
                        WHEN assigned_budget = 0 THEN 0
                        ELSE ROUND((executed_budget * 100.0) / assigned_budget, 2)
                    END AS execution_percentage
                FROM budgets
                WHERE fiscal_year = (
                    SELECT MAX(fiscal_year)
                    FROM budgets b2
                    WHERE b2.department_id = budgets.department_id
                )
                ORDER BY department_id
                """;

        exportToCsv(
                "budgets.csv",
                sql,
                "department_id,execution_percentage",
                rs -> rs.getLong("department_id") + "," +
                        rs.getBigDecimal("execution_percentage")
        );
    }

    // ── Plantilla genérica ──────────────────────────────────

    /**
     * Plantilla de exportación CSV: ejecuta la consulta SQL y escribe el archivo.
     * <p>
     * Centraliza conexión, escritura de cabecera, iteración de resultados,
     * logging de progreso y manejo de errores de E/S.
     * </p>
     *
     * @param fileName  nombre del archivo CSV (ej. "collections.csv").
     * @param sql       consulta SQL a ejecutar contra SQL Server.
     * @param header    línea de cabecera CSV (columnas separadas por coma).
     * @param rowMapper función que convierte una fila del {@link ResultSet} en línea CSV.
     * @throws SQLException si la consulta falla.
     */
    private void exportToCsv(
            String fileName,
            String sql,
            String header,
            CsvRowMapper rowMapper
    ) throws SQLException {
        ensureExportDir();

        File file = new File(exportDir, fileName);
        String filePath = file.getPath();
        long start = System.currentTimeMillis();

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                BufferedWriter writer = new BufferedWriter(new FileWriter(file))
        ) {
            writer.write(header);
            writer.newLine();

            int rowCount = 0;
            while (rs.next()) {
                writer.write(rowMapper.mapRow(rs));
                writer.newLine();
                rowCount++;
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info("ETL export: {} → {} filas ({} ms)", filePath, rowCount, elapsed);

        } catch (IOException e) {
            throw new RuntimeException("Error exportando " + filePath, e);
        }
    }

    // ── Helpers ─────────────────────────────────────────────

    /** Convierte una fila activa del {@link ResultSet} en una línea CSV. */
    @FunctionalInterface
    private interface CsvRowMapper {
        String mapRow(ResultSet rs) throws SQLException;
    }

    /** Crea el directorio de exportación si no existe. */
    private void ensureExportDir() {
        File dir = new File(exportDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Sanitiza un valor de texto para CSV.
     * <p>
     * Reemplaza comas por espacios y escapa comillas dobles
     * duplicándolas (estándar CSV RFC 4180).
     * </p>
     */
    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\"", "\"\"")
                .replace(",", " ");
    }
}
