package com.govconnect.automation.controller;

import com.govconnect.automation.dto.AutomationLogRequest;
import com.govconnect.automation.dto.AutomationLogResponse;
import com.govconnect.automation.service.AutomationLogService;
import com.govconnect.shared.constants.ApiMessages;
import com.govconnect.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para el registro y consulta de ejecuciones
 * de automatización desde herramientas externas como n8n.
 */
@RestController
@RequestMapping("/api/v1/automation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Automation", description = "Endpoints de registro y consulta de ejecuciones de automatización")
public class AutomationController {

    private final AutomationLogService service;

    @PostMapping("/logs")
    @Operation(
            summary = "Registra la ejecución de un proceso de automatización",
            description = "Recibe los datos de ejecución de un workflow externo y los almacena en la base de datos transaccional."
    )
    public ResponseEntity<ApiResponse<Void>> registerLog(@RequestBody AutomationLogRequest request) {
        log.info("Recibida solicitud de registro de automatización: process={}, status={}",
                request.process(), request.status());

        service.registerExecution(request);

        return ResponseEntity.ok(
                ApiResponse.success(ApiMessages.AUTOMATION_LOG_STORED, null)
        );
    }

    @GetMapping("/logs")
    @Operation(
            summary = "Obtiene el historial de automatizaciones",
            description = "Devuelve todas las ejecuciones de automatización ordenadas desde la más reciente."
    )
    public ResponseEntity<ApiResponse<List<AutomationLogResponse>>> getLogs() {
        log.info("Consultando historial de automatizaciones");

        List<AutomationLogResponse> logs = service.getAutomationLogs();

        return ResponseEntity.ok(
                ApiResponse.success(ApiMessages.AUTOMATION_LOGS_HISTORY_SUCCESS, logs)
        );
    }
}
