package com.govconnect.contracts.repository;

import com.govconnect.contracts.dto.ContractResponse;
import com.govconnect.contracts.dto.ContractSummaryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de solo lectura para el catálogo de contratos.
 * <p>
 * Consulta la tabla {@code contracts} (JOIN a {@code departments}) mediante
 * SQL nativo a través del {@link EntityManager}, en consistencia con el resto
 * del proyecto (ver {@code AutomationLogRepository}). El filtrado dinámico por
 * estado y búsqueda justifica la consulta directa en lugar de una vista.
 * </p>
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ContractRepository {

    private final EntityManager entityManager;

    /**
     * Lista contratos con filtros opcionales por estado y búsqueda libre.
     *
     * @param status estado exacto (ACTIVE, SUSPENDED, FINISHED) o {@code null}/blanco.
     * @param search término de búsqueda sobre número, contratista u objeto, o {@code null}/blanco.
     * @return lista de contratos ordenados por fecha de fin ascendente (nunca {@code null}).
     */
    @SuppressWarnings("unchecked")
    public List<ContractResponse> findAll(String status, String search) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    c.id,
                    c.contract_number,
                    c.contractor_name,
                    c.object,
                    c.contract_value,
                    c.start_date,
                    c.end_date,
                    c.status,
                    d.name
                FROM contracts c
                JOIN departments d ON c.department_id = d.id
                """);

        List<String> predicates = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            predicates.add("c.status = :status");
        }
        if (search != null && !search.isBlank()) {
            predicates.add("(c.contract_number LIKE :search OR c.contractor_name LIKE :search OR c.object LIKE :search)");
        }
        if (!predicates.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", predicates));
        }
        sql.append(" ORDER BY c.end_date ASC");

        Query query = entityManager.createNativeQuery(sql.toString());
        if (status != null && !status.isBlank()) {
            query.setParameter("status", status);
        }
        if (search != null && !search.isBlank()) {
            query.setParameter("search", "%" + search + "%");
        }

        List<Object[]> rows = query.getResultList();

        List<ContractResponse> list = new ArrayList<>();
        for (Object[] row : rows) {
            list.add(new ContractResponse(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (BigDecimal) row[4],
                    toLocalDate(row[5]),
                    toLocalDate(row[6]),
                    (String) row[7],
                    (String) row[8]
            ));
        }
        log.info("Se recuperaron {} contratos", list.size());
        return list;
    }

    /**
     * Calcula métricas agregadas del catálogo de contratos.
     *
     * @return resumen con totales y conteos por estado (nunca {@code null}).
     */
    public ContractSummaryResponse getSummary() {
        String sql = """
                SELECT
                    COUNT(*) AS total_contracts,
                    COALESCE(SUM(contract_value), 0) AS total_value,
                    SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_contracts,
                    SUM(CASE WHEN status = 'SUSPENDED' THEN 1 ELSE 0 END) AS suspended_contracts,
                    SUM(CASE WHEN status = 'FINISHED' THEN 1 ELSE 0 END) AS finished_contracts
                FROM contracts
                """;

        Object[] row = (Object[]) entityManager.createNativeQuery(sql).getSingleResult();

        return new ContractSummaryResponse(
                ((Number) row[0]).intValue(),
                (BigDecimal) row[1],
                ((Number) row[2]).intValue(),
                ((Number) row[3]).intValue(),
                ((Number) row[4]).intValue()
        );
    }

    /**
     * Convierte el valor de una columna DATE/TIMESTAMP en {@link LocalDate}.
     */
    private LocalDate toLocalDate(Object dateObj) {
        if (dateObj == null) {
            return null;
        }
        if (dateObj instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (dateObj instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        if (dateObj instanceof LocalDate ld) {
            return ld;
        }
        return null;
    }
}
