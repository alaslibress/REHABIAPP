# PLAN.md — Data Pipeline: Kubernetes Readiness

> **Agent:** Sonnet (Doer) under Agent 1 (API + Data)
> **Domain:** `/data/`
> **Prerequisites:** Read `data/CLAUDE.md` and `data/.claude/skills/springboot4-mongodb/SKILL.md` BEFORE starting.
> **Scope:** Make the Spring Boot 4 data pipeline container-ready for the K8s manifests defined in `/infra/PLAN.md`.

---

## Context

The data pipeline is an INTERNAL service — it receives game telemetry ONLY from the Core API, stores it in MongoDB, and provides analytics via aggregation pipelines. It is NEVER exposed to external traffic.

The skill `springboot4-mongodb/SKILL.md` mandates Spring Boot 4.0.5 + Java 24 (overriding the original Node.js stack in CLAUDE.md). All implementation MUST use Spring Boot.

The `/infra/` K8s manifests expect this service to:
- Run on port 8081
- Expose Actuator health endpoints for probes
- Support Spring profiles (`local`, `aws`)
- Read secrets from CSI-mounted files at `/mnt/secrets/` (AWS)
- Run as UID 1000 with read-only filesystem

---

## Step 1: Initialize Spring Boot project

If not already initialized:
- Spring Boot 4.0.5, Java 24, Maven.
- GroupId: `com.rehabiapp`, ArtifactId: `data`.
- Dependencies: Spring Web, Spring Data MongoDB, Spring Boot Actuator, Micrometer Prometheus.

---

## Step 2: Configure server port

### `src/main/resources/application.yml`

```yaml
server:
  port: ${SERVER_PORT:8081}
  tomcat:
    basedir: /tmp
```

Port MUST default to 8081 to avoid collision with the Core API on 8080.

---

## Step 3: Configure Actuator health probes

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      probes:
        enabled: true
      show-details: never
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

Endpoints:
- `GET /actuator/health/liveness` — K8s livenessProbe
- `GET /actuator/health/readiness` — K8s readinessProbe (includes MongoDB connectivity check)
- `GET /actuator/prometheus` — Prometheus metrics scraping

---

## Step 4: Configure Spring profiles

### 4.1 `application-local.yml`

```yaml
spring:
  data:
    mongodb:
      uri: ${SPRING_DATA_MONGODB_URI:mongodb://localhost:27017/rehabiapp_telemetry}
logging:
  level:
    com.rehabiapp: DEBUG
    org.springframework.data.mongodb: DEBUG
```

### 4.2 `application-aws.yml`

```yaml
spring:
  config:
    import: optional:configtree:file:/mnt/secrets/
  data:
    mongodb:
      uri: ${SPRING_DATA_MONGODB_URI}
      auto-index-creation: false
logging:
  level:
    root: INFO
```

In AWS, the MongoDB URI (including DocumentDB connection string with TLS) is injected via CSI-mounted secrets. `auto-index-creation: false` because indexes are managed explicitly per the skill.

---

## Step 5: Configure structured JSON logging

### 5.1 Add dependency to `pom.xml`

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### 5.2 Create `src/main/resources/logback-spring.xml`

```xml
<configuration>
    <springProfile name="local">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO"><appender-ref ref="CONSOLE" /></root>
    </springProfile>
    <springProfile name="aws,production">
        <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
        </appender>
        <root level="INFO"><appender-ref ref="JSON" /></root>
    </springProfile>
</configuration>
```

---

## Step 6: Configure Prometheus metrics

### `pom.xml`

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

---

## Step 7: Configure CSFLE foundation (per skill requirement)

The `springboot4-mongodb` skill MANDATES Client-Side Field Level Encryption (CSFLE). At this stage, set up the configuration structure:

### 7.1 `application.yml`

```yaml
rehabiapp:
  csfle:
    enabled: ${CSFLE_ENABLED:false}
    key-vault-namespace: "encryption.__keyVault"
    kms-provider: ${CSFLE_KMS_PROVIDER:local}
    master-key-path: ${CSFLE_MASTER_KEY_PATH:/mnt/secrets/csfle-master-key}
```

### 7.2 Implementation notes for the Doer

- In local profile: CSFLE can be disabled or use a local master key file.
- In AWS profile: CSFLE uses AWS KMS. The IRSA role attached to the ServiceAccount grants access to the KMS key.
- The actual CSFLE MongoClient configuration (auto-encryption settings, JSON Schema per collection) is implemented when the data models are created — not in this K8s readiness phase.

---

## Step 8: Create health check endpoint (non-actuator)

