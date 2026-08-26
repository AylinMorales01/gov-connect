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

La alerta de contratos por vencer (G3) se dispara de dos formas:

- **Programada (cron):** `app.alert.expiring-contracts.cron` (por defecto lunes–viernes
  7:00). Solo corre si `app.alert.expiring-contracts.enabled=true`; con `false` se
  desactiva **únicamente el cron**, no el disparo manual.
- **Manual (ADMIN):** `POST /automation/expiring-contracts/alert`. No hay botón en la
  UI; se dispara desde Swagger o por `curl` (ver la sección *Automatización* del README).

Detecta los contratos `ACTIVE` que vencen en los próximos `days` días y envía un correo
HTML a `recipients`. Cada ejecución queda en `automation_logs` con estado según el caso:
sin destinatarios → `SKIPPED`; `spring.mail.host` vacío (SMTP no configurado) → `ERROR`;
sin contratos por vencer → `SUCCESS` (mensaje "No hay contratos por vencer…"); envío
correcto → `SUCCESS` con el número de correos enviados. En ningún caso la app se detiene.

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

### SECOP II

Los exports de **Contratos Electrónicos** de SECOP II usan nombres de columna y
estados propios. La ingesta los resuelve con `SecopColumnMapper` y `StatusNormalizer`,
normalizando las cabeceras antes de casarlas: se quitan tildes, se pasa a minúsculas,
se recortan espacios y los guiones bajos se tratan como espacios (por eso `referencia_del_contrato`
equivale a `referencia del contrato`).

**Columnas aceptadas (alias).** Las columnas requeridas para `/ingestion/contracts` y
sus cabeceras alternativas:

| Campo interno | Cabeceras alternativas aceptadas |
|---|---|
| `numero_contrato` | `numero_contrato`, `referencia_del_contrato`, `referencia del contrato`, `referencia_del_proceso`, `referencia del proceso` |
| `contratista` | `contratista`, `proveedor_adjudicado`, `proveedor adjudicado`, `nombre_del_proveedor`, `nombre del proveedor adjudicado`, `razon_social` |
| `objeto` | `objeto`, `objeto_del_contrato`, `objeto del contrato`, `descripcion_del_procedimiento`, `descripcion del procedimiento` |
| `valor` | `valor`, `valor_del_contrato`, `valor del contrato`, `valor_total_adjudicacion`, `valor total adjudicacion` |
| `fecha_inicio` | `fecha_inicio`, `fecha_de_inicio_del_contrato`, `fecha de inicio del contrato`, `fecha_de_publicacion_del`, `fecha de publicacion del proceso` |
| `fecha_fin` | `fecha_fin`, `fecha_de_fin_del_contrato`, `fecha de fin del contrato`, `fecha_fin_liquidacion`, `fecha fin liquidacion`, `fecha_de_terminacion` |
| `estado` | `estado`, `estado_contrato`, `estado contrato`, `estado_del_procedimiento`, `estado del procedimiento` |
| `dependencia` | `dependencia`, `nombre_entidad`, `nombre entidad`, `entidad` |

Columna opcional (respaldo): `fecha_firma` (`fecha_firma`, `fecha_de_firma`, `fecha de firma`).
Se usa como fecha de inicio cuando el export trae `fecha_inicio` vacía. Las columnas del
export que no están en la tabla (SECOP II trae 58 o más) se ignoran de forma explícita.

**Estados.** Los estados de SECOP II se normalizan al dominio interno:

| Estado interno | Estados aceptados |
|---|---|
| `ACTIVE` | `ACTIVO`, `EN EJECUCION`, `EJECUCION`, `CELEBRADO`, `EJECUCION ACTIVA`, `MODIFICADO` |
| `SUSPENDED` | `SUSPENDIDO` |
| `FINISHED` | `TERMINADO`, `FINALIZADO`, `LIQUIDADO`, `CERRADO`, `EXPIRADO`, `CANCELADO`, `CEDIDO` |

Los estados de **pre-ejecución** se omiten de la importación (no son contratos vigentes
ni fiables para la alerta): `BORRADOR`, `APROBADO`, `ENVIADO PROVEEDOR`, `EN APROBACION`.

**Otras reglas de la importación de contratos:**

- **Upsert por `numero_contrato`**: si el número ya existe, se **actualiza**; si no, se **inserta**.
- **Dependencia de respaldo**: si la entidad no coincide con las secretarías registradas,
  la fila cae en el departamento `SIN` ("Sin asignar").
- **Anchos de columna**: `numero_contrato` es obligatorio y no debe exceder 50 caracteres
  (si excede, la fila se registra como error); `contratista` se trunca a 150 y `objeto` a 500.
- **Migración previa (solo bases antiguas)**: para importar un export real sobre una base
  creada antes de ampliar `contracts`, aplica primero `08-secop-columns-migration.sql`.
  Una base nueva ya crea las columnas anchas (`02-create-tables.sql`).
