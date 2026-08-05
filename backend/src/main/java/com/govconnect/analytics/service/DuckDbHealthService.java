package com.govconnect.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Service
@RequiredArgsConstructor
public class DuckDbHealthService {

    private final Connection connection;

    public String test() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT 'DuckDB OK'");
            rs.next();
            return rs.getString(1);
        }
    }
}