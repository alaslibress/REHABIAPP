# PLAN.md — Infrastructure: Kubernetes Manifests & Dockerfiles

> **Agent:** Sonnet (Doer) under Agent 0 (Orchestrator)
> **Domain:** `/infra/`
> **Prerequisites:** Read `infra/.claude/skills/k8s-zero-trust/SKILL.md` and `infra/.claude/skills/aws-k8s-zero-trust/SKILL.md` BEFORE starting.
> **Scope:** Create the full Kustomize-based K8s manifest set + Dockerfiles for 3 backend services.

---

## EXCLUSIONS

- **Unity Games are NOT part of this cluster.** They are hosted on AWS S3/CloudFront and communicate with the API as external HTTPS clients through the Ingress. Do NOT create any manifests, Dockerfiles, or configurations for Unity Games.
- **Desktop ERP** is a JavaFX client — not containerized.
- **Mobile Frontend** is React Native/Expo — not containerized.

---

## Step 1: Create directory scaffold

```bash
mkdir -p infra/docker/api
mkdir -p infra/docker/data
mkdir -p infra/docker/mobile-backend
mkdir -p infra/k8s/base/network
mkdir -p infra/k8s/base/api
mkdir -p infra/k8s/base/data
mkdir -p infra/k8s/base/mobile-backend
mkdir -p infra/k8s/base/postgresql
mkdir -p infra/k8s/base/mongodb
mkdir -p infra/k8s/overlays/local
mkdir -p infra/k8s/overlays/aws
```

---

## Step 2: Create Dockerfiles

### 2.1 `infra/docker/api/Dockerfile`

- Multi-stage: `eclipse-temurin:24-jdk-alpine` (builder) -> `eclipse-temurin:24-jre-alpine` (runtime).
- Builder stage: copy `api/mvnw`, `api/.mvn`, `api/pom.xml` first (dependency cache layer), then `api/src`, then `./mvnw clean package -DskipTests -B`.
- Runtime stage: create user `appuser` UID 1000 / GID 1000. Create `/app/tmp` and `/app/cache` dirs (writable mounts for readOnlyRootFilesystem). Copy JAR from builder as `app.jar`.
- `USER 1000:1000`, `EXPOSE 8080`.
- HEALTHCHECK: `wget -qO- http://localhost:8080/actuator/health/liveness || exit 1`.
- ENTRYPOINT: `java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.io.tmpdir=/app/tmp -jar app.jar`.

### 2.2 `infra/docker/data/Dockerfile`

- Identical structure to api Dockerfile but:
  - Copy from `data/` source directory instead of `api/`.
  - `EXPOSE 8081`.
  - HEALTHCHECK targets port 8081.

### 2.3 `infra/docker/mobile-backend/Dockerfile`

- Multi-stage: `node:20-alpine` (deps) -> `node:20-alpine` (runtime).
- Deps stage: copy `mobile/backend/package.json` + `package-lock.json`, run `npm ci --only=production`.
- Runtime stage: create user UID 1000, create `/app/tmp`. Copy `node_modules` from deps stage + `mobile/backend/src` + `package.json`.
- `USER 1000:1000`, `EXPOSE 3000`.
- HEALTHCHECK: `wget -qO- http://localhost:3000/health || exit 1`.
- ENTRYPOINT: `node src/index.js`.

---

## Step 3: Create namespace + default-deny (zero-trust foundation)

### 3.1 `infra/k8s/base/namespace.yaml`

Create 3 namespaces with Pod Security Standards at `restricted` level:

- `rehabiapp-api` — label: `name: rehabiapp-api`
- `rehabiapp-data` — label: `name: rehabiapp-data`
- `rehabiapp-mobile` — label: `name: rehabiapp-mobile`

Each namespace MUST have these labels:
```yaml
pod-security.kubernetes.io/enforce: restricted
pod-security.kubernetes.io/audit: restricted
pod-security.kubernetes.io/warn: restricted
```

