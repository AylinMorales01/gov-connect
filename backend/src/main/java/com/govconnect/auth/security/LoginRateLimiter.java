package com.govconnect.auth.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter en memoria para el endpoint de login.
 * <p>
 * Usa Bucket4j con buckets por IP. Sin dependencias externas (sin Redis).
 * </p>
 * <p>
 * <b>Límites:</b> 5 intentos por minuto por IP.
 * Una vez agotados, el bucket se recarga a razón de 1 intento cada 12 segundos.
 * </p>
 */
@Service
@Slf4j
public class LoginRateLimiter {

    /** Intentos permitidos por ventana de tiempo. */
    private static final int CAPACITY = 5;

    /** Ventana en segundos para recargar completamente el bucket. */
    private static final long REFILL_PERIOD_SECONDS = 60;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Verifica si una IP puede realizar un intento de login.
     *
     * @param clientIp dirección IP del cliente.
     * @return {@code true} si el intento está permitido.
     */
    public boolean tryConsume(String clientIp) {
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            long available = bucket.getAvailableTokens();
            log.warn("Rate limit alcanzado para IP '{}' — tokens disponibles: {}",
                    clientIp, available);
        }
        return allowed;
    }

    /**
     * Cantidad de tokens disponibles para una IP (para diagnóstico).
     */
    public long availableTokens(String clientIp) {
        Bucket bucket = buckets.get(clientIp);
        return bucket != null ? bucket.getAvailableTokens() : CAPACITY;
    }

    private Bucket createBucket(String key) {
        Bandwidth limit = Bandwidth.classic(
                CAPACITY,
                Refill.greedy(CAPACITY, Duration.ofSeconds(REFILL_PERIOD_SECONDS))
        );
        return Bucket.builder().addLimit(limit).build();
    }
}
