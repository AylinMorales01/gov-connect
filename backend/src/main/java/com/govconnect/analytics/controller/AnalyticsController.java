package com.govconnect.analytics.controller;

import com.govconnect.analytics.dto.DepartmentRankingResponse;
import com.govconnect.analytics.dto.FinancialOverviewResponse;
import com.govconnect.analytics.dto.MonthlyTrendDto;
import com.govconnect.analytics.etl.EtlService;
import com.govconnect.analytics.service.DepartmentRankingService;
import com.govconnect.analytics.service.DuckDbHealthService;
import com.govconnect.analytics.service.FinancialOverviewService;
import com.govconnect.analytics.service.TrendAnalyticsService;
import com.govconnect.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics Engine", description = "Motor analítico de alto rendimiento para SIA Connect")
public class AnalyticsController {

    private final DuckDbHealthService healthService;
    private final EtlService etlService;
    private final TrendAnalyticsService trendService;
    private final FinancialOverviewService overviewService;
    private final DepartmentRankingService rankingService;

    // Endpoint de prueba
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() throws SQLException {
        return ResponseEntity.ok(ApiResponse.success("DuckDB conectado correctamente", healthService.test()));
    }

    // ejecutar el ETL (Extraer -> CSV -> DuckDB)
    @PostMapping("/etl/run")
    public ResponseEntity<ApiResponse<String>> runEtl() throws SQLException {
        etlService.runFullEtl();
        return ResponseEntity.ok(ApiResponse.success("Proceso ETL finalizado correctamente", null));
    }

    //Endpoint de tendencia mensual (Paso 6)
    @GetMapping("/monthly-trend")
    public ResponseEntity<ApiResponse<List<MonthlyTrendDto>>> getMonthlyTrend() throws SQLException {
        return ResponseEntity.ok(ApiResponse.success(
                "Tendencia mensual obtenida correctamente",
                trendService.getMonthlyTrend()
        ));
    }

    @GetMapping("/financial-overview")
    public ResponseEntity<ApiResponse<FinancialOverviewResponse>> getFinancialOverview() throws SQLException {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Resumen analítico obtenido correctamente",
                        overviewService.getFinancialOverview()
                )
        );
    }

    @GetMapping("/department-ranking")
    public ResponseEntity<ApiResponse<List<DepartmentRankingResponse>>> getDepartmentRanking() throws SQLException {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Ranking de dependencias obtenido correctamente",
                        rankingService.getDepartmentRanking()
                )
        );
    }
}