For compatibility with the Dockerfile HEALTHCHECK (which runs outside K8s context):

### `src/main/java/com/rehabiapp/data/presentation/HealthController.java`

Simple controller returning 200 OK at `/health`. In K8s, the actuator probes are used instead, but the Dockerfile HEALTHCHECK uses this simpler endpoint.

---

## Step 9: Verify Dockerfile compatibility

Ensure Maven produces a single fat JAR:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

The Dockerfile at `infra/docker/data/Dockerfile` copies `data/target/*.jar`.

---

## Checklist

- [x] Step 1: Spring Boot 4 project initialized (Maven, Java 24)
- [x] Step 2: Server port configured to 8081
- [x] Step 3: Actuator health probes enabled
- [x] Step 4: Spring profiles configured (local + aws)
- [x] Step 5: Structured JSON logging configured
- [x] Step 6: Prometheus metrics endpoint enabled
- [x] Step 7: CSFLE configuration structure created
- [x] Step 8: /health endpoint created for Dockerfile HEALTHCHECK
- [x] Step 9: Maven produces single fat JAR

---

## Step 10: Containerization and Kubernetes Deployment

This step establishes the Docker containerization and Kubernetes deployment strategy for the Data pipeline service. Base K8s manifests exist at `/infra/k8s/base/data/`. Database StatefulSets exist at `/infra/k8s/base/postgresql/` and `/infra/k8s/base/mongodb/`.

### 10.1: Create Java Dockerfile at `/data/Dockerfile` (CRITICAL FIX)

**WARNING**: The existing Dockerfile at `/infra/docker/data/Dockerfile` uses `node:20-alpine` with `npm ci` and `node src/index.js`. This is WRONG — the Data service is now Spring Boot 4.0.0 + Java 24 (see `/data/pom.xml`). A new Java-based Dockerfile must be created.

Create multi-stage Dockerfile at `/data/Dockerfile`:

**Stage 1 - Builder:**
```dockerfile
FROM eclipse-temurin:24-jdk-alpine AS builder
WORKDIR /build
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw clean package -DskipTests -B
```

**Stage 2 - Runtime:**
```dockerfile
FROM eclipse-temurin:24-jre-alpine AS runtime
RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -D -h /app appuser
WORKDIR /app
RUN mkdir -p /app/tmp /app/cache && chown -R 1000:1000 /app
COPY --from=builder --chown=1000:1000 /build/target/*.jar /app/app.jar
USER 1000:1000
EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.io.tmpdir=/app/tmp", "-jar", "app.jar"]
```

Create `/data/.dockerignore`:
```
target/
.mvn/repository/
.git
.idea
*.iml
.env
.env.*
```

**Action on old Dockerfile**: Delete `/infra/docker/data/Dockerfile` (Node.js — obsolete and incorrect).

**Note**: Build context is `/data/` directory (not monorepo root): `docker build -t rehabiapp-data:dev -f data/Dockerfile data/`

### 10.2: Update Deployment to 3 replicas

Modify existing K8s manifests for high availability:

- `/infra/k8s/base/data/deployment.yaml`: change `replicas: 2` to `replicas: 3`
- `/infra/k8s/base/data/hpa.yaml`: change `minReplicas: 2` to `minReplicas: 3`

### 10.3: Create ConfigMap `rehabiapp-data-config`

Create `/infra/k8s/base/data/configmap.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: rehabiapp-data-config
  namespace: rehabiapp-data
  labels:
    app: rehabiapp-data
    tier: backend
data:
  SPRING_PROFILES_ACTIVE: "production"
  SERVER_PORT: "8081"
  JAVA_TOOL_OPTIONS: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
  CSFLE_ENABLED: "false"
  CSFLE_KMS_PROVIDER: "local"
  MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: "health,info,prometheus"
```

Update `/infra/k8s/base/data/kustomization.yaml` to include `- configmap.yaml` in resources list.

### 10.4: Refactor Deployment environment variables

Modify the Deployment container spec to separate general config (ConfigMap) from credentials (Secrets):

```yaml
envFrom:
  - configMapRef:
      name: rehabiapp-data-config
env:
  - name: SPRING_DATA_MONGODB_URI
    valueFrom:
      secretKeyRef:
        name: mongodb-credentials
        key: uri
```

Remove hardcoded env entries (`SPRING_PROFILES_ACTIVE`, `SERVER_PORT`) now provided by ConfigMap.

### 10.5: Verify MongoDB StatefulSet (no changes needed)

Already exists at `/infra/k8s/base/mongodb/statefulset.yaml`:

