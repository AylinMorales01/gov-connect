# Arquitectura del Sistema

Gov Connect implementa una arquitectura modular orientada al dominio (ADR-006).

Módulos actuales:

```
dashboard    — KPIs operacionales desde vistas de SQL Server
analytics    — consultas analíticas desde DuckDB
contracts    — gestión de contratos
ingestion    — ingesta de datos operacionales desde CSV (SECOP, presupuestos, recaudos)
automation   — registro de ejecuciones de automatización + alerta de contratos por vencer por correo (Spring Mail/SMTP)
auth         — autenticación JWT y roles
shared       — configuración y utilidades transversales
```

Cada módulo es autocontenido con `controller`, `service`, `repository`, `dto`.

---

## Bases de datos (ADR-007)

Dos motores con responsabilidades separadas:

- **SQL Server** — almacén transaccional/operacional (CRUD, datos diarios).
- **DuckDB** — motor analítico embebido (KPIs, tendencias, comparativos).

Se mantienen en sincronía mediante un **ETL asíncrono** (ver `08-DuckDB.md`):

```
SQL Server ──ExportService (CSV)──▶ exports/ ──ImportService (read_csv_auto)──▶ DuckDB
```

---

## Flujo de una petición

    Cliente
        ↓
    Controller  (delgado, sin lógica de negocio)
        ↓
    Service     (lógica de negocio)
        ↓
    Repository  (acceso a datos)
        ↓
    SQL Server (dashboard/contracts)  ó  DuckDB (analytics)
        ↓
    ApiResponse<T>  (contrato único de respuesta)

---

## Componentes

- **Controller** — expone la API REST (thin).
- **Service** — implementa la lógica de negocio.
- **Repository** — accede a los datos (vistas de SQL Server / JDBC en DuckDB).
- **DTO** — Java Records inmutables.
- **Shared** — `config`, `constants`, `exception`, `response` (`ApiResponse<T>`).
