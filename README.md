# Gov Connect

**Sistema Inteligente de Automatización y Analítica para Entidades Públicas**

## Descripción

Gov Connect es una plataforma web para entidades públicas colombianas que centraliza indicadores financieros, presupuestales y contractuales en un **dashboard ejecutivo**, **analítica de datos** y **automatización de procesos**.

Resuelve el problema de tener la información dispersa (SECOP, hojas de cálculo, sistemas financieros): unifica contratos, presupuestos y recaudos en un solo lugar, los transforma en indicadores accionables (p. ej. *qué contratos vencen en los próximos 30 días*) y automatiza la notificación de eventos críticos por correo.

Está pensado para directores/as de contratación, secretarías de hacienda/planeación y equipos de control de entidades públicas.

## Características principales

- **Dashboard ejecutivo** (rol `ADMIN`): resumen ejecutivo, recaudos mensuales, contratos por vencer y ejecución presupuestal por dependencia.
- **Analítica de datos** (DuckDB): tendencia mensual, resumen financiero, ranking de dependencias, recaudos por concepto y medio de pago, contratos por estado, valor contratado por dependencia y top de contratistas.
- **Gestión de contratos**: catálogo con filtros por estado y búsqueda libre, más resumen agregado.
- **ETL asíncrono**: sincroniza SQL Server → DuckDB (con `taskId` para seguir el avance).
- **Ingesta de datos desde CSV**: importación asíncrona de contratos (SECOP II), presupuestos y recaudos.
- **Automatización**: alerta de contratos por vencer enviada por correo (SMTP), con ejecución programada o manual, y registro de ejecuciones en `automation_logs`.
- **Autenticación JWT** con cookies `HttpOnly`, refresh token rotativo y rate limiting en el login.
- **Control de acceso por roles** (`ADMIN` / `USER`).

## Arquitectura

Arquitectura modular orientada al dominio (ADR-006), separada en dos capas: una SPA React (frontend) y una API REST Spring Boot (backend). Los datos se dividen en dos motores (ADR-007):

```mermaid
flowchart LR
    subgraph Frontend["Frontend — SPA React"]
        FE["React 19 + MUI + React Query"]
    end

    subgraph Backend["Backend — Spring Boot (Java 21)"]
        C["Controllers REST"]
        S["Services"]
        R["Repositories"]
        SEC["Spring Security + JWT"]
    end

    subgraph Datos["Almacenamiento"]
        SQL[("SQL Server<br/>operacional")]
        DUCK[("DuckDB<br/>analítico")]
    end

    FE -- "HTTPS (REST JSON)" --> C
    C --> S --> R
    R --> SQL
    R --> DUCK
    SEC -. "autentica y autoriza" .-> C

    SQL -- "ETL: ExportService (CSV)" --> EXP["exports/*.csv"]
    EXP -- "ImportService (read_csv_auto)" --> DUCK
```

- **SQL Server** es el almacén transaccional/operacional (CRUD, datos diarios). El dashboard consulta **vistas** (`vw_*`), nunca tablas directamente (ADR-003).
- **DuckDB** es el motor analítico embebido (KPIs, tendencias, comparativos). Se alimenta por ETL.
- Cada módulo (`dashboard`, `analytics`, `contracts`, `ingestion`, `automation`, `auth`, `shared`) es autocontenido con `controller`, `service`, `repository` y `dto`.

## Stack tecnológico

| Categoría | Tecnologías |
|---|---|
| **Backend** | Java 21, Spring Boot 4.1, Maven (wrapper) |
| **Frontend** | React 19, TypeScript 6, Vite 8, Material UI 9, React Query 5, Recharts 3, React Router 7 |
| **Bases de datos** | SQL Server (operacional), DuckDB 1.3 (analítico) |
| **Analítica / ETL** | DuckDB JDBC, Apache Commons CSV, export/import CSV |
| **Seguridad** | Spring Security, JWT (jjwt 0.12.5), BCrypt, Bucket4j (rate limiting) |
| **Automatización** | Spring Mail (SMTP), scheduling por cron |
| **Documentación / API** | springdoc-openapi (Swagger UI), Spring Boot Actuator |

