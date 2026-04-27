# PLAN.md — Data Pipeline: Phase 5 Treatment progress timeline

> **Date:** 2026-04-27
> **Agent:** Sonnet (Doer) under Agent 1 (API + Data)
> **Domain:** `/data/`
> **Prerequisitos:**
>  1. Phases 1-4 cerradas (ver archivo al final del documento).
>  2. Leer `data/CLAUDE.md` y `data/.claude/skills/springboot4-mongodb/SKILL.md` ANTES de empezar.
> **Scope:** materializar la línea de tiempo de progreso clínico por articulación que consume `/api` (`ProgresoService`) para regenerar `paciente.progreso_md`. Incluye pipeline Mongo, dos endpoints (`treatment-timeline`, `has-new-sessions`) y un generador de markdown determinista.

---

## Status summary

| # | Phase 5 item | Status |
|---|---|---|
| 5.1 | `TreatmentProgressTimelinePipeline` (group articulación + day, primer vs último, serie diaria) | PENDING |
| 5.2 | GET `/analytics/patient/{dni}/treatment-timeline` (X-Internal-Key) | PENDING |
| 5.3 | GET `/analytics/patient/{dni}/has-new-sessions?desde=ISO_TS` | PENDING |
| 5.4 | `ProgressMarkdownGenerator` (markdown determinista por articulación con tendencia SimpleRegression) | PENDING |
| 5.5 | Tests Testcontainers Mongo + snapshot del markdown | PENDING |

Todas las rutas relativas a `/home/alaslibres/DAM/RehabiAPP/data/`.

---

## Step 5.1 — TreatmentProgressTimelinePipeline

**Path:** `data/src/main/java/com/rehabiapp/data/application/pipeline/TreatmentProgressTimelinePipeline.java`

**Input:**
- `String patientDni` (obligatorio).
- `Instant from` (opcional; `null` = todo el histórico).

**Aggregation stages:**

1. `$match`:
   ```
   { patientDni: <dni> }
   ```
   Si `from != null`: añadir `sessionStart: { $gte: from }`.

2. `$sort`:
   ```
   { sessionStart: 1 }
   ```
   Garantiza que `$first` y `$last` reflejen primer y último registro temporal.

3. `$group` por articulación + día:
   ```
   _id: {
     articulacionCodigo: "$movementMetrics.articulacionCodigo",
     day: { $dateToString: { format: "%Y-%m-%d", date: "$sessionStart" } }
   },
   firstSession: { $first: "$$ROOT" },
   lastSession:  { $last: "$$ROOT" },
   points: { $push: {
       fecha: "$sessionStart",
       valor: "$movementMetrics.rangeOfMotionDegrees"
   } }
   ```

**Output Java:** `List<ArticulacionTimeline>` (DTO declarado en `application/service/dto/`):

```java
public record ArticulacionTimeline(
    String articulacionCodigo,
    String articulacionNombre,
    String metricKey,                  // p.ej. "rangeOfMotionDegrees"
    MetricSnapshot inicial,            // valor primer día
    MetricSnapshot actual,             // valor último día
    double deltaAbs,                   // actual - inicial
    double deltaPct,                   // (actual - inicial) / inicial * 100
    List<MetricPoint> serie            // serie diaria ordenada asc por fecha
) {}

public record MetricSnapshot(Instant fecha, double valor) {}
public record MetricPoint(Instant fecha, double valor) {}
```

**Trend:** usar `org.apache.commons.math3.stat.regression.SimpleRegression` (ya disponible vía Phase 4 step 0) sobre la serie diaria con `x = índice del día` para obtener `slope` y `r2`. El slope se expone también como log informativo (no parte del DTO público todavía).

---

## Step 5.2 — Endpoint treatment-timeline

**Path:** extender `data/src/main/java/com/rehabiapp/data/api/AnalyticsController.java` (o ruta equivalente del controller existente).

**Auth:** `X-Internal-Key` (filter ya cubre `/analytics/**`, root CLAUDE §6).

**Query params:**
- `from` (opcional, ISO instant). Sin valor → todo el histórico.

**Response:**

```java
public record TreatmentTimelineResponse(
    String patientDni,
    Instant generatedAt,
    List<ArticulacionTimeline> articulaciones
) {}
```

Endpoint:

```java
@GetMapping("/patient/{dni}/treatment-timeline")
public TreatmentTimelineResponse treatmentTimeline(
    @PathVariable @Pattern(regexp = DNI_REGEX) String dni,
    @RequestParam(required = false) String from) {
    return service.treatmentTimeline(dni, from);
}
```

`service.treatmentTimeline(...)` orquesta `TreatmentProgressTimelinePipeline.run(dni, fromInstant)` y rellena `generatedAt = Instant.now()`. Si la lista resultante está vacía → 404 `not_found` (consistente con Phase 4).

