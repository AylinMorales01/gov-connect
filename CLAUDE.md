# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Gov Connect** (*Sistema Inteligente de Automatización y Analítica para Entidades Públicas*) is a platform for Colombian public entities. It centralizes financial, budgetary, and contractual indicators into executive dashboards, analytics, and automation. UI copy, docs, and code comments are in **Spanish**.

Monorepo with four independent areas (no root-level build tooling):

- `backend/` — Spring Boot REST API (Java 21, Maven, Spring Boot 4.x parent)
- `frontend/` — React 19 + TypeScript SPA (Vite, MUI, React Query, Recharts)
- `database/` — SQL Server schema/seed scripts + the DuckDB analytics file (`database/analytics/analytics.duckdb`)
- `docs/` — architecture documentation (Spanish); synced with the code as of the contracts/analytics milestone

## Commands

Backend:
- Run the API: `cd backend && ./mvnw spring-boot:run` (requires a reachable SQL Server, see Gotchas about CWD and DB name)
- Run tests: `cd backend && ./mvnw test`
- Package: `cd backend && ./mvnw package`

Frontend:
- Dev server: `cd frontend && npm run dev` (default http://localhost:5173)
- Build: `cd frontend && npm run build` (runs `tsc -b` then `vite build`)
- Lint: `cd frontend && npm run lint`

## Architecture

### Backend — domain-modular Spring Boot

Packages under `com.govconnect` are organized by business domain (ADR-006), each self-contained with `controller`, `service`, `repository`, `dto` subpackages:

- `dashboard` — operational KPIs served from SQL Server **views**.
- `analytics` — analytical queries served from **DuckDB**.
- `shared` — cross-cutting concerns: config (`DatabaseConfig`, `SecurityConfig`, `OpenApiConfig`), the `ApiResponse<T>` contract, `ApiMessages`, `GlobalExceptionHandler`.

Request flow is always Controller → Service → Repository → DB, with two coexisting data-access styles:

- `dashboard` uses JPA `EntityManager.createNativeQuery(...)` against SQL Server **views** (never tables directly — ADR-003). See `DashboardQueryRepository`.
- `analytics` uses raw JDBC `Statement`/`Connection` against the DuckDB connection bean.

**Dual-database split (ADR-007):** SQL Server is the transactional/operational store; DuckDB is the analytical engine. They are kept in sync by an ETL pipeline: `EtlService.runFullEtl()` → `ExportService` dumps `collections`, `departments`, `budgets`, `contracts` to CSV under `exports/` → `ImportService` loads each CSV into DuckDB via `CREATE OR REPLACE TABLE ... AS SELECT * FROM read_csv_auto(...)`. New analytics features must run the ETL (or extend it) before data exists to query. ETL is triggered manually via `POST /analytics/etl/run`.

### API contract

Every endpoint returns `ApiResponse<T>`: `{ success, message, timestamp, data }` (from `shared/response/ApiResponse.java`). Frontend clients unwrap it by reading `response.data.data`.

Base path `/api/v1`:
- Auth: `POST /auth/login`, `/auth/refresh`, `/auth/logout`; `GET /auth/me`
- `GET /dashboard/summary`, `/dashboard/monthly-collections`, `/dashboard/expiring-contracts`, `/dashboard/budget-execution` (ADMIN only)
- `GET /analytics/health`, `/monthly-trend`, `/financial-overview`, `/department-ranking`, `/collections-by-concept`, `/collections-by-payment-method`, `/contracts-by-status`, `/contracts-value-by-department`, `/top-contractors`
- `POST /analytics/etl/run` (async, returns `taskId`); `GET /analytics/etl/status/{taskId}`
- `GET /contracts`, `/contracts/summary`
- `POST /ingestion/contracts|budgets|collections` (async, `202` + `taskId`); `GET /ingestion/status/{taskId}`
- `POST /automation/logs`, `GET /automation/logs`; `POST /automation/expiring-contracts/alert` (ADMIN, alerta de correo)

Swagger UI is served by springdoc at `/swagger-ui`.

### Backend data sources (config)

- `DatabaseConfig` binds the `@Primary` `primaryDataSource` from `spring.datasource.primary` (SQL Server). A commented-out `erp` datasource shows the pattern for adding additional SQL Server connections.
- `DuckDbConfig` exposes a raw `Connection` bean to `jdbc:duckdb:database/analytics/analytics.duckdb`. The path is **relative to the process working directory**.
- `SecurityConfig` enforces JWT (cookie HttpOnly + `Authorization: Bearer` fallback): `/api/v1/dashboard/**`, `/api/v1/ingestion/**` and `/api/v1/automation/expiring-contracts/alert` require `ADMIN`; `/api/v1/analytics/**`, `/contracts/**`, `/automation/**` and `/auth/me` require authentication; `/auth/**` and Swagger are public. CORS is scoped to localhost:5173 / 3000.
- Spring Mail (`spring.mail.*`) envía la alerta de contratos por vencer (G3). Si `spring.mail.host` está vacío, la app arranca sin SMTP y la alerta registra el fallo en `automation_logs` sin detenerse. La alerta se configura con `app.alert.expiring-contracts.*` (enabled, recipients, days, from, cron).

### Frontend — feature-based React SPA

Feature-based layout under `frontend/src/`:
- `api/` — shared axios instance (`baseURL` from `VITE_API_URL`, default `http://localhost:8080/api/v1`) + typed endpoint functions that unwrap `ApiResponse`
- `hooks/` — React Query wrappers (`useDashboard`, `useMonthlyTrend`, `useDepartmentRanking`, `useExpiringContracts`)
- `context/` — `AuthContext` (session state, roles, login/logout)
- `pages/` — route views: `DashboardPage`, `AnalyticsPage`, `ContractsPage`, `AutomationPage`, `LoginPage`, `ForbiddenPage`. Dashboard/Analytics/Contracts are implemented; Automation is minimal (logs).
- `components/` — `cards/`, `charts/`, `tables/` reusable units
- `types/`, `utils/` — TS interfaces and formatters (currency, percentage, month)

Routing lives in `routes/AppRoutes.tsx`, nested under `MainLayout`.

## Conventions

Codified in `docs/03-Convenciones.md` and the ADRs (`docs/09-Architecture-Decision-Records.md`). The ones that affect how you write code:

- Java 21; DTOs are **immutable Java Records** (`ApiResponse.java` is the canonical example).
- **Constructor injection only** — never field injection (see `DashboardQueryService`).
- **No business logic in controllers** — thin controllers delegating to services.
- Dashboard queries go through **SQL views** (`vw_dashboard_summary`, `vw_monthly_collections`, `vw_contracts_expiring`, `vw_budget_execution`); no complex SQL in Java for the operational path.
- Log with **SLF4J**; log only meaningful events, never sensitive data.
- All API responses use `ApiResponse<T>`.

## Gotchas

- **Database name:** `database/sqlserver/01-create-database.sql` creates `GOV_CONNECT_DB`, matching `backend/src/main/resources/application.yaml`. `06-sprint72-migration.sql` seeds the `admin` and `usuario` test users into `GOV_CONNECT_DB`. Before importing a real SECOP II export, apply `08-secop-columns-migration.sql` (widens `contracts.object`/`contract_number`); `02-create-tables.sql` already creates them wide for fresh databases.
- **Run the backend with CWD at the repo root**, not `backend/`: the DuckDB file path (`database/analytics/analytics.duckdb`) and CSV exports (`exports/`) are relative to the process working directory. `cd backend && ./mvnw spring-boot:run` silently creates `backend/database/...` and `backend/exports/` instead. From the root, use `./backend/mvnw -f backend/pom.xml spring-boot:run`.
- `docs/` reflects the current code (Spring Boot 4.x, DuckDB and React implemented, endpoint names and routes synced). ADRs remain the authoritative design rationale; the code is the source of truth for endpoints.
- The context-loads test class is named `GovConnectApiApplicationTests` while the application class is `GovConnectApiApplication` — cosmetic, but don't be confused when grepping.
