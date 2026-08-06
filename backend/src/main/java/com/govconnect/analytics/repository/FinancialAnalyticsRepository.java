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

/**
 * Repositorio de solo lectura para agregaciones mensuales de recaudos en SQL Server.
 * <p>
 * <b>Responsabilidad única:</b> extraer datos crudos. Toda lógica de
 * negocio (cálculo de crecimiento, derivación de tendencias) reside en
 * {@link com.govconnect.analytics.service.FinancialOverviewService}.
 * </p>
 */
@Repository
@RequiredArgsConstructor
public class FinancialAnalyticsRepository {

    @Qualifier("primaryDataSource")
    private final DataSource dataSource;

    private static final String MONTHLY_CTE = """
            WITH monthly_collections AS (
                SELECT FORMAT(collection_date, 'yyyy-MM') AS month,
                       SUM(amount) AS total_amount
                FROM collections
                GROUP BY FORMAT(collection_date, 'yyyy-MM')
            )
            """;

    /**
     * Agregados mensuales crudos — sin procesar, sin interpretar.
     * El servicio es responsable de convertirlos en un
     * {@link com.govconnect.analytics.dto.FinancialOverviewResponse}.
     */
    public record MonthlyAggregates(
            String bestMonth,
            BigDecimal bestAmount,
            String worstMonth,
            BigDecimal worstAmount,
            BigDecimal average,
            BigDecimal currentMonthAmount,
            BigDecimal previousMonthAmount
    ) {}

    /**
     * Extrae los datos agregados directamente desde SQL Server.
     *
     * @return los agregados mensuales crudos (nunca {@code null})
     * @throws SQLException si ocurre un error en la consulta
     */
    public MonthlyAggregates getMonthlyAggregates() throws SQLException {
        String bestMonth = null;
        BigDecimal bestAmount = BigDecimal.ZERO;
        String worstMonth = null;
        BigDecimal worstAmount = BigDecimal.ZERO;
        BigDecimal average = BigDecimal.ZERO;
        BigDecimal currentMonthAmount = BigDecimal.ZERO;
        BigDecimal previousMonthAmount = BigDecimal.ZERO;

        try (Connection conn = dataSource.getConnection()) {

            // 1. Mes con mayor recaudo
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         MONTHLY_CTE + " SELECT month, total_amount FROM monthly_collections ORDER BY total_amount DESC, month OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY")) {
                if (rs.next()) {
                    bestMonth = rs.getString("month");
                    bestAmount = rs.getBigDecimal("total_amount");
                }
            }

            // 2. Mes con menor recaudo
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         MONTHLY_CTE + " SELECT month, total_amount FROM monthly_collections ORDER BY total_amount ASC, month OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY")) {
                if (rs.next()) {
                    worstMonth = rs.getString("month");
                    worstAmount = rs.getBigDecimal("total_amount");
                }
            }

            // 3. Promedio mensual
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         MONTHLY_CTE + " SELECT AVG(total_amount) AS avg_amount FROM monthly_collections")) {
                if (rs.next()) {
                    average = rs.getBigDecimal("avg_amount");
                }
            }

            // 4. Últimos dos meses (actual y anterior)
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         MONTHLY_CTE + " SELECT month, total_amount FROM monthly_collections ORDER BY month DESC OFFSET 0 ROWS FETCH NEXT 2 ROWS ONLY")) {
                if (rs.next()) {
                    currentMonthAmount = rs.getBigDecimal("total_amount");
                    if (rs.next()) {
                        previousMonthAmount = rs.getBigDecimal("total_amount");
                    }
                }
            }
        }

        return new MonthlyAggregates(
                bestMonth, bestAmount,
                worstMonth, worstAmount,
                average,
                currentMonthAmount, previousMonthAmount
        );
    }
}