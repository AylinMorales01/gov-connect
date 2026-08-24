package com.govconnect.ingestion.task;

/**
 * Estados del ciclo de vida de una importación asíncrona.
 */
public enum IngestionTaskState {
    /** El archivo quedó en disco y la tarea está encolada, aún no comienza. */
    PENDING,
    /** La importación está en ejecución. */
    RUNNING,
    /** La importación finalizó y su resumen está disponible. */
    COMPLETED,
    /** La importación finalizó con error. */
    FAILED
}
