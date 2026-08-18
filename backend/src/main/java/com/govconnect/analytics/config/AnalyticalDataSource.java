package com.govconnect.analytics.config;

import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifier semántico para el {@code DataSource} analítico (DuckDB).
 * <p>
 * Reemplaza el {@code @Qualifier("duckDbDataSource")} basado en el nombre
 * del bean por una anotación con significado de dominio. Al ser una
 * meta-anotación de {@link Qualifier}, Spring la trata exactamente igual
 * para resolver la inyección, pero sin depender del nombre del bean
 * (más robusto ante refactorings y sin ambigüedad por nombre).
 * </p>
 *
 * <p>Uso típico (inyección por constructor):</p>
 * <pre>
 * public MiServicio(@AnalyticalDataSource DataSource dataSource) { ... }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
        ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Qualifier
public @interface AnalyticalDataSource {
}
