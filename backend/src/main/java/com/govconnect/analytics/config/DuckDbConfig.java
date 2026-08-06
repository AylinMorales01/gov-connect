package com.govconnect.analytics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Configuración del DataSource para DuckDB (motor analítico embebido).
 * <p>
 * A diferencia de la implementación anterior —que exponía un {@code Connection}
 * como bean singleton— este DataSource entrega una conexión nueva en cada
 * llamada a {@code getConnection()}. Los consumidores deben abrir y cerrar
 * la conexión con <b>try-with-resources</b>, garantizando aislamiento entre
 * hilos y liberación correcta de recursos.
 * </p>
 * <p>
 * Se usa {@link DriverManagerDataSource} sin pool porque DuckDB es una base
 * de datos embebida; las conexiones son ligeras y no se benefician de un pool.
 * </p>
 */
@Configuration
public class DuckDbConfig {

    /**
     * Ruta relativa al archivo DuckDB, resuelta desde el directorio de trabajo
     * del proceso (debe ser la raíz del repositorio).
     */
    private static final String DUCKDB_URL =
            "jdbc:duckdb:database/analytics/analytics.duckdb";

    @Bean
    public DataSource duckDbDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(DUCKDB_URL);
        // DuckDB no requiere usuario ni contraseña en modo embebido
        return dataSource;
    }
}