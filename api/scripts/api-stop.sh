#!/usr/bin/env bash
# Para el API local de RehabiAPP.
# Lee /tmp/rehabiapp-api.pid si existe; si no, busca por puerto 8080.
#
# Uso:
#   ./scripts/api-stop.sh           # confirma antes de matar proceso ajeno
#   ./scripts/api-stop.sh --force   # mata sin preguntar (solo procesos RehabiAPP)
set -euo pipefail

PORT=${PORT:-8080}
PID_FILE=/tmp/rehabiapp-api.pid
FORCE=false

for arg in "$@"; do
    case $arg in
        --force) FORCE=true ;;
        *) echo "Uso: $0 [--force]" >&2; exit 1 ;;
    esac
done

es_rehabiapp() {
    local pid=$1
    ps -p "$pid" -o cmd= 2>/dev/null | grep -q "ApiApplication" || return 1
}

pid_en_puerto() {
    ss -tlnp 2>/dev/null \
        | awk -v port=":${PORT}" '$4 ~ port {print $0}' \
        | grep -oP 'pid=\K[0-9]+' \
        | head -1 || true
}

matar_pid() {
    local pid=$1
    echo "[api-stop] Enviando SIGTERM a PID $pid..."
    kill "$pid" 2>/dev/null || true
    for i in $(seq 1 10); do
        sleep 1
        if ! kill -0 "$pid" 2>/dev/null; then
            echo "[api-stop] PID $pid terminado."
            return 0
        fi
    done
    echo "[api-stop] SIGTERM ignorado, enviando SIGKILL a PID $pid..."
    kill -9 "$pid" 2>/dev/null || true
    sleep 1
    kill -0 "$pid" 2>/dev/null && echo "[api-stop] ERROR: no se pudo matar PID $pid" >&2 || echo "[api-stop] PID $pid eliminado con SIGKILL."
}

# Rama 1: PID file valido
if [[ -f "$PID_FILE" ]]; then
    pid=$(cat "$PID_FILE")
    if kill -0 "$pid" 2>/dev/null; then
        if es_rehabiapp "$pid"; then
            matar_pid "$pid"
        else
            echo "[api-stop] WARN: PID $pid del PID file no es ApiApplication." >&2
            echo "[api-stop] Comando: $(ps -p "$pid" -o cmd= 2>/dev/null || echo 'desconocido')" >&2
        fi
    else
        echo "[api-stop] PID $pid del PID file ya no existe (proceso terminado externamente)."
    fi
    rm -f "$PID_FILE"
    echo "[api-stop] PID file eliminado."
fi

# Rama 2: fallback — algo sigue en el puerto
pid_restante=$(pid_en_puerto)
if [[ -n "$pid_restante" ]]; then
    if es_rehabiapp "$pid_restante"; then
        echo "[api-stop] Aun hay un ApiApplication en puerto $PORT (PID $pid_restante)."
        matar_pid "$pid_restante"
    else
        echo "[api-stop] Puerto $PORT aun ocupado por proceso ajeno (PID $pid_restante)." >&2
        echo "[api-stop] Comando: $(ps -p "$pid_restante" -o cmd= 2>/dev/null || echo 'desconocido')" >&2
        if ! $FORCE; then
            read -rp "[api-stop] No es RehabiAPP. Liberar igualmente? [s/N] " respuesta
            [[ "$respuesta" =~ ^[sS]$ ]] || exit 2
        fi
        matar_pid "$pid_restante"
    fi
fi

# Verificacion final
pid_final=$(pid_en_puerto)
if [[ -z "$pid_final" ]]; then
    echo "[api-stop] Puerto $PORT libre."
else
    echo "[api-stop] ERROR: Puerto $PORT sigue ocupado por PID $pid_final." >&2
    exit 1
fi