---

## Step 5.3 — Endpoint has-new-sessions

**Path:** mismo controller `AnalyticsController`.

**Lógica Mongo:**

```
db.gameSession
  .find({ patientDni: <dni>, receivedAt: { $gt: <desde> } })
  .sort({ receivedAt: -1 })
  .limit(1)
```

Si `desde IS NULL` → `hayNuevos = (count({ patientDni }) > 0)` y `ultimoRegistro = max(receivedAt)`.

Si el cliente nunca sincronizó (`ultima_sync_progreso IS NULL` en /api) y por tanto envía `desde` vacío, /api debe interpretarlo como primera sync → `hayNuevos = true` por convención.

**Response:**

```java
public record HasNewSessionsResponse(
    boolean hayNuevos,
    Instant ultimoRegistro      // null si hayNuevos == false
) {}
```

Endpoint:

```java
@GetMapping("/patient/{dni}/has-new-sessions")
public HasNewSessionsResponse hasNewSessions(
    @PathVariable @Pattern(regexp = DNI_REGEX) String dni,
    @RequestParam(required = false) String desde) {
    return service.hasNewSessions(dni, desde);
}
```

**Índices:** Verificar `MongoIndexInitializer` ya declara índice compuesto `{patientDni:1, receivedAt:-1}`; si falta, añadirlo. No usar `@CompoundIndex` si el initializer es single source of truth.

```
gameSession: { patientDni: 1, receivedAt: -1 }
```

Si ya existe `{ patientDni: 1 }` simple, el compuesto lo sustituye. Documentar la regla en el initializer.

---

## Step 5.4 — ProgressMarkdownGenerator

**Path:** `data/src/main/java/com/rehabiapp/data/application/service/ProgressMarkdownGenerator.java`

**Input:** `TreatmentTimelineResponse`.

**Output:** `String` markdown determinista. Mismo input → mismo output (requisito para snapshot tests).

**Plantilla:**

```
# Progreso paciente {dni}
Generado: {ISO}

## Articulación {codigo} — {nombre}
- Estado inicial: valor X el día Y
- Estado actual: valor X el día Y
- Variación: ±N% (slope=...)

### Serie diaria
| Fecha | Valor |
|---|---|
| 2026-04-10 | 68.20 |
| 2026-04-11 | 69.50 |
...
```

**Reglas de determinismo:**
- Iterar `articulaciones` en orden ascendente por `articulacionCodigo`.
- Iterar `serie` en orden ascendente por `fecha`.
- Formato numérico fijo: 2 decimales, locale `Locale.ROOT`.
- Slope con 4 decimales, locale `Locale.ROOT`.
- Fechas formateadas con `DateTimeFormatter.ISO_LOCAL_DATE` (UTC).
- `generatedAt` se inserta literalmente desde el DTO; los tests deben fijarlo (clock injection).

`SimpleRegression` se recalcula aquí sobre `serie` para imprimir `slope=...`.

---

## Step 5.5 — Tests

**`TreatmentProgressTimelinePipelineIT`** — Testcontainers Mongo:
- Sembrar 3 articulaciones × 5 días con valores monótonos crecientes.
- Asserts:
  - `articulaciones.size() == 3`.
  - Cada `ArticulacionTimeline.serie.size() == 5`.
  - `inicial.fecha < actual.fecha`.
  - `deltaAbs > 0` y `deltaPct > 0` para datos crecientes.
- Caso `from = null`: incluye todo.
- Caso `from = day3`: excluye días 1 y 2.

**`HasNewSessionsServiceIT`** — Testcontainers Mongo:
- `desde = null, count > 0` → `hayNuevos = true`.
- `desde > maxReceivedAt` → `hayNuevos = false`.
- `desde < maxReceivedAt` → `hayNuevos = true` y `ultimoRegistro == maxReceivedAt`.

**`ProgressMarkdownGeneratorTest`** — snapshot:
- Construir `TreatmentTimelineResponse` con `generatedAt` fijo (`Instant.parse("2026-04-27T08:00:00Z")`).
- Generar markdown.
- Comparar contra fixture en `src/test/resources/markdown/progreso_paciente_12345678Z.md` (commiteado).
- Segundo test: mismo input → mismo output (idempotencia).

**TestSprite — `backend_test_plan`:**
- Contratos HTTP `/treatment-timeline` y `/has-new-sessions`.
- 401 sin `X-Internal-Key`.
- 400 con DNI mal formado.
- 404 con paciente sin sesiones en `/treatment-timeline`.

---

## Aceptación

