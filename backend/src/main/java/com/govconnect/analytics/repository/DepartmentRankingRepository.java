package com.govconnect.analytics.repository;

import lombok.RequiredArgsConstructor;
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

@Repository
@RequiredArgsConstructor
public class DepartmentRankingRepository {

    @Qualifier("primaryDataSource")
    private final DataSource dataSource;

    /**
     * DTO interno para capturar los datos crudos antes del cálculo en el servicio.
     */
    public record DepartmentRawData(
            String department,
            BigDecimal executionPercentage,
            BigDecimal totalCollections
    ) {}

    /**
     * Obtiene los datos crudos para el ranking desde SQL Server.
     * <p>
     * {@code execution_percentage} se calcula en T-SQL porque no existe
     * como columna física en la tabla {@code budgets}. La subconsulta
     * permite agrupar por el alias sin repetir la expresión {@code CASE}.
     * </p>
     */
    public List<DepartmentRawData> getRawRankingData() throws SQLException {
        List<DepartmentRawData> results = new ArrayList<>();

        String sql = """
                SELECT
                    department,
                    execution_percentage,
                    ISNULL(SUM(amount), 0) AS total_collections
                FROM (
                    SELECT
                        d.name AS department,
                        CASE
                            WHEN b.assigned_budget = 0 THEN 0
                            ELSE (b.executed_budget * 100.0) / b.assigned_budget
                        END AS execution_percentage,
                        c.amount
                    FROM departments d
                    JOIN budgets b ON d.id = b.department_id
                    JOIN collections c ON d.id = c.department_id
                ) AS dept_data
                GROUP BY department, execution_percentage
                """;

        try (Connection conn = dataSource.getConnection();
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