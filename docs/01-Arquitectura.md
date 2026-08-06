# Arquitectura del Sistema

Gov Connect implementa una arquitectura modular orientada al dominio.

Cada módulo representa un área funcional independiente.

Actualmente el proyecto está conformado por:

```
dashboard
shared
```

En las siguientes fases se incorporarán:

```
analytics
automation
auth
reports
```

---

## Flujo de una petición

    Cliente
        ↓
    Controller
        ↓
    Service
        ↓
    Repository
        ↓
    SQL Server
        ↓
    Exportación (CSV)
        ↓
    DuckDB
        ↓
    Analytics Module
        ↓
    REST API v1

---

## Componentes

### Controller

Expone la API REST.

### Service

Implementa la lógica de negocio.

### Repository

Accede a la base de datos mediante consultas nativas.

### DTO

Representa los datos intercambiados entre backend y frontend.

### Shared

Contiene componentes reutilizables del sistema.
