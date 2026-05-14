# PLAN — Polymorphic telemetry + dual persistence (Mongo + Postgres)

> **File:** `/data/PLAN-polymorphic-telemetry.md`
> **Branch:** `stats-implementation`
> **Iteration date:** 2026-05-11
> **Author:** Agent 1 + Agent 4 Thinker (Opus) — PRESCRIPTIVE for Doer (Sonnet).
> **Language:** code/comments in Spanish. Plan in English.
> **Scope:** Refactor `GameSession` to a polymorphic contract (`rawMetrics` Map/JSON), persist (a) raw JSON in MongoDB (current `/data`), (b) per-session Markdown report in PostgreSQL `session_reports` (new table in `/api`), wire dual-write with idempotent error handling, enforce a strict ingestion contract via Bean Validation, and document impact on `/mobile/*` and `/desktop`.

---

## 0. STACK ALIGNMENT (READ FIRST — DO NOT SKIP)

User-facing request mentioned **Mongoose**, **Joi/Zod**, **Node.js pooling**. The real stack is:

| User said | Real stack — use this instead |
|-----------|-------------------------------|
| Mongoose model | Spring Data MongoDB `@Document` record (existing convention) |
| Mongoose connection script + pooling | `spring.data.mongodb.uri` + `MongoClientSettings` connection-pool tuning |
| Joi/Zod validation middleware | Jakarta Bean Validation (`jakarta.validation.*`) + `@ControllerAdvice` exception handler — already partially wired in `GameSessionIngestionRequest` |
| Env-driven config | `application.yml` + `${...}` placeholders + `RH_DATA_*` env vars (existing pattern) |

**Do NOT introduce Node.js, Mongoose, Joi, or Zod into this repository.** Translate every user requirement into the Java/Spring equivalent listed above.

Mandatory context BEFORE coding:
1. `/CLAUDE.md` §4.5 (Spanish comments, no emojis), §4.6 (healthcare security), §6 (BFF pattern).
2. `/data/CLAUDE.md` §2 (operating rules), §5 (Mongo collections).
3. `/data/.claude/skills/springboot4-mongodb/SKILL.md` (CSFLE on PII, server-side aggregation, anonymisation).
4. `/api/CLAUDE.md` (Flyway discipline, RBAC, audit).
5. Existing `IngestService` (`/data/src/main/java/com/rehabiapp/data/ingestion/service/IngestService.java`) — DO NOT delete; refactor in place.
6. Existing migration head: `V16__fix_campos_clinicos_vacios.sql`. **Next free version: `V17`.**

---

## 1. GOAL (single sentence)

Make game telemetry ingestion **game-agnostic** by replacing the fixed `MovementMetrics` record with a polymorphic `rawMetrics` map, persist the raw JSON in MongoDB for real-time analytics, and synchronously generate a per-session Markdown summary that gets inserted into a new `session_reports` table in PostgreSQL via an internal `/api` callback, with strict validation, idempotency, and an explicit failure-handling policy that prevents orphan records.

---

## 2. DESIGN DECISIONS (FROZEN — do not negotiate)

### 2.1 Polymorphic shape

`GameSession` keeps mandatory clinical metadata (`patientDni`, `gameId`, `progressionLevel`, `sessionStart`, `sessionEnd`, `durationSeconds`, `completed`, `receivedAt`, `patientToken`, optional treatment enrichment) and adds:

```
rawMetrics: Map<String, Object>     // free-form per-game payload, validated by schema (§5)
schemaVersion: String               // ej. "v1", "v2" — set by Unity client
metricsHash: String                 // SHA-256(canonicalize(rawMetrics)) for idempotency
```

The legacy embedded `MovementMetrics` record is **kept as a compatibility shim** for one iteration: if `rawMetrics` contains keys `rangeOfMotionDegrees`, `averageSpeed`, `maxSpeed`, the ingestion service projects them into the legacy field so existing pipelines (`WeeklyGamePipeline`, `RomTimeSeriesPipeline`, `TreatmentProgressPipeline`) keep working unchanged. Remove the shim only after Phase F clean-up.

