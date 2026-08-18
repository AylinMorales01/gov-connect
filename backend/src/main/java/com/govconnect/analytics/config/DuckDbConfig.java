package com.govconnect.analytics.config;

import org.springframework.beans.factory.annotation.Value;
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
     * Ruta al archivo DuckDB. Configurable vía la propiedad {@code duckdb.path}
     * en {@code application.yaml} o la variable de entorno {@code DUCKDB_PATH}.
     * <p>
     * El valor por defecto {@code database/analytics/analytics.duckdb} es relativo
     * al directorio de trabajo del proceso (raíz del repositorio).
     * </p>
     */
    @Value("${duckdb.path:database/analytics/analytics.duckdb}")
    private String duckDbPath;

    @Bean
    public DataSource duckDbDataSource() {
        String url = "jdbc:duckdb:" + duckDbPath;
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        // DuckDB no requiere usuario ni contraseña en modo embebido
        return dataSource;
    }
}