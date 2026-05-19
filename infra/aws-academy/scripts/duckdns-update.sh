#!/usr/bin/env bash
# duckdns-update.sh — actualiza el subdominio DuckDNS con la IP publica actual.
#
# Se ejecuta via cron (cada 5 min, ver ec2-bootstrap.sh). La IP publica EC2 puede
# cambiar si el Elastic IP se desasocia (sesion Academy nueva, por ejemplo) y
# DuckDNS debe reflejar el cambio para que Let's Encrypt siga validando.
#
# Requiere DUCKDNS_DOMAIN y DUCKDNS_TOKEN en .env.aws-academy.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env.aws-academy"

if [ ! -f "$ENV_FILE" ]; then
    echo "[duckdns] FATAL: $ENV_FILE no existe."
    exit 1
fi

# Cargar solo las dos variables necesarias (sin source completo para evitar fugas).
DUCKDNS_DOMAIN=$(grep -E '^DUCKDNS_DOMAIN=' "$ENV_FILE" | cut -d= -f2-)
DUCKDNS_TOKEN=$(grep -E '^DUCKDNS_TOKEN=' "$ENV_FILE" | cut -d= -f2-)

if [ -z "$DUCKDNS_DOMAIN" ] || [ -z "$DUCKDNS_TOKEN" ]; then
    echo "[duckdns] FATAL: faltan DUCKDNS_DOMAIN o DUCKDNS_TOKEN."
    exit 1
fi

# DuckDNS espera solo el subdominio (sin .duckdns.org).
SUBDOMAIN="${DUCKDNS_DOMAIN%.duckdns.org}"

# ip vacia = DuckDNS detecta la IP del cliente automaticamente.
RESPONSE=$(curl -fsS "https://www.duckdns.org/update?domains=${SUBDOMAIN}&token=${DUCKDNS_TOKEN}&ip=" || true)

TS=$(date -u +%Y-%m-%dT%H:%M:%SZ)
echo "[duckdns] $TS subdomain=${SUBDOMAIN} respuesta=${RESPONSE}"

if [ "$RESPONSE" != "OK" ]; then
    echo "[duckdns] WARN: respuesta inesperada — verificar DUCKDNS_TOKEN."
    exit 1
fi