- `curl -H 'X-Internal-Key:<key>' http://localhost:8081/analytics/patient/{dni}/treatment-timeline` → 200 con `TreatmentTimelineResponse` válido.
- `curl -H 'X-Internal-Key:<key>' http://localhost:8081/analytics/patient/{dni}/has-new-sessions?desde=<ISO>` → 200 coherente con datos sembrados.
- Snapshot del markdown reproducible sin diferencias entre ejecuciones consecutivas.
- TestSprite `backend_test_plan` cierra al 100 % (Self-Healing root §10.3 hasta 5 intentos).

---

## Archivo

### PLAN.md previo (cerrado 2026-04-20) — Phase 4 Advanced Analytics

> **Agent:** Sonnet (Doer) under Agent 1 (API + Data)
> **Domain:** `/data/`
> **Prerequisites:**
>  1. Phases 1, 2, and 3 are complete.
>  2. Read `data/CLAUDE.md` and `data/.claude/skills/springboot4-mongodb/SKILL.md` BEFORE starting.
> **Scope:** Implement the four Phase 4 items from `data/CLAUDE.md`: ROM time-series, cohort comparison, export endpoint (CSV/JSON), chart-ready format for the desktop ERP.

---

## Status summary

| # | Phase 4 item | Status | Target |
|---|---|---|---|
| 1 | Time-series ROM improvement over rehab period | **PENDING** | Step 1 |
| 2 | Cohort-comparison analytics (patient vs. same disability+level) | **PENDING** | Step 2 |
| 3 | Data export endpoint (CSV / JSON) | **PENDING** | Step 3 |
| 4 | Desktop ERP integration — chart-ready format | **PENDING** | Step 4 |

All paths relative to `/home/alaslibres/DAM/RehabiAPP/data/`.

---

## Step 0 — Dependencies

Add to `pom.xml` inside `<dependencies>`:

```xml
<!-- Apache Commons CSV: streaming writer for the export endpoint -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.11.0</version>
</dependency>

<!-- Apache Commons Math: linear regression for the ROM trend slope -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-math3</artifactId>
    <version>3.6.1</version>
</dependency>
```

Verification: `./mvnw dependency:tree -q | grep -E "commons-(csv|math3)"` prints two lines.

---

## Step 1 — ROM time-series endpoint

### 1.1 Contract

```
GET /analytics/patient/{dni}/rom-timeseries
Header:  X-Internal-Key: <API_INTERNAL_SHARED_KEY>
Query:
  bucket = day | week | month     (default: week)
  from   = ISO-8601 date           (optional, inclusive, default = now - 180 days)
  to     = ISO-8601 date           (optional, inclusive, default = now)
  gameId = string                  (optional filter)
200 OK:
{
  "patientDni": "12345678Z",
  "bucket": "week",
  "from":   "2025-10-27",
  "to":     "2026-04-24",
  "points": [
     { "date": "2025-W44", "romAvg": 68.2, "romMax": 75.0, "romMin": 62.0, "sampleSize": 5 },
     ...
  ],
  "trend": {
     "slopeDegreesPerBucket": 1.83,
     "intercept": 65.1,
     "r2": 0.74
  }
}
400 if bucket is invalid or from > to.
404 if no sessions exist in the window.
```

### 1.2 Aggregation pipeline — `application/pipeline/RomTimeSeriesPipeline.java`

Input: `dni`, `bucket`, `from`, `to`, optional `gameId`.
Stages:

1. `$match` — `{ patientDni: dni, sessionStart: { $gte: from, $lte: to } }` + optional `gameId`.
2. `$sort` — `{ sessionStart: 1 }` (preserve temporal order).
3. `$project` — compute bucket key:
   - `day`    → `{ $dateToString: { format: "%Y-%m-%d", date: "$sessionStart" } }`
   - `week`   → `{ $concat: [ { $toString: { $isoWeekYear: "$sessionStart" } }, "-W", { $toString: { $isoWeek: "$sessionStart" } } ] }`
   - `month`  → `{ $dateToString: { format: "%Y-%m", date: "$sessionStart" } }`
4. `$group` by bucket key:
   - `avg`, `min`, `max` of `movementMetrics.rangeOfMotionDegrees`
   - `count` for `sampleSize`
5. `$sort` by bucket key asc.

### 1.3 Trend computation (Java, after aggregation)

```java
// application/service/TrendCalculator.java
import org.apache.commons.math3.stat.regression.SimpleRegression;

public record Trend(double slopeDegreesPerBucket, double intercept, double r2) {}

public static Trend compute(List<Double> romAvgByBucketOrder) {
    SimpleRegression r = new SimpleRegression(true);
    for (int i = 0; i < romAvgByBucketOrder.size(); i++) {
        r.addData(i, romAvgByBucketOrder.get(i));
    }
    return new Trend(r.getSlope(), r.getIntercept(), r.getRSquare());
}
```