## Estructura del proyecto

```
gov-connect/
├── backend/            # API REST — Spring Boot (Java 21, Maven)
│   └── src/main/java/com/govconnect/
│       ├── auth/       # autenticación JWT y roles
│       ├── dashboard/  # KPIs operacionales (vistas SQL Server)
│       ├── analytics/  # analítica DuckDB + ETL
│       ├── contracts/  # catálogo de contratos
│       ├── ingestion/  # ingesta asíncrona de CSV
│       ├── automation/ # alertas por correo + logs
│       └── shared/     # config, constantes, excepciones, ApiResponse
├── frontend/           # SPA — React 19 + TypeScript (Vite)
│   └── src/            # api/, components/, context/, hooks/, pages/, routes/, types/, utils/
├── database/
│   ├── sqlserver/      # DDL: tablas, constraints, índices, vistas y migraciones
│   ├── seed/           # datos semilla de demostración
│   └── analytics/      # archivo DuckDB (analytics.duckdb — NO versionado)
├── docs/               # documentación de arquitectura y decisiones
└── exports/            # CSV generados por el ETL (NO versionados)
```

## Módulos principales

| Módulo | Responsabilidad |
|---|---|
| `auth` | Login, refresh y logout con JWT en cookies `HttpOnly`; roles `ADMIN`/`USER`. |
| `dashboard` | KPIs operacionales servidos desde vistas de SQL Server (exclusivo `ADMIN`). |
| `analytics` | Consultas analíticas sobre DuckDB + orquestación del ETL asíncrono. |
| `contracts` | Listado del catálogo de contratos con filtros y resumen agregado. |
| `ingestion` | Importación asíncrona de CSV (contratos SECOP, presupuestos, recaudos). |
| `automation` | Alerta de contratos por vencer por correo (SMTP) + registro de ejecuciones. |
| `shared` | Configuración, `ApiResponse<T>`, mensajes, excepciones y utilidades transversales. |

## Requisitos previos

| Componente | Requisito | Versión |
|---|---|---|
| **Backend** | Java JDK | 21+ |
| **Backend** | Maven | wrapper incluido (`./backend/mvnw`) |
| **Base de datos** | SQL Server | 2017+ (desarrollo probado sobre SQL Server 2025) |
| **Frontend** | Node.js | ≥ 20.19 (requerido por Vite 8) |
| **Analítica** | DuckDB | embebido, sin instalación (se resuelve por Maven) |

> No es necesario instalar Maven ni DuckDB manualmente: el wrapper de Maven y el driver JDBC de DuckDB se resuelven solos.

## Instalación y ejecución

### 1. Clonar el repositorio

```bash
git clone https://github.com/AylinMorales01/gov-connect.git
cd gov-connect
```

### 2. Configurar variables de entorno

```bash
cp .env.example .env
```

