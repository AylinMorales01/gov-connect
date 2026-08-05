package com.govconnect.analytics.service;

import com.govconnect.analytics.dto.MonthlyTrendDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrendAnalyticsService {

    private final Connection duckDbConnection;

    public List<MonthlyTrendDto> getMonthlyTrend() throws SQLException {
        List<MonthlyTrendDto> trend = new ArrayList<>();

        String sql = """
                SELECT 
                    strftime(collection_date, '%Y-%m') AS month, 
                    SUM(amount) AS total 
                FROM collections 
                GROUP BY month 
                ORDER BY month
                """;

        try (Statement stmt = duckDbConnection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
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