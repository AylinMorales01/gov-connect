package com.govconnect.analytics.etl;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro en memoria del estado de las tareas ETL asíncronas.
 * <p>
 * Mantiene un mapa {@code taskId → EtlTask} thread-safe. Es suficiente
 * para un despliegue de instancia única (MVP). Para un despliegue
 * distribuido, este estado debería persistirse en una caché compartida
 * (p. ej. Redis) o en la base de datos.
 * </p>
 */
@Component
public class EtlTaskManager {

    private final ConcurrentHashMap<String, EtlTask> tasks = new ConcurrentHashMap<>();

    /**
     * Crea una tarea en estado {@link EtlTaskState#PENDING} y devuelve su id.
     */
    public String createTask() {
        String id = UUID.randomUUID().toString();
        tasks.put(id, new EtlTask(id, EtlTaskState.PENDING, null, null, null));
        return id;
    }

    /**
     * Marca la tarea como {@link EtlTaskState#RUNNING} y registra su inicio.
     */
    public void markRunning(String taskId) {
        tasks.computeIfPresent(taskId, (id, t) ->
                new EtlTask(id, EtlTaskState.RUNNING, null, LocalDateTime.now(), null));
    }

    /**
     * Marca la tarea como {@link EtlTaskState#COMPLETED} y registra su fin.
     */
    public void markCompleted(String taskId) {
        tasks.computeIfPresent(taskId, (id, t) ->
                new EtlTask(id, EtlTaskState.COMPLETED, null, t.startedAt(), LocalDateTime.now()));
    }

    /**
     * Marca la tarea como {@link EtlTaskState#FAILED} con su mensaje de error.
     */
    public void markFailed(String taskId, String message) {
        tasks.computeIfPresent(taskId, (id, t) ->
                new EtlTask(id, EtlTaskState.FAILED, message, t.startedAt(), LocalDateTime.now()));
    }

    /**
     * Obtiene el estado actual de una tarea, si existe.
     */
    public Optional<EtlTask> getTask(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }
}
