package com.govconnect.dashboard.dto;

import java.math.BigDecimal;

public record MonthlyCollectionResponse(
        Integer monthNumber,
        String month,
        BigDecimal amount
) {}