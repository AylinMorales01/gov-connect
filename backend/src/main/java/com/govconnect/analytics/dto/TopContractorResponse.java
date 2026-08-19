package com.govconnect.analytics.dto;

import java.math.BigDecimal;

/**
 * DTO para el ranking de contratistas por valor contratado.
 */
public record TopContractorResponse(
        String contractor,
        BigDecimal totalValue,
        long contractCount
) {}
