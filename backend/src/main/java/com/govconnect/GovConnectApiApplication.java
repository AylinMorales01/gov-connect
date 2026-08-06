package com.govconnect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
public class GovConnectApiApplication {

    private static final Logger log = LoggerFactory.getLogger(GovConnectApiApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GovConnectApiApplication.class, args);
    }

    @Bean
    CommandLineRunner testConnection(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                log.info("Conexión a la base de datos establecida correctamente: {}", connection.getCatalog());
            } catch (Exception e) {
                log.error("No se pudo conectar a la base de datos: {}", e.getMessage());
            }
        };
    }
}