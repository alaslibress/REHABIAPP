# CLAUDE.md - RehabiAPP Data (Data Engineering Pipeline)

> **File:** `/data/CLAUDE.md`
> **Agent:** Agent 1 (Backend and Data Engineer)
> **Role:** Thinker (Opus) + Doer (Sonnet)

---

## 1. PROJECT DEFINITION

Pipeline de ingesta y analitica de telemetria de juegos terapeuticos. Recibe sesiones desde el API Core, persiste en MongoDB, y expone agregaciones para visualizacion del progreso del paciente. Genera tambien archivos Markdown por paciente (cache para futuro analisis IA).

Servicio INTERNO. Sin acceso externo. Solo recibe peticiones desde `/api`.

---

## 2. OPERATING RULES

1. **Global context:** Leer raiz `/CLAUDE.md` antes de cualquier decision cross-domain.
2. **Skills mandatory:** Leer `.claude/skills/springboot4-mongodb/SKILL.md` SIEMPRE. CSFLE obligatorio para PII, agregaciones server-side, anonimizacion via patientToken.
3. **Maintain this file:** Marcar `[x]`. Eliminar items resueltos.
4. **Schema design:** Optimizado para read-heavy analitica. Embed lo que se lee junto. Indices en campos de aggregation pipelines.
5. **ETL discipline:** Idempotencia obligatoria. Documentar entrada/transformacion/salida.
6. **No external access:** Solo recibe del API Core. Nunca expuesto.

---

## 3. LOCAL STACK

- Spring Boot 4.0.0, Java 24, Maven.
- Spring Data MongoDB (records `@Document`, MongoTemplate para aggregations).
- Spring Boot Actuator + Micrometer Prometheus.
- Logstash logback encoder (logs JSON).
- Spring Validation (DTOs).

```
./mvnw compile            # Compilar
./mvnw test               # Tests
./mvnw package -DskipTests # Fat JAR
./mvnw spring-boot:run    # Modo desarrollo
docker build -t rehabiapp-data:dev -f data/Dockerfile data/
```

---

## 4. ARCHITECTURE

```
src/main/java/com/rehabiapp/data/
    |-- config/            CsfleConfig, CsfleProperties, MongoIndexConfig
    |-- domain/
    |   |-- document/      GameSession, PatientProgress (records @Document)
    |   |-- repository/    GameSessionRepository, PatientProgressRepository
    |-- ingestion/
    |   |-- controller/    IngestController
    |   |-- dto/           GameSessionIngestionRequest
    |   |-- service/       IngestService, DuplicateSessionException
    |-- analytics/
    |   |-- controller/    AnalyticsController
    |   |-- dto/           Weekly/Monthly/Global/TimeSeriesRom/Cohort/PatientAnalytics DTOs
    |   |-- pipeline/      Weekly/Monthly/Global/TimeSeriesRom/Cohort + (NEW) TreatmentProgressPipeline
    |   |-- service/       AnalyticsService + (NEW) MarkdownService + (NEW) TreatmentProgressService
    |-- internal/          (NEW) Controllers internos para el API Core: CheckNewDataController
    |-- markdown/          (NEW) MdGeneratorService, plantillas de exportacion
    |-- presentation/      HealthController
    |-- scheduler/         PatientProgressRefreshScheduler + (NEW) MarkdownRefreshScheduler
    |-- util/              PseudonymUtil, PeriodUtil + (NEW) TreatmentMetricResolver
```

Controllers thin. Logica en services + pipelines. Respuestas con patientToken (anonimizado).

---

## 5. MONGOOSE/MONGODB COLLECTIONS

### Existentes

```
game_sessions   — telemetria cruda (Phase 2)
patient_progress — pre-agregado semanal (Phase 3, refresh nocturno 02:00 UTC)
```

### Nuevas (Phase 5-6)

```
treatment_progress      — progreso por (patientDni, codTrat, parteCuerpo)
                          campos: baseline, current, deltas, entradas[]
patient_markdown        — cache MD por paciente
                          campos: patientDni, content (string), updatedAt, sessionCount
```

### Schema treatment_progress

```javascript
TreatmentProgress {
  _id,
  patientDni: String (CSFLE Deterministic),
  patientToken: String (SHA-256 anonimo),
  codTrat: String,
  tratamientoNombre: String,
  parteCuerpo: String,
  metricaNombre: String,                  // ej. "Rango de movimiento (grados)"
  baseline: { fecha: Date, valor: Number },
  current:  { fecha: Date, valor: Number },
  deltaPorcentaje: Number,
  entradas: [ { fecha: Date, valor: Number } ],   // 1 por dia
  lastUpdated: Date
}
```

### Schema patient_markdown

```javascript
PatientMarkdown {
  _id,
  patientDni: String (CSFLE Deterministic),
  patientToken: String,
  content: String,                        // markdown completo
  sessionCount: Number,                   // # de sesiones consideradas en la ultima generacion
  updatedAt: Date,
  lastSessionAt: Date                     // timestamp de la sesion mas reciente al generar
}
```

