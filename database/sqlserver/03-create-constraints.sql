-- ==========================================
-- FK: budgets.department_id → departments.id
-- ==========================================
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'FK_BUDGET_DEPARTMENT'
      AND parent_object_id = OBJECT_ID(N'dbo.budgets')
)
BEGIN
    ALTER TABLE budgets
        ADD CONSTRAINT FK_BUDGET_DEPARTMENT
            FOREIGN KEY (department_id)
                REFERENCES departments(id);
END
GO

-- ==========================================
-- FK: contracts.department_id → departments.id
-- ==========================================
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'FK_CONTRACT_DEPARTMENT'
      AND parent_object_id = OBJECT_ID(N'dbo.contracts')
)
BEGIN
    ALTER TABLE contracts
        ADD CONSTRAINT FK_CONTRACT_DEPARTMENT
            FOREIGN KEY (department_id)
                REFERENCES departments(id);
END
GO

-- ==========================================
-- FK: collections.department_id → departments.id
-- ==========================================
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'FK_COLLECTION_DEPARTMENT'
      AND parent_object_id = OBJECT_ID(N'dbo.collections')
)
BEGIN
    ALTER TABLE collections
        ADD CONSTRAINT FK_COLLECTION_DEPARTMENT
            FOREIGN KEY (department_id)
                REFERENCES departments(id);
END
GO

-- ──────────────────────────────────────────
-- NOTA: Las FKs para users/automation_logs se eliminan
-- intencionalmente. Estas tablas pertenecen al módulo de
-- autenticación (Sprint 6 del roadmap), aún no implementado.
-- Cuando ese módulo se aborde, se deberá:
--   1. Crear el DDL de users y automation_logs en 02-create-tables.sql
--   2. Agregar las FKs correspondientes en este archivo.
-- ──────────────────────────────────────────