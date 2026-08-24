package com.govconnect.automation.dto;

import java.util.List;

/**
 * Resumen del resultado de una ejecución de la alerta de contratos por vencer.
 *
 * @param contractsFound contratos por vencer encontrados en la ventana configurada.
 * @param emailsSent     correos enviados (uno por destinatario).
 * @param recipients      destinatarios a los que se intentó enviar la alerta.
 * @param message         descripción del resultado (incluye el motivo cuando no se envió nada).
 */
public record ExpiringContractsAlertResponse(
        int contractsFound,
        int emailsSent,
        List<String> recipients,
        String message
) {
}