### 3.2 `infra/k8s/base/network/default-deny.yaml`

Create 3 NetworkPolicy resources (one per namespace), each named `default-deny-all`:
```yaml
spec:
  podSelector: {}
  policyTypes:
    - Ingress
    - Egress
```

This blocks ALL traffic by default. Only explicit allow-list policies (created in step 4) open specific paths.

---

## Step 4: Create per-service base manifests

For EACH service, create 7 files. Follow the specifications below exactly.

### 4.1 API service (`infra/k8s/base/api/`)

**Identifiers:** `app: rehabiapp-api`, `tier: backend`, namespace: `rehabiapp-api`

| File | Key specs |
|------|-----------|
| `serviceaccount.yaml` | Name: `rehabiapp-api-sa`. No annotations in base (IRSA added by AWS overlay). |
| `deployment.yaml` | 2 replicas. Image: `rehabiapp-api:latest` (overridden by overlays). Port 8080. `automountServiceAccountToken: false`. Full securityContext (see below). Env: `SPRING_PROFILES_ACTIVE=production`, `SERVER_PORT=8080`. Probes: startup at `/actuator/health/liveness` (initialDelay 10s, failureThreshold 30), liveness at same path (period 10s), readiness at `/actuator/health/readiness` (period 5s). Volumes: `tmp` emptyDir Memory 100Mi at `/tmp`, `app-cache` emptyDir 200Mi at `/app/cache`. |
| `service.yaml` | ClusterIP, port 8080 -> 8080. |
| `networkpolicy.yaml` | **Ingress:** (1) from ingress-nginx namespace + pod `app.kubernetes.io/name: ingress-nginx` on TCP:8080. (2) from namespace `rehabiapp-mobile` + pod `app: mobile-backend, tier: bff` on TCP:8080. **Egress:** (1) to pod `app: postgresql, tier: database` on TCP:5432 (same namespace). (2) to namespace `rehabiapp-data` + pod `app: rehabiapp-data, tier: backend` on TCP:8081. (3) to kube-dns on UDP+TCP:53. |
| `hpa.yaml` | Min 2, max 6 replicas. CPU target 70%, memory target 80%. |
| `pdb.yaml` | `minAvailable: 1`. |
| `kustomization.yaml` | List all 6 files above as resources. |

**Mandatory securityContext for ALL deployments (pod level):**
```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  runAsGroup: 1000
  fsGroup: 1000
  seccompProfile:
    type: RuntimeDefault
```

**Mandatory securityContext for ALL containers:**
```yaml
securityContext:
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true
  runAsNonRoot: true
  runAsUser: 1000
  capabilities:
    drop:
      - ALL
```

### 4.2 Data service (`infra/k8s/base/data/`)

**Identifiers:** `app: rehabiapp-data`, `tier: backend`, namespace: `rehabiapp-data`

| File | Key specs |
|------|-----------|
| `serviceaccount.yaml` | Name: `rehabiapp-data-sa`. |
| `deployment.yaml` | 2 replicas. Image: `rehabiapp-data:latest`. Port 8081. Same securityContext as api. Env: `SPRING_PROFILES_ACTIVE=production`, `SERVER_PORT=8081`. Resources: requests 200m/512Mi, limits 1000m/1Gi. Same probe paths as api but on port 8081. Same volume mounts. |
| `service.yaml` | ClusterIP, port 8081 -> 8081. |
| `networkpolicy.yaml` | **Ingress:** ONLY from namespace `rehabiapp-api` + pod `app: rehabiapp-api, tier: backend` on TCP:8081. **Egress:** (1) to pod `app: mongodb, tier: database` on TCP:27017 (same namespace). (2) kube-dns UDP+TCP:53. NO ingress from ingress-nginx — data is internal only. |
| `hpa.yaml` | Min 2, max 4. CPU 70%. |
| `pdb.yaml` | `minAvailable: 1`. |
| `kustomization.yaml` | List all 6 resources. |

