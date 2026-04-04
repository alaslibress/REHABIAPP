# IMPORTANT.md — Backend Contract for RehabiAPP Mobile Frontend

> **Purpose:** This document defines everything the backend agent (Agent 2 — Doer) needs to know to implement the Node.js Express GraphQL backend at `/mobile/backend/` that serves the mobile frontend.
> **Scope:** Contract definition only. This document does NOT design the backend — it defines what the frontend expects to receive.
> **Architecture:** The backend is a BFF (Backend-for-Frontend) that translates GraphQL requests from the mobile app into REST calls to the central Spring Boot API (`/api`).

---

## 1. Communication Protocol

| Aspect | Specification |
|--------|--------------|
| **Protocol** | GraphQL over HTTP |
| **Endpoint** | `/graphql` (single endpoint) |
| **Client** | Apollo Client (frontend) |
| **Transport** | HTTP POST with `Content-Type: application/json` |
| **Authentication** | JWT Bearer token in `Authorization` header |
| **BFF Role** | Receives GraphQL requests, translates to REST, calls central API, returns GraphQL response |

### Key constraints

1. The frontend communicates **EXCLUSIVELY** via GraphQL. There are no REST endpoints consumed directly by the mobile app.
2. The backend MUST expose a single `/graphql` endpoint.
3. Every request (except `login` and `refreshToken` mutations) MUST require a valid JWT in the `Authorization: Bearer <token>` header.
4. The backend is a **translation layer**: it receives structured GraphQL requests, maps them to the appropriate REST endpoints on the central Spring Boot API, and returns the data in GraphQL format.

---

## 2. GraphQL Schema

This is the **complete schema** the frontend will query. The backend must implement resolvers for every type, query, and mutation defined here.

```graphql
# ============================================================
# ENUMS
# ============================================================

enum TreatmentType {
  MEDICATION
  EXERCISE
  GAMIFICATION
  TEXT_INSTRUCTION
}

enum AppointmentStatus {
  SCHEDULED
  COMPLETED
  CANCELLED
}

# ============================================================
# TYPES
# ============================================================

type AuthPayload {
  accessToken: String!
  refreshToken: String!
  expiresAt: Int!
}

type Patient {
  id: ID!
  name: String!
  surname: String!
  email: String!
  dni: String!
  phone: String
  birthDate: String
  address: String
  active: Boolean!
}

type Disability {
  id: ID!
  name: String!
  description: String
  currentLevel: Int!
}

type Treatment {
  id: ID!
  name: String!
  description: String
  type: TreatmentType!
  visible: Boolean!
  progressionLevel: Int!
}

type Appointment {
  id: ID!
  date: String!
  time: String!
  practitionerName: String!
  practitionerSpecialty: String
  status: AppointmentStatus!
  notes: String
}

type GameSession {
  id: ID!
  gameName: String!
  playedAt: String!
  score: Int
  duration: Int
  metrics: GameMetrics
}

type GameMetrics {
  accuracy: Float
  reactionTime: Float
  completionRate: Float
}

type ProgressSummary {
  totalSessions: Int!
  averageScore: Float
  improvementRate: Float
  lastSessionDate: String
}

# ============================================================
# QUERIES
# ============================================================

type Query {
  """Returns the authenticated patient's profile"""
  me: Patient!

  """Returns disabilities assigned to the authenticated patient"""
  myDisabilities: [Disability!]!

  """Returns treatments, optionally filtered by disability and progression level"""
  myTreatments(disabilityId: ID, level: Int): [Treatment!]!

  """Returns appointments, optionally filtered by status or upcoming flag"""
  myAppointments(status: AppointmentStatus, upcoming: Boolean): [Appointment!]!

  """Returns game session history with pagination"""
  myGameSessions(limit: Int, offset: Int): [GameSession!]!

  """Returns aggregated progress summary"""
  myProgress: ProgressSummary!
}

# ============================================================
# MUTATIONS
# ============================================================

type Mutation {
  """Authenticate with DNI or email + password. Returns JWT tokens."""
  login(identifier: String!, password: String!): AuthPayload!

  """Refresh an expired access token using a valid refresh token."""
  refreshToken(refreshToken: String!): AuthPayload!

  """Book a new appointment with a practitioner."""
  bookAppointment(date: String!, time: String!, practitionerId: ID!): Appointment!

  """Cancel an existing appointment."""
  cancelAppointment(appointmentId: ID!): Appointment!
}
```

