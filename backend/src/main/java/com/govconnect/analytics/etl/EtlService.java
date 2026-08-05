package com.govconnect.analytics.etl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
@RequiredArgsConstructor
public class EtlService {

    private final ExportService exportService;
    private final ImportService importService;

    public void runFullEtl() throws SQLException {
        // 1. Exportar todos a CSV
        exportService.exportCollectionsToCsv();
        exportService.exportDepartmentsToCsv();
        exportService.exportBudgetsToCsv();

        // 2. Importar todos a DuckDB
        importService.loadCollectionsCsv();
        importService.loadDepartmentsCsv();
        importService.loadBudgetsCsv();
    }
}