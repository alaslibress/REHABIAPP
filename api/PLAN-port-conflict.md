# PLAN: Resolver conflicto de puerto 8080 al arrancar el API

> **Autor:** Agent 1 Thinker (Opus)
> **Destinatario:** Doer Sonnet
> **Dominio:** `/api`
> **Prioridad:** BLOCKING — el API no arranca
> **Fecha:** 2026-05-13

---

## 1. PROBLEMA OBSERVADO

Al arrancar el API desde IntelliJ aparece este stacktrace y la app no
levanta:

```
Failed to start bean 'webServerStartStop'
Caused by: org.springframework.boot.web.server.WebServerException: Unable to start embedded Tomcat server
Caused by: java.net.BindException: La dirección ya se está usando
```

Es decir, Tomcat embebido no puede bind al puerto 8080 porque ya hay otro
proceso escuchando ahi.

---

## 2. CAUSA RAIZ

1. Hay un proceso huerfano de `mvn spring-boot:run` lanzado en una sesion
   CLI previa (probablemente con `nohup` para tests E2E con MongoDB /
   PostgreSQL). El PID actual confirmado es **99527** y su wrapper Maven
   es PID **99299**.
2. Ese proceso quedo retenido en el puerto 8080 y nadie lo paro.
3. IntelliJ no detecta el conflicto antes de arrancar — lanza el JVM y
   Tomcat falla durante el `start` del lifecycle.
4. NO existe ningun mecanismo en el repo (script, hook, healthcheck) que
   detecte un API ya corriendo o que evite el doble-arranque.

---

## 3. OBJETIVO

- **Corto plazo:** liberar el puerto 8080 ahora mismo para que el API
  pueda arrancar de nuevo desde IntelliJ.
- **Medio plazo:** eliminar la posibilidad de que un arranque CLI deje
  procesos huerfanos cruzados con IntelliJ. Cualquier futuro arranque
  CLI debe ser idempotente: si ya hay un API corriendo, o lo reutiliza
  o lo apaga antes de relanzar.

NO se va a dockerizar el API en este plan — eso es un cambio mayor que
requiere actualizar K8s, perfiles, healthchecks de docker-compose y la
URL que usa `/data` para llamar al API. Queda fuera de alcance.

---

## 4. STEP-BY-STEP PRESCRIPTIVO

### Phase A — Limpieza inmediata del proceso huerfano

A.1. Identificar el PID que ocupa 8080:

```bash
ss -tlnp 2>/dev/null | grep 8080
# o si no devuelve user info:
fuser 8080/tcp 2>/dev/null
lsof -i :8080 2>/dev/null
```

A.2. Verificar que el comando del PID es Java/Spring de RehabiAPP, no
otro servicio del usuario:

```bash
ps -p <PID> -o pid,etime,cmd | head -2
```

Si en `cmd` aparece `ApiApplication` o `com.rehabiapp.api.ApiApplication`
es nuestro huerfano. En otro caso ABORTAR y pedir confirmacion al dev.

A.3. Matar el proceso huerfano y su wrapper Maven (si existe). Usar
SIGTERM primero, SIGKILL solo si no responde en 5s:

```bash
kill 99527; kill 99299
sleep 5
# Verificar que se fueron
ss -tlnp | grep 8080 && echo "Aun ocupado" || echo "Puerto libre"
# Si sigue ahi:
kill -9 99527 99299 2>/dev/null
```

A.4. Confirmar puerto libre antes de continuar:

```bash
ss -tlnp | grep 8080 || echo "OK: 8080 libre"
```

### Phase B — Script de arranque idempotente (`scripts/api-dev.sh`)

B.1. Crear directorio `api/scripts/` si no existe.

B.2. Crear `api/scripts/api-dev.sh` con este comportamiento:

