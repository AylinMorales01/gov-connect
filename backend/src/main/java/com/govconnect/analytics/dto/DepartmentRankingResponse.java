package com.govconnect.analytics.dto;

import java.math.BigDecimal;

public record DepartmentRankingResponse(
        Integer position,
        String department,
        BigDecimal executionPercentage,
        BigDecimal totalCollections,
        BigDecimal score
) {}