Use `x = bucket index` (0, 1, 2, …) not absolute date — keeps slope interpretable as "degrees gained per bucket".

### 1.4 DTO — `application/service/dto/RomTimeSeriesResponse.java`

```java
public record RomTimeSeriesPoint(String date, Double romAvg, Double romMax, Double romMin, Long sampleSize) {}

public record RomTimeSeriesResponse(
    String patientDni,
    String bucket,
    String from,
    String to,
    List<RomTimeSeriesPoint> points,
    Trend trend
) {}
```

### 1.5 Service — `application/service/RomTimeSeriesService.java`

Thin orchestrator: calls `RomTimeSeriesPipeline.run(...)`, feeds `romAvg` list to `TrendCalculator.compute`, assembles `RomTimeSeriesResponse`. Throws `NotFoundException` (new, simple runtime exception mapped to 404 in `GlobalExceptionHandler`) if `points` is empty.

### 1.6 Controller — extend `AnalyticsController`

```java
@GetMapping("/patient/{dni}/rom-timeseries")
public ResponseEntity<RomTimeSeriesResponse> romTimeSeries(
    @PathVariable @Pattern(regexp = DNI_REGEX, message = "DNI invalido") String dni,
    @RequestParam(defaultValue = "week") @Pattern(regexp = "day|week|month") String bucket,
    @RequestParam(required = false) String from,
    @RequestParam(required = false) String to,
    @RequestParam(required = false) String gameId) {
    return ResponseEntity.ok(romTimeSeriesService.compute(dni, bucket, from, to, gameId));
}
```

`DNI_REGEX` is the constant `"^[0-9]{8}[A-HJ-NP-TV-Z]$"`; promote it to a `public static final` in a new `util/ValidationPatterns.java` and reuse everywhere.

### 1.7 Add `NotFoundException` + handler

`application/service/NotFoundException.java`:
```java
public class NotFoundException extends RuntimeException {
    public NotFoundException(String msg) { super(msg); }
}
```

In `GlobalExceptionHandler`:
```java
@ExceptionHandler(NotFoundException.class)
public ResponseEntity<Map<String,Object>> handleNotFound(NotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
        "timestamp", Instant.now().toString(),
        "status", 404,
        "error", "not_found",
        "message", e.getMessage()));
}
```

---

## Step 2 — Cohort-comparison analytics

### 2.1 Contract

```
GET /analytics/cohort-compare/patient/{dni}
Header:  X-Internal-Key
Query:
  disability = string   (REQUIRED — CIE-10 / internal disability code)
  level      = 1..4     (REQUIRED — progressionLevel)
  from, to   = ISO date (optional, default last 180 days)
200 OK:
{
  "patientDni": "12345678Z",
  "disabilityCode": "M25.5",
  "progressionLevel": 2,
  "patient": {
     "totalSessions": 42,
     "averageScore": 712.4,
     "averageDuration": 290.5,
     "completionRate": 0.88,
     "averageRangeOfMotion": 78.3
  },
  "cohort": {
     "cohortSize": 17,      // distinct DNIs, excluding the target patient
     "totalSessions": 540,
     "averageScore": 680.1,
     "averageDuration": 301.2,
     "completionRate": 0.81,
     "averageRangeOfMotion": 72.4
  },
  "delta": {                // patient - cohort
     "averageScore":         +32.3,
     "averageDuration":      -10.7,
     "completionRate":       +0.07,
     "averageRangeOfMotion": +5.9
  },
  "percentile": {           // patient's percentile within cohort for each metric
     "averageScore":         0.72,
     "completionRate":       0.65,
     "averageRangeOfMotion": 0.80
  }
}
```

### 2.2 Aggregation strategy

Two parallel aggregations on `GameSession` (reusing indexes from Phases 2 and 3):

**Target patient** (`$match: { patientDni, disabilityCode, progressionLevel, sessionStart: [from,to] }` → `$group` metrics).

**Cohort** (`$match` same filters but `patientDni: { $ne: dni }` → `$group` by patient first to get per-patient aggregates → `$group` null to average those per-patient aggregates → also compute per-patient metric arrays for percentile calculation).

Two-stage grouping prevents heavier-playing patients from skewing the cohort average. "Average cohort" in the contract means "average of per-patient averages", not "average of all raw sessions".

Pseudocode for cohort:

```
match  { disabilityCode, progressionLevel, sessionStart in window, patientDni != target }
group _id=$patientDni,
  sessions=$sum 1, score=$avg, duration=$avg, completion=$avg(cond completed 1 0), rom=$avg "movementMetrics.rangeOfMotionDegrees"
facet {
  summary: [ group _id:null, cohortSize=$sum 1, avgScore=$avg score, avgDuration=$avg duration, avgCompletion=$avg completion, avgRom=$avg rom, totalSessions=$sum sessions ],
  rawScores: [ project score ],
  rawCompletions: [ project completion ],
  rawRoms: [ project rom ]
}
```

