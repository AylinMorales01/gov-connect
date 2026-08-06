package com.govconnect.dashboard.dto;

import java.math.BigDecimal;

/**
 * DTO para ejecución presupuestal por dependencia.
 * Mapea 1:1 las columnas de {@code vw_budget_execution}.
 */
public record BudgetExecutionResponse(
        String department,
        BigDecimal assignedBudget,
        BigDecimal executedBudget,
        BigDecimal percentage
) {}