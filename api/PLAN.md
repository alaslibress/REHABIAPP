# PLAN.md — API: Kubernetes Readiness

> **Agent:** Sonnet (Doer) under Agent 1 (API + Data)
> **Domain:** `/api/`
> **Prerequisites:** Read `api/CLAUDE.md` and `api/.claude/skills/springboot4-postgresql/SKILL.md` BEFORE starting.
> **Scope:** Make the Spring Boot 4 API container-ready for the K8s manifests defined in `/infra/PLAN.md`.

---

## Context

The `/infra/` plan creates Kubernetes Deployments that expect the API to:
- Run on port 8080 (default Spring Boot)
- Expose Spring Actuator health endpoints for liveness/readiness probes
- Support Spring profiles (`local`, `aws`, `production`) for environment-specific config
- Read secrets from files mounted at `/mnt/secrets/` (AWS) or environment variables (local)
- Run as non-root user (UID 1000) with a read-only filesystem
- Write temp files only to `/tmp` and `/app/cache`

This plan ensures the Spring Boot application meets these container requirements.

---

## Step 1: Verify project skeleton exists

If the Spring Boot project has not been initialized yet, initialize it first:
- Spring Boot 4.0.5, Java 24, Maven.
- GroupId: `com.rehabiapp`, ArtifactId: `api`.
- Dependencies: Spring Web, Spring Data JPA, Spring Security, Spring Boot Actuator, Flyway, PostgreSQL Driver, MapStruct.

If the project already exists, skip to Step 2.

---

## Step 2: Configure Spring Boot Actuator for K8s probes

### 2.1 `src/main/resources/application.yml`

Add or verify these properties:

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

This enables:
- `GET /actuator/health/liveness` — used by K8s livenessProbe
- `GET /actuator/health/readiness` — used by K8s readinessProbe
- `GET /actuator/prometheus` — used by Prometheus metrics scraping

### 2.2 Verify port configuration

```yaml
server:
  port: ${SERVER_PORT:8080}
```

The port MUST default to 8080 and be overridable via environment variable (K8s sets `SERVER_PORT=8080`).

---

## Step 3: Configure Spring profiles for multi-environment

### 3.1 `application-local.yml` (local K8s / development)

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/rehabiapp}
    username: ${SPRING_DATASOURCE_USERNAME:rehabiapp_dev}
    password: ${SPRING_DATASOURCE_PASSWORD:dev_password_change_me}
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    com.rehabiapp: DEBUG
    org.hibernate.SQL: DEBUG
```

### 3.2 `application-aws.yml` (AWS EKS / production)

```yaml
spring:
  config:
    import: optional:configtree:file:/mnt/secrets/
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    hikari:
      maximum-pool-size: 10
  jpa:
    show-sql: false
logging:
  level:
    root: INFO
    com.rehabiapp: INFO
```

The `configtree:file:/mnt/secrets/` import allows Spring Boot to read secrets mounted by the AWS Secrets Manager CSI Driver as files. Each file name becomes a property key, file content becomes the value.

### 3.3 `application-production.yml` (base production, inherited by aws)

```yaml
server:
  error:
    include-stacktrace: never
    include-message: never
spring:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
```

---

## Step 4: Configure structured JSON logging

K8s skill mandates structured JSON logs to stdout.

### 4.1 Add `logstash-logback-encoder` dependency to `pom.xml`

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### 4.2 Create `src/main/resources/logback-spring.xml`

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

## Step 5: Configure Prometheus metrics endpoint

### 5.1 Add Micrometer Prometheus dependency to `pom.xml`

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

This auto-configures the `/actuator/prometheus` endpoint for K8s Prometheus scraping.

---

## Step 6: Ensure filesystem compatibility with read-only root

Spring Boot and JVM write temp files. The K8s deployment mounts writable emptyDir volumes at `/tmp` and `/app/cache`. Verify:

### 6.1 `application.yml`

```yaml
spring:
  web:
    resources:
      cache:
        period: 0
server:
  tomcat:
    basedir: /tmp
```

Setting `server.tomcat.basedir=/tmp` ensures Tomcat writes session and temp files to the writable `/tmp` mount.

---

## Step 7: Create internal endpoint for data service communication

The API routes game telemetry to the data service. Create a REST client configuration:

### 7.1 Data service client configuration

```yaml
rehabiapp:
  data-service:
    url: ${REHABIAPP_DATA_SERVICE_URL:http://localhost:8081}
```

The K8s deployment sets `REHABIAPP_DATA_SERVICE_URL=http://rehabiapp-data.rehabiapp-data.svc.cluster.local:8081` for cross-namespace communication.

---

## Step 8: Verify Dockerfile compatibility

Ensure the Maven build produces a single fat JAR:

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

The Dockerfile at `infra/docker/api/Dockerfile` copies `api/target/*.jar` — this must resolve to exactly one file.

---

## Checklist

- [x] Step 1: Spring Boot 4 project skeleton exists with Maven
- [x] Step 2: Actuator health probes enabled (liveness + readiness endpoints work)
- [x] Step 3: Spring profiles configured (local, aws, production)
- [x] Step 4: Structured JSON logging configured (logback-spring.xml)
- [x] Step 5: Prometheus metrics endpoint enabled
- [x] Step 6: Tomcat basedir set to /tmp for read-only filesystem
- [x] Step 7: Data service URL configurable via environment variable
- [x] Step 8: Maven produces single fat JAR compatible with Dockerfile
