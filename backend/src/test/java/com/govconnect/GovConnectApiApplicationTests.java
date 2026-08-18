package com.govconnect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

/**
 * Test de carga de contexto de la aplicación.
 * <p>
 * Usa H2 en memoria como reemplazo de SQL Server y DuckDB en memoria
 * como reemplazo del archivo DuckDB en disco. Esto permite ejecutar los
 * tests sin bases de datos externas.
 * </p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        // H2 en memoria como reemplazo de SQL Server
        "spring.datasource.primary.jdbc-url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MSSQLServer",
        "spring.datasource.primary.username=sa",
        "spring.datasource.primary.password=",
        "spring.datasource.primary.driver-class-name=org.h2.Driver",

        // Hibernate: crear schema desde entidades JPA
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",

        // Permitir que @TestConfiguration sobrescriba beans
        "spring.main.allow-bean-definition-overriding=true",

        // JWT para tests
        "jwt.secret=test-secret-key-for-junit-minimum-32-bytes!!",
        "jwt.expiration=60",
        "jwt.refresh-expiration=300"
})
@DisplayName("Carga de contexto de la aplicación")
class GovConnectApiApplicationTests {

    /**
     * Configuración de test que reemplaza el DataSource de DuckDB en disco
     * por uno en memoria ({@code jdbc:duckdb::memory:}).
     */
    @TestConfiguration
    static class TestDuckDbConfig {

        @Bean
        DataSource duckDbDataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setUrl("jdbc:duckdb::memory:");
            return ds;
        }
    }

    @Test
    @DisplayName("El contexto de Spring Boot debe cargarse correctamente")
    void contextLoads() {
        // Si llega aquí, todos los beans se crearon correctamente
    }
}
