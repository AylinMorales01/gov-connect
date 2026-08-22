package com.govconnect.shared.csv;

import com.govconnect.shared.exception.CsvImportException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para {@link CsvParser}.
 */
@DisplayName("CsvParser — parsing de archivos CSV")
class CsvParserTest {

    private final CsvParser parser = new CsvParser();

    private MockMultipartFile file(String name, String content) {
        return new MockMultipartFile(name, name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("parsea un CSV válido y devuelve las filas como mapas")
    void parsesValidCsv() {
        MockMultipartFile f = file("x.csv", "a,b,c\n1,2,3\n4,5,6\n");

        List<Map<String, String>> rows = parser.parse(f, Set.of("a", "b", "c"));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0))
                .containsEntry("a", "1")
                .containsEntry("b", "2")
                .containsEntry("c", "3");
    }

    @Test
    @DisplayName("soporta campos entre comillas con comas internas")
    void parsesQuotedFields() {
        MockMultipartFile f = file("x.csv", "a,b\n\"valor, con coma\",otro\n");

        List<Map<String, String>> rows = parser.parse(f, Set.of("a", "b"));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("a")).isEqualTo("valor, con coma");
        assertThat(rows.get(0).get("b")).isEqualTo("otro");
    }

    @Test
    @DisplayName("tolera el BOM UTF-8 al inicio del archivo")
    void toleratesBom() {
        MockMultipartFile f = file("x.csv", "﻿a,b\n1,2\n");

        List<Map<String, String>> rows = parser.parse(f, Set.of("a", "b"));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("a", "1");
    }

    @Test
    @DisplayName("lanza CsvImportException si faltan columnas requeridas")
    void failsWhenRequiredHeaderMissing() {
        MockMultipartFile f = file("x.csv", "a,b\n1,2\n");

        assertThatThrownBy(() -> parser.parse(f, Set.of("a", "b", "c")))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("c");
    }

    @Test
    @DisplayName("lanza CsvImportException si el archivo está vacío")
    void failsOnEmptyFile() {
        MockMultipartFile f = file("x.csv", "");

        assertThatThrownBy(() -> parser.parse(f, Set.of("a")))
                .isInstanceOf(CsvImportException.class);
    }

    @Test
    @DisplayName("parsea un CSV desde disco, la vía de la ingesta asíncrona")
    void parsesFromPath(@TempDir Path dir) throws IOException {
        Path csv = Files.writeString(dir.resolve("x.csv"), "a,b\n1,2\n");

        List<Map<String, String>> rows = parser.parse(csv, Set.of("a", "b"));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("a", "1").containsEntry("b", "2");
    }

    @Test
    @DisplayName("tolera el BOM UTF-8 leyendo desde disco")
    void toleratesBomFromPath(@TempDir Path dir) throws IOException {
        Path csv = Files.writeString(dir.resolve("x.csv"), "﻿a,b\n1,2\n");

        List<Map<String, String>> rows = parser.parse(csv, Set.of("a", "b"));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("a", "1");
    }

    @Test
    @DisplayName("una fila con menos columnas que la cabecera no aborta el archivo entero")
    void toleratesShortRow(@TempDir Path dir) throws IOException {
        Path csv = Files.writeString(dir.resolve("x.csv"), "a,b,c\n1,2\n4,5,6\n");

        List<Map<String, String>> rows = parser.parse(csv, Set.of("a", "b", "c"));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("c", "");
        assertThat(rows.get(1)).containsEntry("c", "6");
    }

    @Test
    @DisplayName("dos cabeceras alias del mismo canónico: una columna vacía posterior no pisa el valor")
    void keepsFirstNonBlankValueForAliasCollision(@TempDir Path dir) throws IOException {
        Path csv = Files.writeString(dir.resolve("x.csv"),
                "Fecha de Fin del Contrato,Fecha Fin Liquidacion\n2025-12-31,\n");

        List<Map<String, String>> rows = parser.parse(
                csv,
                Set.of("fecha"),
                Map.of("fecha de fin del contrato", "fecha", "fecha fin liquidacion", "fecha"));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("fecha", "2025-12-31");
    }
}
