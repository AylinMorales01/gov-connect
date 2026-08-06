# Estructura del Proyecto

```
src
└── main
    └── java
        └── com.govconnect

            dashboard
                controller
                service
                repository
                dto

            shared
                config
                constants
                exception
                response
```

---

## Convención

Cada módulo debe contener únicamente las clases relacionadas con su responsabilidad.

No se permite lógica de negocio dentro de los Controllers.

Toda interacción con SQL Server se realiza desde Repository.

Los DTO son inmutables utilizando Java Record.