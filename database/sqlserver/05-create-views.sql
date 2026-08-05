CREATE VIEW vw_dashboard_summary AS
SELECT
    (SELECT ISNULL(SUM(amount),0)
     FROM collections
     WHERE MONTH(collection_date)=MONTH(GETDATE())
       AND YEAR(collection_date)=YEAR(GETDATE())) AS collections_this_month,

    (SELECT COUNT(*)
     FROM contracts
     WHERE status='ACTIVE') AS active_contracts,

    (SELECT COUNT(*)
     FROM contracts
     WHERE end_date BETWEEN GETDATE() AND DATEADD(DAY,30,GETDATE())) AS contracts_expiring,

    (SELECT
         CASE
             WHEN SUM(assigned_budget)=0 THEN 0
             ELSE (SUM(executed_budget)*100.0)/SUM(assigned_budget)
             END
     FROM budgets) AS budget_execution_percentage;

-- ==========================================
-- 1. Vista: Recaudo mensual (vw_monthly_collections)
-- ==========================================
CREATE OR ALTER VIEW vw_monthly_collections AS
SELECT
    MONTH(collection_date) AS month_number,
    DATENAME(MONTH, collection_date) AS month_name,
    SUM(amount) AS total_amount
FROM collections
GROUP BY
    MONTH(collection_date),
    DATENAME(MONTH, collection_date);
GO

-- ==========================================
-- 2. Vista: Contratos próximos a vencer (vw_contracts_expiring)
-- ==========================================
CREATE OR ALTER VIEW vw_contracts_expiring AS
SELECT
    contract_number,
    contractor_name,
    end_date,
    DATEDIFF(DAY, GETDATE(), end_date) AS remaining_days
FROM contracts
WHERE end_date BETWEEN GETDATE() AND DATEADD(DAY, 30, GETDATE());
GO

-- ==========================================
-- 3. Vista: Ejecución presupuestal (vw_budget_execution)
-- ==========================================
CREATE OR ALTER VIEW vw_budget_execution AS
SELECT
    d.name AS department,
    b.assigned_budget,
    b.executed_budget,
    CASE
        WHEN b.assigned_budget = 0 THEN 0
        ELSE (b.executed_budget * 100.0) / b.assigned_budget
        END AS percentage
FROM budgets b
         JOIN departments d ON b.department_id = d.id;
GO