- Antes de arrancar comprueba si hay algo escuchando en 8080.
- Si lo hay y es un API de RehabiAPP (matchea por `ApiApplication` en
  `ps`), pregunta confirmacion y ofrece matarlo. En modo `--force` lo
  mata sin preguntar.
- Si lo hay y NO es nuestro API (otro servicio en 8080), ABORTA con
  mensaje claro y sin tocar nada.
- Si no hay nada, arranca `./mvnw spring-boot:run` y escribe el PID en
  `/tmp/rehabiapp-api.pid`.
- Soporta `--background` (nohup + logs en `/tmp/rehabiapp-api.log`) y
  modo foreground por defecto.

Estructura sugerida (Bash, sin dependencias externas):

```bash
#!/usr/bin/env bash
# Lanzador idempotente del API local. Evita conflicto con IntelliJ.
set -euo pipefail

PORT=${PORT:-8080}
PID_FILE=/tmp/rehabiapp-api.pid
LOG_FILE=/tmp/rehabiapp-api.log
FORCE=${1:-}

esRehabiapp() {
    local pid=$1
    ps -p "$pid" -o cmd= 2>/dev/null | grep -q "ApiApplication"
}

ocupado() {
    ss -tlnp 2>/dev/null | awk '{print $4}' | grep -E ":${PORT}\$" >/dev/null
}

# Bloque principal — ver Section "Comportamiento" arriba.
# (Doer: implementar las 4 ramas: libre, ocupado-nuestro, ocupado-otro,
#  con PID_FILE valido y sin el)
```

B.3. Hacer ejecutable: `chmod +x api/scripts/api-dev.sh`.

B.4. Tests manuales del script:

- Sin nada en 8080 → arranca normalmente, escribe PID file.
- Con un API ya corriendo en 8080 (nuestro) → ofrece matarlo, lo mata y
  rearranca.
- Con otro proceso ajeno en 8080 → aborta con codigo 2 y mensaje.
- Tras `kill` externo, PID file queda obsoleto → script lo detecta
  (`kill -0 $PID` falla) y lo limpia.

### Phase C — Script `api/scripts/api-stop.sh`

C.1. Crear `api/scripts/api-stop.sh` que:

- Lee `/tmp/rehabiapp-api.pid`.
- Si existe y el proceso vive, hace `kill <pid>` y espera hasta 10s.
- Si no muere en 10s, `kill -9`.
- Borra el PID file al final.
- Si el PID file no existe pero algo escucha en 8080, hace fallback a
  `fuser -k 8080/tcp` con confirmacion (`--force` la salta).

C.2. Hacer ejecutable y probar:

```bash
api/scripts/api-dev.sh --background
sleep 25
curl -sf http://localhost:8080/actuator/health
api/scripts/api-stop.sh
sleep 2
ss -tlnp | grep 8080 && echo "FAIL" || echo "OK"
```

### Phase D — Actualizar runbook en `api/CLAUDE.md`

D.1. En la **Section 7. RUNBOOK** de `api/CLAUDE.md` reemplazar el bloque
de comandos `./mvnw spring-boot:run` por una nota que dirija a los
scripts:

```markdown
## 7. RUNBOOK

```bash
# Local stack (BD)
docker compose -f infra/docker-compose.yml up postgresql mongodb data-pipeline

# API — dev (gestiona conflicto con IntelliJ automaticamente)
./scripts/api-dev.sh              # foreground
./scripts/api-dev.sh --background # background con PID file
./scripts/api-stop.sh             # parar

