# Architecture Decision Records (ADR)

## Introducción

Este documento registra las principales decisiones arquitectónicas tomadas durante el desarrollo de Gov Connect.

Su objetivo es documentar el contexto, las alternativas evaluadas, la decisión adoptada y su justificación.

---

# ADR-001

## Uso de Java 21

### Contexto

El proyecto requería una versión moderna de Java que permitiera escribir código más limpio, seguro y mantenible.

### Alternativas

- Java 17
- Java 21

### Decisión

Se seleccionó Java 21 como versión principal del proyecto.

### Justificación

- Soporte LTS.
- Uso de Records.
- Mejor rendimiento.
- Sintaxis moderna.
- Compatibilidad con Spring Boot 4.x.

---

# ADR-002

## Uso de SQL Server como base de datos operacional

### Contexto

El sistema necesita almacenar información financiera, presupuestal y contractual.

### Alternativas

- PostgreSQL
- MySQL
- SQL Server

### Decisión

Se seleccionó SQL Server.

### Justificación

- Amplio uso en organizaciones públicas y privadas.
- Excelente integración con herramientas Microsoft.
- Compatibilidad con procedimientos almacenados, vistas e índices.
- Alineado con las tecnologías sugeridas para el proyecto.

---

# ADR-003

## Uso de Views para el Dashboard

### Contexto

El Dashboard requiere consultas agregadas provenientes de múltiples tablas.

### Problema

Implementar toda la lógica SQL dentro del backend aumenta el acoplamiento y dificulta el mantenimiento.

### Decisión

Toda la información del Dashboard se obtiene mediante Views.

### Justificación

- Consultas más simples.
- Mejor mantenimiento.
- Mayor rendimiento.
- Backend desacoplado de la estructura física de la base de datos.

---

# ADR-004

## Uso de DTO mediante Java Record

### Contexto

Los DTO únicamente transportan información.

### Decisión

Todos los DTO se implementan con Records.

### Justificación

- Inmutabilidad.
- Menor cantidad de código.
- Mayor legibilidad.
- Menor posibilidad de errores.

---

# ADR-005

## Respuesta estándar de la API

### Contexto

Cada endpoint devolvía estructuras diferentes.

### Decisión

Crear `ApiResponse<T>` como contrato único.

### Justificación

- Consistencia.
- Facilidad para el frontend.
- Mejor documentación en Swagger.
- Escalabilidad.

---

# ADR-006

## Arquitectura modular

### Decisión

Dividir el sistema en módulos independientes.

### Módulos actuales

- dashboard
- analytics
- contracts
- ingestion
- automation
- auth
- shared

### Justificación

- Bajo acoplamiento.
- Alta cohesión.
- Escalabilidad.
- Fácil mantenimiento.

---

# ADR-007

## DuckDB como motor analítico (doble base de datos)

### Contexto

El proyecto necesita ejecutar consultas analíticas sin afectar el rendimiento de la base operacional.

### Problema

Las consultas agregadas complejas pueden impactar el rendimiento del sistema transaccional.

### Decisión

Utilizar DuckDB exclusivamente como motor analítico; SQL Server continúa como base operacional.
Ambas se sincronizan mediante un ETL.

### Justificación

- Alto rendimiento para consultas analíticas.
- Excelente integración con archivos Parquet y CSV.
- Ideal para KPIs y reportes.
- Separación entre procesamiento transaccional y analítico.

---

# ADR-008

## Arquitectura preparada para crecimiento

### Decisión

Priorizar una arquitectura preparada para crecer antes de incorporar nuevas funcionalidades.

### Justificación

Permite integrar nuevos módulos sin modificar la estructura existente.

---

# ADR-009

## Autenticación JWT con cookies HttpOnly

### Contexto

El sistema requería autenticación con roles (`ADMIN`/`USER`) sin exponer tokens al JavaScript del navegador.

### Decisión

Los tokens JWT viajan en **cookies HttpOnly** (`access_token`, `refresh_token`) con `SameSite=Lax`,
en lugar de en el cuerpo de la respuesta o en `localStorage`.

### Justificación

- El token no es accesible por XSS (HttpOnly).
- `SameSite=Lax` mitiga CSRF sin tokens adicionales.
- Refresh token rotativo con invalidación server-side vía `tokenVersion`.
- Rate limiting en login (5 intentos/minuto por IP).

---

# ADR-010

## ETL asíncrono con taskId

### Contexto

El ETL (SQL Server → CSV → DuckDB) puede tardar y bloquear una petición síncrona.

### Decisión

El ETL se ejecuta de forma **asíncrona**: `POST /analytics/etl/run` devuelve un `taskId`
y el estado se consulta en `GET /analytics/etl/status/{taskId}`.

### Justificación

- No bloquea al cliente.
- Permite monitorear el progreso.
- Prepara el terreno para automatizaciones periódicas.
