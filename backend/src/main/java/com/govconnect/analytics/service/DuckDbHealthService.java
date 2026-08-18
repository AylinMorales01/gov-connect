package com.govconnect.analytics.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Service
public class DuckDbHealthService {

    private final DataSource duckDbDataSource;

    // Constructor explícito: @Qualifier en el parámetro evita ambigüedad en la
    // inyección (no depende del nombre del field ni del comportamiento de Lombok).
    public DuckDbHealthService(@Qualifier("duckDbDataSource") DataSource duckDbDataSource) {
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