package com.govconnect.analytics.dto;

import java.math.BigDecimal;

/**
 * DTO para el valor contratado por dependencia.
 */
public record ContractDepartmentBreakdownResponse(
        String department,
        BigDecimal totalValue,
        long totalCount
) {}
