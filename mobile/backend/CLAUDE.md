# CLAUDE.md - RehabiAPP Mobile Backend (BFF)

> **File:** `/mobile/backend/CLAUDE.md`
> **Agent:** Agent 2 (Mobile Developer)
> **Role:** Thinker (Opus) + Doer (Sonnet)

---

## 1. PROJECT DEFINITION

This directory contains the **Backend-For-Frontend (BFF)** that serves exclusively the patient mobile application. It is NOT a general-purpose backend. Its sole responsibility is to:

1. Expose a **GraphQL API** (Apollo Server 4) consumed by the React Native mobile app.
2. **Orchestrate** HTTP requests to the Core Java API (`[ SERVICE: API ]`).
3. **Generate and validate** its own JWT tokens for patient mobile sessions.
4. **Format and filter** data for the mobile UI — only expose fields the frontend needs.
5. **Standardize errors** into a consistent structure for the frontend.
6. **Inject presentation context** (timezone-based greeting).

This service acts as the **security gateway** for the mobile app. All traffic from `/mobile/frontend` passes through this BFF before reaching the Java API. The mobile frontend NEVER calls the Java API directly.

---

## 2. OPERATING RULES

1. **Global context:** Read and respect the root `/CLAUDE.md` before any cross-domain decision. This local file takes precedence for mobile backend-specific decisions only.

2. **PLAN.md is mandatory:** Before implementing any step, read `/mobile/backend/PLAN.md` which contains the step-by-step instructions with exact specifications. The plan is the implementation blueprint.

3. **Maintain this file:** When you complete a task, change `[ ]` to `[x]`. Remove resolved items that no longer provide useful context.

4. **ZERO databases:** This service has ABSOLUTELY NO database connections. No `pg`, `mongoose`, `sequelize`, `typeorm`, `prisma`, `knex`, or any ORM/driver. All persistence is the exclusive responsibility of the Java API (`[ SERVICE: API ]`) and the data pipeline (`[ SERVICE: DATA ]`).

5. **ZERO games:** Unity therapeutic minigames are hosted independently on AWS S3/CloudFront. This service does not interact with them, store their files, or manage their traffic.

6. **ZERO clinical business logic:** This BFF does not make clinical decisions. It does not calculate progression levels, validate treatments, or apply medical rules. It only orchestrates and formats. Business logic resides in the Java API.

7. **Stateless design:** The service is designed for horizontal scaling in Kubernetes. No shared state between pods except the in-memory JWT token cache (acceptable for MVP, migrates to Redis in production).

8. **Read-only filesystem:** The K8s container runs with `readOnlyRootFilesystem: true`. Only `/tmp` is writable. Never use `fs.writeFileSync` to any path outside `/tmp`.

---

## 3. LOCAL STACK

- **Runtime:** Node.js 20 (LTS)
- **HTTP Framework:** Express 5
- **GraphQL:** Apollo Server 4, graphql, graphql-tag
- **Authentication:** jsonwebtoken (JWT generation/validation)
- **HTTP Client:** fetch (native Node.js 20, no axios)
- **Logging:** pino, pino-http (structured JSON for K8s)
- **Metrics:** prom-client (Prometheus)
- **Language:** JavaScript (CommonJS modules, no TypeScript, no build step)

### PROHIBITED packages (never install):

- Database: `pg`, `mongoose`, `sequelize`, `typeorm`, `prisma`, `knex`, `better-sqlite3`
- Deprecated GraphQL: `express-graphql`
- Unnecessary HTTP: `axios`, `node-fetch`, `got`, `superagent`
- Unnecessary timezone: `moment`, `moment-timezone`, `luxon`, `dayjs`
- State management: `redis`, `memcached`, `ioredis` (future enhancement only)

### Build commands

```
npm start             # Start production server (node src/index.js)
npm run dev           # Start development with watch mode (node --watch src/index.js)
npm test              # Run tests (node --test 'test/*.test.js')
```

### Environment variables

