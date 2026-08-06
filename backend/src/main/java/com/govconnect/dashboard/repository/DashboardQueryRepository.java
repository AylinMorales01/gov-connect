package com.govconnect.dashboard.repository;

import com.govconnect.dashboard.dto.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DashboardQueryRepository {

    private final EntityManager entityManager;

    // ==========================================
    // 1. Resumen Ejecutivo
    // ==========================================
    public DashboardSummaryDTO getSummary() {
        String sql = """
            SELECT
                collections_this_month,
                active_contracts,
                contracts_expiring,
                budget_execution_percentage
            FROM vw_dashboard_summary
            """;

        Object[] result = (Object[]) entityManager.createNativeQuery(sql).getSingleResult();

        return new DashboardSummaryDTO(
                (BigDecimal) result[0],
                ((Number) result[1]).intValue(),
                ((Number) result[2]).intValue(),
                (BigDecimal) result[3]
        );
    }

    // ==========================================
    // 2. Recaudo Mensual
    // ==========================================
    public List<MonthlyCollectionResponse> getMonthlyCollections() {
        String sql = """
            SELECT month_number, month_name, total_amount
            FROM vw_monthly_collections
            ORDER BY month_number
            """;

        List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();

        List<MonthlyCollectionResponse> list = new ArrayList<>();
        for (Object[] row : results) {
            list.add(new MonthlyCollectionResponse(
                    (Integer) row[0],
                    (String) row[1],
                    (BigDecimal) row[2]
            ));
        }
        return list;
    }

    // ==========================================
    // 3. Contratos por Vencer
    // ==========================================
    public List<ContractExpiringResponse> getContractsExpiring() {
        String sql = """
            SELECT
                contract_number,
                contractor_name,
                end_date,
                remaining_days
            FROM vw_contracts_expiring
            ORDER BY remaining_days ASC
            """;

        List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();

        List<ContractExpiringResponse> list = new ArrayList<>();
        for (Object[] row : results) {
            LocalDate endDate = null;
            Object dateObj = row[2];

            if (dateObj instanceof java.sql.Date sqlDate) {
                endDate = sqlDate.toLocalDate();
            } else if (dateObj instanceof java.sql.Timestamp ts) {
                endDate = ts.toLocalDateTime().toLocalDate();
            } else if (dateObj instanceof LocalDate ld) {
                endDate = ld;
            }

            list.add(new ContractExpiringResponse(
                    (String) row[0],
                    (String) row[1],
                    endDate,
                    ((Number) row[3]).intValue()
            ));
        }
        return list;
    }

    // ==========================================
    // 4. Ejecución Presupuestal
    // ==========================================
    public List<BudgetExecutionResponse> getBudgetExecution() {
        String sql = """
            SELECT
                department,
                assigned_budget,
                executed_budget,
                percentage
            FROM vw_budget_execution
            ORDER BY percentage DESC
            """;

        List<Object[]> rows = entityManager
                .createNativeQuery(sql)
                .getResultList();

        return rows.stream()
                .map(row -> new BudgetExecutionResponse(
                        (String) row[0],
                        (BigDecimal) row[1],
                        (BigDecimal) row[2],
                        (BigDecimal) row[3]
                ))
                .toList();
    }
}