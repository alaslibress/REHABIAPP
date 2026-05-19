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

> Phases A-C (infraestructura + GraphQL + bugfixes login) completadas y eliminadas para reducir tokens. Phase D pendiente. Phase E nueva.

### Phase D: Login inalcanzable desde dispositivo movil (cerrada)

- [x] URL dinamica con expo-constants (localhost → IP LAN automatica). Implementado en frontend `client.ts` via `resolverUrlGraphQL()` — extrae IP de `Constants.expoConfig.hostUri`.
- [x] fetchProfile movido a pantalla de inicio — race condition resuelta via `bootstrapStore.hydrate` con `Promise.allSettled` + modo silencioso (Phase 5-bridge F.2).
- [x] Errores de userStore parseados con `parseGraphQLError` — `userStore.fetchProfile` ya lanza `parseGraphQLError(err)` en su catch.
- [x] Flujo login verificado desde telefono fisico — Checkpoint B confirmado por el developer en Pixel 8 (2026-05-05).

### Phase H: Integracion real con Java API (MOCK_API=false) (2026-05-05)

- [x] H.1 `authService.js` — en modo real llama a `POST /api/auth/login-paciente` con body `{identifier, contrasena}` en lugar del antiguo `POST /api/auth/login` (que solo autentica sanitarios). En modo mock llama al mismo endpoint nuevo — `apiClient.js` lo maneja identico.
- [x] H.2 `apiClient.js` mock handler unificado: `path === '/api/auth/login' || path === '/api/auth/login-paciente'` → ambos devuelven el mismo mock token. Retrocompatible con cualquier llamada residual al endpoint de sanitarios.
- [x] H.3 `POST /api/auth/refresh-paciente` implementado en /api Phase 13. BFF actualizado: `authService.js` llama a `/api/auth/refresh-paciente`; `apiClient.js` mock handler cubre ambos paths de refresh. 37/37 API tests + 31/31 BFF tests verdes.

### Phase E: Treatment PDF + games launcher + dashboard (current iteration)

> Detalles en `/mobile/backend/PLAN.md`. Consume nuevos endpoints del API (`api/CLAUDE.md` Phase 5-9).

- [x] E.1 GraphQL TypeDef `TreatmentPdfPayload { codTrat, filename, sizeBytes, base64Content }`. Query `treatmentPdf(codTrat: String!): TreatmentPdfPayload`. Resolver llama `GET /api/tratamientos/{cod}/pdf` y serializa a base64. Limita a 10MB en respuesta.
- [x] E.2 GraphQL TypeDef `Game { idVideojuego, codigo, nombre, descripcion, codDis, parteCuerpo, urlUnity }`. Query `availableGames: [Game!]!`. Resolver llama al dashboard del API y devuelve `juegosDesbloqueados`.
- [x] E.3 GraphQL Mutation `startGame(idVideojuego: ID!): GameSessionLaunch`. Devuelve `{ urlUnity, ephemeralToken, expiresAt }` con un JWT corto (5 min, scope GAMES_PLAY) firmado por el BFF.
- [x] E.4 GraphQL TypeDef `PatientProgress { tratamientos: [TreatmentProgress!]!, lastUpdate }`. Query `myProgress: PatientProgress`. Resolver consume `GET /api/pacientes/{dni}/progreso`.
- [x] E.5 GraphQL TypeDef `Dashboard` con todos los campos del API. Query `myDashboard: Dashboard`.
- [x] E.6 Tests Apollo con jest mockeando `apiClient.fetch` para los 5 nuevos resolvers.
- [x] E.7 Documentar las queries en `/mobile/backend/README.md` o playground GraphQL.

### Phase G: Frontend ↔ BFF Schema Sync (2026-05-05)

- [x] G.1 `me` expone `numSs: String`, `sexo: SexoPaciente`, `avatarDataUri: String`. Enum `SexoPaciente {MASCULINO,FEMENINO,OTRO}` en common.js. patientService mapea los campos. Test verde.
- [x] G.2 `myTreatments` expone `codTrat, disabilityCode, summary, materials: [String!]!, medication: [String!]!, documentUrl, hasDocument`. Mock MOCK_TRATAMIENTOS_ADMIN enriquecido con todos los campos. treatmentService actualizado. Test verde.
- [x] G.3 `treatmentDocument(codTrat: ID!): TreatmentDocument` nueva query. Tipo `TreatmentDocument {fileName, mimeType, base64, url}`. Resolver reutiliza treatmentPdfService. Test verde.
- [x] G.4 `myBodyPartProgress: [BodyPartProgress!]!` y `bodyPartMetrics(bodyPartId: BodyPartId!): [BodyPartMetric!]!`. Nuevo archivo bodyProgress.js (typedef + resolver + service). 15 partes del cuerpo con progreso determinista derivado de discapacidades. Tests (2) verdes.
- [x] G.5 `myAssignedGames: [AssignedGame!]!`. Tipo `AssignedGame {id,name,description,thumbnailUrl,webglUrl,difficulty,assignedAt}`. Enum `GameDifficulty`. Resolver llama gameService.obtenerJuegosAsignados. Test verde.
- [x] G.6 `requestAppointment(...): AppointmentRequest!`. Tipo `AppointmentRequest` con `estado: AppointmentRequestStatus`. Mock devuelve PENDING. Test verde.
- [x] G.7 `registerDeviceToken` y `unregisterDeviceToken` stubs en nuevo modulo settings.js. Devuelven true, solo loguean. Test verde.
- [x] G.8 `myProgressSummary: ProgressSummary` (tipo plano {totalSessions,averageScore,improvementRate,lastSessionDate}) para compatibilidad con GET_MY_PROGRESS del frontend.
- [x] 25/25 tests verdes (`npm test`). Checkpoint A cumplido.

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
