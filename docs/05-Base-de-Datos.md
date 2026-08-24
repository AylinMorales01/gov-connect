# Base de Datos

Motores:

- **SQL Server** (operacional) — instancia local de desarrollo: SQL Server 2025.
- **DuckDB** (analítico) — embebido, `database/analytics/analytics.duckdb`.

---

## Tablas (SQL Server)

- `departments`
- `budgets`
- `contracts`
- `collections`
- `users` (`username`, `email`, `password_hash`, `full_name`, `role`, `active`, `token_version`, …)
- `automation_logs`

## Views (SQL Server)

- `vw_dashboard_summary`
- `vw_monthly_collections`
- `vw_contracts_expiring`
- `vw_budget_execution`

---

## Estrategia

- El Dashboard consume únicamente **Views** (desacopla el backend de la estructura física).
- La analítica se sirve desde **DuckDB** (ver `08-DuckDB.md`), alimentada por ETL.

---

## Datos de demostración

Datos de ejemplo para demostrar capacidades analíticas (no representan información real):

- 8 dependencias.
- 8 presupuestos.
- 75 contratos.
- 24 meses de recaudos (15 registros/mes ≈ 360 registros).
