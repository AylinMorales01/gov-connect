# =============================================================================
# Gov Connect — Imagen del backend (Spring Boot 4 / Java 21)
# =============================================================================
# Build multi-etapa: se compila con Maven y solo el JAR pasa a la imagen final,
# que lleva únicamente el JRE.
#
# El contexto de build es la RAÍZ del repositorio (no backend/), porque la
# aplicación resuelve rutas relativas al directorio de trabajo del proceso.
#
# Construcción local:
#   docker build -t gov-connect-api .
#   docker run -p 8080:8080 --env-file .env gov-connect-api
# =============================================================================

# ── Etapa 1: compilación ─────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Se copia primero el POM para que la capa de dependencias se reutilice
# mientras no cambien las dependencias.
COPY backend/pom.xml .
COPY backend/src ./src

# Los tests requieren contexto de Spring y base de datos; se omiten en el build
# de despliegue. Se ejecutan en local/CI con `./mvnw test`.
RUN mvn -B -DskipTests clean package

# ── Etapa 2: ejecución ───────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl se usa en el healthcheck del contenedor (docker compose / orquestadores).
# eclipse-temurin es Debian, así que se instala vía apt y se limpia la cache.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/target/gov-connect-api-*.jar app.jar

# DuckDbConfig y EtlService NO crean sus directorios: si no existen, la
# aplicación falla al abrir la base analítica. Las rutas por defecto
# (duckdb.path, etl.export-dir) son relativas a este WORKDIR.
# En producción conviene apuntar DUCKDB_PATH y ETL_EXPORT_DIR a un disco
# persistente, porque el filesystem del contenedor es efímero.
RUN mkdir -p /app/database/analytics /app/exports \
    && useradd -r -u 1001 govconnect \
    && chown -R govconnect:govconnect /app
USER govconnect

# MaxRAMPercentage ajusta el heap al límite del contenedor en lugar de al de
# la máquina anfitriona (crítico en planes con poca memoria).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
