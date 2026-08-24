package com.govconnect.ingestion.service;

import com.govconnect.ingestion.dto.ImportSummaryResponse;
import com.govconnect.ingestion.repository.IngestionRepository;
import com.govconnect.shared.csv.CsvParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link IngestionService}.
 * Verifica normalización de estado, montos y fechas del export de SECOP II,
 * resolución de dependencia, cálculo de disponible, upsert y acumulación de errores.
 */
@DisplayName("IngestionService — importación de CSV")
@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private CsvParser csvParser;

    @Mock
    private IngestionRepository repository;

    @Mock
    private SecopColumnMapper secopColumnMapper;

    @InjectMocks
    private IngestionService service;

    private static final Path CSV = Path.of("contratos.csv");

    private Map<String, String> contractRow(String number, String status, String department) {
        Map<String, String> row = new HashMap<>();
        row.put("numero_contrato", number);
        row.put("contratista", "Contratista " + number);
        row.put("objeto", "Objeto de prueba");
        row.put("valor", "1000.00");
        row.put("fecha_inicio", "2026-01-01");
        row.put("fecha_fin", "2026-12-31");
        row.put("estado", status);
        row.put("dependencia", department);
        return row;
    }

    /** Prepara el parseo de contratos con las filas indicadas. */
    private void stubContractCsv(List<Map<String, String>> rows) {
        when(secopColumnMapper.requiredHeaders()).thenReturn(Set.of("numero_contrato"));
        when(secopColumnMapper.aliases()).thenReturn(Map.of());
        when(csvParser.parse(any(Path.class), anySet(), anyMap())).thenReturn(rows);
    }

    /** Departamentos disponibles y contratos ya existentes en base de datos. */
    private void stubDepartments(String... existingContractNumbers) {
        when(repository.findAllDepartments()).thenReturn(List.<Object[]>of(
                new Object[]{1L, "INF", "Infraestructura"},
                new Object[]{2L, "EDU", "Educación"}));
        when(repository.findAllContractNumbers())
                .thenReturn(new HashSet<>(Set.of(existingContractNumbers)));
    }

    @Test
    @DisplayName("importContracts: inserta nuevos y actualiza existentes normalizando estado y dependencia")
    void importsContractsWithUpsert() {
        stubContractCsv(List.of(
                contractRow("SECOP-1", "En ejecución", "INF"),
                contractRow("SECOP-2", "Liquidado", "Educación")));
        stubDepartments("SECOP-2");
        when(repository.findDepartmentIdByCode("SIN")).thenReturn(null);

        ImportSummaryResponse result = service.importContracts(CSV);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        verify(repository).insertContract(eq("SECOP-1"), anyString(), anyString(), any(), any(), any(), eq("ACTIVE"), eq(1L));
        verify(repository).updateContract(eq("SECOP-2"), anyString(), anyString(), any(), any(), any(), eq("FINISHED"), eq(2L));
    }

    @Test
    @DisplayName("importContracts: una referencia repetida en el archivo se resuelve como actualización")
    void treatsDuplicateReferenceAsUpdate() {
        stubContractCsv(List.of(
                contractRow("SECOP-7", "En ejecución", "INF"),
                contractRow("SECOP-7", "En ejecución", "INF")));
        stubDepartments();
        when(repository.findDepartmentIdByCode("SIN")).thenReturn(null);

        ImportSummaryResponse result = service.importContracts(CSV);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(1);
        verify(repository).insertContract(eq("SECOP-7"), anyString(), anyString(), any(), any(), any(), any(), any());
        verify(repository).updateContract(eq("SECOP-7"), anyString(), anyString(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("importContracts: omite filas con estado no reconocido")
    void skipsUnknownStatus() {
        stubContractCsv(List.of(contractRow("SECOP-9", "Estado raro", "INF")));
        stubDepartments();
        when(repository.findDepartmentIdByCode("SIN")).thenReturn(null);

        ImportSummaryResponse result = service.importContracts(CSV);

        assertThat(result.imported()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0)).contains("estado no reconocido");
        verify(repository, never()).insertContract(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("importContracts: omite los estados de pre-ejecución del export de SECOP II")
    void skipsPreExecutionStatus() {
        stubContractCsv(List.of(
                contractRow("SECOP-10", "Aprobado", "INF"),
                contractRow("SECOP-11", "enviado Proveedor", "INF"),
                contractRow("SECOP-12", "En aprobación", "INF")));
        stubDepartments();
        when(repository.findDepartmentIdByCode("SIN")).thenReturn(null);

        ImportSummaryResponse result = service.importContracts(CSV);

        assertThat(result.imported()).isZero();
        assertThat(result.skipped()).isEqualTo(3);
        assertThat(result.errors()).allMatch(e -> e.contains("pre-ejecución"));
        verify(repository, never()).insertContract(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("importContracts: entidad no reconocida cae al departamento de respaldo")
    void fallsBackToUnassignedDepartment() {
        stubContractCsv(List.of(contractRow("SECOP-13", "En ejecución", "Empresa de Energía X")));
        stubDepartments();
        when(repository.findDepartmentIdByCode("SIN")).thenReturn(99L);

        ImportSummaryResponse result = service.importContracts(CSV);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        verify(repository).insertContract(eq("SECOP-13"), anyString(), anyString(), any(), any(), any(),
                eq("ACTIVE"), eq(99L));
    }

    @Test
    @DisplayName("importContracts: crea el departamento de respaldo si no existe en la base")
    void createsFallbackDepartmentWhenMissing() {
        stubContractCsv(List.of(contractRow("SECOP-20", "En ejecución", "ICBF REGIONAL BOGOTA")));
        stubDepartments();
        when(repository.findDepartmentIdByCode("SIN")).thenReturn(null, 99L);

        ImportSummaryResponse result = service.importContracts(CSV);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        verify(repository).insertDepartment(eq("SIN"), eq("Sin asignar"), anyString());
        verify(repository).insertContract(eq("SECOP-20"), anyString(), anyString(), any(), any(), any(),
                eq("ACTIVE"), eq(99L));
    }

    @Test
    @DisplayName("importContracts: acepta montos con separador de miles y con coma decimal")
    void parsesColombianAmounts() {
        Map<String, String> thousands = contractRow("SECOP-14", "En ejecución", "INF");
        thousands.put("valor", "$15.921.852");
        Map<String, String> decimals = contractRow("SECOP-15", "En ejecución", "INF");
        decimals.put("valor", "$1.234,56");
        stubContractCsv(List.of(thousands, decimals));
        stubDepartments();
        when(repository.findDepartmentIdByCode("SIN")).thenReturn(null);

        ImportSummaryResponse result = service.importContracts(CSV);

        assertThat(result.imported()).isEqualTo(2);
        verify(repository).insertContract(eq("SECOP-14"), anyString(), anyString(),
                eq(new BigDecimal("15921852")), any(), any(), any(), any());
        verify(repository).insertContract(eq("SECOP-15"), anyString(), anyString(),
                eq(new BigDecimal("1234.56")), any(), any(), any(), any());
    }

    @Test
    @DisplayName("importContracts: interpreta las fechas del export como MM/dd/yyyy")
    void parsesSecopDatesAsMonthFirst() {
        Map<String, String> row = contractRow("SECOP-16", "En ejecución", "INF");
        row.put("fecha_inicio", "09/01/2025");
        row.put("fecha_fin", "12/31/2025");
        stubContractCsv(List.of(row));
        stubDepartments();
        when(repository.findDepartmentIdByCode("SIN")).thenReturn(null);

        service.importContracts(CSV);

        verify(repository).insertContract(eq("SECOP-16"), anyString(), anyString(), any(),
                eq(LocalDate.of(2025, 9, 1)), eq(LocalDate.of(2025, 12, 31)), any(), any());
    }

    @Test
    @DisplayName("importContracts: si el primer componente no puede ser un mes, la fecha es dd/MM/yyyy")
    void parsesDayFirstWhenMonthImpossible() {
        Map<String, String> row = contractRow("SECOP-17", "En ejecución", "INF");
        row.put("fecha_inicio", "25/03/2025");
        row.put("fecha_fin", "2025-12-31");
        stubContractCsv(List.of(row));
        stubDepartments();
        when(repository.findDepartmentIdByCode("SIN")).thenReturn(null);

        service.importContracts(CSV);

        verify(repository).insertContract(eq("SECOP-17"), anyString(), anyString(), any(),
                eq(LocalDate.of(2025, 3, 25)), any(), any(), any());
    }

    @Test
    @DisplayName("importContracts: usa la fecha de firma cuando no hay fecha de inicio")
    void fallsBackToSignatureDate() {
        Map<String, String> row = contractRow("SECOP-18", "En ejecución", "INF");
        row.put("fecha_inicio", "");
        row.put("fecha_firma", "03/15/2025");
        stubContractCsv(List.of(row));
        stubDepartments();
        when(repository.findDepartmentIdByCode("SIN")).thenReturn(null);

        ImportSummaryResponse result = service.importContracts(CSV);

        assertThat(result.imported()).isEqualTo(1);
        verify(repository).insertContract(eq("SECOP-18"), anyString(), anyString(), any(),
                eq(LocalDate.of(2025, 3, 15)), any(), any(), any());
    }

    @Test
    @DisplayName("importContracts: recorta el objeto al ancho de la columna")
    void truncatesLongObject() {
        Map<String, String> row = contractRow("SECOP-19", "En ejecución", "INF");
        row.put("objeto", "O".repeat(600));
        stubContractCsv(List.of(row));
        stubDepartments();
        when(repository.findDepartmentIdByCode("SIN")).thenReturn(null);

        service.importContracts(CSV);

        ArgumentCaptor<String> object = ArgumentCaptor.forClass(String.class);
        verify(repository).insertContract(eq("SECOP-19"), anyString(), object.capture(), any(), any(), any(), any(), any());
        assertThat(object.getValue()).hasSize(500);
    }

    @Test
    @DisplayName("importContracts: omite la fila si la referencia no cabe en la columna")
    void skipsOverlongContractNumber() {
        stubContractCsv(List.of(contractRow("R".repeat(60), "En ejecución", "INF")));
        stubDepartments();

        ImportSummaryResponse result = service.importContracts(CSV);

        assertThat(result.imported()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.errors().get(0)).contains("excede 50 caracteres");
    }

    @Test
    @DisplayName("importContracts: acota la lista de errores sin perder la cuenta de omitidas")
    void capsErrorList() {
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 0; i < 600; i++) {
            Map<String, String> row = contractRow("SECOP-CAP-" + i, "En ejecución", "INF");
            row.put("valor", "no es un monto");
            rows.add(row);
        }
        stubContractCsv(rows);
        stubDepartments();

        ImportSummaryResponse result = service.importContracts(CSV);

        assertThat(result.skipped()).isEqualTo(600);
        assertThat(result.errors()).hasSize(500);
    }

    @Test
    @DisplayName("importBudgets: deriva disponible cuando la columna no viene")
    void derivesAvailableBudget() {
        Map<String, String> row = Map.of(
                "dependencia", "INF",
                "anio", "2026",
                "asignado", "1000.00",
                "ejecutado", "700.00");
        when(csvParser.parse(any(Path.class), anySet())).thenReturn(List.of(row));
        when(repository.findAllDepartments()).thenReturn(List.<Object[]>of(new Object[]{1L, "INF", "Infraestructura"}));
        when(repository.budgetExists(1L, 2026)).thenReturn(false);

        ImportSummaryResponse result = service.importBudgets(Path.of("presupuestos.csv"));

        assertThat(result.imported()).isEqualTo(1);
        verify(repository).insertBudget(
                eq(1L), eq(2026),
                eq(new BigDecimal("1000.00")), eq(new BigDecimal("700.00")), eq(new BigDecimal("300.00")));
    }

    @Test
    @DisplayName("importCollections: inserta recaudos (append) y resuelve dependencia por nombre")
    void importsCollections() {
        Map<String, String> row = Map.of(
                "fecha", "2026-01-15",
                "concepto", "Impuesto Predial",
                "contribuyente", "Contribuyente A",
                "monto", "3500000.00",
                "medio_pago", "PSE",
                "dependencia", "Hacienda");
        when(csvParser.parse(any(Path.class), anySet())).thenReturn(List.of(row));
        when(repository.findAllDepartments()).thenReturn(List.<Object[]>of(new Object[]{1L, "HAC", "Hacienda"}));

        ImportSummaryResponse result = service.importCollections(Path.of("recaudos.csv"));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        verify(repository).insertCollection(any(), eq("Impuesto Predial"), eq("Contribuyente A"),
                eq(new BigDecimal("3500000.00")), eq("PSE"), eq(1L));
    }
}
