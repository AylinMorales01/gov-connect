package com.govconnect.ingestion.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Repositorio de escritura para la ingesta de datos operacionales
 * (contratos, presupuestos y recaudos) en SQL Server.
 * <p>
 * Usa SQL nativo a través del {@link EntityManager}, en consistencia con
 * el resto del proyecto (ver {@code AutomationLogRepository}). Los métodos
 * de escritura se invocan dentro de una transacción del {@code IngestionService}.
 * </p>
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class IngestionRepository {

    private final EntityManager entityManager;

    // ── Dependencias ──────────────────────────────────────

    /**
     * Obtiene todas las dependencias (id, code, name) para resolver el mapeo
     * en memoria durante una importación.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findAllDepartments() {
        return entityManager.createNativeQuery(
                "SELECT id, code, name FROM departments").getResultList();
    }

    /**
     * Devuelve el id de un departamento por su código, o {@code null} si no existe.
     */
    public Long findDepartmentIdByCode(String code) {
        List<?> result = entityManager.createNativeQuery(
                        "SELECT id FROM departments WHERE code = :code")
                .setParameter("code", code)
                .getResultList();
        return result.isEmpty() ? null : ((Number) result.get(0)).longValue();
    }

    /**
     * Inserta un departamento nuevo (sin {@code id}, autogenerado). Usado para
     * asegurar el departamento de respaldo "Sin asignar" antes de importar
     * contratos de entidades no reconocidas.
     */
    public void insertDepartment(String code, String name, String description) {
        entityManager.createNativeQuery("""
                        INSERT INTO departments (code, name, description)
                        VALUES (:code, :name, :description)
                        """)
                .setParameter("code", code)
                .setParameter("name", name)
                .setParameter("description", description)
                .executeUpdate();
    }

    // ── Contratos ─────────────────────────────────────────

    /**
     * Devuelve los números de contrato ya registrados, para resolver el upsert
     * en memoria. Un export de SECOP II trae decenas de miles de filas y
     * consultar la existencia una por una dominaba el tiempo de importación.
     */
    @SuppressWarnings("unchecked")
    public Set<String> findAllContractNumbers() {
        List<String> numbers = entityManager.createNativeQuery(
                "SELECT contract_number FROM contracts").getResultList();
        return new HashSet<>(numbers);
    }

    public void insertContract(String contractNumber, String contractorName, String object,
                               BigDecimal value, LocalDate startDate, LocalDate endDate,
                               String status, Long departmentId) {
        entityManager.createNativeQuery("""
                        INSERT INTO contracts
                            (contract_number, contractor_name, object, contract_value,
                             start_date, end_date, status, department_id)
                        VALUES
                            (:contractNumber, :contractorName, :object, :value,
                             :startDate, :endDate, :status, :departmentId)
                        """)
                .setParameter("contractNumber", contractNumber)
                .setParameter("contractorName", contractorName)
                .setParameter("object", object)
                .setParameter("value", value)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("status", status)
                .setParameter("departmentId", departmentId)
                .executeUpdate();
    }

    public void updateContract(String contractNumber, String contractorName, String object,
                               BigDecimal value, LocalDate startDate, LocalDate endDate,
                               String status, Long departmentId) {
        entityManager.createNativeQuery("""
                        UPDATE contracts SET
                            contractor_name = :contractorName,
                            object = :object,
                            contract_value = :value,
                            start_date = :startDate,
                            end_date = :endDate,
                            status = :status,
                            department_id = :departmentId,
                            updated_at = GETDATE()
                        WHERE contract_number = :contractNumber
                        """)
                .setParameter("contractNumber", contractNumber)
                .setParameter("contractorName", contractorName)
                .setParameter("object", object)
                .setParameter("value", value)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("status", status)
                .setParameter("departmentId", departmentId)
                .executeUpdate();
    }

    // ── Presupuestos ──────────────────────────────────────

    public boolean budgetExists(Long departmentId, int fiscalYear) {
        Object count = entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM budgets WHERE department_id = :departmentId AND fiscal_year = :fiscalYear")
                .setParameter("departmentId", departmentId)
                .setParameter("fiscalYear", fiscalYear)
                .getSingleResult();
        return ((Number) count).longValue() > 0;
    }

    public void insertBudget(Long departmentId, int fiscalYear,
                             BigDecimal assigned, BigDecimal executed, BigDecimal available) {
        entityManager.createNativeQuery("""
                        INSERT INTO budgets (department_id, fiscal_year, assigned_budget, executed_budget, available_budget)
                        VALUES (:departmentId, :fiscalYear, :assigned, :executed, :available)
                        """)
                .setParameter("departmentId", departmentId)
                .setParameter("fiscalYear", fiscalYear)
                .setParameter("assigned", assigned)
                .setParameter("executed", executed)
                .setParameter("available", available)
                .executeUpdate();
    }

    public void updateBudget(Long departmentId, int fiscalYear,
                             BigDecimal assigned, BigDecimal executed, BigDecimal available) {
        entityManager.createNativeQuery("""
                        UPDATE budgets SET
                            assigned_budget = :assigned,
                            executed_budget = :executed,
                            available_budget = :available,
                            updated_at = GETDATE()
                        WHERE department_id = :departmentId AND fiscal_year = :fiscalYear
                        """)
                .setParameter("departmentId", departmentId)
                .setParameter("fiscalYear", fiscalYear)
                .setParameter("assigned", assigned)
                .setParameter("executed", executed)
                .setParameter("available", available)
                .executeUpdate();
    }

    // ── Recaudos ──────────────────────────────────────────

    public void insertCollection(LocalDate date, String concept, String taxpayer,
                                 BigDecimal amount, String paymentMethod, Long departmentId) {
        entityManager.createNativeQuery("""
                        INSERT INTO collections (collection_date, concept, taxpayer, amount, payment_method, department_id)
                        VALUES (:date, :concept, :taxpayer, :amount, :paymentMethod, :departmentId)
                        """)
                .setParameter("date", date)
                .setParameter("concept", concept)
                .setParameter("taxpayer", taxpayer)
                .setParameter("amount", amount)
                .setParameter("paymentMethod", paymentMethod)
                .setParameter("departmentId", departmentId)
                .executeUpdate();
    }
}