### 4.3 Mobile Backend BFF (`infra/k8s/base/mobile-backend/`)

**Identifiers:** `app: mobile-backend`, `tier: bff`, namespace: `rehabiapp-mobile`

| File | Key specs |
|------|-----------|
| `serviceaccount.yaml` | Name: `mobile-backend-sa`. |
| `deployment.yaml` | 2 replicas. Image: `mobile-backend:latest`. Port 3000. Same securityContext. Resources: requests 100m/256Mi, limits 500m/512Mi. Env: `NODE_ENV=production`, `PORT=3000`, `API_BASE_URL=http://rehabiapp-api.rehabiapp-api.svc.cluster.local:8080`. Probes: all at `/health` port 3000 (startup: initialDelay 5s, failureThreshold 10). Volume: `tmp` emptyDir Memory 50Mi. |
| `service.yaml` | ClusterIP, port 3000 -> 3000. |
| `networkpolicy.yaml` | **Ingress:** ONLY from ingress-nginx namespace + pod on TCP:3000. **Egress:** (1) ONLY to namespace `rehabiapp-api` + pod `app: rehabiapp-api, tier: backend` on TCP:8080. (2) kube-dns UDP+TCP:53. mobile-backend NEVER talks to data, postgresql, or mongodb. |
| `hpa.yaml` | Min 2, max 6. CPU 70%, memory 80%. |
| `pdb.yaml` | `minAvailable: 1`. |
| `kustomization.yaml` | List all 6 resources. |

---

## Step 5: Create database manifests (local development only)

### 5.1 PostgreSQL (`infra/k8s/base/postgresql/`)

**Identifiers:** `app: postgresql`, `tier: database`, namespace: `rehabiapp-api`

- `statefulset.yaml`: Image `bitnami/postgresql:16` (supports arbitrary UIDs). 1 replica. Same securityContext (UID 1000). Env from K8s Secret `postgresql-credentials` (keys: `username`, `password`). `POSTGRES_DB=rehabiapp`, `PGDATA=/var/lib/postgresql/data/pgdata`. Liveness/readiness: `pg_isready`. volumeClaimTemplate: 5Gi ReadWriteOnce. Extra volumes: `tmp` emptyDir Memory 50Mi, `run` emptyDir Memory 10Mi at `/var/run/postgresql`.
- `service.yaml`: Headless (`clusterIP: None`), port 5432.
- `networkpolicy.yaml`: **Ingress:** ONLY from pod `app: rehabiapp-api, tier: backend` on TCP:5432 (same namespace). **Egress:** kube-dns only.
- `kustomization.yaml`: List the 3 files.

### 5.2 MongoDB (`infra/k8s/base/mongodb/`)

**Identifiers:** `app: mongodb`, `tier: database`, namespace: `rehabiapp-data`

- `statefulset.yaml`: Image `bitnami/mongodb:7.0`. 1 replica. Same securityContext. Env from Secret `mongodb-credentials`. `MONGODB_DATABASE=rehabiapp_telemetry`. Data mount at `/bitnami/mongodb`. Liveness/readiness: `mongosh --eval "db.adminCommand('ping')"`. volumeClaimTemplate: 5Gi.
- `service.yaml`: Headless, port 27017.
- `networkpolicy.yaml`: **Ingress:** ONLY from pod `app: rehabiapp-data, tier: backend` on TCP:27017. **Egress:** kube-dns only.
- `kustomization.yaml`: List the 3 files.

---

## Step 6: Create root base kustomization

### `infra/k8s/base/kustomization.yaml`

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - namespace.yaml
  - network/default-deny.yaml
  - api/
  - data/
  - mobile-backend/
  - postgresql/
  - mongodb/
commonLabels:
  project: rehabiapp
  managed-by: kustomize
