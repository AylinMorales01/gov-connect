-- ==========================================
-- Índices de rendimiento para Gov Connect
-- ==========================================
-- Todos los CREATE INDEX usan IF NOT EXISTS
-- (ejecución idempotente).

-- contracts: filtrado por estado
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IDX_CONTRACT_STATUS')
    CREATE INDEX IDX_CONTRACT_STATUS ON contracts(status);

-- contracts: filtrado por fecha de vencimiento
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IDX_CONTRACT_END_DATE')
    CREATE INDEX IDX_CONTRACT_END_DATE ON contracts(end_date);

-- collections: agrupación por fecha (tendencia mensual)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IDX_COLLECTION_DATE')
    CREATE INDEX IDX_COLLECTION_DATE ON collections(collection_date);

-- collections: JOIN con departments
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IDX_COLLECTION_DEPARTMENT')
    CREATE INDEX IDX_COLLECTION_DEPARTMENT ON collections(department_id);

-- ==========================================
-- Nuevos índices (Sprint 7.3)
-- ==========================================

-- collections: búsqueda y agrupación por concepto de recaudo
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IDX_COLLECTION_CONCEPT')
    CREATE INDEX IDX_COLLECTION_CONCEPT ON collections(concept);

-- budgets: filtrado por año fiscal (usado en ETL y vistas)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IDX_BUDGET_FISCAL_YEAR')
    CREATE INDEX IDX_BUDGET_FISCAL_YEAR ON budgets(fiscal_year);

-- budgets: JOIN con departments (usado en ranking y ejecución presupuestal)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IDX_BUDGET_DEPARTMENT')
    CREATE INDEX IDX_BUDGET_DEPARTMENT ON budgets(department_id);