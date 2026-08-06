package com.govconnect.analytics.service;

import com.govconnect.analytics.dto.FinancialOverviewResponse;
import com.govconnect.analytics.repository.FinancialAnalyticsRepository;
import com.govconnect.analytics.repository.FinancialAnalyticsRepository.MonthlyAggregates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;

/**
 * Servicio de resumen financiero analítico.
 * <p>
 * Responsable de procesar los datos crudos del repositorio y aplicar
 * la lógica de negocio: cálculo de crecimiento intermensual y
 * derivación de tendencias.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class FinancialOverviewService {

    private final FinancialAnalyticsRepository repository;

    /** Umbral para considerar tendencia de crecimiento. */
    private static final BigDecimal GROWTH_THRESHOLD = new BigDecimal("5");

    /**
     * Construye el resumen financiero completo a partir de los
     * agregados mensuales crudos obtenidos del repositorio.
     *
     * @return respuesta lista para serializar al cliente
     * @throws SQLException si falla la consulta subyacente
     */
    public FinancialOverviewResponse getFinancialOverview() throws SQLException {
        MonthlyAggregates raw = repository.getMonthlyAggregates();

        GrowthResult growth = calculateGrowth(raw.currentMonthAmount(), raw.previousMonthAmount());

        return new FinancialOverviewResponse(
                raw.bestMonth(),
                raw.bestAmount(),
                raw.worstMonth(),
                raw.worstAmount(),
                raw.average(),
                growth.percentage(),
                growth.trend()
        );
    }

    /**
     * Calcula el porcentaje de crecimiento intermensual y deriva la
     * etiqueta de tendencia a partir de los umbrales de negocio.
     *
     * <ul>
     *   <li>+5% o más  → {@code CRECIMIENTO}</li>
     *   <li>-5% o menos → {@code DESCENSO}</li>
     *   <li>Entre -5% y +5% → {@code ESTABLE}</li>
     * </ul>
     *
     * @param current  monto del mes actual
     * @param previous monto del mes anterior
     * @return porcentaje calculado y etiqueta de tendencia
     */
    private GrowthResult calculateGrowth(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) <= 0) {
            return new GrowthResult(BigDecimal.ZERO, "ESTABLE");
        }

        BigDecimal difference = current.subtract(previous);
        BigDecimal percentage = difference
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        String trend;
        if (percentage.compareTo(GROWTH_THRESHOLD) > 0) {
            trend = "CRECIMIENTO";
        } else if (percentage.compareTo(GROWTH_THRESHOLD.negate()) < 0) {
            trend = "DESCENSO";
        } else {
            trend = "ESTABLE";
        }

        return new GrowthResult(percentage, trend);
    }

    /**
     * Resultado interno del cálculo de crecimiento.
     */
    private record GrowthResult(BigDecimal percentage, String trend) {}
}