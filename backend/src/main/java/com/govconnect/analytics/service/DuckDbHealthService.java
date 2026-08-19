package com.govconnect.analytics.service;

import com.govconnect.analytics.config.AnalyticalDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Service
public class DuckDbHealthService {

    private final DataSource duckDbDataSource;

    // Constructor explícito: la anotación @AnalyticalDataSource evita ambigüedad
    // en la inyección sin depender del nombre del bean ni del comportamiento de Lombok.
    public DuckDbHealthService(@AnalyticalDataSource DataSource duckDbDataSource) {
        this.duckDbDataSource = duckDbDataSource;
    }

    public String test() throws SQLException {
        try (Connection conn = duckDbDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 'DuckDB OK'")) {
            rs.next();
            return rs.getString(1);
        }
    }
}