---

## 6. IMPLEMENTATION CHECKLIST

> Phases 1-4 (project setup, schema GameSession/PatientProgress, ingestion, analytics) y Step 10 (K8s readiness) YA completados. Eliminados de este checklist.

### Phase 5 — Treatment progress aggregation (current iteration)

> Detalles en `data/PLAN.md` Phase 5.

- [ ] 5.1 Crear `TreatmentMetricResolver` (util) que mapea `(codTrat, parteCuerpo)` a la metrica relevante de `MovementMetrics`. Mapping inicial: codTrat empieza por "ROM-" → `rangeOfMotionDegrees`; "VEL-" → `averageSpeed`; "FUERZA-" → `maxSpeed`. Configurable via `application.yml` `rehabiapp.metric-map.*`.
- [ ] 5.2 Crear `TreatmentProgressPipeline.java` con metodo `execute(patientDni)`:
  - Pipeline MongoDB: $match → $addFields(`day` = date truncado a dia) → $sort(sessionStart) → $group por `(codTrat, parteCuerpo, day)` con avg de la metrica → output entradas diarias.
  - Para baseline: primera entrada de cada (codTrat, parteCuerpo).
  - Para current: ultima entrada de cada (codTrat, parteCuerpo).
  - Calcula `deltaPorcentaje = (current - baseline) / baseline * 100`.
- [ ] 5.3 Crear `TreatmentProgressService` que orquesta el pipeline + persiste en `treatment_progress` collection (upsert por `patientDni + codTrat + parteCuerpo`).
- [ ] 5.4 Endpoint en `AnalyticsController`:
  - `GET /analytics/patient/{dni}/treatment-progress` → `List<TreatmentProgressDto>`.
- [ ] 5.5 DTO `TreatmentProgressDto` con baseline, current, delta, entradas.
- [ ] 5.6 Endpoint interno para el API: `GET /internal/patient/{dni}/check-new-data?since=<Instant>` → `{ hasNewData, lastSessionAt, count }`. Solo cuenta sesiones con `receivedAt > since`.
- [ ] 5.7 Endpoint adicional: `GET /analytics/patient/{dni}/last-session` → `LastSessionDto` con timestamp + gameId. Usado por DashboardService del API.
- [ ] 5.8 Tests unitarios + integration con `@DataMongoTest` o equivalente con embedded MongoDB.

### Phase 6 — Markdown generation (current iteration)

> Detalles en `data/PLAN.md` Phase 6.

- [ ] 6.1 Crear `MdGeneratorService.java` que dado un `dni`:
  - Carga `TreatmentProgressDto[]` (Phase 5).
  - Carga las ultimas N=20 sesiones del paciente.
  - Aplica plantilla Markdown estructurada con secciones: Cabecera (DNI anonimizado), Resumen ejecutivo, Por tratamiento (baseline vs current, % delta), Sesiones recientes (tabla), Notas para IA.
  - Devuelve string MD.
- [ ] 6.2 Crear `MarkdownService` (orquesta MdGenerator + persiste en `patient_markdown` upsert).
- [ ] 6.3 Endpoints en `AnalyticsController`:
  - `GET /analytics/patient/{dni}/markdown` (Content-Type: `text/markdown;charset=UTF-8`) — devuelve cache si existe, o genera on-demand.
  - `POST /analytics/patient/{dni}/markdown/regenerar` — fuerza regeneracion + actualiza cache.
- [ ] 6.4 Anonimizacion: el MD usa `patientToken` (no DNI) en su contenido. Solo el endpoint admite `dni` como path param.
- [ ] 6.5 `MarkdownRefreshScheduler` (@Scheduled cron `0 */15 * * * *`): cada 15 minutos, regenera MD de pacientes con `lastSessionAt > markdown.updatedAt`. Limita a 50 pacientes por ejecucion para no saturar.
- [ ] 6.6 Tests con plantilla MD esperada (golden files).

### Phase 7 — Audit + observability (current iteration)

- [ ] 7.1 Anadir Micrometer counters: `rehabiapp.data.ingest.sessions` (incremento por sesion ingestada), `rehabiapp.data.markdown.regenerations` (incremento por MD regenerado).
- [ ] 7.2 Anadir timer `rehabiapp.data.pipeline.duration` con tag `pipeline=weekly|monthly|treatment-progress|...`.
- [ ] 7.3 Logs estructurados al regenerar MD: `{event: "md_regenerated", dni: <token>, sessionCount, durationMs}`.

---

## 7. RUNBOOK LOCAL

```bash
# MongoDB
docker compose -f infra/docker-compose.yml up mongodb

# Data pipeline
cd data && ./mvnw spring-boot:run
# puerto 8081

# Health
curl http://localhost:8081/actuator/health
curl http://localhost:8081/actuator/health/readiness
```

---

## Memory

You have access to Engram persistent memory via MCP tools (mem_save, mem_search, mem_session_summary, etc.).
- Save proactively after significant work — don't wait to be asked.
- After any compaction or context reset, call `mem_context` to recover session state before continuing.
