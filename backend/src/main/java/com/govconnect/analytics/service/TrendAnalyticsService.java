package com.govconnect.analytics.service;

import com.govconnect.analytics.dto.MonthlyTrendDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrendAnalyticsService {

    @Qualifier("primaryDataSource")
    private final DataSource dataSource;

    /**
     * Obtiene la tendencia mensual de recaudos desde SQL Server.
     * <p>
     * La agrupación por mes usa {@code FORMAT(date, 'yyyy-MM')}
     * nativa de T-SQL, compatible con SQL Server 2012+.
     * </p>
     */
    public List<MonthlyTrendDto> getMonthlyTrend() throws SQLException {
        List<MonthlyTrendDto> trend = new ArrayList<>();

        String sql = """
                SELECT
                    FORMAT(collection_date, 'yyyy-MM') AS month,
                    SUM(amount) AS total
                FROM collections
                GROUP BY FORMAT(collection_date, 'yyyy-MM')
                ORDER BY month
                """;

        try (Connection conn = dataSource.getConnection();
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