### 2.2 Dual-write policy

Order:

```
1. Mongo INSERT (rawMetrics + metadata)            — source of truth for analytics
2. Build Markdown report (in-memory, deterministic) — pure function of step 1 doc
3. POST /api/internal/session-reports               — Postgres insert in /api
```

If step 3 fails, the Mongo doc is marked `reportStatus = "PENDING"` and a scheduled job (Phase D.4) retries every 5 min up to 6 hours. **No two-phase commit, no XA.** This is an outbox-like compensation pattern, justified by:

- Mongo is the analytics source-of-truth → must succeed first.
- Postgres `session_reports` is a derived cache for reporting → eventual consistency acceptable.
- 5 retries + alert → operator visibility, no silent data loss.

If step 1 fails, the call returns 5xx to the Unity client and **nothing is written anywhere** — Unity must retry idempotently (handled by `metricsHash` uniqueness).

### 2.3 Idempotency

Unique compound index `(patientDni, gameId, sessionStart, metricsHash)` on `game_sessions`. Duplicate inserts return the existing document, not 409, so Unity retries are safe under network flakiness. `session_reports` uses Mongo `_id` as UNIQUE FK key to prevent duplicates downstream.

### 2.4 Postgres column type

`session_reports.contenido_md` is **`TEXT`**, not `BYTEA`. Markdown is human-readable UTF-8; `TEXT` enables full-text search later (`tsvector`) and avoids encoding hops. `BYTEA` is reserved for true binaries (PDF, images).

### 2.5 Validation strategy

Two layers:

- **Structural** (Bean Validation, fail-fast at controller): mandatory fields, ranges, ISO-8601 dates. Reject with HTTP 400 + JSON problem detail.
- **Semantic** (`MetricSchemaRegistry`, fail at service): per-`gameId` JSON schema declaring allowed keys, types, and value bounds. Loaded from `classpath:metric-schemas/{gameId}.json` (or `application.yml`). Unknown `gameId` rejected with 422.

---

## 3. PHASE A — POLYMORPHIC GameSession

### A.1 Refactor record

File: `/data/src/main/java/com/rehabiapp/data/domain/document/GameSession.java`

Add fields:

```java
@Field("rawMetrics")
Map<String, Object> rawMetrics,

@Field("schemaVersion")
String schemaVersion,

@Field("metricsHash")
String metricsHash,

@Field("reportStatus")
String reportStatus,            // "PENDING" | "OK" | "FAILED"

@Field("reportAttempts")
Integer reportAttempts,

@Field("reportLastError")
String reportLastError,
```

Keep `movementMetrics` as legacy nullable field. Add canonical constructor that null-defaults the new fields.

### A.2 Repository

File: `/data/src/main/java/com/rehabiapp/data/domain/repository/GameSessionRepository.java`

Add:

```java
Optional<GameSession> findByPatientDniAndGameIdAndSessionStartAndMetricsHash(
        String patientDni, String gameId, Instant sessionStart, String metricsHash);

List<GameSession> findTop50ByReportStatusOrderByReceivedAtAsc(String reportStatus);
```

### A.3 Index migration

File: `/data/src/main/java/com/rehabiapp/data/config/MongoIndexConfig.java`

Add compound index `{patientDni:1, gameId:1, sessionStart:1, metricsHash:1}` UNIQUE, and `{reportStatus:1, receivedAt:1}` for retry scheduler scans.

### A.4 DTO refactor

File: `/data/src/main/java/com/rehabiapp/data/ingestion/dto/GameSessionIngestionRequest.java`

Replace `MovementMetricsRequest` with:

```java
@NotNull @NotEmpty
Map<String, @NotNull Object> rawMetrics,

@NotBlank @Pattern(regexp = "^v[0-9]+$")
String schemaVersion,
```

Keep legacy `movementMetrics` field as `@Deprecated` optional for backward compat during the transition. If both are sent, `rawMetrics` wins.

### A.5 Acceptance for Phase A

