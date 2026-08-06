package com.govconnect.dashboard.dto;

import java.math.BigDecimal;

/**
 * DTO para el resumen ejecutivo del dashboard.
 * Mapea 1:1 las columnas de {@code vw_dashboard_summary}.
 */
public record DashboardSummaryDTO(
        BigDecimal collectionsThisMonth,
        Integer activeContracts,
        Integer contractsExpiring,
        BigDecimal budgetExecutionPercentage
) {}