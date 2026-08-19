package com.govconnect.contracts.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para un contrato del catálogo de gestión.
 * Mapea 1:1 las columnas de la consulta en {@code ContractRepository}.
 */
public record ContractResponse(
        Long id,
        String contractNumber,
        String contractorName,
        String object,
        BigDecimal contractValue,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String department
) {}
