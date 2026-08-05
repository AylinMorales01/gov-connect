package com.govconnect.dashboard.repository;

import com.govconnect.dashboard.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DashboardQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // ==========================================
    // 1. Resumen Ejecutivo (Viene del Sprint 1.5)
    // ==========================================
    public DashboardSummaryDTO getSummary() {
        String sql = "SELECT collections_this_month, active_contracts, contracts_expiring, budget_execution_percentage, last_updated FROM vw_dashboard_summary";
        Object[] result = (Object[]) entityManager.createNativeQuery(sql).getSingleResult();

        BigDecimal collections = (BigDecimal) result[0];
        Integer activeContracts = ((Number) result[1]).intValue();
        Integer expiringContracts = ((Number) result[2]).intValue();
        BigDecimal budgetPercentage = (BigDecimal) result[3];

        LocalDateTime lastUpdated = null;
        if (result[4] instanceof Timestamp) {
            lastUpdated = ((Timestamp) result[4]).toLocalDateTime();
        } else if (result[4] instanceof LocalDateTime) {
            lastUpdated = (LocalDateTime) result[4];
        }

        return new DashboardSummaryDTO(collections, activeContracts, expiringContracts, budgetPercentage, lastUpdated);
    }

    // ==========================================
    // 2. Recaudo Mensual
    // ==========================================
    public List<MonthlyCollectionResponse> getMonthlyCollections() {
        String sql = "SELECT month_number, month_name, total_amount FROM vw_monthly_collections ORDER BY month_number";
        List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();

        List<MonthlyCollectionResponse> list = new ArrayList<>();
        for (Object[] row : results) {
            list.add(new MonthlyCollectionResponse((Integer) row[0], (String) row[1], (BigDecimal) row[2]));
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
                remaining_days,
                status
            FROM vw_contracts_expiring
            ORDER BY remaining_days ASC
            """;

        List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();

        List<ContractExpiringResponse> list = new ArrayList<>();
        for (Object[] row : results) {
            LocalDate endDate = null;
            Object dateObj = row[2];

            // Conversión segura independiente de lo que devuelva JDBC
            if (dateObj instanceof java.sql.Date) {
                endDate = ((java.sql.Date) dateObj).toLocalDate();
            } else if (dateObj instanceof java.sql.Timestamp) {
                endDate = ((java.sql.Timestamp) dateObj).toLocalDateTime().toLocalDate();
            } else if (dateObj instanceof LocalDate) {
                endDate = (LocalDate) dateObj;
            }

            list.add(new ContractExpiringResponse(
                    (String) row[0],
                    (String) row[1],
                    endDate,
                    ((Number) row[3]).intValue(),
                    (String) row[4]
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
            id,
            department_name,
            assigned_budget,
            executed_budget,
            available_budget,
            execution_percentage
        FROM vw_budget_execution
        ORDER BY execution_percentage DESC
        """;

        List<Object[]> rows = entityManager
                .createNativeQuery(sql)
                .getResultList();

        return rows.stream()
                .map(row -> new BudgetExecutionResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (BigDecimal) row[2],
                        (BigDecimal) row[3],
                        (BigDecimal) row[4],
                        (BigDecimal) row[5]
                ))
                .toList();
    }
}