| Property | Value |
|----------|-------|
| Image | `bitnami/mongodb:7.0` |
| Replicas | 1 (databases are NOT scaled to 3) |
| PVC | 5Gi ReadWriteOnce via `volumeClaimTemplates` |
| Mount | `/bitnami/mongodb` |
| Credentials | Secret `mongodb-credentials` |
| Probes | `mongosh --eval "db.adminCommand('ping')"` |
| NetworkPolicy | Ingress ONLY from pod `rehabiapp-data`; Egress ONLY to kube-dns |

The PVC ensures total data persistence (all collections, indexes) across pod restarts and rescheduling. **No modifications needed.**

### 10.6: Verify PostgreSQL StatefulSet (no changes needed)

Already exists at `/infra/k8s/base/postgresql/statefulset.yaml`:

| Property | Value |
|----------|-------|
| Image | `bitnami/postgresql:16` |
| Replicas | 1 |
| PVC | 5Gi ReadWriteOnce via `volumeClaimTemplates` |
| Mount | `/var/lib/postgresql/data` |
| DB Name | `rehabiapp` |
| Credentials | Secret `postgresql-credentials` |
| Probes | `pg_isready` |
| NetworkPolicy | Ingress ONLY from pod `rehabiapp-api`; Egress ONLY to kube-dns |

The PVC guarantees persistence of ALL tables (including `audit_log`) and data across pod restarts. **Note**: PostgreSQL is consumed by the API service, not by Data directly. Included here for completeness of the data layer architecture. **No modifications needed.**

### 10.7: Data service K8s topology

Complete Kubernetes architecture reference for the implementer:

| Resource | Name | Specification |
|----------|------|---------------|
| Deployment | `rehabiapp-data` | 3 replicas, port 8081 (INTERNAL — no Ingress exposure) |
| Service | `rehabiapp-data` | ClusterIP, port 8081 |
| ConfigMap | `rehabiapp-data-config` | 6 configuration keys |
| Secret | `mongodb-credentials` | MongoDB connection URI |
| StatefulSet | `mongodb` | 1 replica, 5Gi PVC, bitnami/mongodb:7.0 |
| StatefulSet | `postgresql` | 1 replica, 5Gi PVC, bitnami/postgresql:16 |
| HPA | `rehabiapp-data` | min 3, max 4 replicas, CPU 70% |
| PDB | `rehabiapp-data` | minAvailable: 1 |
| NetworkPolicy | `allow-data-traffic` | Ingress: ONLY from namespace `rehabiapp-api`; Egress: MongoDB:27017 + kube-dns |
| ServiceAccount | `rehabiapp-data-sa` | IRSA in AWS overlay |

**Probes:**
- Startup: `GET /actuator/health/liveness:8081` (initialDelaySeconds: 10, failureThreshold: 30)
- Liveness: `GET /actuator/health/liveness:8081` (periodSeconds: 10)
- Readiness: `GET /actuator/health/readiness:8081` (periodSeconds: 5)

**Resources per pod:**
- Requests: 200m CPU, 512Mi memory
- Limits: 1000m CPU, 1Gi memory

**Security (ENS Alto):**
- Non-root user UID 1000:1000
- `readOnlyRootFilesystem: true` (emptyDir volumes at `/tmp` and `/app/cache`)
- `allowPrivilegeEscalation: false`
- Drop ALL capabilities
- seccomp profile: RuntimeDefault

### Checklist Step 10

- [x] Step 10.1: Java Dockerfile created at `/data/Dockerfile` (multi-stage, Eclipse Temurin 24, NOT Node.js)
- [x] Step 10.1: `.dockerignore` created at `/data/.dockerignore`
- [ ] Step 10.1: Obsolete Node.js Dockerfile at `/infra/docker/data/Dockerfile` deleted
- [ ] Step 10.2: Deployment replicas updated from 2 to 3
- [ ] Step 10.2: HPA minReplicas updated from 2 to 3
- [ ] Step 10.3: ConfigMap `rehabiapp-data-config` created at `/infra/k8s/base/data/configmap.yaml`
- [ ] Step 10.3: Kustomization updated to include configmap.yaml
- [ ] Step 10.4: Deployment env refactored with `envFrom` ConfigMap + `env` Secret ref for MongoDB
- [ ] Step 10.5: MongoDB StatefulSet verified (5Gi PVC, bitnami/mongodb:7.0)
- [ ] Step 10.6: PostgreSQL StatefulSet verified (5Gi PVC, bitnami/postgresql:16)
- [ ] Verification: `docker build -t rehabiapp-data:dev -f data/Dockerfile data/` succeeds
- [ ] Verification: `kubectl kustomize infra/k8s/overlays/local/` valid