- `./mvnw compile` SUCCESS.
- New tests in `GameSessionTest.java`: serialise/deserialise `rawMetrics` with mixed types (Number, String, Boolean, nested Map). All green.

---

## 4. PHASE B — POSTGRES `session_reports`

### B.1 Flyway migration

File: `/api/src/main/resources/db/migration/V17__session_reports.sql`

```sql
CREATE TABLE session_reports (
    id              BIGSERIAL PRIMARY KEY,
    mongo_id        VARCHAR(48)  NOT NULL UNIQUE,
    paciente_dni    VARCHAR(20)  NOT NULL,
    cod_juego       VARCHAR(32)  NOT NULL,
    cod_trat        VARCHAR(32),
    fecha_sesion    TIMESTAMPTZ  NOT NULL,
    duracion_seg    INTEGER      NOT NULL,
    contenido_md    TEXT         NOT NULL,
    md_hash         VARCHAR(64)  NOT NULL,
    fecha_creacion  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_session_reports_paciente
        FOREIGN KEY (paciente_dni) REFERENCES paciente(dni_pac)
        ON DELETE RESTRICT,
    CONSTRAINT fk_session_reports_juego
        FOREIGN KEY (cod_juego) REFERENCES juego(cod_juego)
        ON DELETE RESTRICT
);

CREATE INDEX idx_session_reports_paciente_fecha
    ON session_reports (paciente_dni, fecha_sesion DESC);

CREATE INDEX idx_session_reports_juego
    ON session_reports (cod_juego);

COMMENT ON TABLE session_reports IS
    'Resumen Markdown de cada sesion de juego. Cache derivado de game_sessions en MongoDB.';
```

**DO NOT** modify existing migrations. Append-only.

### B.2 JPA entity

File: `/api/src/main/java/com/rehabiapp/api/domain/entity/SessionReport.java`

Standard `@Entity` mapping the table above. Use `@CreationTimestamp` for `fechaCreacion`. `contenidoMd` annotated `@Lob` (Hibernate maps to TEXT under PostgreSQL dialect when length unspecified, but `@Lob` makes intent explicit).

### B.3 Repository

File: `/api/src/main/java/com/rehabiapp/api/domain/repository/SessionReportRepository.java`

```java
public interface SessionReportRepository extends JpaRepository<SessionReport, Long> {
    Optional<SessionReport> findByMongoId(String mongoId);
    List<SessionReport> findByPacienteDniOrderByFechaSesionDesc(String dni, Pageable pageable);
}
```

### B.4 Internal controller (API side, not exposed)

File: `/api/src/main/java/com/rehabiapp/api/presentation/controller/InternalSessionReportController.java`

Endpoint:

```
POST /api/internal/session-reports
Header: X-Internal-Key: ${rehabiapp.internal-key}
Body:  SessionReportRequest
Resp:  201 Created + { id } | 200 OK (idempotent reuse) | 4xx | 5xx
```

`SessionReportRequest`:

```java
public record SessionReportRequest(
        @NotBlank String mongoId,
        @NotBlank String pacienteDni,
        @NotBlank String codJuego,
        String codTrat,
        @NotNull Instant fechaSesion,
        @Positive Integer duracionSeg,
        @NotBlank String contenidoMd,
        @NotBlank @Pattern(regexp = "^[a-f0-9]{64}$") String mdHash
) {}
```

Idempotency: if `mongoId` already exists, return 200 with the existing row instead of inserting a duplicate.

Auth: same `InternalAuthFilter` already in `/api`. Reject if header mismatch with 401.

Audit: write an `audit_log` row with `accion='REPORT_CREATED'`, `tabla='session_reports'`, `usuario_id=NULL`, `detalle=mongoId`. Use existing `AuditService`.

### B.5 Acceptance for Phase B

- `./mvnw -pl api flyway:migrate` SUCCESS against a fresh DB.
- Integration test `SessionReportControllerIT.java`:
  - 201 on first insert.
  - 200 (idempotent) on second insert with same `mongoId`.
  - 401 without `X-Internal-Key`.
  - 400 on missing `contenidoMd`.

---

## 5. PHASE C — MARKDOWN REPORT GENERATOR (per-session)

