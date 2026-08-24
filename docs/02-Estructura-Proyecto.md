# Estructura del Proyecto

```
backend/src/main/java/com/govconnect

    dashboard
        controller / service / repository / dto

    analytics
        controller / service / repository / dto / etl / config

    contracts
        controller / service / repository / dto

    ingestion
        controller / service / repository / dto / task / config

    automation
        controller / service / repository / dto / config

    auth
        controller / service / repository / dto / entity / security

    shared
        config / constants / csv / exception / response
```

---

## Convención

- Cada módulo contiene únicamente las clases de su responsabilidad.
- No hay lógica de negocio en los Controllers.
- La interacción con datos se hace desde Repository.
- Los DTO son Java Records inmutables.

Frontend (ver `10-Frontend.md`): `frontend/src/` con layout feature-based
(`api`, `components`, `context`, `hooks`, `pages`, `routes`, `types`, `utils`).
