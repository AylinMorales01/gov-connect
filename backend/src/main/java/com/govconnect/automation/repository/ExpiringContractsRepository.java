package com.govconnect.automation.repository;

import com.govconnect.automation.dto.ExpiringContractAlertItem;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de solo lectura para los contratos por vencer usados por la
 * alerta de correo (G3).
 * <p>
 * Consulta la tabla {@code contracts} (JOIN a {@code departments}) mediante
 * SQL nativo, filtrando contratos {@code ACTIVE} cuyo {@code end_date} cae
 * dentro de los próximos {@code days} días. El filtro de ventana es
 * parametrizable (a diferencia de la vista {@code vw_contracts_expiring},
 * fija en 30 días) para respetar {@code app.alert.expiring-contracts.days}.
 * </p>
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ExpiringContractsRepository {

    private final EntityManager entityManager;

    /**
     * Devuelve los contratos activos que vencen dentro de los próximos {@code days} días,
     * ordenados del que vence antes al que vence después.
     *
     * @param days ventana de días hacia adelante (nunca negativo).
     * @return lista de contratos por vencer (nunca {@code null}, puede estar vacía).
     */
    @SuppressWarnings("unchecked")
    public List<ExpiringContractAlertItem> findExpiring(int days) {
        String sql = """
                SELECT
                    c.contract_number,
                    c.contractor_name,
                    c.object,
                    c.contract_value,
                    c.end_date,
                    DATEDIFF(DAY, GETDATE(), c.end_date) AS remaining_days,
                    d.name
                FROM contracts c
                JOIN departments d ON c.department_id = d.id
                WHERE c.status = 'ACTIVE'
                  AND c.end_date BETWEEN GETDATE() AND DATEADD(DAY, :days, GETDATE())
                ORDER BY remaining_days ASC
                """;

        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("days", days)
                .getResultList();

        List<ExpiringContractAlertItem> list = new ArrayList<>();
        for (Object[] row : rows) {
            list.add(new ExpiringContractAlertItem(
                    (String) row[0],
                    (String) row[1],
                    (String) row[2],
                    (BigDecimal) row[3],
                    toLocalDate(row[4]),
                    ((Number) row[5]).intValue(),
                    (String) row[6]
            ));
        }
        log.info("Se encontraron {} contratos por vencer en {} días", list.size(), days);
        return list;
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
