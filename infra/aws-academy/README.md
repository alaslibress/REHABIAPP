# Deploy RehabiAPP en AWS Academy Learner Lab

Stack EC2-Docker para sesiones del **Learner Lab** (sin EKS, sin DocumentDB, sin ALB). PostgreSQL en RDS, MongoDB en contenedor Docker, TLS via DuckDNS + Let's Encrypt.

> **Solo `/api` esta expuesto al exterior.** `/data` y `MongoDB` quedan en la red interna Docker y solo los alcanza `/api`. El BFF mobile, el desktop ERP y el frontend React siguen corriendo en local del developer.

---

## Arquitectura

```
        Internet
           |
           v
   +---------------+
   |     Caddy     |  puerto 80/443, TLS automatico DuckDNS+Let's Encrypt
   +-------+-------+
           |
           v
   +---------------+         +---------------------+
   | rehabiapp-api |<------->|   rehabiapp-data    |
   |  (Spring 4)   |  HTTP   |     (Spring 4)      |
   +-------+-------+ internal +---------+----------+
           |                            |
           v                            v
   +---------------+         +---------------------+
   |   RDS Postgres |         |  rehabiapp-mongodb  |
   |  (db.t3.micro) |         |     (Mongo 7)       |
   +---------------+         +---------------------+
                                       |
                              volumen `mongo-data`
```

Servicios en la EC2 (un solo nodo t3.small):
- **caddy** — puertos 80/443 expuestos al mundo.
- **rehabiapp-api** — puerto 8080 solo en red interna.
- **rehabiapp-data** — puerto 8081 solo en red interna.
- **rehabiapp-mongodb** — puerto 27017 solo en red interna.

PostgreSQL no esta aqui: corre en RDS gestionado.

---

## Pre-requisitos manuales

1. **Cuenta AWS Academy Learner Lab activa** (sesion <4h, recordar `Start Lab` periodicamente).
2. **Subdominio DuckDNS** registrado en https://www.duckdns.org/ (ej. `rehabiapp-api.duckdns.org`). Apuntar a `0.0.0.0` por ahora — se actualizara cuando exista la Elastic IP.
3. **Cliente SSH** con `.pem` key del Lab.

---

## Pasos de deploy

### 1) Provisionar infraestructura en AWS Console

#### 1.a — RDS PostgreSQL
- **Engine:** PostgreSQL 16 o 17.
- **Instance class:** `db.t3.micro` (free tier Academy).
- **Storage:** 20 GB gp3 (suficiente para academy).
- **Public access:** **NO**.
- **VPC:** la misma que usara la EC2.
- **DB name:** `rehabiapp`.
- **Master username:** `rehabiapp_admin`.
- **Master password:** genera uno fuerte y guardalo.
- **Security Group RDS:** ingress 5432/TCP desde el **SG de la EC2** (no `0.0.0.0/0`).

Anotar el **endpoint** RDS (`xxx.xxx.us-east-1.rds.amazonaws.com`).

#### 1.b — EC2
- **AMI:** Amazon Linux 2023.
- **Instance type:** `t3.small` (2 vCPU, 2 GB RAM). `t3.medium` si vas justo de memoria con Spring + Mongo.
- **Key pair:** la del Lab.
- **VPC/Subnet:** la misma que RDS, en subred publica.
- **Auto-assign public IP:** SI.
- **Security Group EC2:** ingress 22 desde tu IP, 80/443 desde `0.0.0.0/0`.
- **Storage:** 30 GB gp3.
- **User-Data (opcional):** pegar `scripts/ec2-bootstrap.sh` reemplazando `REPO_URL` por la URL del repo.

#### 1.c — Elastic IP
- Allocar una EIP y asociarla a la EC2 — sin esto la IP publica cambia con cada reinicio y Let's Encrypt deja de validar.

