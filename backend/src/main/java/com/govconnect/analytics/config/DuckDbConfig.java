package com.govconnect.analytics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
public class DuckDbConfig {

    @Bean
    public Connection duckDbConnection() throws SQLException {
        // Generará el archivo analytics.duckdb automáticamente en esta ruta si no existe
        return DriverManager.getConnection("jdbc:duckdb:database/analytics/analytics.duckdb");
    }
}