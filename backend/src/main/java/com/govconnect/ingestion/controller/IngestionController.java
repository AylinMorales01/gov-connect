package com.govconnect.ingestion.controller;

import com.govconnect.ingestion.service.IngestionImportService;
import com.govconnect.ingestion.task.IngestionTask;
import com.govconnect.shared.constants.ApiMessages;
import com.govconnect.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controlador de ingesta de datos operacionales desde CSV.
 * <p>
 * Las importaciones son asíncronas: el endpoint responde {@code 202 Accepted}
 * con el {@code taskId} de la tarea y el cliente sigue su avance por
 * {@code GET /ingestion/status/{taskId}}. Un export de SECOP II tarda decenas de
 * segundos y mantener la petición abierta hacía que el cliente cortara la conexión.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
@Tag(name = "Ingestión de datos", description = "Importación de datos operacionales desde archivos CSV")
public class IngestionController {

    private final IngestionImportService ingestionImportService;

    @Operation(summary = "Importar contratos (CSV SECOP)",
            description = "Encola la importación de un CSV de contratos y devuelve el id de la tarea.")
    @PostMapping("/contracts")
    public ResponseEntity<ApiResponse<IngestionTask>> importContracts(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.accepted().body(ApiResponse.success(
                ApiMessages.INGESTION_CONTRACTS_STARTED, ingestionImportService.submitContracts(file)));
    }

    @Operation(summary = "Importar presupuestos (CSV)",
            description = "Encola la importación de un CSV de presupuestos por dependencia/año.")
    @PostMapping("/budgets")
    public ResponseEntity<ApiResponse<IngestionTask>> importBudgets(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.accepted().body(ApiResponse.success(
                ApiMessages.INGESTION_BUDGETS_STARTED, ingestionImportService.submitBudgets(file)));
    }

    @Operation(summary = "Importar recaudos (CSV)",
            description = "Encola la importación de un CSV de recaudos (eventos append-only).")
    @PostMapping("/collections")
    public ResponseEntity<ApiResponse<IngestionTask>> importCollections(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.accepted().body(ApiResponse.success(
                ApiMessages.INGESTION_COLLECTIONS_STARTED, ingestionImportService.submitCollections(file)));
    }

    @Operation(summary = "Estado de una importación",
            description = "Consulta el avance y el resumen de una importación encolada.")
    @GetMapping("/status/{taskId}")
    public ResponseEntity<ApiResponse<IngestionTask>> getStatus(@PathVariable String taskId) {
        IngestionTask task = ingestionImportService.getTask(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Tarea de importación no encontrada: " + taskId));
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessages.INGESTION_STATUS_SUCCESS, task));
    }
}