#### 1.d — Apuntar DuckDNS a la EIP
```bash
curl "https://www.duckdns.org/update?domains=rehabiapp-api&token=<TU_TOKEN>&ip=<TU_EIP>"
# Verificar
dig +short rehabiapp-api.duckdns.org
```

### 2) SSH a la EC2

```bash
ssh -i tu-lab-key.pem ec2-user@<EC2_PUBLIC_DNS_O_EIP>
```

### 3) Bootstrap (si no usaste User-Data)

```bash
curl -fsSL https://raw.githubusercontent.com/<USER>/RehabiAPP/main/infra/aws-academy/scripts/ec2-bootstrap.sh -o /tmp/bootstrap.sh
sudo REPO_URL=https://github.com/<USER>/RehabiAPP.git bash /tmp/bootstrap.sh
# Re-login SSH para que ec2-user este en el grupo docker.
exit
ssh -i tu-lab-key.pem ec2-user@<EC2_PUBLIC_DNS>
```

### 4) Rellenar `.env.aws-academy`

```bash
cd /home/ec2-user/RehabiAPP/infra/aws-academy
vim .env.aws-academy
# Pegar valores reales: endpoint RDS, password RDS, JWT_SIGNING_KEY (openssl rand -base64 32),
# ENCRYPTION_KEY, RH_INTERNAL_KEY, DUCKDNS_DOMAIN, DUCKDNS_TOKEN, ACME_EMAIL.
chmod 600 .env.aws-academy
```

### 5) Aplicar migraciones Flyway contra RDS

```bash
bash scripts/rds-init.sh
# Espera "Migraciones aplicadas. RDS listo."
```

Si Flyway detecta checksum mismatch (raro en primer deploy), arrancar con flag de reparacion:

```bash
REHABIAPP_FLYWAY_REPAIR_ON_STARTUP=true bash scripts/rds-init.sh
```

### 6) Levantar el stack

```bash
docker compose --env-file .env.aws-academy -f docker-compose.aws.yml up -d --build
docker compose --env-file .env.aws-academy -f docker-compose.aws.yml ps
```

Esperar ~60 s a que `rehabiapp-api` esten `healthy`.

### 7) Verificacion

```bash
# Health desde fuera (via Caddy + Let's Encrypt cert real).
curl https://rehabiapp-api.duckdns.org/actuator/health
# Debe devolver: {"status":"UP","groups":["liveness","readiness"]}

# Login del paciente Juan (seed V18).
curl -s -X POST https://rehabiapp-api.duckdns.org/api/auth/login-paciente \
  -H 'Content-Type: application/json' \
  -d '{"identifier":"11111111H","contrasena":"Juan1234!"}' | jq

# Dashboard de Juan.
JWT=$(curl -s -X POST https://rehabiapp-api.duckdns.org/api/auth/login-paciente \
  -H 'Content-Type: application/json' \
  -d '{"identifier":"11111111H","contrasena":"Juan1234!"}' | jq -r .accessToken)
curl -s -H "Authorization: Bearer $JWT" \
  https://rehabiapp-api.duckdns.org/api/pacientes/11111111H/dashboard | jq .juegosDesbloqueados
```

### 8) Test E2E desde el juego Unity en S3

Abrir Firefox:
```
http://s3-bucket-rehabiapp-piano-640681720314.s3-website-us-east-1.amazonaws.com/?dni=11111111H&token=<JWT>&api=https://rehabiapp-api.duckdns.org&cod_tratamiento=REAL&parte_cuerpo=MANO_DERECHA&disability_id=M-PIANO
```

Jugar una sesion completa. Verificar persistencia:

```bash
# Mongo (dentro EC2 SSH)
docker exec rehabiapp-mongodb mongosh rehabiapp_telemetry --quiet \
  --eval 'db.game_sessions.find({patientDni:"11111111H"}).sort({receivedAt:-1}).limit(1).pretty()'

# RDS Postgres (desde EC2)
PGPASSWORD=<RDS_PWD> psql -h <RDS_ENDPOINT> -U rehabiapp_admin -d rehabiapp \
  -c "SELECT * FROM session_reports WHERE paciente_dni='11111111H' ORDER BY fecha_creacion DESC LIMIT 1;"
```

