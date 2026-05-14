#!/usr/bin/env bash
# rds-init.sh — aplica las migraciones Flyway de /api contra el RDS PostgreSQL.
#
# Se lanza una sola vez tras crear la instancia RDS y antes de levantar el stack.
# Reutiliza la imagen rehabiapp-api:aws-academy (o la builda si no existe) con un
# override del entrypoint para ejecutar Flyway migrate y salir.
#
# Idempotente: Flyway compara checksums y aplica solo lo nuevo.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env.aws-academy"

if [ ! -f "$ENV_FILE" ]; then
    echo "[rds-init] FATAL: $ENV_FILE no existe."
    exit 1
fi

# Construir la imagen si no existe localmente.
if ! docker image inspect rehabiapp-api:aws-academy >/dev/null 2>&1; then
    echo "[rds-init] Construyendo imagen rehabiapp-api:aws-academy..."
    docker build -t rehabiapp-api:aws-academy -f "$ROOT_DIR/../../api/Dockerfile" "$ROOT_DIR/../../api"
fi

echo "[rds-init] Aplicando migraciones Flyway contra RDS..."

# Spring arranca, detecta Flyway habilitado, aplica migraciones, y cerramos el contenedor.
# spring.flyway.enabled queda en true por defecto en /api.
docker run --rm \
    --env-file "$ENV_FILE" \
    -e SPRING_PROFILES_ACTIVE=aws-academy \
    -e REHABIAPP_DATA_SERVICE_URL=http://localhost:0 \
    -e REHABIAPP_FLYWAY_REPAIR_ON_STARTUP=${REHABIAPP_FLYWAY_REPAIR_ON_STARTUP:-false} \
    rehabiapp-api:aws-academy \
    java -Dspring.main.web-application-type=none \
         -jar app.jar \
         --spring.task.scheduling.enabled=false \
         --logging.level.com.rehabiapp=INFO

echo "[rds-init] Migraciones aplicadas. RDS listo."
