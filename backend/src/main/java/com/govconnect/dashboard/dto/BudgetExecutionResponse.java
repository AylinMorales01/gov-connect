package com.govconnect.dashboard.dto;

import java.math.BigDecimal;

public record BudgetExecutionResponse(

        Long departmentId,
        String department,
        BigDecimal assignedBudget,
        BigDecimal executedBudget,
        BigDecimal availableBudget,
        BigDecimal executionPercentage

) {}