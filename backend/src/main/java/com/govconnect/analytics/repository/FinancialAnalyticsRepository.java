package com.govconnect.analytics.repository;

import com.govconnect.analytics.config.AnalyticalDataSource;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Repositorio de solo lectura para agregaciones mensuales de recaudos.
 * <p>
 * Consulta DuckDB (base analítica) poblada por el ETL desde SQL Server.
 * </p>
 * <p>
 * <b>Responsabilidad única:</b> extraer datos crudos. Toda lógica de
 * negocio (cálculo de crecimiento, derivación de tendencias) reside en
 * {@link com.govconnect.analytics.service.FinancialOverviewService}.
 * </p>
 * <p>
 * <b>Diferencias de sintaxis respecto a SQL Server:</b>
 * <ul>
 *   <li>{@code FORMAT(date, 'yyyy-MM')} → {@code strftime(date, '%Y-%m')}</li>
 *   <li>{@code OFFSET … FETCH NEXT N ROWS ONLY} → {@code LIMIT N}</li>
 * </ul>
 * </p>
 */
@Repository
public class FinancialAnalyticsRepository {

    private final DataSource duckDbDataSource;

    // Constructor explícito: la anotación @AnalyticalDataSource evita ambigüedad
    // en la inyección sin depender del nombre del bean ni del comportamiento de Lombok.
    public FinancialAnalyticsRepository(@AnalyticalDataSource DataSource duckDbDataSource) {
        this.duckDbDataSource = duckDbDataSource;
    }

    /**
     * CTE compartida para todas las subconsultas.
     * {@code strftime} de DuckDB equivale a {@code FORMAT} de T-SQL
     * para la agrupación por año-mes.
     */
    private static final String MONTHLY_CTE = """
            WITH monthly_collections AS (
                SELECT strftime(collection_date, '%Y-%m') AS month,
                       SUM(amount) AS total_amount
                FROM collections
                GROUP BY strftime(collection_date, '%Y-%m')
            )
            """;

    /**
     * Agregados mensuales crudos — sin procesar, sin interpretar.
     * El servicio es responsable de convertirlos en un
     * {@link com.govconnect.analytics.dto.FinancialOverviewResponse}.
     */
    public record MonthlyAggregates(
            String bestMonth,
            BigDecimal bestAmount,
            String worstMonth,
            BigDecimal worstAmount,
            BigDecimal average,
            BigDecimal currentMonthAmount,
            BigDecimal previousMonthAmount
    ) {}

    /**
     * Extrae los datos agregados directamente desde DuckDB.
     * <p>
     * Delega en 4 subconsultas independientes dentro de una misma conexión.
     * </p>
     *
     * @return los agregados mensuales crudos (nunca {@code null})
     * @throws SQLException si ocurre un error en la consulta
     */
    public MonthlyAggregates getMonthlyAggregates() throws SQLException {
        try (Connection conn = duckDbDataSource.getConnection()) {
            return new MonthlyAggregates(
                    queryBestMonth(conn),
                    queryBestAmount(conn),
                    queryWorstMonth(conn),
                    queryWorstAmount(conn),
                    queryAverage(conn),
                    queryCurrentMonthAmount(conn),
                    queryPreviousMonthAmount(conn)
            );
        }
    }

    // ── Subconsultas ──────────────────────────────────────

    private String queryBestMonth(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     MONTHLY_CTE
                     + " SELECT month FROM monthly_collections"
                     + " ORDER BY total_amount DESC, month LIMIT 1")) {
            return rs.next() ? rs.getString("month") : null;
        }
    }

    private BigDecimal queryBestAmount(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     MONTHLY_CTE
                     + " SELECT total_amount FROM monthly_collections"
                     + " ORDER BY total_amount DESC, month LIMIT 1")) {
            return rs.next() ? rs.getBigDecimal("total_amount") : BigDecimal.ZERO;
        }
    }

    private String queryWorstMonth(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     MONTHLY_CTE
                     + " SELECT month FROM monthly_collections"
                     + " ORDER BY total_amount ASC, month LIMIT 1")) {
            return rs.next() ? rs.getString("month") : null;
        }
    }

    private BigDecimal queryWorstAmount(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     MONTHLY_CTE
                     + " SELECT total_amount FROM monthly_collections"
                     + " ORDER BY total_amount ASC, month LIMIT 1")) {
            return rs.next() ? rs.getBigDecimal("total_amount") : BigDecimal.ZERO;
        }
    }

    private BigDecimal queryAverage(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     MONTHLY_CTE
                     + " SELECT AVG(total_amount) AS avg_amount FROM monthly_collections")) {
            return rs.next() ? rs.getBigDecimal("avg_amount") : BigDecimal.ZERO;
        }
    }

    private BigDecimal queryCurrentMonthAmount(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     MONTHLY_CTE
                     + " SELECT total_amount FROM monthly_collections"
                     + " ORDER BY month DESC LIMIT 1")) {
            return rs.next() ? rs.getBigDecimal("total_amount") : BigDecimal.ZERO;
        }
    }

    private BigDecimal queryPreviousMonthAmount(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     MONTHLY_CTE
                     + " SELECT total_amount FROM monthly_collections"
                     + " ORDER BY month DESC LIMIT 1 OFFSET 1")) {
            return rs.next() ? rs.getBigDecimal("total_amount") : BigDecimal.ZERO;
        }
    }
}