# IntelliJ: usar el run config existente. Si IntelliJ falla con
# BindException, ejecutar ./scripts/api-stop.sh y reintentar.
```

D.2. Añadir un parrafo de 3 lineas en **Section 2. OPERATING RULES**
indicando: "Para arrancar el API en local **siempre** usar el script
`api/scripts/api-dev.sh`. NO ejecutar `mvn spring-boot:run` directo a
menos que se este depurando el script mismo."

### Phase E — Verificacion end-to-end

E.1. Cerrar IntelliJ y todos los terminales del API.

E.2. `ss -tlnp | grep 8080` → debe estar libre.

E.3. Arrancar el API desde IntelliJ. Debe levantar limpio sin
`BindException`.

E.4. Sin parar IntelliJ, ejecutar `./scripts/api-dev.sh` en terminal.
Debe detectar a IntelliJ (otro Java con `ApiApplication` en 8080),
preguntar si matarlo, y si se elige NO, salir con codigo 1.

E.5. Aceptar el kill, `./scripts/api-dev.sh` debe parar IntelliJ y
levantar uno nuevo en CLI.

E.6. `./scripts/api-stop.sh` debe pararlo todo y dejar 8080 libre.

E.7. Smoke test del endpoint que ya teniamos:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login-paciente \
  -H "Content-Type: application/json" \
  -d '{"identifier":"55667788Z","contrasena":"Juan1234!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
curl -sf -X POST http://localhost:8080/actuator/health \
  -H "Authorization: Bearer $TOKEN" >/dev/null && echo OK
```

---

## 5. ACEPTACION (DEFINITION OF DONE)

- [ ] Puerto 8080 libre cuando ningun API se ha arrancado.
- [ ] `api/scripts/api-dev.sh` y `api/scripts/api-stop.sh` existen,
      ejecutables, sin dependencias externas (solo Bash + coreutils).
- [ ] El script detecta y resuelve correctamente las 3 situaciones:
      libre, ocupado-por-nuestro-API, ocupado-por-otro-proceso.
- [ ] `api/CLAUDE.md` documenta el flujo nuevo.
- [ ] `./mvnw test` sigue verde (no se ha tocado codigo Java; los
      scripts son externos).
- [ ] Smoke test post-fix devuelve 200 OK al `/actuator/health`.

---

## 6. NO HACER

- NO añadir `spring.main.web-application-type=none` ni cambiar el puerto
  por defecto. El puerto 8080 esta cableado en el BFF, en `/data`, en
  Unity y en el `application-local.yml`. Cambiarlo es un refactor mayor.
- NO matar procesos en el script sin verificar que son nuestros API
  (`ps cmd` contiene `ApiApplication`). Otros servicios del usuario
  podrian estar en 8080 (raro pero posible).
- NO dockerizar el API en este plan. Es un cambio aparte que toca K8s,
  perfiles `aws`/`production` y el flujo IntelliJ.
- NO usar `--no-verify`, `--force` por defecto, o `pkill -f java` (mata
  cualquier Java del sistema incluyendo IntelliJ).
- NO commitear el PID file ni los logs. Anadir `/tmp/rehabiapp-api.*` no
  hace falta porque viven en `/tmp`, pero confirma que `api/scripts/`
  esta dentro del `.gitignore` de IDE (`.idea/`).

---

## 7. RIESGOS Y MITIGACIONES

| Riesgo | Mitigacion |
|---|---|
| El script mata IntelliJ pensando que es huerfano | Pedir confirmacion interactiva siempre; `--force` solo en CI |
| Otro servicio del usuario ocupa 8080 | Detectar `ApiApplication` en `cmd`; si no matchea, abortar |
| PID file stale tras crash | Validar con `kill -0 $PID`; si falla, limpiar y seguir |
| Bash no portable a macOS | Usar solo `ss`, `ps`, `kill` POSIX. Evitar `pgrep -a` (BSD pgrep difiere) |
| El dev olvida usar el script | Documentacion en CLAUDE.md + nota en error de IntelliJ comun |

---

## 8. ENTREGABLES

1. `/api/scripts/api-dev.sh` (ejecutable).
2. `/api/scripts/api-stop.sh` (ejecutable).
3. Update `/api/CLAUDE.md` Sections 2 y 7.
4. Tag de commit: `fix(api): script idempotente para arranque local evita BindException`.
