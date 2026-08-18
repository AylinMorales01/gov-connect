package com.govconnect.analytics.etl;

import java.time.LocalDateTime;

/**
 * Estado observable de una tarea ETL asíncrona.
 *
 * @param taskId      identificador único de la tarea (UUID).
 * @param state       estado actual del ciclo de vida.
 * @param message     mensaje de error (solo presente si {@code state == FAILED}).
 * @param startedAt   momento en que la tarea comenzó a ejecutarse (o {@code null}).
 * @param completedAt momento en que la tarea finalizó (o {@code null}).
 */
public record EtlTask(
        String taskId,
        EtlTaskState state,
        String message,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {}
