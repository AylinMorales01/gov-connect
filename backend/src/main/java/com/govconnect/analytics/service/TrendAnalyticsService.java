package com.govconnect.analytics.service;

import com.govconnect.analytics.dto.MonthlyTrendDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de tendencia mensual de recaudos.
 * <p>
 * Consulta DuckDB (base analítica) poblada por el ETL desde SQL Server.
 * </p>
 */
@Service
public class TrendAnalyticsService {

    private final DataSource duckDbDataSource;

    // Constructor explícito: @Qualifier en el parámetro evita ambigüedad en la
    // inyección (no depende del nombre del field ni del comportamiento de Lombok).
    public TrendAnalyticsService(@Qualifier("duckDbDataSource") DataSource duckDbDataSource) {
        this.duckDbDataSource = duckDbDataSource;
    }

    /**
     * Obtiene la tendencia mensual de recaudos desde DuckDB.
     * <p>
     * {@code strftime(date, '%Y-%m')} equivale al {@code FORMAT(date, 'yyyy-MM')}
     * de SQL Server. Produce el mismo resultado: agrupación por año-mes.
     * </p>
     */
    public List<MonthlyTrendDto> getMonthlyTrend() throws SQLException {
        List<MonthlyTrendDto> trend = new ArrayList<>();

        String sql = """
                SELECT
                    strftime(collection_date, '%Y-%m') AS month,
                    SUM(amount) AS total
                FROM collections
                GROUP BY strftime(collection_date, '%Y-%m')
                ORDER BY month
                """;

        try (Connection conn = duckDbDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                trend.add(new MonthlyTrendDto(
                        rs.getString("month"),
                        rs.getBigDecimal("total")
                ));
            }
        }
        return trend;
    }
}