---

## 3. Type Mapping — GraphQL to Frontend TypeScript

The frontend defines TypeScript types that mirror the GraphQL schema exactly. The backend must ensure field names and nullability match precisely.

| GraphQL Type | Frontend Type (in `src/types/`) | Notes |
|---|---|---|
| `AuthPayload` | `AuthToken` (`auth.ts`) | Field names match exactly |
| `Patient` | `Patient` (`user.ts`) | All nullable fields use `String` (nullable) in GraphQL |
| `Disability` | `Disability` (`treatments.ts`) | `currentLevel` is `Int!` (non-null) |
| `Treatment` | `Treatment` (`treatments.ts`) | `type` uses `TreatmentType` enum |
| `Appointment` | `Appointment` (`appointments.ts`) | `status` uses `AppointmentStatus` enum |
| `GameSession` | `GameSession` (`games.ts`) | `metrics` can be null |
| `GameMetrics` | `GameMetrics` (`games.ts`) | All fields nullable |
| `ProgressSummary` | `ProgressSummary` (`games.ts`) | `totalSessions` is non-null |

### Critical nullability rules

- Fields marked `!` in GraphQL MUST always be present in the response. Never return `null` for a non-null field.
- Fields WITHOUT `!` can be `null`. The frontend handles null checks for these.
- Array types `[Type!]!` mean: the array itself is non-null, and each element is non-null. Return an empty array `[]` if there are no results, never `null`.

---

## 4. Error Response Contract

The frontend parses errors from the GraphQL `errors` array. Each error MUST follow this structure:

```json
{
  "errors": [
    {
      "message": "Human-readable error message (can be in English)",
      "extensions": {
        "code": "ERROR_CODE",
        "subtitle": "Short problem name in Spanish"
      }
    }
  ]
}
```

### Error Code Table

| Code | Subtitle (Spanish) | HTTP Context | When to Return |
|------|-------------------|--------------|----------------|
| `INVALID_CREDENTIALS` | Credenciales invalidas | Login attempt | Wrong DNI/email or password |
| `ACCOUNT_DEACTIVATED` | Cuenta desactivada | Login attempt | Patient has `active = false` |
| `TOKEN_EXPIRED` | Sesion expirada | Any authenticated request | JWT `exp` claim is in the past |
| `TOKEN_INVALID` | Token invalido | Any authenticated request | JWT signature verification fails, malformed token |
| `NETWORK_ERROR` | Error de conexion | Any request | Backend cannot reach the central Spring Boot API |
| `PATIENT_NOT_FOUND` | Paciente no encontrado | Profile queries | Patient ID from JWT doesn't match any record |
| `APPOINTMENT_CONFLICT` | Conflicto de cita | `bookAppointment` mutation | Requested time slot is already taken |
| `APPOINTMENT_NOT_FOUND` | Cita no encontrada | `cancelAppointment` mutation | Appointment ID doesn't exist |
| `VALIDATION_ERROR` | Datos invalidos | Any mutation | Request body fails validation (missing fields, wrong format) |
| `INTERNAL_ERROR` | Error interno | Any request | Unhandled exception, unexpected server state |

### Error handling rules

1. **Always** return errors in the `extensions.code` format shown above. The frontend uses `code` to look up localized messages.
2. **Never** return raw stack traces or internal error details in the `message` field in production.
3. The `subtitle` field in `extensions` is used by the frontend's ErrorPopup component as the error subtitle displayed to the user.
4. If the central API returns a REST error, the BFF must translate it to the appropriate GraphQL error code above.

