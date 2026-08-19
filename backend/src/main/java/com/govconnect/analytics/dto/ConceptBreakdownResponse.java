package com.govconnect.analytics.dto;

import java.math.BigDecimal;

/**
 * DTO para el desglose de recaudos por concepto.
 */
public record ConceptBreakdownResponse(
        String concept,
        BigDecimal totalAmount,
        long totalCount
) {}
