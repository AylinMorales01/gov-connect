package com.govconnect.ingestion.service;

import com.govconnect.ingestion.task.IngestionTask;
import com.govconnect.ingestion.task.IngestionTaskManager;
import com.govconnect.ingestion.task.IngestionTaskState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link IngestionImportService}.
 * Verifica el volcado a disco previo al trabajo asíncrono y el tratamiento del
 * rechazo del executor.
 */
@DisplayName("IngestionImportService — encolado de importaciones")
@ExtendWith(MockitoExtension.class)
class IngestionImportServiceTest {

    @Mock
    private IngestionFileStore fileStore;

    @Mock
    private IngestionTaskManager taskManager;

    @Mock
    private IngestionAsyncService asyncService;

    @InjectMocks
    private IngestionImportService service;

    private static final Path STAGED = Path.of("temporal.csv");

    private MultipartFile file() {
        return new MockMultipartFile("file", "x.csv", "text/csv", "a,b\n1,2\n".getBytes());
    }

    private void stubStagedTask(IngestionTaskState state) {
        when(fileStore.stage(any())).thenReturn(STAGED);
        when(taskManager.createTask()).thenReturn("task-1");
        when(taskManager.getTask("task-1")).thenReturn(Optional.of(
                new IngestionTask("task-1", state, null, null, null, null)));
    }

    @Test
    @DisplayName("submitContracts: vuelca el archivo y lanza el trabajo asíncrono")
    void stagesFileAndLaunchesWorker() {
        stubStagedTask(IngestionTaskState.PENDING);

        IngestionTask task = service.submitContracts(file());

        assertThat(task.taskId()).isEqualTo("task-1");
        assertThat(task.state()).isEqualTo(IngestionTaskState.PENDING);
        verify(fileStore).stage(any());
        verify(asyncService).runContracts("task-1", STAGED);
    }

    @Test
    @DisplayName("submitContracts: si el executor rechaza la tarea, la marca fallida y borra el temporal")
    void handlesRejectedExecution() {
        stubStagedTask(IngestionTaskState.FAILED);
        doThrow(new TaskRejectedException("cola llena"))
                .when(asyncService).runContracts(anyString(), any());

        IngestionTask task = service.submitContracts(file());

        assertThat(task.state()).isEqualTo(IngestionTaskState.FAILED);
        verify(fileStore).deleteQuietly(STAGED);
        verify(taskManager).markFailed(eq("task-1"), contains("capacidad"));
    }
}
