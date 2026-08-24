package com.govconnect.automation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Propiedades tipadas de la alerta de contratos por vencer (G3).
 * <p>
 * Se enlazan al prefijo {@code app.alert.expiring-contracts} definido en
 * {@code application.yaml}. Los valores por defecto viven en ese archivo,
 * por lo que aquí solo se declaran los campos.
 * </p>
 *
 * @param enabled    habilita la alerta automática programada (no afecta el disparo manual).
 * @param recipients destinatarios de la alerta (lista separada por coma en la configuración).
 * @param days       ventana de días para considerar un contrato "por vencer".
 * @param from       remitente de los correos.
 * @param cron       expresión cron de Spring para la ejecución programada.
 */
@ConfigurationProperties(prefix = "app.alert.expiring-contracts")
public record ExpiringContractsAlertProperties(
        boolean enabled,
        List<String> recipients,
        int days,
        String from,
        String cron
) {
}
