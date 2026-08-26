#!/bin/sh
# =============================================================================
# Gov Connect — Bootstrap de SQL Server (contenedor one-shot `db-init`)
# =============================================================================
# Se ejecuta UNA sola vez tras arrancar el contenedor `db` (ver depends_on:
# condition: service_healthy en docker-compose.yml). Aplica, en orden:
#
#   database/sqlserver/01…07  → esquema, vistas, índices y usuarios de prueba
#   database/seed/01…04       → datos demo (solo si SEED_DEMO=true)
#
# Se omite 08-secop-columns-migration.sql: solo aplica a bases antiguas con
# columnas estrechas; 02 ya crea las anchas en una base nueva (y 08 insertaría
# un departamento 'SIN' que choca con el seed).
#
# Es idempotente: si GOV_CONNECT_DB ya existe, se salta la creación (script 01,
# que NO es idempotente) y el resto se reaplica de forma segura (IF NOT EXISTS /
# CREATE OR ALTER). Pensado para POSIX sh; corre sobre la imagen
# mcr.microsoft.com/mssql/server (que incluye sqlcmd en /opt/mssql-tools18).
#
# Variables de entorno requeridas:
#   MSSQL_SA_PASSWORD  — contraseña de `sa` (debe cumplir la política de SQL Server)
#   SEED_DEMO          — "true" para cargar datos demo (default: true)
# =============================================================================

set -eu

# ── Configuración ────────────────────────────────────────────────────────────
DB_HOST="${DB_HOST:-db}"
DB_PORT="${DB_PORT:-1433}"
SA_USER="sa"
SA_PASSWORD="${MSSQL_SA_PASSWORD:?MSSQL_SA_PASSWORD no está definida}"
SEED_DEMO="${SEED_DEMO:-true}"

SCHEMA_DIR="/database/sqlserver"
SEED_DIR="/database/seed"

log() { printf '[db-init] %s\n' "$1"; }
fail() { printf '[db-init][ERROR] %s\n' "$1" >&2; exit 1; }

# ── Localizar sqlcmd ─────────────────────────────────────────────────────────
SQLCMD=""
for candidate in \
    /opt/mssql-tools18/bin/sqlcmd \
    /opt/mssql-tools/bin/sqlcmd \
    /usr/bin/sqlcmd; do
    if command -v "$candidate" >/dev/null 2>&1; then
        SQLCMD="$candidate"
        break
    fi
done
[ -n "$SQLCMD" ] || fail "No se encontró sqlcmd en el contenedor db-init"

# Encapsula la invocación a sqlcmd. -C confía en el certificado autofirmado de
# SQL Server; -b hace que sqlcmd devuelva un código de error si el lote falla.
run_sql() {
    "$SQLCMD" -S "$DB_HOST,$DB_PORT" -U "$SA_USER" -P "$SA_PASSWORD" -C -b "$@"
}

# ── Espera de disponibilidad ─────────────────────────────────────────────────
log "Esperando a que SQL Server ($DB_HOST:$DB_PORT) acepte conexiones…"
i=0
until run_sql -l 5 -t 5 -Q "SELECT 1" >/dev/null 2>&1; do
    i=$((i + 1))
    if [ "$i" -ge 60 ]; then
        fail "SQL Server no quedó listo tras ${i} intentos"
    fi
    sleep 2
done
log "SQL Server listo."

# ── Creación de la base de datos (solo si no existe) ─────────────────────────
# El script 01 no es idempotente (CREATE DATABASE + crea ERP_DB), así que se
# ejecuta únicamente la primera vez. Se usa un valor centinela para evitar
# depender del padding/espaciado de la salida de sqlcmd.
if run_sql -d master -h -1 -W -Q "SET NOCOUNT ON; SELECT CASE WHEN DB_ID('GOV_CONNECT_DB') IS NULL THEN 'NO' ELSE 'YES' END" | grep -q 'YES'; then
    log "GOV_CONNECT_DB ya existe; se omite $SCHEMA_DIR/01-create-database.sql."
else
    log "Creando base de datos desde $SCHEMA_DIR/01-create-database.sql…"
    run_sql -d master -i "$SCHEMA_DIR/01-create-database.sql" >/dev/null || fail "Fallo en 01-create-database.sql"
fi

# ── Esquema y migraciones (idempotentes) ─────────────────────────────────────
# 02…07 no llevan `USE`, así que se les fija el contexto con -d GOV_CONNECT_DB.
#
# NOTA: se OMITE 08-secop-columns-migration.sql a propósito. Ese script es para
#   migrar BASES ANTIGUAS que aún tienen las columnas de contracts estrechas;
#   02-create-tables.sql ya las crea ANCHAS (NVARCHAR(50)/NVARCHAR(500)) en una
#   base nueva. Además, 08 inserta el departamento 'SIN', y el seed
#   01_departments.sql también lo inserta → chocaría con el UNIQUE de code.
for script in \
    02-create-tables.sql \
    03-create-constraints.sql \
    04-create-indexes.sql \
    05-create-views.sql \
    06-sprint72-migration.sql \
    07-refresh-token-migration.sql; do
    log "Aplicando $script…"
    run_sql -d GOV_CONNECT_DB -i "$SCHEMA_DIR/$script" >/dev/null || fail "Fallo en $script"
done

# ── Datos demo (opcional) ────────────────────────────────────────────────────
# Los seed no son idempotentes (INSERT directos y asumen ids de departments 1..N),
# así que se cargan solo la primera vez: si `departments` ya tiene filas, se salta.
if [ "$SEED_DEMO" = "true" ]; then
    if run_sql -d GOV_CONNECT_DB -h -1 -W -Q "SET NOCOUNT ON; SELECT CASE WHEN EXISTS (SELECT 1 FROM departments) THEN 'YES' ELSE 'NO' END" | grep -q 'YES'; then
        log "Ya existen filas en departments; se omiten los datos demo."
    else
        log "Cargando datos demo (database/seed/)…"
        for script in \
            01_departments.sql \
            02_budgets.sql \
            03_contracts.sql \
            04_collections.sql; do
            log "  Aplicando seed/$script…"
            run_sql -d GOV_CONNECT_DB -i "$SEED_DIR/$script" >/dev/null || fail "Fallo en seed/$script"
        done
    fi
else
    log "SEED_DEMO != true: no se cargan datos demo."
fi

log "Base de datos GOV_CONNECT_DB inicializada correctamente."
