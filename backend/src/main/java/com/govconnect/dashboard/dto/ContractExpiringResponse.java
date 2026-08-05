package com.govconnect.dashboard.dto;

import java.time.LocalDate;

public record ContractExpiringResponse(
        String contractNumber,
        String contractor,
        LocalDate endDate,
        Integer remainingDays,
        String status
) {}