| Variable            | Default                    | Description                           |
|---------------------|----------------------------|---------------------------------------|
| `PORT`              | `3000`                     | HTTP port                             |
| `API_BASE_URL`      | `http://localhost:8080`    | Java API internal URL                 |
| `NODE_ENV`          | `development`              | Environment mode                      |
| `LOG_LEVEL`         | `info`                     | Pino log level                        |
| `JWT_SECRET`        | CSI or fallback            | HMAC-SHA256 signing key for BFF JWT   |
| `JWT_EXPIRATION_MS` | `1800000` (30 min)         | Access token TTL                      |
| `JWT_REFRESH_MS`    | `604800000` (7 days)       | Refresh token TTL                     |
| `MOCK_API`          | `false`                    | Use mock data instead of Java API     |
| `GRAPHQL_PATH`      | `/graphql`                 | GraphQL endpoint path                 |
| `SECRETS_DIR`       | `/mnt/secrets`             | CSI secrets mount path (AWS)          |

---

## 4. ARCHITECTURE

```
src/
|-- index.js                        # Entry point (Express + Apollo Server)
|-- config.js                       # Centralized configuration + secrets
|
|-- graphql/
|   |-- typeDefs/
|   |   |-- index.js                # Merges all type definitions
|   |   |-- auth.js                 # AuthPayload, login/refreshToken mutations
|   |   |-- patient.js              # Patient, Disability, ProgressSummary, queries
|   |   |-- appointment.js          # Appointment type, queries, mutations
|   |   |-- treatment.js            # Treatment type, query
|   |   |-- game.js                 # GameSession, GameMetrics, query
|   |   |-- common.js               # ErrorCode, AppointmentStatus enums
|   |
|   |-- resolvers/
|       |-- index.js                # Merges all resolvers
|       |-- auth.js                 # login, refreshToken
|       |-- patient.js              # me, myDisabilities, myProgress
|       |-- appointment.js          # myAppointments, bookAppointment, cancelAppointment
|       |-- treatment.js            # myTreatments
|       |-- game.js                 # myGameSessions
|
|-- services/
|   |-- apiClient.js                # HTTP wrapper for Java API (fetch + error mapping)
|   |-- authService.js              # BFF JWT generation/validation + Java token cache
|   |-- patientService.js           # Patient data orchestration
|   |-- appointmentService.js       # Appointment orchestration
|   |-- treatmentService.js         # Treatment data orchestration
|   |-- gameService.js              # Game session orchestration
|
|-- middleware/
|   |-- auth.js                     # JWT extraction from Authorization header
|   |-- greeting.js                 # Timezone-based greeting injection
|   |-- errorFormatter.js           # Apollo formatError standardization
|
|-- utils/
    |-- errors.js                   # Error code catalog + GraphQLError builder
    |-- timezone.js                 # Timezone greeting calculator (Intl API)
```

### Data flow:

```
Mobile App
    |
    | GraphQL over HTTPS (JWT BFF in Authorization header)
    | X-Timezone header for greeting
    v
BFF Node.js (this service)
    |
    | REST over HTTP (JWT Java in Authorization header)
    | Internal K8s network (ClusterIP)
    v
Java API (Spring Boot)
    |
    | JPA / JDBC
    v
PostgreSQL
```

### Authentication architecture:

```
Mobile <--[JWT BFF]--> BFF Node.js <--[JWT Java]--> Java API

JWT BFF:   Issued by this service, signed with BFF JWT_SECRET
           Payload: { sub: dniPac, tipo: 'access'|'refresh', iat, exp }

JWT Java:  Issued by Java API, signed with Java's own key
           Cached in-memory by BFF, used for internal API calls
```

---

## 5. COMMUNICATION CONTRACTS

### GraphQL API (exposed to mobile)

| Operation | Type     | Auth Required | Description                     |
|-----------|----------|---------------|---------------------------------|
| `login`   | Mutation | No            | Authenticate patient            |
| `refreshToken` | Mutation | No       | Renew JWT pair                  |
| `me`      | Query    | Yes           | Patient profile                 |
| `myDisabilities` | Query | Yes       | Patient's assigned disabilities |
| `myProgress` | Query | Yes           | Therapeutic progress summary    |
| `myTreatments` | Query | Yes         | Patient's treatments            |
| `myAppointments` | Query | Yes       | Patient's appointments          |
| `bookAppointment` | Mutation | Yes   | Book new appointment            |
| `cancelAppointment` | Mutation | Yes | Cancel existing appointment     |
| `myGameSessions` | Query | Yes       | Game session history            |

### Java API endpoints consumed (internal)