The Doer fetches the three `raw*` arrays, sorts them, finds the patient's rank via `Collections.binarySearch` (or a simple loop), and computes percentile = `rank / size`.

### 2.3 Files

- `application/pipeline/CohortComparisonPipeline.java` — runs both target + cohort aggregations, returns a tuple DTO.
- `application/service/CohortComparisonService.java` — computes deltas + percentiles.
- `application/service/dto/CohortComparisonResponse.java` — records matching §2.1 body.
- `presentation/AnalyticsController` — new `@GetMapping("/cohort-compare/patient/{dni}")`.

### 2.4 Edge cases

| Condition | Response |
|---|---|
| Target patient has 0 sessions in window | 404 `not_found` |
| Cohort size = 0 | 200 with `cohort=null`, `delta=null`, `percentile=null`, and a top-level `"note":"empty_cohort"` |
| Invalid level (not 1–4) | 400 `validation_failed` |

### 2.5 Privacy note

Per skill §Privacy, raw cohort DNIs MUST NOT leak. This endpoint exposes ONLY aggregates and rank-based percentiles — no per-patient rows. Verified: the response schema contains ZERO DNI values for cohort members.

---

## Step 3 — Export endpoint (CSV / JSON)

### 3.1 Contract

```
GET /analytics/export/patient/{dni}
Header:  X-Internal-Key
Query:
  format = csv | json   (REQUIRED)
  from, to (optional)   ISO-8601 dates
Response:
  200 OK, Content-Disposition: attachment; filename="patient_{dni}_{yyyymmdd}.csv|json"
  Body: streamed
  400 on unsupported format or bad DNI
  404 if empty
```

### 3.2 Streaming controller — `presentation/ExportController.java`

```java
package com.rehabiapp.data.presentation;

import com.rehabiapp.data.application.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analytics/export")
@Validated
public class ExportController {

    private final ExportService service;

    public ExportController(ExportService service) { this.service = service; }

    @GetMapping("/patient/{dni}")
    public void export(
            @PathVariable @Pattern(regexp = "^[0-9]{8}[A-HJ-NP-TV-Z]$", message = "DNI invalido") String dni,
            @RequestParam @Pattern(regexp = "csv|json", message = "format must be csv or json") String format,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletResponse response) throws Exception {

        String stamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String filename = "patient_" + dni + "_" + stamp + "." + format;
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        if ("csv".equals(format)) {
            response.setContentType("text/csv; charset=utf-8");
            service.streamCsv(dni, from, to, response.getWriter());
        } else {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            service.streamJson(dni, from, to, response.getWriter());
        }
    }
}
```

### 3.3 Service — `application/service/ExportService.java`

```java
@Service
public class ExportService {

    private static final String[] CSV_HEADERS = {
        "sessionId", "patientDni", "gameId", "disabilityCode", "progressionLevel",
        "sessionStart", "sessionEnd", "durationSeconds", "score",
        "repetitionsCompleted", "repetitionsTarget",
        "rangeOfMotionDegrees", "averageSpeed", "maxSpeed",
        "completed", "receivedAt"
    };

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    public ExportService(MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
    }

    public void streamCsv(String dni, String from, String to, PrintWriter out) throws IOException {
        Query q = buildQuery(dni, from, to);
        try (CloseableIterator<GameSession> it = mongoTemplate.stream(q, GameSession.class);
             CSVPrinter printer = new CSVPrinter(out,
                 CSVFormat.DEFAULT.builder().setHeader(CSV_HEADERS).build())) {
            boolean any = false;
            while (it.hasNext()) {
                any = true;
                GameSession g = it.next();
                printer.printRecord(g.getSessionId(), g.getPatientDni(), g.getGameId(),
                    g.getDisabilityCode(), g.getProgressionLevel(),
                    g.getSessionStart(), g.getSessionEnd(),
                    g.getDurationSeconds(), g.getScore(),
                    g.getRepetitionsCompleted(), g.getRepetitionsTarget(),
                    g.getMovementMetrics().rangeOfMotionDegrees(),
                    g.getMovementMetrics().averageSpeed(),
                    g.getMovementMetrics().maxSpeed(),
                    g.getCompleted(), g.getReceivedAt());
            }
            if (!any) throw new NotFoundException("no sessions for " + dni);
        }
    }

    public void streamJson(String dni, String from, String to, PrintWriter out) throws IOException {
        Query q = buildQuery(dni, from, to);
        try (CloseableIterator<GameSession> it = mongoTemplate.stream(q, GameSession.class);
             JsonGenerator gen = objectMapper.getFactory().createGenerator(out)) {
            gen.writeStartArray();
            boolean any = false;
            while (it.hasNext()) {
                any = true;
                gen.writeObject(it.next());
            }
            gen.writeEndArray();
            gen.flush();
            if (!any) throw new NotFoundException("no sessions for " + dni);
        }
    }

    private Query buildQuery(String dni, String from, String to) {
        Criteria c = Criteria.where("patientDni").is(dni);
        if (from != null) c = c.and("sessionStart").gte(Instant.parse(from));
        if (to != null)   c = c.lte(Instant.parse(to));
        return new Query(c).with(Sort.by(Sort.Direction.ASC, "sessionStart"));
    }
}
```

