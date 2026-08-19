package com.govconnect.analytics.repository;

import com.govconnect.analytics.config.AnalyticalDataSource;
import com.govconnect.analytics.dto.ConceptBreakdownResponse;
import com.govconnect.analytics.dto.PaymentMethodBreakdownResponse;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de solo lectura para desglosar recaudos por dimensiones
 * no explotadas de la tabla {@code collections} en DuckDB: concepto y
 * método de pago.
 */
@Repository
public class CollectionsBreakdownRepository {

    private final DataSource duckDbDataSource;

    // Constructor explícito: la anotación @AnalyticalDataSource evita ambigüedad
    // en la inyección sin depender del nombre del bean ni del comportamiento de Lombok.
    public CollectionsBreakdownRepository(@AnalyticalDataSource DataSource duckDbDataSource) {
        this.duckDbDataSource = duckDbDataSource;
    }

    /**
     * Agrupa los recaudos por concepto, del mayor al menor.
     */
    public List<ConceptBreakdownResponse> getByConcept() throws SQLException {
        String sql = """
                SELECT
                    concept,
                    SUM(amount) AS total_amount,
                    COUNT(*) AS total_count
                FROM collections
                GROUP BY concept
                ORDER BY total_amount DESC
                """;

        List<ConceptBreakdownResponse> results = new ArrayList<>();
        try (Connection conn = duckDbDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(new ConceptBreakdownResponse(
                        rs.getString("concept"),
                        rs.getBigDecimal("total_amount"),
                        rs.getLong("total_count")
                ));
            }
        }
        return results;
    }

    /**
     * Agrupa los recaudos por método de pago, del mayor al menor.
     * Los valores nulos se presentan como "Sin método".
     */
    public List<PaymentMethodBreakdownResponse> getByPaymentMethod() throws SQLException {
        String sql = """
                SELECT
                    COALESCE(payment_method, 'Sin método') AS payment_method,
                    SUM(amount) AS total_amount,
                    COUNT(*) AS total_count
                FROM collections
                GROUP BY payment_method
                ORDER BY total_amount DESC
                """;

        List<PaymentMethodBreakdownResponse> results = new ArrayList<>();
        try (Connection conn = duckDbDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(new PaymentMethodBreakdownResponse(
                        rs.getString("payment_method"),
                        rs.getBigDecimal("total_amount"),
                        rs.getLong("total_count")
                ));
            }
        }
        return results;
    }
}