```

---

## Step 7: Create local overlay

### `infra/k8s/overlays/local/kustomization.yaml`

References `../../base`. Includes `secrets.yaml` as additional resource. Patches listed below.

### Deployment patches (1 per service)

- `api-deployment-patch.yaml`: 1 replica, image `rehabiapp-api:dev`, resources 100m/256Mi -> 500m/512Mi, env overrides: `SPRING_PROFILES_ACTIVE=local`, `SPRING_DATASOURCE_URL=jdbc:postgresql://postgresql.rehabiapp-api.svc.cluster.local:5432/rehabiapp`, username/password from Secret `postgresql-credentials`, `REHABIAPP_DATA_SERVICE_URL=http://rehabiapp-data.rehabiapp-data.svc.cluster.local:8081`.
- `data-deployment-patch.yaml`: 1 replica, image `rehabiapp-data:dev`, resources 100m/256Mi -> 500m/512Mi, env: `SPRING_PROFILES_ACTIVE=local`, `SPRING_DATA_MONGODB_URI` from Secret `mongodb-credentials` key `uri`.
- `mobile-backend-deployment-patch.yaml`: 1 replica, image `mobile-backend:dev`, resources 50m/128Mi -> 250m/256Mi, env: `NODE_ENV=development`.

### Service patches

- `api-service-patch.yaml`: type NodePort, nodePort 30080.
- `mobile-backend-service-patch.yaml`: type NodePort, nodePort 30300.

### Namespace patch

- `namespace-patch.yaml`: Override PSS to `baseline` enforce for `rehabiapp-api` and `rehabiapp-data` namespaces (Bitnami DB images may need relaxed permissions). Keep audit/warn at `restricted`.

### Secrets (dev only)

- `secrets.yaml`: 3 Kubernetes native Secrets (FORBIDDEN in production):
  - `postgresql-credentials` in `rehabiapp-api`: username `rehabiapp_dev`, password `dev_password_change_me`.
  - `mongodb-credentials` in `rehabiapp-data`: username, password, uri `mongodb://rehabiapp_dev:dev_password_change_me@mongodb.rehabiapp-data.svc.cluster.local:27017/rehabiapp_telemetry`.
  - `api-secrets` in `rehabiapp-api`: `jwt-signing-key`, `encryption-key` (dev placeholder values).

---

## Step 8: Create AWS overlay

### `infra/k8s/overlays/aws/kustomization.yaml`

References `../../base`. Additional resources: 3 SecretProviderClass files + `ingress.yaml`. Patches listed below. Inline patches to scale `postgresql` and `mongodb` StatefulSets to `replicas: 0`.

### Deployment patches (1 per service)

- `api-deployment-patch.yaml`: Image `ACCOUNT_ID.dkr.ecr.eu-west-1.amazonaws.com/rehabiapp-api@sha256:PLACEHOLDER`. Env: `SPRING_PROFILES_ACTIVE=aws`, `SPRING_DATASOURCE_URL` pointing to RDS with `?sslmode=verify-full`. Mount CSI volume `secrets-store` at `/mnt/secrets` readOnly. Volume references `secretProviderClass: rehabiapp-api-secrets`.
- `data-deployment-patch.yaml`: ECR image with sha256 digest. CSI volume with `secretProviderClass: rehabiapp-data-secrets`.
- `mobile-backend-deployment-patch.yaml`: ECR image with sha256 digest. CSI volume with `secretProviderClass: mobile-backend-secrets`.

### ServiceAccount IRSA patches (1 per service)

- `api-sa-patch.yaml`: Add annotation `eks.amazonaws.com/role-arn: arn:aws:iam::ACCOUNT_ID:role/rehabiapp-api-role`.
- `data-sa-patch.yaml`: Annotation with `rehabiapp-data-role`.
- `mobile-backend-sa-patch.yaml`: Annotation with `rehabiapp-mobile-role`.

### NetworkPolicy patches (replace pod selectors with CIDR for managed DBs)