Edita `.env` y completa las variables con tus valores (ver [Configuración](#configuración)).

### 3. Crear la base de datos

Ejecuta los scripts SQL en orden desde `database/sqlserver/`:

```
01-create-database.sql
02-create-tables.sql
03-create-constraints.sql
04-create-indexes.sql
05-create-views.sql
06-sprint72-migration.sql        # usuarios de prueba (admin / usuario)
07-refresh-token-migration.sql   # columna token_version (solo si migras una BD existente)
08-secop-columns-migration.sql   # amplía columnas de contracts para SECOP II (antes de importar un export real)
```

> **Nota**: para una base de datos nueva, `02-create-tables.sql` ya crea las columnas anchas de `contracts`; `08` solo es necesario si importas un export real de SECOP II sobre una base antigua. `07` aplica únicamente a bases creadas antes de la migración de refresh tokens.

(Opcional) Carga datos semilla de demostración desde `database/seed/`.

### 4. Iniciar el backend

> **Importante**: ejecuta desde la **raíz del repositorio** (no desde `backend/`). Las rutas relativas de DuckDB (`database/analytics/…`) y de `exports/` dependen de ello.

```bash
./backend/mvnw -f backend/pom.xml spring-boot:run
```

- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui`
- Actuator: `http://localhost:8080/actuator/health`

### 5. Iniciar el frontend

```bash
cd frontend
npm install
npm run dev
```

La aplicación estará disponible en `http://localhost:5173`.

## Configuración

Todas las variables están documentadas en `.env.example`. Nunca se commitean valores reales: `.env`, claves y certificados están excluidos por `.gitignore`.

```env
# Backend — SQL Server
GOVCONNECT_DB_URL=jdbc:sqlserver://localhost:1433;databaseName=GOV_CONNECT_DB;encrypt=true;trustServerCertificate=true
GOVCONNECT_DB_USERNAME=your_username
GOVCONNECT_DB_PASSWORD=your_password

# JWT (obligatorio; la app no inicia sin JWT_SECRET)
JWT_SECRET=your_secret_32_chars_min
JWT_EXPIRATION=3600
JWT_REFRESH_EXPIRATION=604800

# DuckDB / ETL
DUCKDB_PATH=database/analytics/analytics.duckdb
ETL_EXPORT_DIR=exports

# SMTP (alerta de contratos por vencer; host vacío => la alerta no envía correo)
SMTP_HOST=
SMTP_PORT=587
SMTP_USERNAME=your_username
SMTP_PASSWORD=your_password

# Alerta programada
ALERT_EXPIRING_ENABLED=true
ALERT_EXPIRING_RECIPIENTS=destinatario1@example.com,destinatario2@example.com
ALERT_EXPIRING_DAYS=30
ALERT_EXPIRING_CRON=0 0 7 * * MON-FRI

# Perfiles / entorno
SPRING_PROFILES_ACTIVE=dev
SWAGGER_ENABLED=true
SERVER_PORT=8080

# Frontend
VITE_API_URL=http://localhost:8080/api/v1
```

## API

- **Base URL**: `/api/v1`
- **Swagger/OpenAPI**: `http://localhost:8080/swagger-ui` (habilitado en `dev`, deshabilitado en `prod`).
- **Contrato**: todas las respuestas usan `ApiResponse<T>` → `{ success, message, timestamp, data }`.

Grupos de endpoints:

| Grupo | Acceso | Endpoints principales |
|---|---|---|
| Auth | público | `POST /auth/login`, `/auth/refresh`, `/auth/logout` |
| Auth | autenticado | `GET /auth/me` |
| Dashboard | ADMIN | `GET /dashboard/summary`, `/monthly-collections`, `/expiring-contracts`, `/budget-execution` |
| Analytics | autenticado | `GET /analytics/health`, `/monthly-trend`, `/financial-overview`, `/department-ranking`, `/collections-by-concept`, `/collections-by-payment-method`, `/contracts-by-status`, `/contracts-value-by-department`, `/top-contractors` |
| Analytics (ETL) | autenticado | `POST /analytics/etl/run`, `GET /analytics/etl/status/{taskId}` |
| Contracts | autenticado | `GET /contracts`, `/contracts/summary` |
| Ingestion | ADMIN | `POST /ingestion/contracts\|budgets\|collections`, `GET /ingestion/status/{taskId}` |
| Automation | autenticado | `POST /automation/logs`, `GET /automation/logs` |
| Automation | ADMIN | `POST /automation/expiring-contracts/alert` |

El detalle completo de cada endpoint está en `docs/06-API.md` y en Swagger.

## Flujo de datos

1. **Ingesta**: un CSV (SECOP II, presupuestos o recaudos) se sube por `POST /ingestion/*`, se valida/normaliza y se inserta en **SQL Server**.
2. **ETL**: `POST /analytics/etl/run` (o automáticamente al terminar una ingesta) exporta las tablas a `exports/*.csv` y las carga en **DuckDB** con `read_csv_auto`.
3. **Consulta**: los endpoints de `/analytics` leen de DuckDB; el dashboard y los contratos leen de SQL Server.
4. **Presentación**: la SPA consume la API y muestra gráficas y tablas.

```
CSV → SQL Server → ExportService (CSV) → exports/ → ImportService → DuckDB → Analytics API → Frontend
```

## Automatización

- **Alerta de contratos por vencer**: detecta contratos `ACTIVE` que vencen en los próximos `days` días y envía un correo HTML a los destinatarios configurados. Se ejecuta por cron (`app.alert.expiring-contracts.cron`, por defecto lunes–viernes 7:00) o manualmente con `POST /automation/expiring-contracts/alert` (ADMIN). El envío se hace de forma nativa con **Spring Mail (SMTP)**.
- **Registro de ejecuciones**: cada ejecución queda registrada en `automation_logs` (estado `SUCCESS`/`ERROR`/`SKIPPED`, proceso, mensaje y tiempo). La SPA lo muestra en la página de *Automatización*.
- **Integración futura**: los endpoints `POST /automation/logs` y `GET /automation/logs` son el punto de integración para registrar ejecuciones de orquestadores externos. Workflows tipo **n8n** podrían llegar a implementarse en el futuro llamando a estos endpoints, pero hoy no hay workflows n8n versionados en el repositorio.

Si `spring.mail.host` está vacío o no hay destinatarios, la alerta registra el fallo en `automation_logs` sin detener la aplicación.

## Seguridad

- **Autenticación JWT**: el access token y el refresh token viajan en cookies `HttpOnly` (`access_token`, `refresh_token`) con `SameSite=Lax`, sin exponerlos a JavaScript (ADR-009).
- **Rotación de refresh token**: cada uso emite un nuevo par e invalida el anterior; el logout incrementa `token_version` para invalidar todos los tokens emitidos (invalidación server-side).
- **Roles**: `ADMIN` (dashboard, ingesta, alerta) y `USER` (analítica, contratos, automatización). El backend es la fuente de verdad de la autorización.
- **Rate limiting**: 5 intentos de login por minuto por IP (Bucket4j en memoria).
- **CSRF**: mitigado con cookies `SameSite=Lax` (CSRF deshabilitado en el servidor).
- **Secretos**: `.env`, claves y certificados nunca se versionan (ver `.gitignore`).

## Estado actual del proyecto

### Implementado

- Dashboard ejecutivo, analítica de datos y catálogo de contratos.
- ETL asíncrono SQL Server → DuckDB y consulta de estado (`taskId`).
- Ingesta asíncrona de CSV (contratos SECOP II, presupuestos y recaudos).
- Autenticación JWT con cookies `HttpOnly`, refresh rotativo y rate limiting.
- Alerta de contratos por vencer por correo (SMTP) con cron y disparo manual.
- Registro y consulta de ejecuciones de automatización (`automation_logs`).

### En desarrollo o pendiente

- **Workflows de orquestación externos tipo n8n**: los endpoints de `automation_logs` están listos para recibir ejecuciones de herramientas externas, pero aún no hay workflows n8n implementados.
- **Cobertura de datos reales**: la calidad de la ingesta depende del formato del CSV de SECOP II (se normaliza mediante `SecopColumnMapper`); formatos distintos requieren ajustar el mapeo.

## Documentación adicional

- `docs/01-Arquitectura.md` — Arquitectura general del sistema
- `docs/02-Estructura-Proyecto.md` — Estructura detallada del código
- `docs/03-Convenciones.md` — Convenciones de desarrollo
- `docs/04-Dashboard.md` — Módulo de dashboard ejecutivo
- `docs/05-Base-de-Datos.md` — Esquema de SQL Server y estrategia de datos
- `docs/06-API.md` — Referencia de la API REST
- `docs/07-Roadmap.md` — Historial y estado de avance
- `docs/08-DuckDB.md` — Integración con DuckDB y ETL
- `docs/09-Architecture-Decision-Records.md` — Decisiones de arquitectura (ADRs)
- `docs/10-Frontend.md` — Arquitectura del frontend
- `docs/11-Caso-de-Uso-MVP.md` — Caso de uso mínimo cerrado (MVP)
- `CLAUDE.md` — Guía para asistentes de IA que trabajen en el proyecto
