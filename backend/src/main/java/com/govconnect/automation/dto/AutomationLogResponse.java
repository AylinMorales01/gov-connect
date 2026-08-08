package com.govconnect.automation.dto;

import java.time.LocalDateTime;

/**
 * DTO que representa un registro de ejecución de automatización
 * recuperado del historial almacenado en {@code automation_logs}.
 */
public record AutomationLogResponse(
        Long id,
        Long userId,
        String process,
        String status,
        String message,
        Integer executionTimeMs,
        LocalDateTime createdAt
) {}