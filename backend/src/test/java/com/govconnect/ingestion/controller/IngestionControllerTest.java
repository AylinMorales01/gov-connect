package com.govconnect.ingestion.controller;

import com.govconnect.ingestion.dto.ImportSummaryResponse;
import com.govconnect.ingestion.service.IngestionImportService;
import com.govconnect.ingestion.task.IngestionTask;
import com.govconnect.ingestion.task.IngestionTaskState;
import com.govconnect.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests del controller de ingesta (standalone MockMvc, sin contexto de Spring).
 * Verifica el binding multipart, la respuesta 202 con la tarea encolada y la
 * consulta de estado.
 */
@DisplayName("IngestionController — encolado y consulta de estado")
class IngestionControllerTest {

    private final IngestionImportService importService = mock(IngestionImportService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        IngestionController controller = new IngestionController(importService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "x.csv", "text/csv", "a,b\n1,2\n".getBytes());
    }

    private IngestionTask pendingTask() {
        return new IngestionTask("task-123", IngestionTaskState.PENDING, null, null, null, null);
    }

    @Test
    @DisplayName("importContracts: responde 202 con la tarea encolada")
    void importsContractsReturnsAcceptedTask() throws Exception {
        when(importService.submitContracts(any())).thenReturn(pendingTask());

        mockMvc.perform(multipart("/api/v1/ingestion/contracts").file(file()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value("task-123"))
                .andExpect(jsonPath("$.data.state").value("PENDING"));

        verify(importService).submitContracts(any());
    }

    @Test
    @DisplayName("getStatus: devuelve el resumen cuando la importación terminó")
    void returnsCompletedSummary() throws Exception {
        ImportSummaryResponse summary =
                new ImportSummaryResponse(10, 8, 1, 1, List.of("fila 2: estado no reconocido"), "etl-9");
        when(importService.getTask("task-123")).thenReturn(Optional.of(new IngestionTask(
                "task-123", IngestionTaskState.COMPLETED, null,
                LocalDateTime.now(), LocalDateTime.now(), summary)));

        mockMvc.perform(get("/api/v1/ingestion/status/task-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("COMPLETED"))
                .andExpect(jsonPath("$.data.summary.imported").value(8))
                .andExpect(jsonPath("$.data.summary.etlTaskId").value("etl-9"));
    }

    @Test
    @DisplayName("getStatus: responde 404 si la tarea no existe")
    void returnsNotFoundForUnknownTask() throws Exception {
        when(importService.getTask("desconocida")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/ingestion/status/desconocida"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
