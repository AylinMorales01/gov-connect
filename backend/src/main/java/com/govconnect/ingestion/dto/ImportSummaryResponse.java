package com.govconnect.ingestion.dto;

import java.util.List;

/**
 * Resumen del resultado de una importación CSV.
 *
 * @param totalRows filas de datos procesadas (sin contar la cabecera).
 * @param imported  registros insertados nuevos.
 * @param updated   registros existentes actualizados.
 * @param skipped   filas omitidas por errores de validación.
 * @param errors    mensajes de error por fila (en formato "fila N: motivo").
 * @param etlTaskId id de la tarea ETL disparada para refrescar DuckDB, o {@code null}
 *                  si no se procesó ningún registro.
 */
public record ImportSummaryResponse(
        int totalRows,
        int imported,
        int updated,
        int skipped,
        List<String> errors,
        String etlTaskId
) {
}
