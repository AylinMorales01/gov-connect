package com.govconnect.ingestion.task;

import com.govconnect.ingestion.dto.ImportSummaryResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro en memoria del estado de las importaciones asíncronas.
 * <p>
 * Mantiene un mapa {@code taskId → IngestionTask} thread-safe. Es suficiente
 * para un despliegue de instancia única (MVP); en un despliegue distribuido
 * este estado debería vivir en una caché compartida o en la base de datos.
 * </p>
 */
@Component
public class IngestionTaskManager {

    private final ConcurrentHashMap<String, IngestionTask> tasks = new ConcurrentHashMap<>();

    /**
     * Crea una tarea en estado {@link IngestionTaskState#PENDING} y devuelve su id.
     */
    public String createTask() {
        String id = UUID.randomUUID().toString();
        tasks.put(id, new IngestionTask(id, IngestionTaskState.PENDING, null, null, null, null));
        return id;
    }

    /**
     * Marca la tarea como {@link IngestionTaskState#RUNNING} y registra su inicio.
     */
    public void markRunning(String taskId) {
        tasks.computeIfPresent(taskId, (id, t) ->
                new IngestionTask(id, IngestionTaskState.RUNNING, null, LocalDateTime.now(), null, null));
    }

    /**
     * Marca la tarea como {@link IngestionTaskState#COMPLETED} y adjunta el resumen.
     */
    public void markCompleted(String taskId, ImportSummaryResponse summary) {
        tasks.computeIfPresent(taskId, (id, t) ->
                new IngestionTask(id, IngestionTaskState.COMPLETED, null,
                        t.startedAt(), LocalDateTime.now(), summary));
    }

    /**
     * Marca la tarea como {@link IngestionTaskState#FAILED} con su mensaje de error.
     */
    public void markFailed(String taskId, String message) {
        tasks.computeIfPresent(taskId, (id, t) ->
                new IngestionTask(id, IngestionTaskState.FAILED, message,
                        t.startedAt(), LocalDateTime.now(), null));
    }

    /**
     * Obtiene el estado actual de una tarea, si existe.
     */
    public Optional<IngestionTask> getTask(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }
}
