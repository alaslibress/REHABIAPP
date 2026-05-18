# RehabiAPP — Stack local con Docker Compose

Replica del despliegue de AWS Academy 100% local. Permite a cualquier dev clonar el repo y levantar el ecosistema completo sin depender de credenciales AWS, RDS, EC2 ni DuckDNS.

## Diferencias con AWS Academy

| Aspecto | AWS Academy | Local |
|---------|-------------|-------|
| PostgreSQL | RDS externo | Contenedor `rehabiapp-postgres` con volumen |
| TLS | Let's Encrypt + DuckDNS | Caddy `tls internal` (CA auto-firmada) |
| Dominio | `rehabiapp-api.duckdns.org` | `rehabiapp.localhost` |
| Ollama (chatbot) | Tunnel SSH inverso al laptop del dev | `host.docker.internal` directo |
| Puertos expuestos | Solo 80/443 via Caddy | 80/443 + DB/API/BFF directos para debug |

Todo lo demas (API, Data, BFF, Mongo, Chatbot, healthchecks, networks, volumenes) es identico.

## Quickstart

```bash
# 1. Clonar y entrar al repo
cd RehabiAPP

# 2. Copiar plantilla de env y rellenar secretos
cp infra/local/.env.local.example infra/local/.env.local
sed -i "s|cambiar-por-openssl-rand-base64-32-en-local|$(openssl rand -base64 32)|g" infra/local/.env.local
# (los 3 secretos quedan con valores aleatorios — JWT_SIGNING_KEY/ENCRYPTION_KEY/RH_INTERNAL_KEY)

# 3. (Opcional) Tener Ollama corriendo en el host con qwen2.5 para el chatbot
ollama serve &
ollama pull qwen2.5:latest

# 4. Levantar el stack completo
docker compose --env-file infra/local/.env.local \
               -f infra/local/docker-compose.local.yml up -d --build

# 5. Esperar a que todos esten healthy
docker compose --env-file infra/local/.env.local \
               -f infra/local/docker-compose.local.yml ps
```

## Acceso a los servicios

| URL | Servicio |
|-----|----------|
| https://rehabiapp.localhost | API + BFF detras de Caddy (cert auto-firmado) |
| https://rehabiapp.localhost/graphql | GraphQL playground BFF |
| https://rehabiapp.localhost/actuator/health | Health checks API |
| http://localhost:8080 | API directo (debug) |
| http://localhost:8081 | Data pipeline directo (debug) |
| http://localhost:3000 | BFF directo (debug) |
| postgres://admin:admin@localhost:5432/rehabiapp | PostgreSQL (DBeaver/pgAdmin) |
| mongodb://localhost:27017 | MongoDB (Compass/mongosh) |

## Confiar en la CA local de Caddy

Para evitar warnings de cert auto-firmado en el navegador:

```bash
# Linux
docker cp rehabiapp-caddy:/data/caddy/pki/authorities/local/root.crt /tmp/caddy-root.crt
sudo cp /tmp/caddy-root.crt /etc/pki/ca-trust/source/anchors/   # Fedora/RHEL
# o /usr/local/share/ca-certificates/  en Debian/Ubuntu
sudo update-ca-trust extract                                    # Fedora/RHEL
# o sudo update-ca-certificates  en Debian/Ubuntu
```

## Operaciones tipicas

```bash
# Ver logs de un servicio
docker compose -f infra/local/docker-compose.local.yml logs -f rehabiapp-api

# Recrear solo un servicio tras cambiar codigo
docker compose --env-file infra/local/.env.local \
               -f infra/local/docker-compose.local.yml up -d --build --force-recreate rehabiapp-bff

# Acceso shell al postgres
docker exec -it rehabiapp-postgres psql -U admin -d rehabiapp

# Acceso shell al mongo
docker exec -it rehabiapp-mongodb mongosh rehabiapp_telemetry

# Apagar todo (preserva volumenes)
docker compose --env-file infra/local/.env.local \
               -f infra/local/docker-compose.local.yml down

# Apagar Y borrar volumenes (reset completo de datos)
docker compose --env-file infra/local/.env.local \
               -f infra/local/docker-compose.local.yml down -v
```

## Activar el chatbot WhatsApp

El chatbot esta listo pero requiere scan QR manual:

```bash
# Ver el QR en logs (la primera vez)
docker logs -f rehabiapp-chatbot

# Escanear con el WhatsApp del numero indicado en CHATBOT_HOSPITAL_PHONE_E164
# Sesion persistida en volumen chatbot-wweb-session — no requiere re-scan tras reinicios.
```

## Tests

```bash
# API
cd api && ./mvnw test                  # 42 tests

# Data
cd data && ./mvnw test                 # 15 tests

# BFF
cd mobile/backend && npm test          # 26 tests

# Chatbot
cd chatbot && npm test                 # 7 tests
```

## Notas

- Para arrancar la app desktop apunta `desktop/src/main/resources/config/api.properties` a `http://localhost:8080` (override con env `REHABIAPP_API_URL`).
- Para arrancar el frontend mobile, deja que Expo autoresuelva la IP LAN — el BFF expone `:3000` al host.
- Si el chatbot da error de conexion a Ollama, ejecuta `ollama serve` en el host con el modelo descargado.
