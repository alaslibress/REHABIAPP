#!/usr/bin/env bash
# Lanzador idempotente del API local de RehabiAPP.
# Evita conflictos de puerto entre sesiones CLI e IntelliJ.
#
# Uso:
#   ./scripts/api-dev.sh                # foreground (Ctrl+C para parar)
#   ./scripts/api-dev.sh --background   # background, logs en /tmp/rehabiapp-api.log
#   ./scripts/api-dev.sh --force        # mata proceso existente sin preguntar
#   ./scripts/api-dev.sh --background --force
set -euo pipefail

PORT=${PORT:-8080}
PID_FILE=/tmp/rehabiapp-api.pid
LOG_FILE=/tmp/rehabiapp-api.log
BACKGROUND=false
FORCE=false
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_DIR="$(dirname "$SCRIPT_DIR")"

for arg in "$@"; do
    case $arg in
        --background) BACKGROUND=true ;;
        --force)      FORCE=true ;;
        *) echo "Uso: $0 [--background] [--force]" >&2; exit 1 ;;
    esac
done

# Devuelve el PID escuchando en PORT, o vacio si nadie escucha.
pid_en_puerto() {
    # ss -tlnp muestra users:(("java",pid=XXXX,...))
    ss -tlnp 2>/dev/null \
        | awk -v port=":${PORT}" '$4 ~ port {print $0}' \
        | grep -oP 'pid=\K[0-9]+' \
        | head -1 || true
}

# Devuelve true si el PID dado corresponde a nuestro ApiApplication.
es_rehabiapp() {
    local pid=$1
    ps -p "$pid" -o cmd= 2>/dev/null | grep -q "ApiApplication" || return 1
}

# Limpia un PID file obsoleto (proceso ya no existe).
limpiar_pid_file() {
    if [[ -f "$PID_FILE" ]]; then
        local pid_guardado
        pid_guardado=$(cat "$PID_FILE")
        if ! kill -0 "$pid_guardado" 2>/dev/null; then
            rm -f "$PID_FILE"
        fi
    fi
}

limpiar_pid_file

pid_actual=$(pid_en_puerto)

if [[ -n "$pid_actual" ]]; then
    if es_rehabiapp "$pid_actual"; then
        echo "[api-dev] Puerto $PORT ocupado por RehabiAPP API (PID $pid_actual)."
        if ! $FORCE; then
            read -rp "[api-dev] Matar y relanzar? [s/N] " respuesta
            if [[ ! "$respuesta" =~ ^[sS]$ ]]; then
                echo "[api-dev] Operacion cancelada." >&2
                exit 1
            fi
        fi
        echo "[api-dev] Matando PID $pid_actual..."
        kill "$pid_actual" 2>/dev/null || true
        # Esperar hasta 10s a que libere el puerto
        for i in $(seq 1 10); do
            sleep 1
            [[ -z "$(pid_en_puerto)" ]] && break
            if [[ $i -eq 10 ]]; then
                echo "[api-dev] SIGTERM ignorado, forzando SIGKILL..."
                kill -9 "$pid_actual" 2>/dev/null || true
            fi
        done
        rm -f "$PID_FILE"
        echo "[api-dev] PID $pid_actual terminado."
    else
        echo "[api-dev] ERROR: Puerto $PORT ocupado por un proceso ajeno (PID $pid_actual)." >&2
        echo "[api-dev] Comando: $(ps -p "$pid_actual" -o cmd= 2>/dev/null || echo 'desconocido')" >&2
        echo "[api-dev] No se toca. Libera el puerto manualmente y vuelve a intentarlo." >&2
        exit 2
    fi
fi

echo "[api-dev] Arrancando RehabiAPP API en puerto $PORT..."
cd "$API_DIR"

if $BACKGROUND; then
    nohup ./mvnw spring-boot:run > "$LOG_FILE" 2>&1 &
    API_PID=$!
    echo "$API_PID" > "$PID_FILE"
    echo "[api-dev] Proceso en background PID $API_PID — logs en $LOG_FILE"
    echo "[api-dev] Para parar: ./scripts/api-stop.sh"
    # Esperar hasta 60s a que el healthcheck responda
    echo "[api-dev] Esperando healthcheck..."
    for i in $(seq 1 60); do
        sleep 2
        if curl -sf "http://localhost:${PORT}/actuator/health" > /dev/null 2>&1; then
            echo "[api-dev] API UP en http://localhost:${PORT}"
            exit 0
        fi
    done
    echo "[api-dev] WARN: API no respondio al healthcheck en 120s. Revisa $LOG_FILE" >&2
    exit 3
else
    # Foreground: no escribir PID file (el shell padre controla el proceso)
    exec ./mvnw spring-boot:run
fi