### 3.4 Memory note

Uses `MongoTemplate.stream(...)` — returns a `CloseableIterator` that pulls one document at a time. **Do NOT** load the full result set with `find(...)` — the export MUST work on 50k+ session exports without OOM.

---

## Step 4 — Chart-ready format for the desktop ERP

### 4.1 Rationale

The desktop ERP (JavaFX) renders practitioner dashboards. Today it queries PostgreSQL directly. Once the migration to `/api` is complete (per root `CLAUDE.md`), the desktop will consume analytics via `/api` which proxies to `/data`. Phase 4 defines the chart-ready contract that `/data` exposes; `/api` and `/desktop` wire up in their own PLAN.md files.

### 4.2 Chart format (generic, library-agnostic)

A "chart payload" is a list of named series with parallel x/y arrays. Works directly with JavaFX `LineChart`, `BarChart`, Chart.js, Recharts, etc.

```json
{
  "chartId": "rom_progress",
  "title": "Range of motion progress",
  "xAxisLabel": "Week",
  "yAxisLabel": "Degrees",
  "xType": "category",
  "yType": "number",
  "series": [
    { "name": "Patient",     "x": ["2025-W44","2025-W45",...], "y": [68.2, 69.5, ...] },
    { "name": "Cohort mean", "x": ["2025-W44","2025-W45",...], "y": [72.0, 72.3, ...] }
  ]
}
```

### 4.3 Charts exposed

| Endpoint | Chart | Source |
|---|---|---|
| `GET /analytics/charts/patient/{dni}/rom-progress` | ROM weekly with cohort mean overlay | Step 1 pipeline + Step 2 cohort aggregation |
| `GET /analytics/charts/patient/{dni}/score-by-game` | Bar chart: avg score per `gameId` last 90 days | `patient_progress` collection |
| `GET /analytics/charts/patient/{dni}/completion-trend` | Line chart: weekly completion rate | `patient_progress` collection |
| `GET /analytics/charts/global/level-comparison` | Bar chart: global avg score per progression level | `level_statistics` collection |

### 4.4 DTOs — `application/service/dto/chart/`

```java
public record ChartSeries(String name, List<String> x, List<Double> y) {}

public record ChartPayload(
    String chartId,
    String title,
    String xAxisLabel,
    String yAxisLabel,
    String xType,     // category | time | number
    String yType,     // number
    List<ChartSeries> series
) {}
```

### 4.5 Service — `application/service/ChartService.java`

One method per chart; each returns a `ChartPayload`. Implementations compose existing pipelines/services:
- `romProgress(dni)` → combines `RomTimeSeriesService.compute(dni, "week", ...)` with cohort means from `CohortComparisonService.cohortRomWeeklyMeans(dni)` (new helper). Align on the union of bucket keys; fill missing with `null`.
- `scoreByGame(dni)` → reads `patient_progress` where `patientDni=dni AND gameId!="*"` grouped by `gameId`, averaging `averageScore` across periods.
- `completionTrend(dni)` → reads weekly `patient_progress` rows, sorted by `period`, returns one series `"Patient"`.
- `levelComparison()` → reads all `level_statistics` rows, x = level number as string, y = `averageScore`.

### 4.6 Controller — `presentation/ChartController.java`

```java
@RestController
@RequestMapping("/analytics/charts")
@Validated
public class ChartController {

    private final ChartService svc;
    public ChartController(ChartService svc) { this.svc = svc; }

    @GetMapping("/patient/{dni}/rom-progress")
    public ChartPayload romProgress(@PathVariable @Pattern(regexp = DNI_REGEX) String dni) {
        return svc.romProgress(dni);
    }

    @GetMapping("/patient/{dni}/score-by-game")
    public ChartPayload scoreByGame(@PathVariable @Pattern(regexp = DNI_REGEX) String dni) {
        return svc.scoreByGame(dni);
    }

    @GetMapping("/patient/{dni}/completion-trend")
    public ChartPayload completionTrend(@PathVariable @Pattern(regexp = DNI_REGEX) String dni) {
        return svc.completionTrend(dni);
    }

    @GetMapping("/global/level-comparison")
    public ChartPayload levelComparison() {
        return svc.levelComparison();
    }
}
```

