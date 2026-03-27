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

- [ ] Step 1: Spring Boot 4 project initialized (Maven, Java 24)
- [ ] Step 2: Server port configured to 8081
- [ ] Step 3: Actuator health probes enabled
- [ ] Step 4: Spring profiles configured (local + aws)
- [ ] Step 5: Structured JSON logging configured
- [ ] Step 6: Prometheus metrics endpoint enabled
- [ ] Step 7: CSFLE configuration structure created
- [ ] Step 8: /health endpoint created for Dockerfile HEALTHCHECK
- [ ] Step 9: Maven produces single fat JAR
