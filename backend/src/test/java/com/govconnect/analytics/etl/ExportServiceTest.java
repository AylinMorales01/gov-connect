package com.govconnect.analytics.etl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para la sanitización de texto del {@link ExportService}.
 */
@DisplayName("ExportService — sanitización de valores CSV")
class ExportServiceTest {

    @Test
    @DisplayName("reemplaza comas y duplica comillas dobles")
    void cleansCommasAndQuotes() {
        assertThat(ExportService.clean("a,b\"c")).isEqualTo("a b\"\"c");
    }

    @Test
    @DisplayName("elimina saltos de línea para no partir la fila CSV")
    void removesLineBreaks() {
        assertThat(ExportService.clean("línea 1\nlínea 2\r\nlínea 3"))
                .isEqualTo("línea 1 línea 2 línea 3");
    }

    @Test
    @DisplayName("devuelve vacío para valores nulos")
    void handlesNull() {
        assertThat(ExportService.clean(null)).isEmpty();
    }
}
