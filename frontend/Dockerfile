# =============================================================================
# Gov Connect — Imagen del frontend (React 19 + Vite, servido por nginx)
# =============================================================================
# Build multi-etapa: se compila el bundle estático con Node y se sirve con
# nginx. El contexto de build es `frontend/` (no la raíz del repo).
#
# VITE_API_URL se INCORPORA en tiempo de build (Vite la incrusta en el bundle).
# Como el navegador —no Docker— resuelve esa URL, debe apuntar a la dirección
# visible desde el host, normalmente http://localhost:8080/api/v1.
# =============================================================================

# ── Etapa 1: compilación ─────────────────────────────────────────────────────
FROM node:22-alpine AS build
WORKDIR /app

# VITE_API_URL es un build arg: sin él, el bundle apuntaría a una URL vacía.
ARG VITE_API_URL=http://localhost:8080/api/v1
ENV VITE_API_URL=$VITE_API_URL

# Copia primero los manifiestos para reutilizar la capa de dependencias
# mientras no cambien package.json / package-lock.json.
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# ── Etapa 2: servidor estático ───────────────────────────────────────────────
FROM nginx:1.27-alpine

# Configura nginx con fallback SPA (try_files -> index.html) para React Router
# y proxy opcional de /api/ hacia el backend.
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
