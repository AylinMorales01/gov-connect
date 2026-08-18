package com.govconnect.analytics.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de solo lectura para el ranking de dependencias.
 * <p>
 * Consulta DuckDB (base analítica) poblada por el ETL desde SQL Server.
 * La tabla {@code budgets} en DuckDB ya contiene {@code execution_percentage}
 * pre-calculado durante la exportación, por lo que la consulta es más simple
 * que su equivalente en SQL Server.
 * </p>
 */
@Repository
public class DepartmentRankingRepository {

    private final DataSource duckDbDataSource;

    // Constructor explícito: @Qualifier en el parámetro evita ambigüedad en la
    // inyección (no depende del nombre del field ni del comportamiento de Lombok).
    public DepartmentRankingRepository(@Qualifier("duckDbDataSource") DataSource duckDbDataSource) {
        this.duckDbDataSource = duckDbDataSource;
    }

    /**
     * DTO interno para capturar los datos crudos antes del cálculo en el servicio.
     */
    public record DepartmentRawData(
            String department,
            BigDecimal executionPercentage,
            BigDecimal totalCollections
    ) {}

    /**
     * Obtiene los datos crudos para el ranking desde DuckDB.
     * <p>
     * A diferencia de la consulta original contra SQL Server —que calculaba
     * {@code execution_percentage} inline con un {@code CASE WHEN}— en DuckDB
     * la columna ya existe en la tabla {@code budgets} porque el
     * {@link com.govconnect.analytics.etl.ExportService} la pre-calcula
     * durante la exportación.
     * </p>
     * <p>
     * {@code COALESCE} es el equivalente estándar SQL de {@code ISNULL}
     * de SQL Server. DuckDB soporta ambas, pero se prefiere {@code COALESCE}
     * por portabilidad.
     * </p>
     */
    public List<DepartmentRawData> getRawRankingData() throws SQLException {
        List<DepartmentRawData> results = new ArrayList<>();

        String sql = """
                SELECT
                    d.name AS department,
                    b.execution_percentage,
                    COALESCE(SUM(c.amount), 0) AS total_collections
                FROM departments d
                JOIN budgets b ON d.id = b.department_id
                JOIN collections c ON d.id = c.department_id
                GROUP BY d.name, b.execution_percentage
                """;

        try (Connection conn = duckDbDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(new DepartmentRawData(
                        rs.getString("department"),
                        rs.getBigDecimal("execution_percentage"),
                        rs.getBigDecimal("total_collections")
                ));
            }
        }
        return results;
    }
}
