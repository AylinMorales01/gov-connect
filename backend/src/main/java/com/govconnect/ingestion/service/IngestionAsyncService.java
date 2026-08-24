package com.govconnect.ingestion.service;

import com.govconnect.analytics.etl.EtlAsyncService;
import com.govconnect.analytics.etl.EtlTaskManager;
import com.govconnect.ingestion.dto.ImportSummaryResponse;
import com.govconnect.ingestion.task.IngestionTaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * Orquestador asíncrono de las importaciones de CSV.
 * <p>
 * Ejecuta la importación en un hilo del executor {@code ingestionExecutor} para
 * que la petición HTTP pueda responder de inmediato con un {@code taskId}: un
 * export de SECOP II tarda decenas de segundos y el cliente cortaba la conexión
 * antes de recibir la respuesta.
 * </p>
 * <p>
 * El ETL que refresca DuckDB se dispara aquí, ya fuera de la transacción de
 * {@link IngestionService}, para evitar la carrera entre el commit y el hilo
 * del ETL. El estado se publica en {@link IngestionTaskManager}.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionAsyncService {

    private final IngestionService ingestionService;
    private final IngestionTaskManager taskManager;
    private final IngestionFileStore fileStore;
    private final EtlTaskManager etlTaskManager;
    private final EtlAsyncService etlAsyncService;

    /**
     * Importa contratos de forma asíncrona.
     * <p>
     * <b>Importante:</b> se invoca desde {@link IngestionImportService} a través
     * del proxy de Spring (nunca por auto-invocación), para que {@link Async}
     * surta efecto.
     * </p>
     */
    @Async("ingestionExecutor")
    public void runContracts(String taskId, Path file) {
        run(taskId, file, "contratos", ingestionService::importContracts);
    }

    /** Importa presupuestos de forma asíncrona. */
    @Async("ingestionExecutor")
    public void runBudgets(String taskId, Path file) {
        run(taskId, file, "presupuestos", ingestionService::importBudgets);
    }

    /** Importa recaudos de forma asíncrona. */
    @Async("ingestionExecutor")
    public void runCollections(String taskId, Path file) {
        run(taskId, file, "recaudos", ingestionService::importCollections);
    }

    /**
     * Ciclo común: marcar en ejecución, importar, encadenar el ETL, publicar el
     * resultado y borrar siempre el temporal.
     */
    private void run(String taskId, Path file, String kind,
                     Function<Path, ImportSummaryResponse> importer) {
        taskManager.markRunning(taskId);
        log.info("Importación asíncrona de {} iniciada para la tarea {}", kind, taskId);

        try {
            ImportSummaryResponse summary = triggerEtlIfNeeded(importer.apply(file));
            taskManager.markCompleted(taskId, summary);
            log.info("Importación asíncrona de {} completada para la tarea {}: {} importados, {} actualizados, {} omitidos",
                    kind, taskId, summary.imported(), summary.updated(), summary.skipped());
        } catch (Exception e) {
            taskManager.markFailed(taskId, e.getMessage());
            log.error("Importación asíncrona de {} falló para la tarea {}: {}", kind, taskId, e.getMessage(), e);
        } finally {
            fileStore.deleteQuietly(file);
        }
    }

    /**
     * Dispara el ETL solo si la importación tocó algún registro, y devuelve el
     * resumen enriquecido con el {@code etlTaskId} para que el cliente pueda
     * seguir el refresco de DuckDB.
     */
    private ImportSummaryResponse triggerEtlIfNeeded(ImportSummaryResponse result) {
        if (result.imported() + result.updated() == 0) {
            return result;
        }
        String etlTaskId = etlTaskManager.createTask();
        etlAsyncService.runAsync(etlTaskId);
        return new ImportSummaryResponse(
                result.totalRows(), result.imported(), result.updated(),
                result.skipped(), result.errors(), etlTaskId);
    }
}
