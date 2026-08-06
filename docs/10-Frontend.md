# Frontend: Gov Connect

## Tecnologías Principales

- **React + TypeScript**: Base de la aplicación (Vite).
- **Material UI (MUI)**: Sistema de diseño y componentes base.
- **React Query**: Gestión de estado asíncrono y caché de peticiones.
- **Axios**: Cliente HTTP configurado para la comunicación con el backend (Spring Boot).
- **Recharts**: Renderizado de gráficos para los módulos de analítica.
- **React Router DOM**: Gestión de rutas.

## Arquitectura (Feature-Based)

El proyecto está diseñado para escalar de forma modular. Las páginas se agrupan por dominio de negocio (módulos), no por tipo de archivo técnico.

### Estructura de Directorios

- `src/api/`: Configuración global de Axios y definición de servicios REST.
- `src/components/`:
    - `common/`: Componentes genéricos (botones, modales, alertas).
    - `charts/`: Envolturas de Recharts reutilizables.
    - `cards/`: Tarjetas de indicadores y métricas.
    - `layout/`: Estructuras de vista (Navbar, Sidebar, MainLayout).
- `src/pages/`:
    - `dashboard/`, `analytics/`, `contracts/`, `settings/` (Módulos principales).
- `src/hooks/`: Custom hooks globales (ej. `useAuth`, `useLocalStorage`).
- `src/types/`: Interfaces globales de TypeScript.
- `src/utils/`: Funciones de formateo (fechas, monedas, strings).

## Variables de Entorno

- `VITE_API_URL`: URL base del backend (ej. `http://localhost:8080/api/v1`).
