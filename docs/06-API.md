# API REST

Versión `v1` · Ruta base `/api/v1`

Todas las respuestas usan `ApiResponse<T>`: `{ success, message, timestamp, data }`.

---

## Auth

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| POST | `/auth/login` | Autentica y establece cookies HttpOnly | público |
| POST | `/auth/refresh` | Renueva el access token (rotación) | cookie refresh |
| POST | `/auth/logout` | Invalida sesión (`tokenVersion`) | cookie refresh |
| GET | `/auth/me` | Usuario autenticado actual | autenticado |

## Dashboard (ADMIN)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/dashboard/summary` | Resumen ejecutivo |
| GET | `/dashboard/monthly-collections` | Recaudos mensuales |
| GET | `/dashboard/expiring-contracts` | Contratos próximos a vencer |
| GET | `/dashboard/budget-execution` | Ejecución presupuestal |

## Analytics (autenticado)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/analytics/health` | Estado de DuckDB |
| POST | `/analytics/etl/run` | Ejecutar ETL (asíncrono, devuelve `taskId`) |
| GET | `/analytics/etl/status/{taskId}` | Estado de una tarea ETL |
| GET | `/analytics/monthly-trend` | Tendencia mensual |
| GET | `/analytics/financial-overview` | Resumen financiero |
| GET | `/analytics/department-ranking` | Ranking por dependencia |
| GET | `/analytics/collections-by-concept` | Recaudos por concepto |
| GET | `/analytics/collections-by-payment-method` | Recaudos por medio de pago |
| GET | `/analytics/contracts-by-status` | Contratos por estado |
| GET | `/analytics/contracts-value-by-department` | Valor de contratos por dependencia |
| GET | `/analytics/top-contractors` | Principales contratistas |

## Contracts (autenticado)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/contracts` | Lista de contratos con filtros |
| GET | `/contracts/summary` | Resumen de contratos |

## Automation

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| POST | `/automation/logs` | Registrar ejecución de automatización | autenticado |
| GET | `/automation/logs` | Listar logs de automatización | autenticado |
| POST | `/automation/expiring-contracts/alert` | Ejecutar la alerta de contratos por vencer (envía correo) | ADMIN |

La alerta de contratos por vencer (G3) además se ejecuta automáticamente por cron
(`app.alert.expiring-contracts.cron`, por defecto lunes-viernes 7:00). Detecta los
contratos `ACTIVE` que vencen en los próximos `days` días y envía un correo HTML a
`recipients`. Si `spring.mail.host` está vacío o no hay destinatarios, la ejecución
se registra en `automation_logs` con estado `ERROR`/`SKIPPED` sin detener la app.

El envío de correos se hace de forma nativa mediante **Spring Mail (SMTP)**. Los
endpoints `/automation/logs` son además el punto de integración para registrar
ejecuciones de orquestadores externos: workflows tipo **n8n** podrían llegar a
implementarse en el futuro llamando a `POST /automation/logs`, aunque hoy no hay
workflows n8n versionados en el repositorio.

## Ingestion (ADMIN)

Importación de datos operacionales desde CSV (multipart, campo `file`). Las
importaciones son **asíncronas**: cada `POST` vuelca el archivo a un temporal y
responde `202 Accepted` con una tarea en estado `PENDING`; el cliente sigue su avance
por `GET /ingestion/status/{taskId}`. Si la importación procesa registros, encadena el
ETL de DuckDB al terminar (el `etlTaskId` aparece en `summary`).

| Método | Ruta | CSV esperado |
|---|---|---|
| POST | `/ingestion/contracts` | `numero_contrato, contratista, objeto, valor, fecha_inicio, fecha_fin, estado, dependencia` (SECOP II: ver alias) |
| POST | `/ingestion/budgets` | `dependencia, anio, asignado, ejecutado[, disponible]` |
| POST | `/ingestion/collections` | `fecha, concepto, contribuyente, monto, medio_pago, dependencia` |
| GET | `/ingestion/status/{taskId}` | Estado y resumen de una importación encolada |

La tarea expone `{ taskId, state, message, startedAt, completedAt, summary }`. En
`COMPLETED`, `summary` es `{ totalRows, imported, updated, skipped, errors, etlTaskId }`
(`errors` viene acotado a 500; el total de omitidas lo da `skipped`).
