package com.govconnect.ingestion.service;

import com.govconnect.ingestion.task.IngestionTask;
import com.govconnect.ingestion.task.IngestionTaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Punto de entrada de las importaciones: vuelca el archivo a disco, registra la
 * tarea y lanza el trabajo asíncrono.
 * <p>
 * La copia del multipart ocurre aquí, en el hilo de la petición, porque su
 * almacenamiento temporal se libera al responder y el worker ya no podría leerlo.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionImportService {

    private final IngestionFileStore fileStore;
    private final IngestionTaskManager taskManager;
    private final IngestionAsyncService asyncService;

    /** Encola la importación de un CSV de contratos. */
    public IngestionTask submitContracts(MultipartFile file) {
        return submit(file, "contratos", asyncService::runContracts);
    }

    /** Encola la importación de un CSV de presupuestos. */
    public IngestionTask submitBudgets(MultipartFile file) {
        return submit(file, "presupuestos", asyncService::runBudgets);
    }

    /** Encola la importación de un CSV de recaudos. */
    public IngestionTask submitCollections(MultipartFile file) {
        return submit(file, "recaudos", asyncService::runCollections);
    }

    /**
     * Consulta el estado de una importación.
     */
    public Optional<IngestionTask> getTask(String taskId) {
        return taskManager.getTask(taskId);
    }

    private IngestionTask submit(MultipartFile file, String kind, BiConsumer<String, Path> launcher) {
        Path staged = fileStore.stage(file);
        String taskId = taskManager.createTask();
        try {
            launcher.accept(taskId, staged);
        } catch (TaskRejectedException e) {
            // El executor usa AbortPolicy: sin este tratamiento la tarea se
            // quedaría en PENDING para siempre y el cliente haría polling en vano.
            fileStore.deleteQuietly(staged);
            taskManager.markFailed(taskId,
                    "No hay capacidad para procesar la importación en este momento. Intente de nuevo en unos minutos.");
            log.warn("Importación de {} rechazada por el executor: {}", kind, e.getMessage());
        }
        return taskManager.getTask(taskId).orElseThrow();
    }
}
