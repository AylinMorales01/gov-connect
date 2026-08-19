package com.govconnect.analytics.etl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Orquestador asíncrono del ETL.
 * <p>
 * Permite disparar {@link EtlService#runFullEtl()} en un hilo separado
 * (executor {@code etlExecutor}) y devolver de inmediato un {@code taskId},
 * sin bloquear la petición HTTP que lo invoca.
 * </p>
 * <p>
 * El estado de la tarea se registra en {@link EtlTaskManager}, que es
 * consultado posteriormente vía {@code GET /analytics/etl/status/{taskId}}.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EtlAsyncService {

    private final EtlService etlService;
    private final EtlTaskManager taskManager;

    /**
     * Ejecuta el ETL completo de forma asíncrona.
     * <p>
     * <b>Importante:</b> este método se invoca desde el controller a través
     * del proxy de Spring (nunca por auto-invocación), para que
     * {@link Async} surta efecto.
     * </p>
     *
     * @param taskId identificador de la tarea creada por {@link EtlTaskManager}.
     */
    @Async("etlExecutor")
    public void runAsync(String taskId) {
        taskManager.markRunning(taskId);
        log.info("ETL asíncrono iniciado para la tarea {}", taskId);

        try {
            etlService.runFullEtl();
            taskManager.markCompleted(taskId);
            log.info("ETL asíncrono completado para la tarea {}", taskId);
        } catch (Exception e) {
            taskManager.markFailed(taskId, e.getMessage());
            log.error("ETL asíncrono falló para la tarea {}: {}", taskId, e.getMessage(), e);
        }
    }
}
