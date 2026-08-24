# Frontend: Gov Connect

## Tecnologías Principales

- **React 19 + TypeScript**: base de la aplicación (Vite).
- **Material UI (MUI)**: sistema de diseño y componentes base.
- **React Query**: gestión de estado asíncrono y caché de peticiones.
- **Axios**: cliente HTTP configurado para el backend (Spring Boot).
- **Recharts**: renderizado de gráficos de analítica.
- **React Router DOM**: gestión de rutas.

## Arquitectura (Feature-Based)

Las páginas se agrupan por dominio de negocio (módulos), no por tipo de archivo técnico.

### Estructura de Directorios (`frontend/src/`)

- `api/` — instancia de Axios y funciones tipadas por dominio (`authApi`, etc.).
- `components/`
    - `auth/` — `ProtectedRoute` (control de acceso por rol).
    - `cards/` — tarjetas de indicadores (`DashboardCard`, `ContractsSummaryCards`, …).
    - `charts/` — envolturas de Recharts (`MonthlyTrendChart`, `BudgetExecutionChart`, …).
    - `chips/` — `ContractStatusChip`.
    - `tables/` — `ContractsTable`, `DepartmentRankingTable`, `ExpiringContractsList`, `TopContractorsTable`.
    - `ErrorBoundary.tsx`, `RankPosition.tsx`.
- `context/` — `AuthProvider` + `authContext` (estado de sesión, roles, login/logout).
- `hooks/` — hooks globales (`useAuth`, wrappers de React Query).
- `layouts/` — `MainLayout` (AppBar, Drawer con navegación según rol).
- `pages/` — `LoginPage`, `DashboardPage`, `AnalyticsPage`, `ContractsPage`, `AutomationPage`, `IngestionPage`, `ForbiddenPage`.
- `routes/` — `AppRoutes` (rutas anidadas bajo `MainLayout`).
- `types/` — interfaces globales de TypeScript.
- `utils/` — funciones de formateo (fechas, monedas, porcentajes).

## Autenticación y autorización

- El estado de sesión vive en `AuthContext` y se restaura vía `GET /auth/me` (cookie HttpOnly).
- `ProtectedRoute` controla el acceso por rol: `/` (Dashboard) y `/ingestion` (importación de
  datos) son exclusivos de **ADMIN**; `/analytics`, `/contracts`, `/automation` requieren usuario autenticado.
- El backend es la fuente de verdad de autorización; el frontend solo mejora la UX.

## Variables de Entorno

- `VITE_API_URL`: URL base del backend (ej. `http://localhost:8080/api/v1`).
