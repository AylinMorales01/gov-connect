package com.govconnect.analytics.dto;

import java.math.BigDecimal;

/**
 * DTO para la tendencia mensual de recaudos.
 * Mapea 1:1 las columnas de la consulta en {@code TrendAnalyticsService}.
 */
public record MonthlyTrendDto(
        String month,
        BigDecimal amount
) {}