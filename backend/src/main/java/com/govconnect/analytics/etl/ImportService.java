package com.govconnect.analytics.etl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Carga los CSV exportados desde SQL Server hacia DuckDB.
 * <p>
 * Cada método obtiene y cierra su propia {@link Connection} del
 * {@link DataSource}, garantizando thread-safety en ejecuciones
 * concurrentes del ETL.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ImportService {

    private final DataSource duckDbDataSource;

    public void loadCollectionsCsv() throws SQLException {
        String sql = "CREATE OR REPLACE TABLE collections AS SELECT * FROM read_csv_auto('exports/collections.csv');";

        try (Connection conn = duckDbDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void loadDepartmentsCsv() throws SQLException {
        String sql = "CREATE OR REPLACE TABLE departments AS SELECT * FROM read_csv_auto('exports/departments.csv');";

        try (Connection conn = duckDbDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void loadBudgetsCsv() throws SQLException {
        String sql = "CREATE OR REPLACE TABLE budgets AS SELECT * FROM read_csv_auto('exports/budgets.csv');";

        try (Connection conn = duckDbDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}