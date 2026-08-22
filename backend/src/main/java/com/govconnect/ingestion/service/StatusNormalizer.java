package com.govconnect.ingestion.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Utilidades de normalización de texto para la ingesta de CSV.
 * <p>
 * Los valores de SECOP pueden venir en distintos formatos (mayúsculas/minúsculas,
 * tildes, espacios). Estas utilidades los normalizan a una forma canónica.
 * </p>
 */
public final class StatusNormalizer {

    /**
     * Estados de pre-ejecución del export de SECOP II: el contrato aún no está
     * en firme, así que ni sus fechas ni su valor son fiables.
     */
    private static final Set<String> PRE_EXECUTION_STATUSES = Set.of(
            "BORRADOR", "APROBADO", "ENVIADO PROVEEDOR", "EN APROBACION");

    private StatusNormalizer() {}

    /**
     * Normaliza un texto libre: quita tildes, pasa a mayúsculas y recorta espacios.
     */
    public static String normalizeText(String raw) {
        if (raw == null) {
            return "";
        }
        return Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    /**
     * Normaliza una cabecera de CSV para comparación robusta: quita tildes,
     * pasa a minúsculas, recorta y colapsa guiones bajos a espacios. Útil para
     * casar nombres de columna entre un export (p. ej. SECOP II) y el modelo.
     */
    public static String normalizeHeader(String raw) {
        if (raw == null) {
            return "";
        }
        return normalizeText(raw).replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    /**
     * Normaliza el estado de un contrato al dominio de Gov Connect
     * ({@code ACTIVE}, {@code SUSPENDED}, {@code FINISHED}).
     * <p>
     * Acepta tanto los estados del modelo interno como los estados reales de un
     * export de contratos SECOP II. Los estados de pre-ejecución (borrador,
     * aprobado) se excluyen vía {@link #isSkipped(String)} y no llegan aquí.
     * </p>
     *
     * @return el estado normalizado, o {@link Optional#empty()} si no se reconoce.
     */
    public static Optional<String> normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String value = normalizeText(raw);
        return switch (value) {
            case "ACTIVE", "ACTIVO", "EN EJECUCION", "EJECUCION", "CELEBRADO",
                    "EJECUCION ACTIVA", "MODIFICADO" ->
                    Optional.of("ACTIVE");
            case "SUSPENDED", "SUSPENDIDO" -> Optional.of("SUSPENDED");
            case "FINISHED", "TERMINADO", "FINALIZADO", "LIQUIDADO", "CERRADO",
                    "EXPIRADO", "EXPIRED", "CANCELADO", "CEDIDO" ->
                    Optional.of("FINISHED");
            default -> Optional.empty();
        };
    }

    /**
     * Indica si un estado corresponde a un contrato que aún no entra en ejecución
     * (borrador, aprobado, enviado al proveedor o en aprobación). Estas filas se
     * omiten de la importación: no son contratos vigentes y su fecha de fin no es
     * fiable para la alerta.
     */
    public static boolean isSkipped(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        return PRE_EXECUTION_STATUSES.contains(normalizeText(raw));
    }
}
