package com.govconnect.automation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para un contrato por vencer incluido en la alerta de correo.
 * Mapea 1:1 las columnas de la consulta en {@code ExpiringContractsRepository}.
 */
public record ExpiringContractAlertItem(
        String contractNumber,
        String contractorName,
        String object,
        BigDecimal contractValue,
        LocalDate endDate,
        Integer remainingDays,
        String department
) {
}
