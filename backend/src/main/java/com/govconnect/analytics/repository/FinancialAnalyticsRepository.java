package com.govconnect.analytics.repository;

import com.govconnect.analytics.dto.FinancialOverviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Repository
@RequiredArgsConstructor
public class FinancialAnalyticsRepository {

    private final Connection duckDbConnection;

    public FinancialOverviewResponse getFinancialOverview() throws SQLException {
        String bestMonth = null;
        BigDecimal bestAmount = BigDecimal.ZERO;
        String worstMonth = null;
        BigDecimal worstAmount = BigDecimal.ZERO;
        BigDecimal average = BigDecimal.ZERO;
        BigDecimal growth = BigDecimal.ZERO;
        String trend = "ESTABLE";

        // Creamos un CTE base para reutilizar la lógica de agrupación por mes
        String baseQuery = """
                WITH monthly_collections AS (
                    SELECT strftime(collection_date, '%Y-%m') AS month, SUM(amount) AS total_amount
                    FROM collections
                    GROUP BY month
                )
                """;

        try (Statement stmt = duckDbConnection.createStatement()) {

            // 1. Mes con mayor recaudo
            ResultSet rsBest = stmt.executeQuery(baseQuery + " SELECT month, total_amount FROM monthly_collections ORDER BY total_amount DESC LIMIT 1");
            if (rsBest.next()) {
                bestMonth = rsBest.getString("month");
                bestAmount = rsBest.getBigDecimal("total_amount");
            }

            // 2. Mes con menor recaudo
            ResultSet rsWorst = stmt.executeQuery(baseQuery + " SELECT month, total_amount FROM monthly_collections ORDER BY total_amount ASC LIMIT 1");
            if (rsWorst.next()) {
                worstMonth = rsWorst.getString("month");
                worstAmount = rsWorst.getBigDecimal("total_amount");
            }

            // 3. Promedio mensual
            ResultSet rsAvg = stmt.executeQuery(baseQuery + " SELECT AVG(total_amount) as avg_amount FROM monthly_collections");
            if (rsAvg.next()) {
                average = rsAvg.getBigDecimal("avg_amount");
            }

            // 4. Últimos dos meses para el crecimiento
            ResultSet rsLastTwo = stmt.executeQuery(baseQuery + " SELECT month, total_amount FROM monthly_collections ORDER BY month DESC LIMIT 2");
            BigDecimal currentMonthAmount = BigDecimal.ZERO;
            BigDecimal previousMonthAmount = BigDecimal.ZERO;

            if (rsLastTwo.next()) {
                currentMonthAmount = rsLastTwo.getBigDecimal("total_amount");
                if (rsLastTwo.next()) {
                    previousMonthAmount = rsLastTwo.getBigDecimal("total_amount");
                }
            }

            // 5. Calcular crecimiento y tendencia
            if (previousMonthAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal difference = currentMonthAmount.subtract(previousMonthAmount);
                // ((Actual - Anterior) / Anterior) * 100
                growth = difference.divide(previousMonthAmount, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));

                if (growth.compareTo(new BigDecimal("5")) > 0) {
                    trend = "CRECIMIENTO";
                } else if (growth.compareTo(new BigDecimal("-5")) < 0) {
                    trend = "DESCENSO";
                }
            }
        }

        return new FinancialOverviewResponse(
                bestMonth, bestAmount, worstMonth, worstAmount, average, growth, trend
        );
    }
}