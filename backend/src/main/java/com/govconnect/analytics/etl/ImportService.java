package com.govconnect.analytics.etl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final Connection duckDbConnection;

    public void loadCollectionsCsv() throws SQLException {
        // La maravilla de DuckDB: Ingesta directa de CSV
        String sql = "CREATE OR REPLACE TABLE collections AS SELECT * FROM read_csv_auto('exports/collections.csv');";

        try (Statement stmt = duckDbConnection.createStatement()) {
            stmt.execute(sql);
        }
    }

    // Crear la tabla departments en DuckDB desde el CSV
    public void loadDepartmentsCsv() throws SQLException {
        String sql = "CREATE OR REPLACE TABLE departments AS SELECT * FROM read_csv_auto('exports/departments.csv');";
        try (Statement stmt = duckDbConnection.createStatement()) {
            stmt.execute(sql);
        }
    }

    // Crear la tabla budgets en DuckDB desde el CSV
    public void loadBudgetsCsv() throws SQLException {
        String sql = "CREATE OR REPLACE TABLE budgets AS SELECT * FROM read_csv_auto('exports/budgets.csv');";
        try (Statement stmt = duckDbConnection.createStatement()) {
            stmt.execute(sql);
        }
    }
}