### 4.7 Publish contract for downstream consumers

Write a single-file OpenAPI snippet at `/data/docs/analytics-charts.openapi.yaml` documenting the four endpoints, the `ChartPayload` schema, and error responses. This file is the deliverable that Agent 0 and Agent 3 (desktop) consume to wire their proxy/consumer code. Do NOT auto-generate with Springdoc for this phase — a small, hand-written YAML is more stable as a contract document.

---

## Step 5 — Auth filter update

The existing `InternalAuthFilter` (from Phase 2, extended in Phase 3) already covers `/ingest/**`, `/internal/**`, `/analytics/**`. The new `/analytics/export/**` and `/analytics/charts/**` paths fall under `/analytics/**` — **no change needed**. Verify once with a `404`-on-`/health`-with-bad-key-is-fine + `401`-on-`/analytics/charts/...`-without-key smoke test.

---

## Step 6 — Tests (mandatory)

### 6.1 Pipeline & service tests

- `RomTimeSeriesPipelineTest` — seed 20 sessions across 4 weeks, assert 4 buckets, assert slope > 0 when ROM grows monotonically, slope ≈ 0 when flat.
- `CohortComparisonServiceTest` — seed 5 patients same disability+level + 5 patients DIFFERENT disability; assert cohort size = 4 (excludes target), correct delta sign, percentile in [0,1].
- `ExportServiceTest` — seed 100 sessions, stream CSV to a `StringWriter`, assert headers line + 100 data lines; stream JSON, parse back, assert array length 100.
- `ChartServiceTest` — for each of the 4 charts, assert `series` non-empty, x.size() == y.size(), `chartId` correct.

### 6.2 Controller slice tests (`@WebMvcTest`)

For each new endpoint:

| Test | Expected |
|---|---|
| Happy path | 200 + well-formed JSON matching contract §1.1 / §2.1 / §4.2 |
| Missing X-Internal-Key | 401 |
| Invalid DNI pattern | 400 `validation_failed` |
| No data for DNI | 404 `not_found` (except charts with global scope) |
| `format=xml` on export | 400 `validation_failed` |
| `bucket=year` on rom-timeseries | 400 |
| `level=9` on cohort-compare | 400 |

### 6.3 Integration

A single `@SpringBootTest` integration test that:
1. Ingests 30 sessions across 3 patients, 2 disabilities, 3 weeks (via the `/ingest` endpoint from Phase 2).
2. Calls `AnalyticsRefreshJob.refresh()` (from Phase 3) to build `patient_progress` and `level_statistics`.
3. Hits each Phase 4 endpoint with TestRestTemplate + `X-Internal-Key` header.
4. Asserts status codes and response shapes.

---

## Global verification