### C.1 Service

File: `/data/src/main/java/com/rehabiapp/data/markdown/SessionReportMdGenerator.java`

Pure function: `String render(GameSession session, GameSchema schema)` → returns Markdown.

Template (UTF-8, no emojis, Spanish):

```
# Informe de sesion {session.id}

- **Paciente:** {patientToken}
- **Juego:** {tratamientoNombre} ({gameId})
- **Tratamiento:** {codTrat}
- **Parte del cuerpo:** {parteCuerpo}
- **Inicio:** {sessionStart ISO-8601}
- **Fin:** {sessionEnd ISO-8601}
- **Duracion:** {durationSeconds} s
- **Nivel de progresion:** {progressionLevel}
- **Completada:** Si/No

## Metricas registradas

| Metrica | Valor | Unidad |
|---------|-------|--------|
| ... iterar rawMetrics, ordenado por clave; unidad desde schema.units[key]; valor formateado a 2 decimales si Number ... |

## Resumen ejecutivo

{Si schema define summary template, renderizar. Si no, omitir esta seccion.}
```

Deterministic ordering (sort keys alphabetically) so `md_hash = SHA-256(content)` is reproducible.

### C.2 Wire into IngestService

File: `/data/src/main/java/com/rehabiapp/data/ingestion/service/IngestService.java`

Inject `SessionReportMdGenerator`, `MetricSchemaRegistry`, `ApiInternalClient` (new — Phase D.1). After Mongo `save(session)`:

```java
String md = generator.render(saved, schema);
String mdHash = sha256(md);
try {
    apiClient.postSessionReport(buildRequest(saved, md, mdHash));
    repository.markReportStatus(saved.id(), "OK", null);
} catch (Exception e) {
    repository.markReportStatus(saved.id(), "PENDING", e.getMessage());
    log.warn("session_report POST fallo, marcado PENDING: id={}", saved.id(), e);
}
```

Add helper `markReportStatus(id, status, lastError)` to `GameSessionRepository` via `@Query` partial update (single field update, not a full replace, to avoid race with concurrent analytics writes).

### C.3 Acceptance for Phase C

- Golden-file test `SessionReportMdGeneratorTest.java` with three sample sessions (piano-fingers, rom-shoulder, force-grip) — output matches `src/test/resources/golden/*.md`.
- `md_hash` is stable across runs for identical input.

---

## 6. PHASE D — CROSS-DOMAIN HTTP CLIENT + RETRY LOOP

### D.1 ApiInternalClient

File: `/data/src/main/java/com/rehabiapp/data/internal/client/ApiInternalClient.java`

Spring `RestClient` (Java 24 / Spring Boot 4 idiomatic). Config:

```yaml
rehabiapp:
  api:
    base-url: ${RH_API_BASE_URL:http://api:8080}
    internal-key: ${RH_INTERNAL_KEY:changeme}
    timeout-ms: ${RH_API_TIMEOUT_MS:5000}
    max-retries: ${RH_API_MAX_RETRIES:3}
```

Retry policy on POST: exponential backoff (100ms, 400ms, 1600ms) on 5xx and network errors only. **4xx never retries** — those are contract violations the developer must fix.

### D.2 ConnectionPoolConfig (Mongo)

File: `/data/src/main/java/com/rehabiapp/data/config/MongoConnectionPoolConfig.java`

`@Bean MongoClientSettingsBuilderCustomizer` configuring:

```
minSize = ${RH_MONGO_POOL_MIN:5}
maxSize = ${RH_MONGO_POOL_MAX:50}
maxWaitTime = 30s
maxConnectionIdleTime = 10min
```

This satisfies the user requirement "pooling de conexiones" — in Java/Mongo driver pooling is built-in, we just expose the knobs.

### D.3 Env documentation

Append to `/data/.env.example` (create if missing):

```
RH_MONGO_URI=mongodb://localhost:27017/rehabiapp_data
RH_MONGO_POOL_MIN=5
RH_MONGO_POOL_MAX=50
RH_API_BASE_URL=http://localhost:8080
RH_INTERNAL_KEY=changeme
RH_API_TIMEOUT_MS=5000
RH_API_MAX_RETRIES=3
```

