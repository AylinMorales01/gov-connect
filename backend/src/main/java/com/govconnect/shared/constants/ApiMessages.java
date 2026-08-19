package com.govconnect.shared.constants;

public final class ApiMessages {

    private ApiMessages() {}

    // ── Dashboard ──────────────────────────────────────────
    public static final String DASHBOARD_SUMMARY_SUCCESS =
            "Resumen ejecutivo obtenido correctamente";

    public static final String MONTHLY_COLLECTIONS_SUCCESS =
            "Recaudo mensual obtenido correctamente";

    public static final String CONTRACTS_EXPIRING_SUCCESS =
            "Contratos próximos a vencer obtenidos correctamente";

    public static final String BUDGET_EXECUTION_SUCCESS =
            "Ejecución presupuestal obtenida correctamente";

    // ── Contracts ──────────────────────────────────────────
    public static final String CONTRACTS_LIST_SUCCESS =
            "Contratos obtenidos correctamente";

    public static final String CONTRACTS_SUMMARY_SUCCESS =
            "Resumen de contratos obtenido correctamente";

    // ── Automation ─────────────────────────────────────────
    public static final String AUTOMATION_LOG_STORED =
            "Registro de automatización almacenado correctamente";

    public static final String AUTOMATION_LOGS_HISTORY_SUCCESS =
            "Historial de automatizaciones obtenido correctamente";

    // ── Analytics ──────────────────────────────────────────
    public static final String ANALYTICS_HEALTH_SUCCESS =
            "DuckDB conectado correctamente";

    public static final String ANALYTICS_ETL_STARTED =
            "Proceso ETL iniciado en segundo plano. Consulte su estado con el taskId devuelto.";

    public static final String ANALYTICS_ETL_STATUS =
            "Estado de la tarea ETL obtenido correctamente";

    public static final String ANALYTICS_MONTHLY_TREND_SUCCESS =
            "Tendencia mensual obtenida correctamente";

    public static final String ANALYTICS_FINANCIAL_OVERVIEW_SUCCESS =
            "Resumen analítico obtenido correctamente";

    public static final String ANALYTICS_DEPARTMENT_RANKING_SUCCESS =
            "Ranking de dependencias obtenido correctamente";

    public static final String ANALYTICS_CONCEPT_BREAKDOWN_SUCCESS =
            "Desglose de recaudos por concepto obtenido correctamente";

    public static final String ANALYTICS_PAYMENT_METHOD_BREAKDOWN_SUCCESS =
            "Desglose de recaudos por método de pago obtenido correctamente";

    public static final String ANALYTICS_CONTRACTS_BY_STATUS_SUCCESS =
            "Desglose de contratos por estado obtenido correctamente";

    public static final String ANALYTICS_CONTRACTS_BY_DEPARTMENT_SUCCESS =
            "Valor contratado por dependencia obtenido correctamente";

    public static final String ANALYTICS_TOP_CONTRACTORS_SUCCESS =
            "Ranking de contratistas obtenido correctamente";

    // ── Auth ─────────────────────────────────────────────
    public static final String AUTH_SUCCESS =
            "Autenticación exitosa";

    public static final String AUTH_ME_SUCCESS =
            "Usuario autenticado obtenido correctamente";

    public static final String AUTH_BAD_CREDENTIALS =
            "Credenciales inválidas. Verifique usuario y contraseña.";

    public static final String AUTH_UNAUTHORIZED =
            "Se requiere autenticación para acceder a este recurso.";

    public static final String AUTH_FORBIDDEN =
            "No tiene permisos suficientes para acceder a este recurso.";

    // ── Errores (usados por GlobalExceptionHandler) ─────────
    public static final String ERROR_NOT_FOUND =
            "El recurso solicitado no fue encontrado";

    public static final String ERROR_ENTITY_NOT_FOUND =
            "La entidad solicitada no fue encontrada";

    public static final String ERROR_DATABASE =
            "Error interno al procesar la consulta. Intente nuevamente más tarde.";

    public static final String ERROR_VALIDATION =
            "Error de validación en los datos enviados";

    public static final String ERROR_BAD_REQUEST =
            "Solicitud inválida";

    public static final String ERROR_INTERNAL =
            "Error interno del servidor. Contacte al administrador.";
}