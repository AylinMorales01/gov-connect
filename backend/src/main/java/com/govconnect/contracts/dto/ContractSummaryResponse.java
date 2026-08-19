package com.govconnect.contracts.dto;

import java.math.BigDecimal;

/**
 * DTO con métricas agregadas del catálogo de contratos.
 */
public record ContractSummaryResponse(
        int totalContracts,
        BigDecimal totalValue,
        int activeContracts,
        int suspendedContracts,
        int finishedContracts
) {}