- `api-networkpolicy-patch.yaml`: FULL REPLACEMENT. Same ingress rules as base. Egress: replace postgresql pod selector with `ipBlock: cidr: 10.0.100.0/24` on TCP:5432 (RDS subnet). Keep data cross-namespace egress. Add egress to `10.0.0.0/16:443` for Secrets Manager/KMS. Keep DNS.
- `data-networkpolicy-patch.yaml`: FULL REPLACEMENT. Same ingress. Egress: replace mongodb pod selector with `ipBlock: cidr: 10.0.101.0/24` on TCP:27017 (DocumentDB subnet). Add `10.0.0.0/16:443`. Keep DNS.

### SecretProviderClass resources (AWS Secrets Manager CSI)

- `secret-provider-class-api.yaml`: Provider `aws`. Objects: `rehabiapp/api/db-credentials`, `rehabiapp/api/encryption-key`, `rehabiapp/api/jwt-signing-key`.
- `secret-provider-class-data.yaml`: Objects: `rehabiapp/data/mongodb-credentials`, `rehabiapp/data/csfle-master-key`.
- `secret-provider-class-mobile.yaml`: Objects: `rehabiapp/mobile/session-secret`.

### ALB Ingress

- `ingress.yaml`: Annotations for AWS ALB (internet-facing, ip target-type, HTTPS:443, TLS 1.3, ACM cert ARN, WAFv2 ARN). Host rules:
  - `api.rehabiapp.example.com` -> service `rehabiapp-api` port 8080
  - `mobile.rehabiapp.example.com` -> service `mobile-backend` port 3000

NOTE: Implementer should create 2 Ingress resources (one per namespace) with shared `alb.ingress.kubernetes.io/group.name: rehabiapp` annotation to merge into single ALB.

---

## Step 9: Validate

```bash
# Syntax
kubectl kustomize infra/k8s/overlays/local/ > /dev/null
kubectl kustomize infra/k8s/overlays/aws/ > /dev/null

# Schema validation
kubectl kustomize infra/k8s/overlays/local/ | kubeconform -strict -kubernetes-version 1.30.0
kubectl kustomize infra/k8s/overlays/aws/ | kubeconform -strict -kubernetes-version 1.30.0
```

---

## Checklist

- [x] Step 1: Directory scaffold created
- [x] Step 2: 3 Dockerfiles created (api, data, mobile-backend)
- [x] Step 3: namespace.yaml + default-deny.yaml created
- [x] Step 4.1: api/ — 7 manifests created
- [x] Step 4.2: data/ — 7 manifests created
- [x] Step 4.3: mobile-backend/ — 7 manifests created
- [x] Step 5.1: postgresql/ — 4 manifests created
- [x] Step 5.2: mongodb/ — 4 manifests created
- [x] Step 6: Root base kustomization.yaml created
- [x] Step 7: Local overlay — 8 files created
- [x] Step 8: AWS overlay — 13 files created
- [x] Step 9: Both overlays pass `kubectl kustomize` and `kubeconform` (local: 33/33 valid; aws: 32/32 native valid + 3 SecretProviderClass CRDs skipped — expected, require CSI Driver CRD installed in cluster)

---

## Step 10: Reconciliacion con los planes de los servicios backend

Aplica los cambios de infraestructura K8s dictados por los 3 planes de servicio:
- `api/PLAN.md` Fase 6 (Contenerizacion y Despliegue en Kubernetes)
- `data/PLAN.md` Step 10 (Containerization and Kubernetes Deployment)
- `mobile/backend/PLAN.md` Step 9 (Containerization and Kubernetes Deployment)

Cada servicio requiere: 3 replicas base, ConfigMap para configuracion general, `envFrom` para inyectar ConfigMap, Secret refs para credenciales, y eliminacion de Dockerfiles obsoletos en `infra/docker/`.

### 10.1: Cambios del servicio API

