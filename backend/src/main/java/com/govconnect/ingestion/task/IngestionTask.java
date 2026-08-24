package com.govconnect.ingestion.task;

import com.govconnect.ingestion.dto.ImportSummaryResponse;

import java.time.LocalDateTime;

/**
 * Estado observable de una importación asíncrona.
 *
 * @param taskId      identificador único de la tarea (UUID).
 * @param state       estado actual del ciclo de vida.
 * @param message     mensaje de error (solo presente si {@code state == FAILED}).
 * @param startedAt   momento en que la importación comenzó (o {@code null}).
 * @param completedAt momento en que la importación finalizó (o {@code null}).
 * @param summary     resumen de la importación, disponible solo en
 *                    {@link IngestionTaskState#COMPLETED}; incluye el
 *                    {@code etlTaskId} para encadenar el refresco de DuckDB.
 */
public record IngestionTask(
        String taskId,
        IngestionTaskState state,
        String message,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        ImportSummaryResponse summary
) {}
