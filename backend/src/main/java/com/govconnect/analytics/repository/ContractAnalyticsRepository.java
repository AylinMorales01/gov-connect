package com.govconnect.analytics.repository;

import com.govconnect.analytics.config.AnalyticalDataSource;
import com.govconnect.analytics.dto.ContractDepartmentBreakdownResponse;
import com.govconnect.analytics.dto.ContractStatusBreakdownResponse;
import com.govconnect.analytics.dto.TopContractorResponse;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de solo lectura para analítica de contratos en DuckDB.
 * <p>
 * Consulta la tabla {@code contracts} poblada por el ETL desde SQL Server.
 * </p>
 */
@Repository
public class ContractAnalyticsRepository {

    /** Límite de resultados para el ranking de contratistas. */
    private static final int TOP_CONTRACTORS_LIMIT = 10;

    private final DataSource duckDbDataSource;

    // Constructor explícito: la anotación @AnalyticalDataSource evita ambigüedad
    // en la inyección sin depender del nombre del bean ni del comportamiento de Lombok.
    public ContractAnalyticsRepository(@AnalyticalDataSource DataSource duckDbDataSource) {
        this.duckDbDataSource = duckDbDataSource;
    }

    /**
     * Agrupa contratos por estado (valor total y cantidad).
     */
    public List<ContractStatusBreakdownResponse> getByStatus() throws SQLException {
        String sql = """
                SELECT
                    status,
                    SUM(contract_value) AS total_value,
                    COUNT(*) AS total_count
                FROM contracts
                GROUP BY status
                ORDER BY total_value DESC
                """;

        List<ContractStatusBreakdownResponse> results = new ArrayList<>();
        try (Connection conn = duckDbDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(new ContractStatusBreakdownResponse(
                        rs.getString("status"),
                        rs.getBigDecimal("total_value"),
                        rs.getLong("total_count")
                ));
            }
        }
        return results;
    }

    /**
     * Agrupa contratos por dependencia (valor total y cantidad).
     */
    public List<ContractDepartmentBreakdownResponse> getByDepartment() throws SQLException {
        String sql = """
                SELECT
                    d.name AS department,
                    SUM(c.contract_value) AS total_value,
                    COUNT(*) AS total_count
                FROM contracts c
                JOIN departments d ON c.department_id = d.id
                GROUP BY d.name
                ORDER BY total_value DESC
                """;

        List<ContractDepartmentBreakdownResponse> results = new ArrayList<>();
        try (Connection conn = duckDbDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(new ContractDepartmentBreakdownResponse(
                        rs.getString("department"),
                        rs.getBigDecimal("total_value"),
                        rs.getLong("total_count")
                ));
            }
        }
        return results;
    }

    /**
     * Devuelve los contratistas con mayor valor total contratado.
     */
    public List<TopContractorResponse> getTopContractors() throws SQLException {
        String sql = """
                SELECT
                    contractor_name,
                    SUM(contract_value) AS total_value,
                    COUNT(*) AS contract_count
                FROM contracts
                GROUP BY contractor_name
                ORDER BY total_value DESC
                LIMIT %d
                """.formatted(TOP_CONTRACTORS_LIMIT);

        List<TopContractorResponse> results = new ArrayList<>();
        try (Connection conn = duckDbDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(new TopContractorResponse(
                        rs.getString("contractor_name"),
                        rs.getBigDecimal("total_value"),
                        rs.getLong("contract_count")
                ));
            }
        }
        return results;
    }
}
