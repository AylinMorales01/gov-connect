package com.govconnect.ingestion.service;

import com.govconnect.analytics.etl.EtlAsyncService;
import com.govconnect.analytics.etl.EtlTaskManager;
import com.govconnect.ingestion.dto.ImportSummaryResponse;
import com.govconnect.ingestion.task.IngestionTaskManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link IngestionAsyncService}.
 * Verifica el ciclo de vida de la tarea, el encadenado condicional del ETL y la
 * limpieza del archivo temporal.
 */
@DisplayName("IngestionAsyncService — ciclo de la importación asíncrona")
@ExtendWith(MockitoExtension.class)
class IngestionAsyncServiceTest {

    @Mock
    private IngestionService ingestionService;

    @Mock
    private IngestionTaskManager taskManager;

    @Mock
    private IngestionFileStore fileStore;

    @Mock
    private EtlTaskManager etlTaskManager;

    @Mock
    private EtlAsyncService etlAsyncService;

    @InjectMocks
    private IngestionAsyncService service;

    private static final Path CSV = Path.of("temporal.csv");

    @Test
    @DisplayName("runContracts: publica el resumen y dispara el ETL cuando hay registros")
    void completesAndTriggersEtl() {
        when(ingestionService.importContracts(CSV))
                .thenReturn(new ImportSummaryResponse(2, 2, 0, 0, List.of(), null));
        when(etlTaskManager.createTask()).thenReturn("etl-1");

        service.runContracts("task-1", CSV);

        verify(taskManager).markRunning("task-1");
        verify(etlAsyncService).runAsync("etl-1");

        ArgumentCaptor<ImportSummaryResponse> summary = ArgumentCaptor.forClass(ImportSummaryResponse.class);
        verify(taskManager).markCompleted(eq("task-1"), summary.capture());
        assertThat(summary.getValue().etlTaskId()).isEqualTo("etl-1");
        verify(fileStore).deleteQuietly(CSV);
    }

    @Test
    @DisplayName("runContracts: no dispara el ETL si no se procesó ningún registro")
    void doesNotTriggerEtlWhenNothingProcessed() {
        when(ingestionService.importContracts(CSV))
                .thenReturn(new ImportSummaryResponse(1, 0, 0, 1, List.of("fila 2: estado no reconocido"), null));

        service.runContracts("task-2", CSV);

        verify(etlTaskManager, never()).createTask();
        verify(etlAsyncService, never()).runAsync(anyString());

        ArgumentCaptor<ImportSummaryResponse> summary = ArgumentCaptor.forClass(ImportSummaryResponse.class);
        verify(taskManager).markCompleted(eq("task-2"), summary.capture());
        assertThat(summary.getValue().etlTaskId()).isNull();
        verify(fileStore).deleteQuietly(CSV);
    }

    @Test
    @DisplayName("runContracts: marca la tarea como fallida y limpia el temporal ante una excepción")
    void marksFailedAndCleansUp() {
        when(ingestionService.importContracts(CSV))
                .thenThrow(new IllegalStateException("columnas faltantes"));

        service.runContracts("task-3", CSV);

        verify(taskManager).markFailed("task-3", "columnas faltantes");
        verify(taskManager, never()).markCompleted(anyString(), any());
        verify(fileStore).deleteQuietly(CSV);
    }

    @Test
    @DisplayName("runBudgets y runCollections delegan en su importación correspondiente")
    void delegatesToTheRightImport() {
        when(ingestionService.importBudgets(CSV))
                .thenReturn(new ImportSummaryResponse(1, 0, 0, 1, List.of(), null));
        when(ingestionService.importCollections(CSV))
                .thenReturn(new ImportSummaryResponse(1, 0, 0, 1, List.of(), null));

        service.runBudgets("task-4", CSV);
        service.runCollections("task-5", CSV);

        verify(ingestionService).importBudgets(CSV);
        verify(ingestionService).importCollections(CSV);
        verify(ingestionService, never()).importContracts(any());
    }
}
