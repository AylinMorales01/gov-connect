package com.govconnect.analytics.etl;

/**
 * Estados del ciclo de vida de una tarea ETL asíncrona.
 */
public enum EtlTaskState {
    /** La tarea fue creada y está encolada, aún no comienza. */
    PENDING,
    /** La tarea está en ejecución. */
    RUNNING,
    /** La tarea finalizó exitosamente. */
    COMPLETED,
    /** La tarea finalizó con error. */
    FAILED
}
