package com.govconnect.analytics.service;

import com.govconnect.analytics.dto.FinancialOverviewResponse;
import com.govconnect.analytics.repository.FinancialAnalyticsRepository;
import com.govconnect.analytics.repository.FinancialAnalyticsRepository.MonthlyAggregates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link FinancialOverviewService}.
 * Verifica cálculo de crecimiento intermensual y derivación de tendencias.
 */
@DisplayName("FinancialOverviewService — resumen financiero")
@ExtendWith(MockitoExtension.class)
class FinancialOverviewServiceTest {

    @Mock
    private FinancialAnalyticsRepository repository;

    @InjectMocks
    private FinancialOverviewService service;

    @Nested
    @DisplayName("Crecimiento intermensual")
    class GrowthCalculation {

        @Test
        @DisplayName("CRECIMIENTO > +5%")
        void shouldDetectGrowth() throws SQLException {
            when(repository.getMonthlyAggregates()).thenReturn(new MonthlyAggregates(
                    "2026-03", new BigDecimal("1200000"),
                    "2026-01", new BigDecimal("800000"),
                    new BigDecimal("1000000"),
                    new BigDecimal("1100000"),
                    new BigDecimal("1000000")     // +10%
            ));

            FinancialOverviewResponse result = service.getFinancialOverview();

            assertThat(result.lastMonthGrowthPercentage()).isGreaterThan(new BigDecimal("5"));
            assertThat(result.trend()).isEqualTo("CRECIMIENTO");
        }

        @Test
        @DisplayName("DESCENSO < -5%")
        void shouldDetectDecline() throws SQLException {
            when(repository.getMonthlyAggregates()).thenReturn(new MonthlyAggregates(
                    "2026-01", new BigDecimal("1200000"),
                    "2026-03", new BigDecimal("900000"),
                    new BigDecimal("1000000"),
                    new BigDecimal("900000"),
                    new BigDecimal("1000000")     // -10%
            ));

            FinancialOverviewResponse result = service.getFinancialOverview();

            assertThat(result.lastMonthGrowthPercentage()).isLessThan(new BigDecimal("0"));
            assertThat(result.trend()).isEqualTo("DESCENSO");
        }

        @Test
        @DisplayName("ESTABLE entre -5% y +5%")
        void shouldDetectStable() throws SQLException {
            when(repository.getMonthlyAggregates()).thenReturn(new MonthlyAggregates(
                    "2026-02", new BigDecimal("1050000"),
                    "2026-01", new BigDecimal("950000"),
                    new BigDecimal("1000000"),
                    new BigDecimal("1020000"),
                    new BigDecimal("1000000")     // +2%
            ));

            FinancialOverviewResponse result = service.getFinancialOverview();

            assertThat(result.trend()).isEqualTo("ESTABLE");
        }

        @Test
        @DisplayName("ESTABLE sin mes anterior (null)")
        void shouldBeStableWithNullPrevious() throws SQLException {
            when(repository.getMonthlyAggregates()).thenReturn(new MonthlyAggregates(
                    "2026-01", new BigDecimal("1000000"),
                    "2026-01", new BigDecimal("1000000"),
                    new BigDecimal("1000000"),
                    new BigDecimal("1000000"),
                    null
            ));

            FinancialOverviewResponse result = service.getFinancialOverview();

            assertThat(result.lastMonthGrowthPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.trend()).isEqualTo("ESTABLE");
        }

        @Test
        @DisplayName("ESTABLE cuando mes anterior es cero")
        void shouldBeStableWithZeroPrevious() throws SQLException {
            when(repository.getMonthlyAggregates()).thenReturn(new MonthlyAggregates(
                    "2026-01", new BigDecimal("1000000"),
                    "2026-01", new BigDecimal("1000000"),
                    new BigDecimal("500000"),
                    new BigDecimal("500000"),
                    BigDecimal.ZERO
            ));

            FinancialOverviewResponse result = service.getFinancialOverview();

            assertThat(result.trend()).isEqualTo("ESTABLE");
        }
    }

    @Nested
    @DisplayName("Mapeo de datos crudos")
    class DataMapping {

        @Test
        @DisplayName("Propaga best/worst months y amounts")
        void shouldPropagateBestAndWorst() throws SQLException {
            when(repository.getMonthlyAggregates()).thenReturn(new MonthlyAggregates(
                    "2026-06", new BigDecimal("2500000"),
                    "2026-02", new BigDecimal("300000"),
                    new BigDecimal("1000000"),
                    new BigDecimal("1500000"),
                    new BigDecimal("1400000")
            ));

            FinancialOverviewResponse result = service.getFinancialOverview();

            assertThat(result.bestCollectionMonth()).isEqualTo("2026-06");
            assertThat(result.bestCollectionAmount()).isEqualByComparingTo(new BigDecimal("2500000"));
            assertThat(result.worstCollectionMonth()).isEqualTo("2026-02");
            assertThat(result.worstCollectionAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        }

        @Test
        @DisplayName("Propaga promedio mensual")
        void shouldPropagateAverage() throws SQLException {
            when(repository.getMonthlyAggregates()).thenReturn(new MonthlyAggregates(
                    "2026-03", new BigDecimal("2000000"),
                    "2026-01", new BigDecimal("1000000"),
                    new BigDecimal("1500000"),
                    new BigDecimal("1600000"),
                    new BigDecimal("1400000")
            ));

            FinancialOverviewResponse result = service.getFinancialOverview();

            assertThat(result.averageMonthlyCollection()).isEqualByComparingTo(new BigDecimal("1500000"));
        }
    }
}
