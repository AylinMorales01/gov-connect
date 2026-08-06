# Base de Datos

Motor:

SQL Server Developer 2022

---

## Tablas

departments

budgets

contracts

collections

users

automation_logs

---

## Views

vw_dashboard_summary

vw_monthly_collections

vw_contracts_expiring

vw_budget_execution

---

## Estrategia

Las consultas del Dashboard consumen únicamente Views.

Esto desacopla la lógica del backend de la estructura física de las tablas.

---

## Datos de demostración

El proyecto incluye un conjunto de datos de ejemplo diseñado para demostrar las capacidades analíticas del sistema.

Características:

- 8 dependencias.
- 80 contratos.
- 24 meses de recaudos.
- Aproximadamente 500 registros de recaudo.
- Presupuestos con diferentes porcentajes de ejecución.

Estos datos no representan información real y tienen fines exclusivamente demostrativos.