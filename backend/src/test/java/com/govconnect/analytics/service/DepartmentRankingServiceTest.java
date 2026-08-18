package com.govconnect.analytics.service;

import com.govconnect.analytics.dto.DepartmentRankingResponse;
import com.govconnect.analytics.repository.DepartmentRankingRepository;
import com.govconnect.analytics.repository.DepartmentRankingRepository.DepartmentRawData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link DepartmentRankingService}.
 * Verifica el cálculo de scores (60% ejecución + 40% recaudo normalizado)
 * y la asignación de posiciones.
 */
@DisplayName("DepartmentRankingService — ranking de dependencias")
@ExtendWith(MockitoExtension.class)
class DepartmentRankingServiceTest {

    @Mock
    private DepartmentRankingRepository repository;

    @InjectMocks
    private DepartmentRankingService service;

    // ── Datos vacíos ────────────────────────────────────

    @Nested
    @DisplayName("Conjunto vacío")
    class EmptyData {

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay dependencias")
        void shouldReturnEmptyList() throws SQLException {
            when(repository.getRawRankingData()).thenReturn(Collections.emptyList());

            List<DepartmentRankingResponse> result = service.getDepartmentRanking();

            assertThat(result).isEmpty();
        }
    }

    // ── Cálculo de scores ──────────────────────────────

    @Nested
    @DisplayName("Cálculo de puntuación")
    class ScoreCalculation {

        @Test
        @DisplayName("Debe asignar posición 1 a la dependencia con mayor score")
        void shouldRankByScore() throws SQLException {
            List<DepartmentRawData> raw = List.of(
                    new DepartmentRawData("Hacienda", new BigDecimal("95.00"), new BigDecimal("500000")),
                    new DepartmentRawData("Salud", new BigDecimal("60.00"), new BigDecimal("300000"))
            );
            when(repository.getRawRankingData()).thenReturn(raw);

            List<DepartmentRankingResponse> result = service.getDepartmentRanking();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).position()).isEqualTo(1);
            assertThat(result.get(0).department()).isEqualTo("Hacienda");
            assertThat(result.get(1).position()).isEqualTo(2);
            assertThat(result.get(1).department()).isEqualTo("Salud");
        }

        @Test
        @DisplayName("Debe calcular score como 60% ejecución + 40% recaudo normalizado")
        void shouldCalculateWeightedScore() throws SQLException {
            // Hacienda: 100% ejecución, recaudo máximo → score cercano a 100
            DepartmentRawData dept = new DepartmentRawData(
                    "Hacienda", new BigDecimal("100.00"), new BigDecimal("1000000")
            );
            when(repository.getRawRankingData()).thenReturn(List.of(dept));

            List<DepartmentRankingResponse> result = service.getDepartmentRanking();

            assertThat(result).hasSize(1);
            // 60% de 100 = 60, 40% de 100 (normalizado al máximo) = 40 → ~100
            assertThat(result.get(0).score()).isGreaterThan(new BigDecimal("90"));
            assertThat(result.get(0).score()).isLessThanOrEqualTo(new BigDecimal("101"));
        }

        @Test
        @DisplayName("Debe manejar ejecución 0% correctamente")
        void shouldHandleZeroExecution() throws SQLException {
            DepartmentRawData dept = new DepartmentRawData(
                    "Cultura", BigDecimal.ZERO, new BigDecimal("100000")
            );
            when(repository.getRawRankingData()).thenReturn(List.of(dept));

            List<DepartmentRankingResponse> result = service.getDepartmentRanking();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).executionPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
            // Score = 60%*0 + 40%*100 = 40
            assertThat(result.get(0).score()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Debe manejar recaudo total cero")
        void shouldHandleZeroCollections() throws SQLException {
            DepartmentRawData dept = new DepartmentRawData(
                    "Planeación", new BigDecimal("80.00"), BigDecimal.ZERO
            );
            when(repository.getRawRankingData()).thenReturn(List.of(dept));

            List<DepartmentRankingResponse> result = service.getDepartmentRanking();

            assertThat(result).hasSize(1);
            // 60%*80 + 40%*0 = 48.0
            assertThat(result.get(0).score()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result.get(0).totalCollections()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ── Ordenamiento ────────────────────────────────────

    @Nested
    @DisplayName("Ordenamiento")
    class Sorting {

        @Test
        @DisplayName("Debe ordenar por score descendente")
        void shouldSortDescendingByScore() throws SQLException {
            List<DepartmentRawData> raw = List.of(
                    new DepartmentRawData("Bajo", new BigDecimal("10.00"), new BigDecimal("10000")),
                    new DepartmentRawData("Medio", new BigDecimal("50.00"), new BigDecimal("50000")),
                    new DepartmentRawData("Alto", new BigDecimal("100.00"), new BigDecimal("100000"))
            );
            when(repository.getRawRankingData()).thenReturn(raw);

            List<DepartmentRankingResponse> result = service.getDepartmentRanking();

            assertThat(result).hasSize(3);
            assertThat(result.get(0).position()).isEqualTo(1);
            assertThat(result.get(0).department()).isEqualTo("Alto");
            assertThat(result.get(1).position()).isEqualTo(2);
            assertThat(result.get(2).position()).isEqualTo(3);
            assertThat(result.get(2).department()).isEqualTo("Bajo");
        }
    }
}
