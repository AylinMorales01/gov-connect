package com.govconnect.analytics.etl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final DataSource dataSource;

    public void exportCollectionsToCsv() throws SQLException {

        File exportDir = new File("exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        String sql = """
                SELECT
                    collection_date,
                    concept,
                    taxpayer,
                    amount,
                    payment_method,
                    department_id
                FROM collections
                ORDER BY collection_date
                """;

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                BufferedWriter writer = new BufferedWriter(new FileWriter("exports/collections.csv"))
        ) {

            // Cabecera
            writer.write("collection_date,concept,taxpayer,amount,payment_method,department_id");
            writer.newLine();

            while (rs.next()) {

                writer.write(
                        rs.getDate("collection_date") + "," +
                                clean(rs.getString("concept")) + "," +
                                clean(rs.getString("taxpayer")) + "," +
                                rs.getBigDecimal("amount") + "," +
                                clean(rs.getString("payment_method")) + "," +
                                rs.getLong("department_id")
                );

                writer.newLine();
            }

        } catch (IOException e) {
            throw new RuntimeException("Error exportando collections.csv", e);
        }
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }

        return value.replace(",", " ");
    }

    // ===========================
    // TEMPORALMENTE LOS DEJAMOS ASÍ
    // ===========================

    public void exportDepartmentsToCsv() throws SQLException {

        String sql = """
            SELECT
                id,
                name
            FROM departments
            ORDER BY id
            """;

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                BufferedWriter writer = new BufferedWriter(new FileWriter("exports/departments.csv"))
        ) {

            writer.write("id,name");
            writer.newLine();

            while (rs.next()) {

                writer.write(
                        rs.getLong("id") + "," +
                                clean(rs.getString("name"))
                );

                writer.newLine();
            }

        } catch (IOException e) {
            throw new RuntimeException("Error exportando departments.csv", e);
        }
    }

    public void exportBudgetsToCsv() throws SQLException {

        String sql = """
            SELECT
                department_id,

                CASE
                    WHEN assigned_budget = 0 THEN 0
                    ELSE ROUND((executed_budget * 100.0) / assigned_budget, 2)
                END AS execution_percentage

            FROM budgets
            WHERE fiscal_year = (
                SELECT MAX(fiscal_year)
                FROM budgets b2
                WHERE b2.department_id = budgets.department_id
            )

            ORDER BY department_id
            """;

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                BufferedWriter writer = new BufferedWriter(new FileWriter("exports/budgets.csv"))
        ) {

            writer.write("department_id,execution_percentage");
            writer.newLine();

            while (rs.next()) {

                writer.write(
                        rs.getLong("department_id") + "," +
                                rs.getBigDecimal("execution_percentage")
                );

                writer.newLine();
            }

        } catch (IOException e) {
            throw new RuntimeException("Error exportando budgets.csv", e);
        }
    }
}