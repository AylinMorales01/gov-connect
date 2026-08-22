package com.govconnect.automation.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuración del módulo de automatización.
 * <p>
 * Habilita la ejecución de tareas programadas ({@code @Scheduled}) para la
 * alerta automática de contratos por vencer (G3) y registra las propiedades
 * tipadas {@link ExpiringContractsAlertProperties}.
 * </p>
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(ExpiringContractsAlertProperties.class)
public class AutomationConfig {
}
