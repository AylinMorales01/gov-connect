package com.govconnect.analytics.controller;

import com.govconnect.analytics.dto.DepartmentRankingResponse;
import com.govconnect.analytics.dto.FinancialOverviewResponse;
import com.govconnect.analytics.dto.MonthlyTrendDto;
import com.govconnect.analytics.etl.EtlAsyncService;
import com.govconnect.analytics.etl.EtlTask;
import com.govconnect.analytics.etl.EtlTaskManager;
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
}