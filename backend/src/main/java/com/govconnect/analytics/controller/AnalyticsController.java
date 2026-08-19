package com.govconnect.analytics.controller;

import com.govconnect.analytics.dto.ConceptBreakdownResponse;
import com.govconnect.analytics.dto.ContractDepartmentBreakdownResponse;
import com.govconnect.analytics.dto.ContractStatusBreakdownResponse;
import com.govconnect.analytics.dto.DepartmentRankingResponse;
import com.govconnect.analytics.dto.FinancialOverviewResponse;
import com.govconnect.analytics.dto.MonthlyTrendDto;
import com.govconnect.analytics.dto.PaymentMethodBreakdownResponse;
import com.govconnect.analytics.dto.TopContractorResponse;
import com.govconnect.analytics.etl.EtlAsyncService;
import com.govconnect.analytics.etl.EtlTask;
import com.govconnect.analytics.etl.EtlTaskManager;
import com.govconnect.analytics.service.CollectionsBreakdownService;
import com.govconnect.analytics.service.ContractAnalyticsService;
import com.govconnect.analytics.service.DepartmentRankingService;
import com.govconnect.analytics.service.DuckDbHealthService;
import com.govconnect.analytics.service.FinancialOverviewService;
import com.govconnect.analytics.service.TrendAnalyticsService;
import com.govconnect.shared.constants.ApiMessages;
import com.govconnect.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.persistence.EntityNotFoundException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics Engine", description = "Motor analítico de alto rendimiento para Gov Connect")
@SecurityRequirement(name = "Bearer Authentication")
public class AnalyticsController {

    private final DuckDbHealthService healthService;
    private final EtlAsyncService etlAsyncService;
    private final EtlTaskManager etlTaskManager;
    private final TrendAnalyticsService trendService;
    private final FinancialOverviewService overviewService;
    private final DepartmentRankingService rankingService;
    private final CollectionsBreakdownService breakdownService;
    private final ContractAnalyticsService contractAnalyticsService;

    @Operation(summary = "Verificar conexión a DuckDB", description = "Prueba la conexión a la base de datos analítica DuckDB.")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() throws SQLException {
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessages.ANALYTICS_HEALTH_SUCCESS, healthService.test())
        );
    }

    @Operation(summary = "Ejecutar ETL (asíncrono)", description = "Dispara el ETL completo en segundo plano y devuelve un taskId para consultar su estado.")
    @PostMapping("/etl/run")
    public ResponseEntity<ApiResponse<EtlTask>> runEtl() {
        String taskId = etlTaskManager.createTask();
        etlAsyncService.runAsync(taskId);
        return ResponseEntity.accepted()
                .body(ApiResponse.success(
                        ApiMessages.ANALYTICS_ETL_STARTED,
                        etlTaskManager.getTask(taskId).orElse(null)
                ));
    }

    @Operation(summary = "Estado de tarea ETL", description = "Consulta el estado de una tarea ETL disparada de forma asíncrona.")
    @GetMapping("/etl/status/{taskId}")
    public ResponseEntity<ApiResponse<EtlTask>> getEtlStatus(@PathVariable String taskId) {
        EtlTask task = etlTaskManager.getTask(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Tarea ETL no encontrada: " + taskId));
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessages.ANALYTICS_ETL_STATUS, task)
        );
    }

    @Operation(summary = "Tendencia mensual de recaudos", description = "Devuelve la tendencia mensual de recaudos desde DuckDB.")
    @GetMapping("/monthly-trend")
    public ResponseEntity<ApiResponse<List<MonthlyTrendDto>>> getMonthlyTrend() throws SQLException {
        return ResponseEntity.ok(
                ApiResponse.success(
                        ApiMessages.ANALYTICS_MONTHLY_TREND_SUCCESS,
                        trendService.getMonthlyTrend()
                )
        );
    }

    @Operation(summary = "Resumen financiero", description = "Devuelve el resumen analítico financiero desde DuckDB.")
    @GetMapping("/financial-overview")
    public ResponseEntity<ApiResponse<FinancialOverviewResponse>> getFinancialOverview() throws SQLException {
        return ResponseEntity.ok(
                ApiResponse.success(
                        ApiMessages.ANALYTICS_FINANCIAL_OVERVIEW_SUCCESS,
                        overviewService.getFinancialOverview()
                )
        );
    }

    @Operation(summary = "Ranking de dependencias", description = "Devuelve el ranking de dependencias por ejecución presupuestal desde DuckDB.")
    @GetMapping("/department-ranking")
    public ResponseEntity<ApiResponse<List<DepartmentRankingResponse>>> getDepartmentRanking() throws SQLException {
        return ResponseEntity.ok(
                ApiResponse.success(
                        ApiMessages.ANALYTICS_DEPARTMENT_RANKING_SUCCESS,
                        rankingService.getDepartmentRanking()
                )
        );
    }

    @Operation(summary = "Desglose por concepto", description = "Devuelve el recaudo agregado por concepto desde DuckDB.")
    @GetMapping("/collections-by-concept")
    public ResponseEntity<ApiResponse<List<ConceptBreakdownResponse>>> getCollectionsByConcept() throws SQLException {
        return ResponseEntity.ok(
                ApiResponse.success(
                        ApiMessages.ANALYTICS_CONCEPT_BREAKDOWN_SUCCESS,
                        breakdownService.getByConcept()
                )
        );
    }

    @Operation(summary = "Desglose por método de pago", description = "Devuelve el recaudo agregado por método de pago desde DuckDB.")
    @GetMapping("/collections-by-payment-method")
    public ResponseEntity<ApiResponse<List<PaymentMethodBreakdownResponse>>> getCollectionsByPaymentMethod() throws SQLException {
        return ResponseEntity.ok(
                ApiResponse.success(
                        ApiMessages.ANALYTICS_PAYMENT_METHOD_BREAKDOWN_SUCCESS,
                        breakdownService.getByPaymentMethod()
                )
        );
    }

    @Operation(summary = "Contratos por estado", description = "Devuelve el valor total y la cantidad de contratos agrupados por estado desde DuckDB.")
    @GetMapping("/contracts-by-status")
    public ResponseEntity<ApiResponse<List<ContractStatusBreakdownResponse>>> getContractsByStatus() throws SQLException {
        return ResponseEntity.ok(
                ApiResponse.success(
                        ApiMessages.ANALYTICS_CONTRACTS_BY_STATUS_SUCCESS,
                        contractAnalyticsService.getByStatus()
                )
        );
    }

    @Operation(summary = "Valor contratado por dependencia", description = "Devuelve el valor total contratado agrupado por dependencia desde DuckDB.")
    @GetMapping("/contracts-value-by-department")
    public ResponseEntity<ApiResponse<List<ContractDepartmentBreakdownResponse>>> getContractsValueByDepartment() throws SQLException {
        return ResponseEntity.ok(
                ApiResponse.success(
                        ApiMessages.ANALYTICS_CONTRACTS_BY_DEPARTMENT_SUCCESS,
                        contractAnalyticsService.getByDepartment()
                )
        );
    }

    @Operation(summary = "Top contratistas", description = "Devuelve los contratistas con mayor valor total contratado desde DuckDB.")
    @GetMapping("/top-contractors")
    public ResponseEntity<ApiResponse<List<TopContractorResponse>>> getTopContractors() throws SQLException {
        return ResponseEntity.ok(
                ApiResponse.success(
                        ApiMessages.ANALYTICS_TOP_CONTRACTORS_SUCCESS,
                        contractAnalyticsService.getTopContractors()
                )
        );
    }
}