| Method | Java Endpoint                                  | Used by             |
|--------|------------------------------------------------|---------------------|
| POST   | `/api/auth/login`                              | authService         |
| POST   | `/api/auth/refresh`                            | authService         |
| GET    | `/api/pacientes/{dni}`                         | patientService      |
| GET    | `/api/pacientes/{dniPac}/discapacidades`       | patientService      |
| GET    | `/api/pacientes/{dniPac}/tratamientos`         | treatmentService    |
| GET    | `/api/citas?fecha={fecha}`                     | appointmentService  |
| POST   | `/api/citas`                                   | appointmentService  |
| DELETE | `/api/citas?dniPac=X&dniSan=Y&fecha=Z&hora=H` | appointmentService  |

### Error structure (guaranteed to mobile):

```json
{
  "errors": [{
    "message": "Descripcion legible",
    "extensions": {
      "code": "ERROR_CODE",
      "titulo": "Error",
      "subtitulo": "Nombre del problema",
      "texto": "Descripcion detallada en castellano"
    }
  }]
}
```

---

## 6. IMPLEMENTATION CHECKLIST

### Phase A: Infrastructure (COMPLETED)

- [x] Node.js project initialized with Express 5.
- [x] Health endpoint (`GET /health`) returns 200 OK.
- [x] Structured JSON logging with pino configured.
- [x] Prometheus metrics endpoint (`GET /metrics`) enabled.
- [x] Secret reading utility for CSI-mounted files.
- [x] Read-only filesystem compatible (no writes outside /tmp).
- [x] Dockerfile multi-stage (Node.js 20 Alpine, UID 1000).
- [x] K8s manifests in `/infra/k8s/base/mobile-backend/`.

### Phase B: GraphQL Application Layer (COMPLETADA)

- [x] GraphQL dependencies installed (Apollo Server 4, graphql, jsonwebtoken).
- [x] Configuration extended with JWT and GraphQL variables.
- [x] Error code catalog centralized (`src/utils/errors.js`).
- [x] Timezone greeting utility (`src/utils/timezone.js`).
- [x] HTTP client for Java API (`src/services/apiClient.js`).
- [x] Authentication service with BFF JWT (`src/services/authService.js`).
- [x] Domain services (patient, treatment, appointment, game).
- [x] GraphQL TypeDefs matching frontend contract.
- [x] GraphQL Resolvers with auth protection.
- [x] JWT authentication middleware.
- [x] Timezone greeting middleware.
- [x] Global error formatter.
- [x] Apollo Server integrated with Express.
- [x] Tests passing (16/16: 6 infrastructure + 10 GraphQL).
- [x] K8s ConfigMap updated with new variables.

### Phase C: Correccion de Errores de Login y Observabilidad (PENDIENTE)

- [x] Puerto del frontend corregido (4000 → 3000) en `/mobile/frontend/src/services/graphql/client.ts`.
- [x] Script `npm run dev` con MOCK_API=true, LOG_LEVEL=debug y pino-pretty.
- [x] Logger centralizado en `src/logger.js` (eliminar instancias duplicadas de pino).
- [x] Flujo login admin verificado en terminal (exito + error visibles).

### Phase D: Login Inalcanzable desde Dispositivo Movil (PENDIENTE)

- [ ] URL dinamica con expo-constants (localhost → IP LAN automatica).
- [ ] fetchProfile movido a pantalla de inicio (eliminar race condition con AuthGuard).
- [ ] Errores de userStore parseados con parseGraphQLError (consistencia con authStore).
- [ ] Flujo login verificado desde telefono fisico.

---

## 7. KUBERNETES TOPOLOGY

| Resource      | Name                     | Specification                     |
|---------------|--------------------------|-----------------------------------|
| Deployment    | `mobile-backend`         | 3 replicas, port 3000             |
| Service       | `mobile-backend`         | ClusterIP, port 3000              |
| ConfigMap     | `mobile-backend-config`  | Environment variables             |
| HPA           | `mobile-backend`         | min 3, max 6, CPU 70%, mem 80%    |
| PDB           | `mobile-backend`         | minAvailable: 1                   |
| NetworkPolicy | `allow-mobile-traffic`   | Ingress: ingress-nginx; Egress: rehabiapp-api:8080 |

Probes: `/health:3000` (startup, liveness, readiness).
Resources per pod: 100m-500m CPU, 256Mi-512Mi memory.

---

*This file is the single source of truth for the mobile backend domain. Update it as tasks are completed.*

## Memory

You have access to Engram persistent memory via MCP tools (mem_save, mem_search, mem_session_summary, etc.).
- Save proactively after significant work — don't wait to be asked.
- After any compaction or context reset, call `mem_context` to recover session state before continuing.
