package com.govconnect.automation.dto;

/**
 * DTO que representa la solicitud de registro de una ejecución
 * de automatización enviada por herramientas externas como n8n.
 */
public record AutomationLogRequest(
        Long userId,
        String process,
        String status,
        String message,
        Integer executionTimeMs
) {}