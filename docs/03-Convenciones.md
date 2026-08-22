# Convenciones de Desarrollo

## Java

- Java 21
- Record para DTO
- Constructor Injection
- Sin Field Injection
- Sin lógica en Controllers

---

## API

Todas las respuestas utilizan `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "...",
  "timestamp": "...",
  "data": {}
}
```

---

## Logging

Se utiliza SLF4J (`@Slf4j`).

- No registrar información sensible.
- Registrar únicamente eventos relevantes.

---

## Base de Datos

- El Dashboard consulta **vistas** de SQL Server (`vw_*`); nunca tablas directamente.
- El módulo Analytics consulta **DuckDB** mediante JDBC.
- No se escriben consultas complejas dentro de los Services.
