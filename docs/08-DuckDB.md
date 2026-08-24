# Integración DuckDB

DuckDB es el motor analítico embebido. **No reemplaza** SQL Server (ADR-007).

## SQL Server (operacional)

Almacenamiento transaccional: CRUD y operaciones diarias.

## DuckDB (analítico)

KPIs, comparativos, tendencias y reportes.

Archivo: `database/analytics/analytics.duckdb` (ruta relativa al CWD del proceso).

## ETL (implementado, asíncrono)

1. `EtlService.runFullEtl()` orquesta el proceso.
2. `ExportService` vuelca `collections`, `departments`, `budgets` y `contracts` a CSV en `exports/`.
3. `ImportService` carga cada CSV con `read_csv_auto` y `CREATE OR REPLACE TABLE`.

El ETL se dispara vía `POST /analytics/etl/run` y devuelve un `taskId`; el estado se
consulta en `GET /analytics/etl/status/{taskId}`.

## Flujo

```
SQL Server ──▶ exports/*.csv ──▶ DuckDB ──▶ Analytics ──▶ API / Dashboard
```
