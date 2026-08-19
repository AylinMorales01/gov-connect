-- ==========================================
-- FK: budgets.department_id - departments.id
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
-- FK: contracts.department_id - departments.id
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
-- FK: collections.department_id - departments.id
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

-- ==========================================
-- FK: automation_logs.user_id - users.id
-- ==========================================
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'FK_AUTOMATION_LOG_USER'
      AND parent_object_id = OBJECT_ID(N'dbo.automation_logs')
)
BEGIN
    ALTER TABLE automation_logs
        ADD CONSTRAINT FK_AUTOMATION_LOG_USER
            FOREIGN KEY (user_id)
                REFERENCES users(id);
END
GO
