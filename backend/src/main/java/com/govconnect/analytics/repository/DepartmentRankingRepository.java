package com.govconnect.analytics.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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

    private final Connection duckDbConnection;

    // DTO interno para capturar los datos crudos antes del cálculo en el servicio
    public record DepartmentRawData(String department, BigDecimal executionPercentage, BigDecimal totalCollections) {}

    public List<DepartmentRawData> getRawRankingData() throws SQLException {
        List<DepartmentRawData> results = new ArrayList<>();

        String sql = """
                SELECT 
                    d.name AS department, 
                    b.execution_percentage, 
                    COALESCE(SUM(c.amount),0) AS total_collections
                FROM departments d
                JOIN budgets b ON d.id = b.department_id
                JOIN collections c ON d.id = c.department_id
                GROUP BY d.name, b.execution_percentage
                """;

        try (Statement stmt = duckDbConnection.createStatement();
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