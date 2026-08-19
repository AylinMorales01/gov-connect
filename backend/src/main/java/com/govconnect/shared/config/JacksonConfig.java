package com.govconnect.shared.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Jackson.
 * <p>
 * Provee un {@link ObjectMapper} explícito con la misma configuración
 * que Spring Boot aplica por defecto:
 * <ul>
 *   <li>{@link JavaTimeModule} — soporte para {@code java.time} (ISO-8601)</li>
 *   <li>{@code WRITE_DATES_AS_TIMESTAMPS = false}</li>
 *   <li>{@code FAIL_ON_UNKNOWN_PROPERTIES = false}</li>
 * </ul>
 * </p>
 * <p>
 * Se define como bean explícito porque {@link com.govconnect.shared.config.SecurityConfig}
 * lo inyecta vía constructor para escribir respuestas de error 401/403 en JSON.
 * </p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
