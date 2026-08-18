package com.govconnect.automation.repository;

import com.govconnect.automation.dto.AutomationLogRequest;
import com.govconnect.automation.dto.AutomationLogResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para la inserción y consulta de registros de auditoría
 * de ejecuciones de automatización en la tabla {@code automation_logs}.
 * <p>
 * Utiliza SQL nativo a través del {@link EntityManager} de JPA,
 * sin entidades mapeadas ni Spring Data JPA, en consistencia
 * con el resto del proyecto.
 * </p>
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class AutomationLogRepository {

    private final EntityManager entityManager;

    private static final String INSERT_SQL = """
            INSERT INTO automation_logs (user_id, process, status, message, execution_time_ms)
            VALUES (:userId, :process, :status, :message, :executionTimeMs)
            """;

    private static final String SELECT_ALL = """
            SELECT
                id,
                user_id,
                process,
                status,
                message,
                execution_time_ms,
                created_at
            FROM automation_logs
            ORDER BY created_at DESC
            """;

    /**
     * Inserta un nuevo registro de automatización.
     *
     * @param request DTO con los datos de la ejecución (nunca {@code null})
     */
    public void insert(AutomationLogRequest request) {
        log.debug("Insertando registro de automatización: process={}, status={}",
                request.process(), request.status());

        entityManager.createNativeQuery(INSERT_SQL)
                .setParameter("userId", request.userId())
                .setParameter("process", request.process())
                .setParameter("status", request.status())
                .setParameter("message", request.message())
                .setParameter("executionTimeMs", request.executionTimeMs())
                .executeUpdate();

        log.info("Registro de automatización insertado correctamente");
    }

    /**
     * Recupera todas las ejecuciones de automatización,
     * ordenadas desde la más reciente.
     *
     * @return lista de registros (nunca {@code null}, puede estar vacía)
     */
    public List<AutomationLogResponse> findAll() {
        log.debug("Consultando historial completo de automatizaciones");

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(SELECT_ALL).getResultList();

        List<AutomationLogResponse> list = new ArrayList<>();
        for (Object[] row : results) {
            LocalDateTime createdAt = null;
            Object dateObj = row[6];

            if (dateObj instanceof java.sql.Timestamp ts) {
                createdAt = ts.toLocalDateTime();
            } else if (dateObj instanceof LocalDateTime ldt) {
                createdAt = ldt;
            }

            list.add(new AutomationLogResponse(
                    ((Number) row[0]).longValue(),
                    row[1] != null ? ((Number) row[1]).longValue() : null,
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    row[5] != null ? ((Number) row[5]).intValue() : null,
                    createdAt
            ));
        }

        log.info("Se recuperaron {} registros de automatización", list.size());
        return list;
    }
}
