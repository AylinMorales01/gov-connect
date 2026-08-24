package com.govconnect.ingestion.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para {@link StatusNormalizer}.
 */
@DisplayName("StatusNormalizer — estados de contrato SECOP II")
class StatusNormalizerTest {

    @Test
    @DisplayName("mapea estados de ejecución a ACTIVE")
    void mapsActiveStates() {
        assertThat(StatusNormalizer.normalize("En ejecución")).contains("ACTIVE");
        assertThat(StatusNormalizer.normalize("Modificado")).contains("ACTIVE");
        assertThat(StatusNormalizer.normalize("Celebrado")).contains("ACTIVE");
    }

    @Test
    @DisplayName("mapea estados terminales a FINISHED")
    void mapsFinishedStates() {
        assertThat(StatusNormalizer.normalize("Terminado")).contains("FINISHED");
        assertThat(StatusNormalizer.normalize("Cerrado")).contains("FINISHED");
        assertThat(StatusNormalizer.normalize("Cancelado")).contains("FINISHED");
        assertThat(StatusNormalizer.normalize("Cedido")).contains("FINISHED");
        assertThat(StatusNormalizer.normalize("Liquidado")).contains("FINISHED");
    }

    @Test
    @DisplayName("identifica borrador y aprobado como estados a omitir")
    void marksPreExecutionAsSkipped() {
        assertThat(StatusNormalizer.isSkipped("Borrador")).isTrue();
        assertThat(StatusNormalizer.isSkipped("Aprobado")).isTrue();
        assertThat(StatusNormalizer.isSkipped("En ejecución")).isFalse();
    }

    @Test
    @DisplayName("devuelve vacío para estados desconocidos")
    void unknownStatusIsEmpty() {
        assertThat(StatusNormalizer.normalize("Estado raro")).isEqualTo(Optional.empty());
    }

    @Test
    @DisplayName("normaliza cabeceras quitando tildes y guiones bajos")
    void normalizesHeaders() {
        assertThat(StatusNormalizer.normalizeHeader("Referencia_del_Contrato"))
                .isEqualTo("referencia del contrato");
        assertThat(StatusNormalizer.normalizeHeader("Fecha de Fin del Contrato"))
                .isEqualTo("fecha de fin del contrato");
    }
}
