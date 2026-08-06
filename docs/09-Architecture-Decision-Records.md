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
- Compatibilidad con Spring Boot 3.

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

Toda la información del Dashboard será obtenida mediante Views.

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

Todos los DTO serán implementados utilizando Records.

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

Crear ApiResponse<T> como contrato único.

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
- shared

### Módulos futuros

- analytics
- automation
- auth
- reports

### Justificación

- Bajo acoplamiento.
- Alta cohesión.
- Escalabilidad.
- Fácil mantenimiento.

---

# ADR-007

## DuckDB como motor analítico



### Contexto

El proyecto necesita ejecutar consultas analíticas sin afectar el rendimiento de la base operacional.

### Problema

Las consultas agregadas complejas pueden impactar el rendimiento del sistema transaccional.

### Decisión

Utilizar DuckDB exclusivamente como motor analítico.

SQL Server continuará siendo la base de datos operacional.

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

Esto permitirá integrar nuevos módulos sin modificar la estructura existente del proyecto.
