package com.govconnect.analytics.dto;

import java.math.BigDecimal;

/**
 * DTO para el desglose de recaudos por método de pago.
 */
public record PaymentMethodBreakdownResponse(
        String paymentMethod,
        BigDecimal totalAmount,
        long totalCount
) {}
