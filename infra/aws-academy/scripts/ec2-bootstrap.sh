#!/usr/bin/env bash
# ec2-bootstrap.sh — instalacion inicial en una EC2 Amazon Linux 2023 nueva.
#
# Usar como User-Data al lanzar la EC2 (o pegar al SSH si la sesion Academy expira).
# Idempotente: ejecutar varias veces no rompe nada.
#
# Pre-requisitos:
#   - EC2 t3.small Amazon Linux 2023 con rol IAM que permita salida a internet.
#   - Security Group con 22/80/443 abiertos al rango necesario.
#   - Elastic IP asociada para que DuckDNS resuelva consistentemente.

set -euo pipefail

log() { echo "[ec2-bootstrap] $*"; }

# ---------- 1) Paquetes base ----------
log "Actualizando paquetes del sistema..."
sudo dnf -y update
sudo dnf -y install git docker docker-compose-plugin curl jq

# ---------- 2) Docker ----------
log "Habilitando Docker..."
sudo systemctl enable --now docker
# Permitir al usuario ec2-user usar docker sin sudo (requiere relogin SSH).
sudo usermod -aG docker ec2-user || true

# ---------- 3) Clonar el monorepo ----------
REPO_URL="${REPO_URL:-https://github.com/<USER>/RehabiAPP.git}"
REPO_BRANCH="${REPO_BRANCH:-main}"
REPO_DIR="/home/ec2-user/RehabiAPP"

if [ ! -d "$REPO_DIR" ]; then
    log "Clonando $REPO_URL ($REPO_BRANCH)..."
    sudo -u ec2-user git clone --branch "$REPO_BRANCH" "$REPO_URL" "$REPO_DIR"
else
    log "Repo ya clonado — actualizando..."
    sudo -u ec2-user git -C "$REPO_DIR" pull --ff-only
fi

# ---------- 4) Plantilla .env ----------
ENV_FILE="$REPO_DIR/infra/aws-academy/.env.aws-academy"
if [ ! -f "$ENV_FILE" ]; then
    log "Creando .env.aws-academy desde la plantilla (rellenar valores antes del up)."
    sudo -u ec2-user cp "$REPO_DIR/infra/aws-academy/.env.aws-academy.example" "$ENV_FILE"
    sudo chmod 600 "$ENV_FILE"
    sudo chown ec2-user:ec2-user "$ENV_FILE"
else
    log ".env.aws-academy ya existe — no se sobreescribe."
fi

# ---------- 5) Cron job DuckDNS ----------
DUCKDNS_SCRIPT="$REPO_DIR/infra/aws-academy/scripts/duckdns-update.sh"
if [ -x "$DUCKDNS_SCRIPT" ]; then
    CRON_LINE="*/5 * * * * $DUCKDNS_SCRIPT >> /var/log/duckdns.log 2>&1"
    if ! sudo -u ec2-user crontab -l 2>/dev/null | grep -F "$DUCKDNS_SCRIPT" >/dev/null; then
        log "Instalando cron job DuckDNS (cada 5 minutos)..."
        (sudo -u ec2-user crontab -l 2>/dev/null; echo "$CRON_LINE") | sudo -u ec2-user crontab -
    fi
fi

log "Bootstrap completado. Pasos manuales restantes:"
log "  1. Rellenar $ENV_FILE con secretos reales y endpoint RDS."
log "  2. Aplicar migraciones Flyway: bash $REPO_DIR/infra/aws-academy/scripts/rds-init.sh"
log "  3. Levantar el stack: docker compose --env-file $ENV_FILE -f $REPO_DIR/infra/aws-academy/docker-compose.aws.yml up -d --build"
