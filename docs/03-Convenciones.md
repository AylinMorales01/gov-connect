# Convenciones de Desarrollo

## Java

- Java 21
- Record para DTO
- Constructor Injection
- Sin Field Injection
- Sin lógica en Controllers

---

## API

Todas las respuestas utilizan ApiResponse<T>

Ejemplo:

{
success,
message,
timestamp,
data
}

---

## Logging

Se utiliza SLF4J.

No registrar información sensible.

Registrar únicamente eventos relevantes.

---

## Base de Datos

Las consultas analíticas se realizan mediante Views.

No se realizan consultas complejas directamente desde Java.