### D.4 Retry scheduler

File: `/data/src/main/java/com/rehabiapp/data/scheduler/SessionReportRetryScheduler.java`

```java
@Scheduled(cron = "0 */5 * * * *")
public void retryPending() {
    var pending = repo.findTop50ByReportStatusOrderByReceivedAtAsc("PENDING");
    for (var s : pending) {
        if (s.reportAttempts() != null && s.reportAttempts() >= 72) {
            repo.markReportStatus(s.id(), "FAILED", "max attempts reached");
            log.error("session_report giving up: id={}", s.id());
            continue;
        }
        try {
            apiClient.postSessionReport(rebuildRequest(s));
            repo.markReportStatus(s.id(), "OK", null);
        } catch (Exception e) {
            repo.incrementReportAttempts(s.id(), e.getMessage());
        }
    }
}
```

72 attempts × 5 min = 6h SLA before alert. Emit Micrometer counter `rehabiapp.data.session_report.failed` when status flips to FAILED.

### D.5 Acceptance for Phase D

- Manual smoke: stop `/api`, send a session to `/data` → Mongo doc has `reportStatus=PENDING`. Restart `/api`. Within 5 min the scheduler flips it to OK and the Postgres row appears.
- Unit test for the scheduler with mocked client.

---

## 7. PHASE E — VALIDATION CONTRACT (Bean Validation + schema registry)

### E.1 MetricSchemaRegistry

File: `/data/src/main/java/com/rehabiapp/data/ingestion/schema/MetricSchemaRegistry.java`

```java
@Component
public class MetricSchemaRegistry {
    private final Map<String, GameSchema> schemas;   // loaded from classpath:metric-schemas/*.json at startup

    public GameSchema require(String gameId) {
        var s = schemas.get(gameId);
        if (s == null) throw new UnknownGameException(gameId);
        return s;
    }

    public void validate(String gameId, Map<String, Object> rawMetrics) {
        var schema = require(gameId);
        for (var entry : rawMetrics.entrySet()) {
            var spec = schema.fields().get(entry.getKey());
            if (spec == null) throw new InvalidMetricException("clave no permitida: " + entry.getKey());
            spec.validate(entry.getValue());   // type + range
        }
        for (var required : schema.requiredKeys()) {
            if (!rawMetrics.containsKey(required))
                throw new InvalidMetricException("clave obligatoria ausente: " + required);
        }
    }
}
```

### E.2 Schema files

`/data/src/main/resources/metric-schemas/PIANO-001.json`:

```json
{
  "gameId": "PIANO-001",
  "requiredKeys": ["fingerSpeed", "notesHit", "notesMissed"],
  "fields": {
    "fingerSpeed":  { "type": "number", "min": 0,   "max": 20,   "unit": "notes/s" },
    "notesHit":     { "type": "integer", "min": 0,  "max": 10000 },
    "notesMissed":  { "type": "integer", "min": 0,  "max": 10000 },
    "accuracyPct":  { "type": "number", "min": 0,   "max": 100,  "unit": "%" }
  }
}
```

`/data/src/main/resources/metric-schemas/ROM-SHOULDER.json`:

```json
{
  "gameId": "ROM-SHOULDER",
  "requiredKeys": ["rangeOfMotionDegrees"],
  "fields": {
    "rangeOfMotionDegrees": { "type": "number", "min": 0, "max": 180, "unit": "deg" },
    "averageSpeed":         { "type": "number", "min": 0, "max": 500, "unit": "deg/s" },
    "maxSpeed":             { "type": "number", "min": 0, "max": 800, "unit": "deg/s" }
  }
}
```

### E.3 Wire validation

In `IngestService.ingestSession`, immediately after Bean Validation passes:

```java
schemaRegistry.validate(req.gameId(), req.rawMetrics());
```

### E.4 Exception handler

File: `/data/src/main/java/com/rehabiapp/data/presentation/IngestExceptionHandler.java` (new `@ControllerAdvice`).

