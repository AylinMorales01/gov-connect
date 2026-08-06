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

    // ── Analytics ──────────────────────────────────────────
    public static final String ANALYTICS_HEALTH_SUCCESS =
            "DuckDB conectado correctamente";

    public static final String ANALYTICS_ETL_SUCCESS =
            "Proceso ETL finalizado correctamente";

    public static final String ANALYTICS_MONTHLY_TREND_SUCCESS =
            "Tendencia mensual obtenida correctamente";

    public static final String ANALYTICS_FINANCIAL_OVERVIEW_SUCCESS =
            "Resumen analítico obtenido correctamente";

    public static final String ANALYTICS_DEPARTMENT_RANKING_SUCCESS =
            "Ranking de dependencias obtenido correctamente";

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