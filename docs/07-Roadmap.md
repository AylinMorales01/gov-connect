# Roadmap

## Completado

1. Infraestructura y SQL Server (vistas del dashboard).
2. Dashboard ejecutivo + API (`ApiResponse`, logging, OpenAPI, Actuator).
3. Integración DuckDB + motor analítico + ETL asíncrono (`taskId`).
4. Frontend React (Dashboard, Analytics, Contratos, Automatización).
5. Autenticación JWT + roles (cookies HttpOnly, refresh rotativo, rate limiting).
6. Módulo de contratos + analítica de contratos en DuckDB.
7. Alerta de contratos por vencer por correo (Spring Mail + SMTP, cron) — gap G3.

---

## Caso de uso MVP cerrado

Ver `11-Caso-de-Uso-MVP.md`. Los gaps de la Fase 1 y 2 están cerrados:

1. **Fase 1** — Ingesta de contratos desde CSV SECOP (gap G1) y flujo
   "contratos por vencer" con dato real (G2).
2. **Fase 2** — Ejecución presupuestal con dato real (gap G4).
3. **Automatización** — alerta de vencimiento de contratos por correo (gap G3).
