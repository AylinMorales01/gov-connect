# Gov Connect

**Sistema Inteligente de Automatización y Analítica para Entidades Públicas**

Plataforma web para entidades públicas colombianas que centraliza indicadores financieros, presupuestales y contractuales en dashboards ejecutivos, analítica avanzada y automatización de procesos.

---

## Estructura del Proyecto

```
gov-connect/
├── backend/          # API REST — Spring Boot (Java 21, Maven)
├── frontend/         # SPA — React 19 + TypeScript (Vite, MUI)
├── database/         # Scripts SQL Server + base DuckDB para analítica
│   ├── sqlserver/    # DDL: creación de tablas, constraints, índices y vistas
│   ├── seed/         # Datos semilla para desarrollo
│   └── analytics/    # Archivo DuckDB (analytics.duckdb — NO versionado)
├── docs/             # Documentación de arquitectura y convenciones
└── exports/          # CSV generados por el ETL (NO versionados)
```

---

## Requisitos Previos

| Componente | Requisito | Versión |
|-----------|-----------|---------|
| **Backend** | Java JDK | 21+ |
| **Backend** | Maven Wrapper | Incluido (`./mvnw`) |
| **Base de datos** | SQL Server | 2017+ |
| **Frontend** | Node.js | 18+ |
| **Frontend** | npm | 9+ |

> **Nota**: El proyecto usa el Maven Wrapper (`./mvnw`) incluido en el repositorio, no es necesario instalar Maven globalmente.

---

## Configuración del Entorno Local

### 1. Clonar el repositorio

```bash
git clone https://github.com/AylinMorales01/gov-connect.git
cd gov-connect
```

### 2. Configurar variables de entorno

```bash
cp .env.example .env
```

Edita el archivo `.env` y completa las variables con tus credenciales:

| Variable | Descripción |
|----------|-------------|
| `GOVCONNECT_DB_URL` | URL JDBC de conexión a SQL Server |
| `GOVCONNECT_DB_USERNAME` | Usuario de SQL Server |
| `GOVCONNECT_DB_PASSWORD` | Contraseña de SQL Server |
| `VITE_API_URL` | URL base de la API para el frontend |

> **Importante**: El backend (Spring Boot) lee las variables del entorno directamente. El frontend (Vite) necesita que las variables con prefijo `VITE_` estén disponibles al momento de compilar o ejecutar el servidor de desarrollo. Puedes exportarlas en tu shell o usar un archivo `frontend/.env` (ver sección Frontend más abajo).

### 3. Crear la base de datos

Ejecutar los scripts SQL en orden desde `database/sqlserver/`:

```
01-create-database.sql
02-create-tables.sql
03-create-constraints.sql
04-create-indexes.sql
05-create-views.sql
```

(Opcional) Cargar datos semilla para desarrollo desde `database/seed/`.

### 4. Iniciar el backend

> **Importante**: Ejecutar el backend desde la **raíz del repositorio** (no desde `backend/`). Las rutas relativas al archivo DuckDB y a la carpeta `exports/` dependen de ello.

```bash
./backend/mvnw -f backend/pom.xml spring-boot:run
```

La API estará disponible en `http://localhost:8080/api/v1`.

Swagger UI: `http://localhost:8080/swagger-ui`.

### 5. Iniciar el frontend

```bash
cd frontend

# Crea tu archivo .env local (opcional — las vars también pueden estar en el shell)
cp ../.env.example .env
# Edita .env si necesitas valores diferentes

npm install
npm run dev
```

La aplicación estará disponible en `http://localhost:5173`.

---

## Pipeline ETL

El proyecto usa una arquitectura de doble base de datos:

| Base | Motor | Propósito |
|------|-------|-----------|
| **SQL Server** | Transaccional | Datos operacionales en tiempo real |
| **DuckDB** | Analítico | Consultas analíticas de alto rendimiento |

Para sincronizar los datos desde SQL Server hacia DuckDB, ejecuta el endpoint ETL:

```bash
curl -X POST http://localhost:8080/api/v1/analytics/etl/run
```

Esto exporta los datos a CSV en `exports/` y los importa a `database/analytics/analytics.duckdb`.

---

## Ejecutar Pruebas

```bash
# Backend
./backend/mvnw -f backend/pom.xml test

# Frontend
cd frontend && npm run lint
```

---

## Documentación Adicional

- `docs/01-Arquitectura.md` — Arquitectura general del sistema
- `docs/02-Estructura-Proyecto.md` — Estructura detallada del código
- `docs/03-Convenciones.md` — Convenciones de desarrollo
- `docs/09-Architecture-Decision-Records.md` — Decisiones de arquitectura (ADRs)
- `CLAUDE.md` — Guía para asistentes de IA que trabajen en el proyecto

---

## Seguridad

- **No** se almacenan credenciales, claves ni secretos en el código fuente.
- Las variables de entorno sensibles se configuran mediante `.env` (excluido del repositorio vía `.gitignore`).
- Usa `.env.example` como referencia de las variables necesarias con valores ficticios.
- Las contraseñas de SQL Server y otros secretos **nunca** deben comitearse.

---