- `infra/k8s/base/api/configmap.yaml` — CREADO con 5 claves: SPRING_PROFILES_ACTIVE, SERVER_PORT, JAVA_TOOL_OPTIONS, REHABIAPP_DATA_SERVICE_URL, MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE
- `infra/k8s/base/api/deployment.yaml` — replicas 2->3, envFrom configMapRef + env secretKeyRef (postgresql-credentials, api-secrets)
- `infra/k8s/base/api/hpa.yaml` — minReplicas 2->3
- `infra/k8s/base/api/kustomization.yaml` — incluye configmap.yaml

### 10.2: Cambios del servicio Data

- `infra/k8s/base/data/configmap.yaml` — CREADO con 6 claves: SPRING_PROFILES_ACTIVE, SERVER_PORT, JAVA_TOOL_OPTIONS, CSFLE_ENABLED, CSFLE_KMS_PROVIDER, MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE
- `infra/k8s/base/data/deployment.yaml` — replicas 2->3, envFrom configMapRef + env secretKeyRef (mongodb-credentials)
- `infra/k8s/base/data/hpa.yaml` — minReplicas 2->3
- `infra/k8s/base/data/kustomization.yaml` — incluye configmap.yaml

### 10.3: Cambios del servicio Mobile Backend

- `infra/k8s/base/mobile-backend/configmap.yaml` — CREADO con 4 claves: NODE_ENV, PORT, API_BASE_URL, LOG_LEVEL
- `infra/k8s/base/mobile-backend/deployment.yaml` — replicas 3, envFrom configMapRef
- `infra/k8s/base/mobile-backend/hpa.yaml` — minReplicas 3
- `infra/k8s/base/mobile-backend/kustomization.yaml` — incluye configmap.yaml

### 10.4: Limpieza de Dockerfiles obsoletos

Eliminados los 3 Dockerfiles de `infra/docker/` y el directorio completo. Los Dockerfiles ahora viven en el directorio raiz de cada servicio:
- `/api/Dockerfile` — Spring Boot multi-stage (Eclipse Temurin 24)
- `/data/Dockerfile` — Spring Boot multi-stage (Eclipse Temurin 24)
- `/mobile/backend/Dockerfile` — Node.js 20 Alpine multi-stage

**CRITICO:** El Dockerfile anterior de data (`infra/docker/data/Dockerfile`) usaba `node:20-alpine` pero el servicio es Spring Boot Java 24.

### 10.5: Verificacion de StatefulSets

Sin cambios necesarios:
- MongoDB (`infra/k8s/base/mongodb/statefulset.yaml`): bitnami/mongodb:7.0, 1 replica, 5Gi PVC
- PostgreSQL (`infra/k8s/base/postgresql/statefulset.yaml`): bitnami/postgresql:16, 1 replica, 5Gi PVC

### Checklist Step 10

**API (10.1):**
- [x] ConfigMap `rehabiapp-api-config` creado
- [x] Kustomization actualizado
- [x] Deployment: replicas 3, envFrom + secretKeyRef
- [x] HPA: minReplicas 3

**Data (10.2):**
- [x] ConfigMap `rehabiapp-data-config` creado (6 claves)
- [x] Kustomization actualizado
- [x] Deployment: replicas 3, envFrom + secretKeyRef
- [x] HPA: minReplicas 3

**Mobile Backend (10.3):**
- [x] ConfigMap `mobile-backend-config` creado
- [x] Kustomization actualizado
- [x] Deployment: replicas 3, envFrom
- [x] HPA: minReplicas 3

**Limpieza (10.4):**
- [x] `infra/docker/` eliminado completamente

**Verificacion (10.5):**
- [x] MongoDB StatefulSet verificado (5Gi PVC)
- [x] PostgreSQL StatefulSet verificado (5Gi PVC)
- [x] `kubectl kustomize infra/k8s/overlays/local/` renderiza sin errores
- [x] `kubectl kustomize infra/k8s/overlays/aws/` renderiza sin errores