Map:

- `MethodArgumentNotValidException` → 400 + `application/problem+json`
- `UnknownGameException` → 422
- `InvalidMetricException` → 422
- `DuplicateSessionException` → 200 with existing doc (NOT 409 — idempotent contract)

### E.5 Acceptance for Phase E

- Integration test sending `rawMetrics={"foo":1}` against `PIANO-001` → 422 with body `{detail:"clave no permitida: foo"}`.
- Missing required key → 422.
- Type mismatch (string where number) → 422.

---

## 8. PHASE F — IMPACT ANALYSIS (frontend / BFF / desktop)

This is the answer to the user's section 3 ("Análisis de Impacto y Notificaciones"). The Doer MUST cross-check each item with the corresponding domain CLAUDE.md before claiming the work is done.

### F.1 `/mobile/backend` (BFF)

| Touchpoint | Status | Action |
|------------|--------|--------|
| GraphQL `myProgress` / `myDashboard` | reads `PatientProgress` aggregates, NOT raw `GameSession`. | **No change needed.** Aggregates are computed by existing pipelines that still see the legacy `MovementMetrics` shim. |
| Any direct query that exposes `movementMetrics` to mobile | grep in `/mobile/backend/src/services/*` for `movementMetrics`, `rangeOfMotion`. | If found: extend the GraphQL schema with a `JSON` scalar `rawMetrics` and a polymorphic `MetricEntry { key, value, unit }` resolver. Default is **no change** — see grep result. |

### F.2 `/mobile/frontend` (React Native)

| Touchpoint | Status | Action |
|------------|--------|--------|
| `progressStore.ts`, `gamesStore.ts`, screens under `(tabs)/progress`, `(tabs)/games` | consume aggregates, not raw sessions. | **No change.** |
| Per-session detail view | does not exist yet. | If product later asks for a per-session view, mobile would call a new BFF query that returns the Markdown report from Postgres (see F.4). Out of scope for this iteration. |

### F.3 `/desktop` (JavaFX)

| Touchpoint | Status | Action |
|------------|--------|--------|
| Progress tab polling `/api/pacientes/{dni}/progreso/check` | unchanged. | **No change.** |
| Session report viewer | does not exist yet. | Add a new view in a follow-up iteration that calls `GET /api/pacientes/{dni}/session-reports` (Phase F.4). Out of scope here. |

### F.4 `/api` — new public endpoint (optional, scope-controlled)

File: `/api/src/main/java/com/rehabiapp/api/presentation/controller/SessionReportController.java`

```
GET /api/pacientes/{dni}/session-reports?page=0&size=20
GET /api/pacientes/{dni}/session-reports/{id}            (returns Markdown body)
```

Role: `SPECIALIST` and `PATIENT_SELF` (patient may read only own reports — enforce via JWT claim).

**Marked optional**: implement only if `/desktop` or `/mobile` PLAN.md introduces a consuming view in this iteration. Otherwise skip — the data is being captured and the consumer can ship later without re-touching `/data`.

### F.5 Unity client contract (Agent 4)

The Unity build MUST send:

```json
{
  "patientDni": "12345678A",
  "gameId": "PIANO-001",
  "schemaVersion": "v1",
  "progressionLevel": 2,
  "sessionStart": "2026-05-11T10:00:00Z",
  "sessionEnd":   "2026-05-11T10:07:32Z",
  "durationSeconds": 452,
  "completed": true,
  "rawMetrics": {
    "fingerSpeed": 3.7,
    "notesHit": 142,
    "notesMissed": 18,
    "accuracyPct": 88.75
  }
}
```

Action items for Agent 4 (Unity Doer) — track in a separate PR on the Unity repo, **out of scope of this `/data` iteration but block release**:

- [ ] Update `TelemetryUploader.cs` to populate `rawMetrics` from each game's metric collector.
- [ ] Set `schemaVersion = "v1"` constant.
- [ ] Remove the old `movementMetrics` payload after `/data` deploys.
- [ ] Implement retry-on-5xx with idempotency key derived client-side (UUID per session attempt).