---

## 5. Authentication Flow

### Login sequence

```
1. User enters DNI/Gmail + password on mobile login screen
2. Frontend sends GraphQL mutation:
   mutation { login(identifier: "12345678A", password: "***") { accessToken refreshToken expiresAt } }
3. Backend receives the mutation
4. Backend calls central API: POST /api/auth/login { identifier, password }
5. Central API validates credentials, returns JWT tokens
6. Backend forwards tokens to frontend as AuthPayload
7. Frontend stores tokens in expo-secure-store
8. All subsequent requests include: Authorization: Bearer {accessToken}
```

### Token refresh sequence

```
1. Frontend detects accessToken is expired (checks expiresAt)
2. Frontend sends: mutation { refreshToken(refreshToken: "...") { accessToken refreshToken expiresAt } }
3. Backend calls: POST /api/auth/refresh { refreshToken }
4. Backend returns new AuthPayload
5. Frontend replaces stored tokens
6. If refresh fails -> frontend clears tokens -> redirects to login
```

### Identifier format

The `identifier` field in the `login` mutation accepts TWO formats:
- **DNI**: 8 digits + 1 uppercase letter (e.g., `12345678A`). Regex: `^\d{8}[A-Z]$`
- **Email**: Standard email format (e.g., `patient@gmail.com`)

The backend should detect which format was sent and handle accordingly when calling the central API.

### Token specifications

| Property | Value |
|----------|-------|
| `accessToken` | JWT, short-lived (recommended: 1 hour) |
| `refreshToken` | Opaque or JWT, long-lived (recommended: 7 days) |
| `expiresAt` | Unix timestamp (seconds) when `accessToken` expires |

---

## 6. BFF Data Mapping — GraphQL to REST

The backend translates each GraphQL operation to one or more REST calls to the central Spring Boot API. Below are the **suggested** REST endpoint mappings. The actual REST API contract is defined by Agent 1 (API team) — adapt as needed.

### Queries

| GraphQL Query | REST Call | Headers | Query Params |
|---|---|---|---|
| `me` | `GET /api/patients/me` | `Authorization: Bearer {token}` | — |
| `myDisabilities` | `GET /api/patients/me/disabilities` | `Authorization: Bearer {token}` | — |
| `myTreatments(disabilityId, level)` | `GET /api/patients/me/treatments` | `Authorization: Bearer {token}` | `?disabilityId={id}&level={n}` |
| `myAppointments(status, upcoming)` | `GET /api/appointments` | `Authorization: Bearer {token}` | `?status={s}&upcoming={bool}` |
| `myGameSessions(limit, offset)` | `GET /api/game-sessions` | `Authorization: Bearer {token}` | `?limit={n}&offset={n}` |
| `myProgress` | `GET /api/patients/me/progress` | `Authorization: Bearer {token}` | — |

### Mutations

| GraphQL Mutation | REST Call | Headers | Body |
|---|---|---|---|
| `login(identifier, password)` | `POST /api/auth/login` | — | `{ "identifier": "...", "password": "..." }` |
| `refreshToken(refreshToken)` | `POST /api/auth/refresh` | — | `{ "refreshToken": "..." }` |
| `bookAppointment(date, time, practitionerId)` | `POST /api/appointments` | `Authorization: Bearer {token}` | `{ "date": "...", "time": "...", "practitionerId": "..." }` |
| `cancelAppointment(appointmentId)` | `PATCH /api/appointments/{id}/cancel` | `Authorization: Bearer {token}` | — |

### Data transformation notes

- The central API may return field names in different casing (e.g., `snake_case`). The BFF must transform them to match the GraphQL schema's `camelCase` field names.
- The central API may return additional fields not needed by the mobile app. The BFF should only forward fields defined in the GraphQL schema.
- For `myAppointments`, the `upcoming` boolean should be translated to whatever date filtering the central API supports (e.g., `?fromDate=2026-04-04`).
- The `expiresAt` field in `AuthPayload` must be a Unix timestamp in **seconds** (not milliseconds).

