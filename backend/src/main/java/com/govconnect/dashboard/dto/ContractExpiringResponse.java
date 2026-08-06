package com.govconnect.dashboard.dto;

import java.time.LocalDate;

/**
 * DTO para contratos próximos a vencer.
 * Mapea 1:1 las columnas de {@code vw_contracts_expiring}.
 */
public record ContractExpiringResponse(
        String contractNumber,
        String contractor,
        LocalDate endDate,
        Integer remainingDays
) {}