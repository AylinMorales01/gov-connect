package com.govconnect.analytics.dto;

import java.math.BigDecimal;

public record FinancialOverviewResponse(
        String bestCollectionMonth,
        BigDecimal bestCollectionAmount,
        String worstCollectionMonth,
        BigDecimal worstCollectionAmount,
        BigDecimal averageMonthlyCollection,
        BigDecimal lastMonthGrowthPercentage,
        String trend
) {}
