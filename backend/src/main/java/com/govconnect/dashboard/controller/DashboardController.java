package com.govconnect.dashboard.controller;

import com.govconnect.dashboard.dto.*;
import com.govconnect.dashboard.service.DashboardQueryService;
import com.govconnect.shared.constants.ApiMessages;
import com.govconnect.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Ejecutivo", description = "Endpoints analíticos del sistema SIA Connect")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardQueryService service;

    @GetMapping("/summary")
    @Operation(summary = "Obtiene el resumen ejecutivo", description = "Devuelve métricas globales clave para la toma de decisiones gerenciales.")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> getSummary() {
        log.info("Consultando resumen ejecutivo");
        return ResponseEntity.ok(
                ApiResponse.success(
                        ApiMessages.DASHBOARD_SUMMARY_SUCCESS,
                        service.getSummary()
                )
        );
    }

    @GetMapping("/monthly-collections")
    @Operation(summary = "Obtiene el recaudo mensual", description = "Devuelve el histórico de recaudos agrupado por mes.")
    public ResponseEntity<ApiResponse<List<MonthlyCollectionResponse>>> getMonthlyCollections() {
        log.info("Consultando recaudo mensual");
        return ResponseEntity.ok(
                ApiResponse.success(
                        ApiMessages.MONTHLY_COLLECTIONS_SUCCESS,
                        service.getMonthlyCollections()
                )
        );
    }

    @GetMapping("/expiring-contracts")
    @Operation(summary = "Obtiene los contratos próximos a vencer", description = "Lista los contratos activos que vencen en los próximos 30 días.")
    public ResponseEntity<ApiResponse<List<ContractExpiringResponse>>> getContractsExpiring() {
        log.info("Consultando contratos próximos a vencer");
        return ResponseEntity.ok(
                ApiResponse.success(
                        ApiMessages.CONTRACTS_EXPIRING_SUCCESS,
                        service.getContractsExpiring()
                )
        );
    }

    @GetMapping("/budget-execution")
    @Operation(summary = "Obtiene la ejecución presupuestal por dependencia", description = "Devuelve el porcentaje y montos de ejecución presupuestal.")
    public ResponseEntity<ApiResponse<List<BudgetExecutionResponse>>> getBudgetExecution() {
        log.info("Consultando ejecución presupuestal");
        return ResponseEntity.ok(
                ApiResponse.success(
                        ApiMessages.BUDGET_EXECUTION_SUCCESS,
                        service.getBudgetExecution()
                )
        );
    }
}