```bash
cd /home/alaslibres/DAM/RehabiAPP/data

./mvnw clean verify

docker run -d --rm --name mongo-dev -p 27017:27017 mongo:7.0
export API_INTERNAL_SHARED_KEY=local-dev-key
./mvnw spring-boot:run -q &
APP_PID=$!
sleep 25

K="X-Internal-Key: local-dev-key"

# 1. Seed multi-patient cohort
for dni in 12345678Z 23456789X 34567890J; do
  for i in 1 2 3; do
    curl -fsS -X POST http://localhost:8081/ingest/game-session -H "Content-Type: application/json" -H "$K" \
      -d "{\"sessionId\":\"s-$dni-$i\",\"patientDni\":\"$dni\",\"gameId\":\"reach\",\"progressionLevel\":2,\"disabilityCode\":\"M25.5\",\"sessionStart\":\"2026-04-$((10+i))T10:00:00Z\",\"sessionEnd\":\"2026-04-$((10+i))T10:05:00Z\",\"durationSeconds\":300,\"score\":$((500+i*50)),\"repetitionsCompleted\":18,\"repetitionsTarget\":20,\"movementMetrics\":{\"rangeOfMotionDegrees\":$((70+i*2)).0,\"averageSpeed\":0.4,\"maxSpeed\":0.9},\"completed\":true}"
  done
done

curl -fsS -X POST -H "$K" http://localhost:8081/internal/analytics/refresh

# 2. ROM time-series
curl -fsS -H "$K" "http://localhost:8081/analytics/patient/12345678Z/rom-timeseries?bucket=week" | \
  python3 -c "import json,sys; d=json.load(sys.stdin); assert d['points']; assert 'trend' in d; print('ROM-TS OK')"

# 3. Cohort compare
curl -fsS -H "$K" "http://localhost:8081/analytics/cohort-compare/patient/12345678Z?disability=M25.5&level=2" | \
  python3 -c "import json,sys; d=json.load(sys.stdin); assert d['cohort']['cohortSize']>=2; assert 'delta' in d; print('COHORT OK')"

# 4. Export CSV
curl -fsS -H "$K" "http://localhost:8081/analytics/export/patient/12345678Z?format=csv" -o /tmp/p.csv
head -1 /tmp/p.csv | grep -q sessionId && echo "CSV OK"

# 5. Export JSON
curl -fsS -H "$K" "http://localhost:8081/analytics/export/patient/12345678Z?format=json" | \
  python3 -c "import json,sys; a=json.load(sys.stdin); assert isinstance(a,list) and len(a)>=3; print('JSON OK')"

# 6. Charts
for ep in rom-progress score-by-game completion-trend; do
  curl -fsS -H "$K" "http://localhost:8081/analytics/charts/patient/12345678Z/$ep" | \
    python3 -c "import json,sys; d=json.load(sys.stdin); assert d['series']; print('CHART OK: $ep')"
done
curl -fsS -H "$K" "http://localhost:8081/analytics/charts/global/level-comparison" | \
  python3 -c "import json,sys; d=json.load(sys.stdin); assert d['series'][0]['x']; print('CHART OK: level-comparison')"

# 7. Negative cases
test "$(curl -s -o /dev/null -w '%{http_code}' -H "$K" "http://localhost:8081/analytics/export/patient/12345678Z?format=xml")" = "400" && echo "EXPORT BAD FORMAT OK"
test "$(curl -s -o /dev/null -w '%{http_code}' -H "$K" "http://localhost:8081/analytics/patient/99999999R/rom-timeseries")" = "404" && echo "EMPTY 404 OK"

kill $APP_PID
docker stop mongo-dev
```

All checks MUST pass.

---

## TestSprite delegation (mandatory)

Scope:
1. Regression of every previous phase endpoint.
2. All new HTTP contracts (§1.1, §2.1, §3.1, §4.3).
3. Stream correctness: CSV header count = field count, every line has the same column count, JSON is valid array.
4. Performance: 10k-session export completes in < 10 s and peak heap does not exceed 256 MiB (streaming guarantee).
5. Privacy: cohort response contains no DNI keys other than the `patientDni` top-level field.

Self-Healing loop on failure (max 5 attempts, root CLAUDE.md §10.3).

---

## Out of scope (do NOT implement in Phase 4)

- CSFLE wiring (separate track).
- Digital-signature / e-prescription integration.
- Anonymized pseudonymized-token layer (planned if endpoints ever go external).
- Actual desktop-side JavaFX chart rendering — that happens in `/desktop` once it migrates to `/api`.
- Any `/api`, `/desktop`, or K8s change.

---

## Checklist for the Doer (copy into commit body)

- [x] Step 0: `commons-csv` + `commons-math3` + `jackson-datatype-jsr310` added to `pom.xml`
- [x] Step 1.2: `RomTimeSeriesPipeline`
- [x] Step 1.3: `TrendCalculator` with `SimpleRegression`
- [x] Step 1.4: `RomTimeSeriesResponse` + `RomTimeSeriesPoint` DTOs
- [x] Step 1.5: `RomTimeSeriesService`
- [x] Step 1.6: `AnalyticsController` rom-timeseries + cohort-compare endpoints
- [x] Step 1.7: `NotFoundException` + 404 handler + `IllegalArgumentException` → 400
- [x] Step 2.3: `CohortComparisonPipeline` + `CohortComparisonService` + DTOs + controller
- [x] Step 2.4: empty cohort → note field, no patient data → 404
- [x] Step 3.2: `ExportController` streaming CSV/JSON
- [x] Step 3.3: `ExportService` using `MongoTemplate.stream` (Stream<T> in SB4)
- [x] Step 4.4: `ChartPayload` + `ChartSeries` DTOs
- [x] Step 4.5: `ChartService` with 4 chart methods
- [x] Step 4.6: `ChartController` with 4 endpoints
- [x] Step 4.7: `docs/analytics-charts.openapi.yaml` contract document
- [x] Step 5: auth filter covers `/analytics/**` — no change needed
- [x] Step 6: 12/20 tests verdes sin MongoDB; 20/20 con MongoDB (integration tests assumeTrue)
- [x] Global verification: 7/7 checks OK (ROM-TS, cohort, CSV, JSON, 4 charts, negative cases)
- [ ] TestSprite — skipped por developer
- [x] `data/CLAUDE.md` Phase 4 checklist fully marked `[x]`
