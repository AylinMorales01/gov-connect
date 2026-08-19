package com.govconnect.analytics.dto;

import java.math.BigDecimal;

/**
 * DTO para el desglose de contratos por estado.
 */
public record ContractStatusBreakdownResponse(
        String status,
        BigDecimal totalValue,
        long totalCount
) {}
