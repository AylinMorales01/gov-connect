/** Estados del ciclo de vida de una tarea ETL asíncrona (espejo del backend). */
export type EtlTaskState = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

/** Estado observable de una tarea ETL devuelto por `GET /analytics/etl/status/{taskId}`. */
export interface EtlTask {
    taskId: string;
    state: EtlTaskState;
    message: string | null;
    startedAt: string | null;
    completedAt: string | null;
}

/** Resumen del resultado de una importación CSV (`ImportSummaryResponse`). */
export interface ImportSummary {
    totalRows: number;
    imported: number;
    updated: number;
    skipped: number;
    /** Errores por fila, topados en el backend; el total real lo da `skipped`. */
    errors: string[];
    etlTaskId: string | null;
}

/** Estados del ciclo de vida de una importación asíncrona (espejo del backend). */
export type IngestionTaskState = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

/** Estado observable de una importación devuelto por `GET /ingestion/status/{taskId}`. */
export interface IngestionTask {
    taskId: string;
    state: IngestionTaskState;
    message: string | null;
    startedAt: string | null;
    completedAt: string | null;
    /** Disponible solo cuando el estado es `COMPLETED`. */
    summary: ImportSummary | null;
}