---

## Operacion

### Logs

```bash
docker compose --env-file .env.aws-academy -f docker-compose.aws.yml logs -f rehabiapp-api
docker compose --env-file .env.aws-academy -f docker-compose.aws.yml logs -f rehabiapp-data
docker compose --env-file .env.aws-academy -f docker-compose.aws.yml logs -f caddy
```

### Reinicio de un servicio

```bash
docker compose --env-file .env.aws-academy -f docker-compose.aws.yml restart rehabiapp-api
```

### Apagar (sin perder datos Mongo ni certs Caddy)

```bash
bash scripts/teardown.sh
```

### Apagar y purgar volumenes

```bash
bash scripts/teardown.sh --purge
```

### Reanudar tras sesion Academy nueva

Cuando la sesion del Lab expira y reactivas:
1. La EC2 puede haber perdido la EIP — reasociar la EIP a la misma EC2.
2. `dig +short rehabiapp-api.duckdns.org` debe seguir apuntando a la EIP (DuckDNS persistente).
3. Si la IP cambio, ejecutar manualmente: `bash scripts/duckdns-update.sh`.
4. Volver a levantar el stack: `docker compose ... up -d`.

---

## Troubleshooting

| Sintoma | Causa probable | Solucion |
|---|---|---|
| Caddy `ACME challenge failed` | DuckDNS no resuelve a la EIP, o puerto 80 cerrado | `dig +short DUCKDNS_DOMAIN`; revisar SG ingress 80; correr `scripts/duckdns-update.sh` |
| `/api` arranca pero healthcheck DOWN | RDS no alcanzable | Comprobar SG RDS permite ingress desde SG EC2; `nc -vz <RDS_ENDPOINT> 5432` desde EC2 |
| Flyway checksum mismatch | Migracion editada despues de aplicada | `REHABIAPP_FLYWAY_REPAIR_ON_STARTUP=true bash scripts/rds-init.sh` |
| Telemetria llega pero MongoDB vacia | Bug DB name | Confirmar env var `SPRING_DATA_MONGODB_DATABASE=rehabiapp_telemetry` en compose |
| Preflight CORS 403 | Origen no listado | Anadir bucket S3 a `APP_CORS_ALLOWED_ORIGIN_0` o `APP_CORS_EXTRA_ORIGIN` en `.env` y restart |
| `mongo-data` ocupa mucho | Sesiones acumuladas | Ejecutar mantenimiento Mongo; o `teardown.sh --purge` para reset completo |

---

## Costes orientativos Academy (free tier)

- EC2 t3.small: ~$0.021/h (~$0.50/dia).
- RDS db.t3.micro: ~$0.018/h.
- EBS gp3 30+20 GB: ~$5/mes.
- Trafico salida: minimo si solo demo.

El lab Academy normalmente cubre estos costes en el credito que asigna. Apagar EC2 + RDS cuando no se usa para extender el credito.

---

## Roadmap a producccion real (post-Academy)

Cuando se migre a una cuenta AWS real (sin restricciones Learner Lab):

- Usar el overlay `infra/k8s/overlays/aws/` (ya existente) con EKS + RDS Multi-AZ + DocumentDB + ALB + ACM + WAFv2 + IRSA.
- Reemplazar Caddy por AWS Certificate Manager + ALB ingress.
- Reemplazar volumen Mongo por DocumentDB con cifrado en transito.
- Activar CSFLE en `/data` (`CSFLE_ENABLED=true`, KMS via IRSA).
- Backups automaticos RDS (point-in-time recovery 35 dias).

Mientras tanto este stack EC2-Docker es suficiente para validar E2E y demo.
