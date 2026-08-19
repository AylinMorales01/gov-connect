package com.govconnect.analytics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Habilita la ejecución asíncrona y define el executor dedicado al ETL.
 * <p>
 * Se usa un {@code ThreadPoolTaskExecutor} acotado para no saturar el
 * sistema con ejecuciones concurrentes del ETL.
 * </p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Executor dedicado para tareas ETL.
     * <p>
     * Referenciado por {@code @Async("etlExecutor")}.
     * </p>
     */
    @Bean(name = "etlExecutor")
    public Executor etlExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("etl-");
        executor.initialize();
        return executor;
    }
}
