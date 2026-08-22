# Dashboard Ejecutivo

Principal módulo funcional. Cuatro indicadores (rutas bajo `/api/v1/dashboard`):

| Indicador | Ruta | Descripción |
|---|---|---|
| Summary | `GET /summary` | Resumen ejecutivo |
| Monthly Collections | `GET /monthly-collections` | Recaudos mensuales |
| Contracts Expiring | `GET /expiring-contracts` | Contratos próximos a vencer |
| Budget Execution | `GET /budget-execution` | Ejecución presupuestal por dependencia |

El Dashboard es exclusivo del rol **ADMIN**: el backend lo exige en `SecurityConfig`
(`hasRole("ADMIN")`) y el frontend en `ProtectedRoute`.

---

## Objetivo

Brindar información ejecutiva para apoyar la toma de decisiones.
