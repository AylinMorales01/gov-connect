package com.govconnect.ingestion.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mapea las cabeceras de un export de contratos (SECOP II u otros sistemas de
 * contratación) a los nombres canónicos que consume {@link IngestionService}.
 * <p>
 * SECOP II exporta columnas con nombres propios (p. ej. {@code referencia_del_contrato},
 * {@code proveedor_adjudicado}) que no coinciden con los campos internos
 * ({@code numero_contrato}, {@code contratista}). Este mapper resuelve esa
 * diferencia mediante una tabla de alias normalizada (sin tildes, minúsculas,
 * guiones bajos colapsados a espacio) para que la ingesta no dependa del nombre
 * exacto que traiga el archivo.
 * </p>
 * <p>
 * Las columnas del export que no tienen correspondencia (SECOP II trae 58 o más)
 * se ignoran de forma explícita: no participan en la validación ni en el resultado.
 * </p>
 */
@Component
public class SecopColumnMapper {

    /** Nombre canónico → cabeceras alternativas de las columnas obligatorias. */
    private static final Map<String, List<String>> REQUIRED_ALIASES = Map.of(
            "numero_contrato", List.of(
                    "numero_contrato", "referencia_del_contrato", "referencia del contrato",
                    "referencia_del_proceso", "referencia del proceso"),
            "contratista", List.of(
                    "contratista", "proveedor_adjudicado", "proveedor adjudicado",
                    "nombre_del_proveedor", "nombre del proveedor adjudicado", "razon_social"),
            "objeto", List.of(
                    "objeto", "objeto_del_contrato", "objeto del contrato",
                    "descripcion_del_procedimiento", "descripcion del procedimiento"),
            "valor", List.of(
                    "valor", "valor_del_contrato", "valor del contrato",
                    "valor_total_adjudicacion", "valor total adjudicacion"),
            "fecha_inicio", List.of(
                    "fecha_inicio", "fecha_de_inicio_del_contrato", "fecha de inicio del contrato",
                    "fecha_de_publicacion_del", "fecha de publicacion del proceso"),
            "fecha_fin", List.of(
                    "fecha_fin", "fecha_de_fin_del_contrato", "fecha de fin del contrato",
                    "fecha_fin_liquidacion", "fecha fin liquidacion", "fecha_de_terminacion"),
            "estado", List.of(
                    "estado", "estado_contrato", "estado contrato",
                    "estado_del_procedimiento", "estado del procedimiento"),
            "dependencia", List.of(
                    "dependencia", "nombre_entidad", "nombre entidad", "entidad"));

    /**
     * Columnas reconocidas pero no obligatorias. {@code fecha_firma} se usa como
     * respaldo cuando el export no trae fecha de inicio.
     * <p>
     * Es una columna canónica propia y no un alias de {@code fecha_inicio}: el
     * {@code CsvParser} vuelca las columnas en orden de cabecera, y como
     * "Fecha de Firma" precede a "Fecha de Inicio del Contrato" en el export de
     * SECOP II, compartir nombre canónico haría que una fecha de inicio vacía
     * pisara a la de firma.
     * </p>
     */
    private static final Map<String, List<String>> OPTIONAL_ALIASES = Map.of(
            "fecha_firma", List.of(
                    "fecha_firma", "fecha_de_firma", "fecha de firma"));

    private final Map<String, String> aliasMap;
    private final Set<String> requiredHeaders;

    public SecopColumnMapper() {
        this.aliasMap = buildAliasMap();
        this.requiredHeaders = Set.copyOf(REQUIRED_ALIASES.keySet());
    }

    /**
     * Cabeceras canónicas requeridas para una importación de contratos.
     */
    public Set<String> requiredHeaders() {
        return requiredHeaders;
    }

    /**
     * Mapa {@code alias normalizado -> nombre canónico} listo para el {@code CsvParser},
     * con las columnas obligatorias y las opcionales.
     */
    public Map<String, String> aliases() {
        return aliasMap;
    }

    /**
     * Construye el mapa invertido de alias: cada variante normalizada apunta a su
     * nombre canónico. El orden de inserción garantiza que las variantes más
     * específicas no se pisen entre sí.
     */
    private static Map<String, String> buildAliasMap() {
        Map<String, String> map = new LinkedHashMap<>();
        putAliases(map, REQUIRED_ALIASES);
        putAliases(map, OPTIONAL_ALIASES);
        return Map.copyOf(map);
    }

    private static void putAliases(Map<String, String> target, Map<String, List<String>> source) {
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            String canonical = entry.getKey();
            for (String alias : entry.getValue()) {
                target.put(normalize(alias), canonical);
            }
        }
    }

    /**
     * Normaliza una cabecera para comparación robusta: sin tildes, minúsculas,
     * recorta espacios y colapsa guiones bajos a espacios.
     */
    static String normalize(String header) {
        return StatusNormalizer.normalizeHeader(header);
    }
}
