#!/usr/bin/env bash
# teardown.sh — apaga y limpia el stack AWS Academy local en la EC2.
#
# Uso:
#   bash teardown.sh           # para contenedores, conserva volumenes (Mongo data persiste).
#   bash teardown.sh --purge   # ademas borra volumenes (datos Mongo + certs Caddy perdidos).
#
# No toca RDS: la limpieza de la instancia RDS se hace desde la consola AWS.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env.aws-academy"
COMPOSE_FILE="$ROOT_DIR/docker-compose.aws.yml"

PURGE=false
if [ "${1:-}" = "--purge" ]; then
    PURGE=true
fi

if [ ! -f "$ENV_FILE" ]; then
    echo "[teardown] WARN: $ENV_FILE no encontrado. Continuando sin env file."
    ENV_ARG=""
else
    ENV_ARG="--env-file $ENV_FILE"
fi

echo "[teardown] Parando contenedores..."
docker compose $ENV_ARG -f "$COMPOSE_FILE" down

if $PURGE; then
    echo "[teardown] --purge: eliminando volumenes (mongo-data, caddy-data, caddy-config)..."
    docker compose $ENV_ARG -f "$COMPOSE_FILE" down -v
fi

echo "[teardown] Completado."
