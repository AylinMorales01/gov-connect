package com.govconnect.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SIA Connect API")
                        .version("1.0.0")
                        .description("Sistema Inteligente de Automatización y Analítica para Entidades Públicas")
                        .contact(new Contact()
                                .name("Aylin Chaverra Morales")
                                .email("aylin.morales2401@gmail.com")));
    }
}