---

## 7. CORS and Security

### Development

| Setting | Value |
|---------|-------|
| CORS Origins | `http://localhost:19006`, `http://localhost:8081`, `exp://*` |
| CORS Methods | `GET, POST, OPTIONS` |
| CORS Headers | `Content-Type, Authorization` |

### Production

| Setting | Value |
|---------|-------|
| CORS Origins | Restrict to app's production origin |
| Helmet | Enable all default security headers |
| Rate limiting | Recommended on `login` mutation (e.g., 5 attempts per minute per IP) |

### Security rules

1. The backend MUST NOT expose any patient data without a valid JWT.
2. The `login` and `refreshToken` mutations are the ONLY operations that work without a JWT.
3. All clinical data is already encrypted at the central API level (AES-256-GCM). The BFF passes it through without additional encryption.
4. Never log patient data or JWT tokens in production logs.
5. JWT validation must check: signature, expiration, and issuer.

---

## 8. Suggested Backend Dependencies

These are recommendations for the backend implementation. The implementing agent may choose alternatives if justified.

| Package | Purpose | Required? |
|---------|---------|-----------|
| `express` | HTTP server | Yes |
| `@apollo/server` | GraphQL server (Apollo Server 4) | Yes |
| `graphql` | GraphQL core library | Yes |
| `jsonwebtoken` | JWT creation and verification | Yes |
| `axios` | HTTP client for calling central REST API | Recommended |
| `cors` | CORS middleware | Yes |
| `helmet` | Security headers | Recommended |
| `dotenv` | Environment variables | Recommended |

### Minimum viable backend structure

```
backend/
  src/
    index.ts              # Express + Apollo Server setup
    schema/
      typeDefs.ts         # GraphQL schema (copy from Section 2)
      resolvers/
        auth.ts           # login, refreshToken resolvers
        patient.ts        # me, myDisabilities, myProgress resolvers
        appointments.ts   # myAppointments, bookAppointment, cancelAppointment resolvers
        games.ts          # myGameSessions resolvers
        treatments.ts     # myTreatments resolvers
    services/
      apiClient.ts        # Axios instance configured for central API
      authService.ts      # JWT verification, token refresh logic
    middleware/
      auth.ts             # JWT extraction and validation middleware
    utils/
      errorMapper.ts      # Maps central API errors to GraphQL error codes
  package.json
  tsconfig.json
```

This is a suggestion. The backend agent owns the final architecture decision.

---

## 9. Environment Variables

The backend should expect these environment variables:

| Variable | Description | Example |
|----------|-------------|---------|
| `PORT` | Backend server port | `4000` |
| `CENTRAL_API_URL` | Base URL of the central Spring Boot API | `http://localhost:8080/api` |
| `JWT_SECRET` | Secret key for JWT verification (must match central API) | `your-secret-key` |
| `NODE_ENV` | Environment mode | `development` / `production` |
| `CORS_ORIGIN` | Allowed CORS origins (comma-separated) | `http://localhost:19006,http://localhost:8081` |

---

## 10. Quick Reference — What the Frontend Sends

### Login mutation example

```json
{
  "query": "mutation Login($identifier: String!, $password: String!) { login(identifier: $identifier, password: $password) { accessToken refreshToken expiresAt } }",
  "variables": {
    "identifier": "12345678A",
    "password": "SecurePass123"
  }
}
```

### Authenticated query example

```
POST /graphql
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "query": "query { me { id name surname email dni phone birthDate address active } }"
}
```

### Error response example

```json
{
  "data": null,
  "errors": [
    {
      "message": "Invalid credentials provided",
      "locations": [{ "line": 1, "column": 57 }],
      "path": ["login"],
      "extensions": {
        "code": "INVALID_CREDENTIALS",
        "subtitle": "Credenciales invalidas"
      }
    }
  ]
}
```
