package com.govconnect.ingestion.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para {@link SecopColumnMapper}.
 */
@DisplayName("SecopColumnMapper — mapeo de cabeceras SECOP II")
class SecopColumnMapperTest {

    private final SecopColumnMapper mapper = new SecopColumnMapper();

    @Test
    @DisplayName("mapea las cabeceras técnicas de un export de contratos a los nombres canónicos")
    void mapsTechnicalHeadersToCanonical() {
        assertThat(mapper.aliases())
                .containsEntry("referencia del contrato", "numero_contrato")
                .containsEntry("proveedor adjudicado", "contratista")
                .containsEntry("objeto del contrato", "objeto")
                .containsEntry("valor del contrato", "valor")
                .containsEntry("fecha de inicio del contrato", "fecha_inicio")
                .containsEntry("fecha de fin del contrato", "fecha_fin")
                .containsEntry("estado contrato", "estado")
                .containsEntry("nombre entidad", "dependencia");
    }

    @Test
    @DisplayName("normaliza tildes y mayúsculas al comparar cabeceras")
    void normalizesAccentsAndCase() {
        assertThat(SecopColumnMapper.normalize("Referencia del Contrato"))
                .isEqualTo("referencia del contrato");
        assertThat(SecopColumnMapper.normalize("FECHA_DE_FIN_DEL_CONTRATO"))
                .isEqualTo("fecha de fin del contrato");
        assertThat(SecopColumnMapper.normalize("Entidad"))
                .isEqualTo("entidad");
    }

    @Test
    @DisplayName("reconoce las variantes de fecha de fin (contrato vs liquidación)")
    void recognizesEndDateVariants() {
        assertThat(mapper.aliases().get("fecha fin liquidacion")).isEqualTo("fecha_fin");
        assertThat(mapper.aliases().get("fecha de fin del contrato")).isEqualTo("fecha_fin");
    }

    @Test
    @DisplayName("expone las 8 cabeceras canónicas requeridas")
    void exposesRequiredHeaders() {
        assertThat(mapper.requiredHeaders())
                .containsExactlyInAnyOrder(
                        "numero_contrato", "contratista", "objeto", "valor",
                        "fecha_inicio", "fecha_fin", "estado", "dependencia");
    }

    @Test
    @DisplayName("la fecha de firma se reconoce como columna propia, opcional y distinta de fecha_inicio")
    void recognizesSignatureDateAsOptionalColumn() {
        assertThat(mapper.aliases()).containsEntry("fecha de firma", "fecha_firma");
        assertThat(mapper.requiredHeaders()).doesNotContain("fecha_firma");
    }
}
