package com.govconnect.ingestion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Executor dedicado a las importaciones de CSV.
 * <p>
 * No se reutiliza el {@code etlExecutor}: una importación ocupa su hilo durante
 * decenas de segundos y además encola una tarea ETL al terminar, así que
 * compartir pool dejaría al ETL esperando detrás de las importaciones. El pool
 * se mantiene pequeño a propósito, porque cada importación retiene una conexión
 * de base de datos durante toda su transacción.
 * </p>
 * <p>
 * {@code @EnableAsync} ya está declarado en {@code analytics.config.AsyncConfig}.
 * </p>
 */
@Configuration
public class IngestionAsyncConfig {

    /**
     * Executor dedicado a la ingesta. Referenciado por
     * {@code @Async("ingestionExecutor")}.
     */
    @Bean(name = "ingestionExecutor")
    public Executor ingestionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(5);
        executor.setThreadNamePrefix("ingestion-");
        executor.initialize();
        return executor;
    }
}