---

## 9. PHASE G — TESTS (mandatory before declaring done)

Per `/CLAUDE.md` §10, TestSprite MCP must return 100% before any `[ ]` flips to `[x]`. Test categories required:

| Category | Files | Coverage target |
|----------|-------|-----------------|
| Unit | `SessionReportMdGeneratorTest`, `MetricSchemaRegistryTest`, `IngestServiceTest` | new logic only, 100% branches |
| Integration MongoDB | `IngestServiceIT` (existing, extend) | polymorphic insert + duplicate dedup + report status transitions |
| Integration HTTP | `IngestControllerIT` | 200/400/422 paths, idempotent re-POST |
| Integration cross-service | `SessionReportControllerIT` (`/api`) | 201/200/401/400 |
| End-to-end | `DualWriteE2ETest` | spins up `/data` + `/api` via Testcontainers, posts a session, asserts Mongo doc + Postgres row both present with matching `md_hash` |
| Failure injection | `RetrySchedulerIT` | `/api` down → PENDING → up → OK within 1 retry tick |

If Testcontainers is not in the POM yet, **add it** under `<scope>test</scope>` — this is the only build-file change required outside what Phases A-F already prescribe.

---

## 10. EXECUTION ORDER (DO NOT REORDER)

```
A.1 → A.2 → A.3 → A.4 → A.5      (compile clean, MongoDB shape ready)
B.1 → B.2 → B.3 → B.4 → B.5      (Postgres side standalone; /api can ship before /data wires the client)
C.1 → C.2 → C.3                  (Markdown generator + wire into ingest service)
D.1 → D.2 → D.3 → D.4 → D.5      (HTTP client, pooling, retry loop)
E.1 → E.2 → E.3 → E.4 → E.5      (validation hardening)
F.1 → F.2 → F.3 → F.4 → F.5      (cross-domain audit, only F.4 may produce code in this iteration)
G                                 (full test sweep + TestSprite green)
```

Phases B and A are independent — they may be done in parallel by two sub-agents if available. Everything else is sequential.

---

## 11. ROLLBACK PLAN

If after deploy production shows `reportStatus=FAILED` rate > 1%:

1. Disable the retry scheduler via `rehabiapp.scheduler.session-report-retry.enabled=false`.
2. Keep accepting ingest (Mongo write still succeeds).
3. Drain pending rows manually via a one-off SQL/Mongo script after fixing `/api`.

Mongo schema changes are additive — no rollback migration needed. Postgres `V17` is additive and can be left in place; revert is `DROP TABLE session_reports` if absolutely required, but data loss is acceptable since the table is a derived cache.

---

## 12. OUT OF SCOPE (explicit)

- Removing the legacy `movementMetrics` field. Will happen in a follow-up after Unity ships the v1 schema and analytics pipelines migrate to read from `rawMetrics`.
- Full-text search on Markdown content (`tsvector` index). Plan only — not implemented here.
- AI summarisation of the Markdown reports. Future iteration.
- Patient-facing view of per-session reports. Mobile/desktop PLANs own this.
- Schema versioning beyond `v1`. Add `v2` only when a real breaking change appears.

---

## 13. CHECKLIST FOR DOER (copy this into the iteration commit message)

- [ ] Phase A — Polymorphic GameSession + index + DTO
- [ ] Phase B — V17 migration + SessionReport entity + internal controller
- [ ] Phase C — Markdown generator + ingest wiring
- [ ] Phase D — HTTP client + pool config + retry scheduler + env example
- [ ] Phase E — Schema registry + JSON files + exception handler
- [ ] Phase F.1-F.3 — Impact grep on mobile/desktop, recorded in commit body
- [ ] Phase F.4 — Public reports endpoint (only if a consumer ships this iteration)
- [ ] Phase G — All test categories green, TestSprite 100%
- [ ] `/data/CLAUDE.md` §6 updated with new "Phase 8 — Polymorphic telemetry" section
- [ ] `mem_save` called with `type=decision`, `topic_key=architecture/polymorphic-telemetry`

End of plan.
