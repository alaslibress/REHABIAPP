# PLAN.md — Data iteration 2026-04-29

> **Branch:** stats-implementation
> **Author:** Agent 0/1 Thinker (Opus) — PRESCRIPTIVE.
> **Language:** code/comments en Spanish. Plan en English.
> **Scope:** Phases 5-7 del checklist `/data/CLAUDE.md` §6.

---

## 0. CONTEXTO OBLIGATORIO

1. `/CLAUDE.md` raiz §4.5, §4.6.
2. `/data/CLAUDE.md` §6.
3. `/data/.claude/skills/springboot4-mongodb/SKILL.md` (CSFLE, agregaciones server-side, anonimizacion).
4. `/api/PLAN.md` Phase 5, 8, 9 — consumidores de los nuevos endpoints.
5. Plantilla MD y formato deseado: ver §6.4 abajo.

---

## PHASE 5 — TREATMENT PROGRESS AGGREGATION

### 5.1 TreatmentMetricResolver

`src/main/java/com/rehabiapp/data/util/TreatmentMetricResolver.java`:

```java
@Component
@ConfigurationProperties(prefix = "rehabiapp.metric-map")
public class TreatmentMetricResolver {

    // Mapping codigo de tratamiento -> nombre del campo en MovementMetrics
    private Map<String, String> prefixToField = Map.of(
        "ROM-", "rangeOfMotionDegrees",
        "VEL-", "averageSpeed",
        "FUERZA-", "maxSpeed"
    );

    private Map<String, String> prefixToLabel = Map.of(
        "ROM-", "Rango de movimiento (grados)",
        "VEL-", "Velocidad media (m/s)",
        "FUERZA-", "Velocidad maxima (m/s)"
    );

    public String resolverCampo(String codTrat) {
        return prefixToField.entrySet().stream()
            .filter(e -> codTrat.startsWith(e.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse("rangeOfMotionDegrees");  // fallback
    }

    public String resolverLabel(String codTrat) {
        return prefixToLabel.entrySet().stream()
            .filter(e -> codTrat.startsWith(e.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse("Metrica");
    }

    // setters para @ConfigurationProperties
}
```

Anadir en `application.yml`:
```yaml
rehabiapp:
  metric-map:
    prefix-to-field:
      "[ROM-]": rangeOfMotionDegrees
      "[VEL-]": averageSpeed
      "[FUERZA-]": maxSpeed
    prefix-to-label:
      "[ROM-]": "Rango de movimiento (grados)"
      "[VEL-]": "Velocidad media (m/s)"
      "[FUERZA-]": "Velocidad maxima (m/s)"
```

### 5.2 TreatmentProgressPipeline

> NOTA: el modelo actual `GameSession` NO tiene `codTrat` ni `parteCuerpo`. La asociacion paciente-tratamiento esta en PostgreSQL (tabla `paciente_tratamiento`). Para enriquecer las sesiones se necesita uno de:
> A) que la API enriquezca el ingest con `codTrat` y `parteCuerpo` (preferible, requiere actualizar `GameSessionIngestionRequest`).
> B) que el data pipeline llame al API para resolver tratamiento (overhead alto, descartado).

**Decision:** Opcion A. Actualizar `GameSession` y `GameSessionIngestionRequest` para incluir:
- `codTrat: String` (opcional, null si Unity no manda)
- `parteCuerpo: String` (opcional)

Si vienen null, fallback: el paciente tiene N tratamientos visibles → asignar al ultimo activo (heuristica). Documentado.

#### Pipeline:

```java
@Component
public class TreatmentProgressPipeline {

    private final MongoTemplate mongoTemplate;
    private final TreatmentMetricResolver resolver;

    public List<TreatmentProgressDto> execute(String patientDni) {
        // Stage 1: $match paciente
        // Stage 2: $addFields day (date truncado), metricValue (metrica seleccionada por codTrat)
        // Stage 3: $group por (codTrat, parteCuerpo, day) -> avg metricValue
        // Stage 4: $group por (codTrat, parteCuerpo) -> push entradas[], min(day) baseline, max(day) current
        // Stage 5: $project con baseline, current, deltaPorcentaje calculado, entradas[] ordenadas

        AggregationOperation match = ctx -> new Document("$match",
                new Document("patientDni", patientDni)
                .append("codTrat", new Document("$ne", null)));

        AggregationOperation addFields = ctx -> new Document("$addFields", new Document()
                .append("day", new Document("$dateTrunc", new Document()
                        .append("date", "$sessionStart")
                        .append("unit", "day")))
                .append("metricValue", new Document("$switch", new Document()
                        .append("branches", List.of(
                            new Document("case", new Document("$gt", List.of(
                                new Document("$indexOfBytes", List.of("$codTrat", "ROM-")), -1)))
                                .append("then", "$movementMetrics.rangeOfMotionDegrees"),
                            new Document("case", new Document("$gt", List.of(
                                new Document("$indexOfBytes", List.of("$codTrat", "VEL-")), -1)))
                                .append("then", "$movementMetrics.averageSpeed"),
                            new Document("case", new Document("$gt", List.of(
                                new Document("$indexOfBytes", List.of("$codTrat", "FUERZA-")), -1)))
                                .append("then", "$movementMetrics.maxSpeed")
                        ))
                        .append("default", "$movementMetrics.rangeOfMotionDegrees")
                ))
        );

        AggregationOperation groupDay = ctx -> new Document("$group", new Document()
                .append("_id", new Document()
                    .append("codTrat", "$codTrat")
                    .append("parteCuerpo", "$parteCuerpo")
                    .append("day", "$day"))
                .append("valorPromedio", new Document("$avg", "$metricValue"))
                .append("patientToken", new Document("$first", "$patientToken"))
                .append("tratamientoNombre", new Document("$first", "$tratamientoNombre"))
        );

        AggregationOperation sortDay = ctx -> new Document("$sort",
                new Document("_id.day", 1));

        AggregationOperation groupTrat = ctx -> new Document("$group", new Document()
                .append("_id", new Document()
                    .append("codTrat", "$_id.codTrat")
                    .append("parteCuerpo", "$_id.parteCuerpo"))
                .append("patientToken", new Document("$first", "$patientToken"))
                .append("tratamientoNombre", new Document("$first", "$tratamientoNombre"))
                .append("entradas", new Document("$push",
                    new Document("fecha", "$_id.day").append("valor", "$valorPromedio")))
                .append("baselineFecha", new Document("$min", "$_id.day"))
                .append("currentFecha", new Document("$max", "$_id.day"))
                .append("baselineValor", new Document("$first", "$valorPromedio"))
                .append("currentValor", new Document("$last", "$valorPromedio"))
        );

        AggregationOperation project = ctx -> new Document("$project", new Document()
                .append("_id", 0)
                .append("patientToken", 1)
                .append("codTrat", "$_id.codTrat")
                .append("parteCuerpo", "$_id.parteCuerpo")
                .append("tratamientoNombre", 1)
                .append("baseline", new Document()
                    .append("fecha", "$baselineFecha")
                    .append("valor", "$baselineValor"))
                .append("current", new Document()
                    .append("fecha", "$currentFecha")
                    .append("valor", "$currentValor"))
                .append("entradas", 1)
                .append("deltaPorcentaje", new Document("$cond", List.of(
                    new Document("$eq", List.of("$baselineValor", 0)),
                    null,
                    new Document("$multiply", List.of(
                        new Document("$divide", List.of(
                            new Document("$subtract", List.of("$currentValor", "$baselineValor")),
                            "$baselineValor")),
                        100))
                )))
        );

        var pipeline = Aggregation.newAggregation(match, addFields, groupDay, sortDay, groupTrat, project);
        var results = mongoTemplate.aggregate(pipeline, "game_sessions", TreatmentProgressDto.class)
                .getMappedResults();

        // Anadir metricaNombre via resolver (post-process en Java porque depende de mapping)
        return results.stream()
            .map(r -> r.withMetricaNombre(resolver.resolverLabel(r.codTrat())))
            .toList();
    }
}
```

### 5.3 TreatmentProgressService

```java
@Service
public class TreatmentProgressService {
    private final TreatmentProgressPipeline pipeline;
    private final TreatmentProgressRepository repository;

    public List<TreatmentProgressDto> getOrCompute(String patientDni) {
        var fresh = pipeline.execute(patientDni);
        // Upsert por patientDni + codTrat + parteCuerpo
        repository.upsertAll(fresh);
        return fresh;
    }
}
```

`TreatmentProgressRepository` es un @Repository custom con MongoTemplate (no Spring Data por flexibilidad del upsert).

### 5.4 Endpoint en AnalyticsController

```java
@GetMapping("/patient/{dni}/treatment-progress")
public ResponseEntity<List<TreatmentProgressDto>> treatmentProgress(@PathVariable String dni) {
    return ResponseEntity.ok(treatmentProgressService.getOrCompute(dni));
}

@GetMapping("/patient/{dni}/last-session")
public ResponseEntity<LastSessionDto> lastSession(@PathVariable String dni) {
    return ResponseEntity.ok(analyticsService.getLastSession(dni));
}
```

`LastSessionDto`:
```java
public record LastSessionDto(
    String patientToken,
    String gameId,
    String codTrat,
    Instant sessionStart,
    Double score
) {}
```

### 5.5 DTO TreatmentProgressDto

```java
public record TreatmentProgressDto(
    String patientToken,
    String codTrat,
    String tratamientoNombre,
    String parteCuerpo,
    String metricaNombre,                // populated post-aggregation via resolver
    BaselineCurrent baseline,
    BaselineCurrent current,
    Double deltaPorcentaje,
    List<EntradaDto> entradas
) {
    public record BaselineCurrent(Instant fecha, Double valor) {}
    public record EntradaDto(Instant fecha, Double valor) {}

    public TreatmentProgressDto withMetricaNombre(String label) {
        return new TreatmentProgressDto(patientToken, codTrat, tratamientoNombre, parteCuerpo,
            label, baseline, current, deltaPorcentaje, entradas);
    }
}
```

### 5.6 Endpoint interno

`InternalController` en `internal/`:

```java
@RestController
@RequestMapping("/internal/patient")
public class InternalController {

    private final GameSessionRepository gameSessionRepository;

    @GetMapping("/{dni}/check-new-data")
    public ResponseEntity<CheckNewDataResponse> checkNewData(
            @PathVariable String dni,
            @RequestParam(required = false) Instant since
    ) {
        Instant ref = since != null ? since : Instant.EPOCH;
        long count = gameSessionRepository.countByPatientDniAndReceivedAtAfter(dni, ref);
        Instant last = gameSessionRepository.findTopByPatientDniOrderByReceivedAtDesc(dni)
            .map(GameSession::receivedAt).orElse(null);
        return ResponseEntity.ok(new CheckNewDataResponse(count > 0, last, (int) count));
    }
}

public record CheckNewDataResponse(boolean hasNewData, Instant lastSessionAt, int count) {}
```

Anadir metodos al repositorio:
```java
long countByPatientDniAndReceivedAtAfter(String patientDni, Instant since);
Optional<GameSession> findTopByPatientDniOrderByReceivedAtDesc(String patientDni);
```

### 5.8 Tests

`TreatmentProgressPipelineIT` con `@DataMongoTest` + Testcontainers MongoDB:
- Insertar 5 sesiones del mismo paciente, mismo tratamiento, fechas distintas.
- Ejecutar pipeline.
- Asertar baseline = primera sesion, current = ultima, entradas.size = 5, delta calculado.

---

## PHASE 6 — MARKDOWN GENERATION

### 6.1 MdGeneratorService

```java
@Service
public class MdGeneratorService {

    private final TreatmentProgressService progressService;
    private final GameSessionRepository sessionRepository;

    public String generar(String patientDni) {
        var token = pseudonymUtil.tokenize(patientDni);
        var progresos = progressService.getOrCompute(patientDni);
        var ultimasSesiones = sessionRepository.findTop20ByPatientDniOrderBySessionStartDesc(patientDni);

        StringBuilder sb = new StringBuilder();
        sb.append("# Progreso del paciente\n\n");
        sb.append("**Token anonimo:** `").append(token).append("`\n");
        sb.append("**Generado:** ").append(Instant.now()).append("\n\n");

        sb.append("## Resumen ejecutivo\n\n");
        if (progresos.isEmpty()) {
            sb.append("Sin datos de progreso disponibles.\n\n");
        } else {
            sb.append("| Tratamiento | Parte | Baseline | Actual | Delta |\n");
            sb.append("|---|---|---|---|---|\n");
            for (var p : progresos) {
                sb.append("| ").append(p.tratamientoNombre()).append(" | ")
                  .append(p.parteCuerpo()).append(" | ")
                  .append(format(p.baseline().valor())).append(" | ")
                  .append(format(p.current().valor())).append(" | ")
                  .append(formatDelta(p.deltaPorcentaje())).append(" |\n");
            }
        }

        sb.append("\n## Detalle por tratamiento\n\n");
        for (var p : progresos) {
            sb.append("### ").append(p.tratamientoNombre()).append(" (").append(p.parteCuerpo()).append(")\n\n");
            sb.append("- Metrica: ").append(p.metricaNombre()).append("\n");
            sb.append("- Baseline (").append(p.baseline().fecha()).append("): ").append(format(p.baseline().valor())).append("\n");
            sb.append("- Actual (").append(p.current().fecha()).append("): ").append(format(p.current().valor())).append("\n");
            sb.append("- Evolucion ").append(p.entradas().size()).append(" puntos\n\n");
        }

        sb.append("\n## Ultimas sesiones\n\n");
        sb.append("| Fecha | Juego | Score | Completada |\n");
        sb.append("|---|---|---|---|\n");
        for (var s : ultimasSesiones) {
            sb.append("| ").append(s.sessionStart()).append(" | ")
              .append(s.gameId()).append(" | ")
              .append(format(s.score())).append(" | ")
              .append(s.completed() ? "Si" : "No").append(" |\n");
        }

        sb.append("\n## Notas para analisis automatico (IA)\n\n");
        sb.append("Este documento se actualiza automaticamente con cada nueva sesion ingestada.\n");
        sb.append("Total de sesiones consideradas: ").append(ultimasSesiones.size()).append("\n");

        return sb.toString();
    }
    // helpers format / formatDelta omitidos
}
```

### 6.2 MarkdownService + Repository

```java
@Service
public class MarkdownService {
    private final MdGeneratorService generator;
    private final PatientMarkdownRepository repository;

    public String getOrGenerate(String dni) {
        var cached = repository.findByPatientDni(dni);
        if (cached.isPresent() && !esStale(cached.get())) {
            return cached.get().content();
        }
        return regenerar(dni);
    }

    public String regenerar(String dni) {
        String md = generator.generar(dni);
        repository.upsert(dni, md);
        return md;
    }

    private boolean esStale(PatientMarkdown md) {
        return md.updatedAt().isBefore(Instant.now().minus(15, ChronoUnit.MINUTES));
    }
}
```

`PatientMarkdownRepository` con upsert custom via MongoTemplate.

### 6.3 Endpoints AnalyticsController

```java
@GetMapping(value = "/patient/{dni}/markdown", produces = "text/markdown;charset=UTF-8")
public ResponseEntity<String> markdown(@PathVariable String dni) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
        .body(markdownService.getOrGenerate(dni));
}

@PostMapping("/patient/{dni}/markdown/regenerar")
public ResponseEntity<Void> regenerar(@PathVariable String dni) {
    markdownService.regenerar(dni);
    return ResponseEntity.accepted().build();
}
```

### 6.5 MarkdownRefreshScheduler

```java
@Component
public class MarkdownRefreshScheduler {
    private final MarkdownService markdownService;
    private final MongoTemplate mongoTemplate;

    @Scheduled(cron = "0 */15 * * * *", zone = "UTC")
    public void refresh() {
        // Encuentra hasta 50 pacientes cuya ultima sesion sea posterior a la generacion del MD
        var pipeline = Aggregation.newAggregation(
            // (pseudo) lookup desde game_sessions: maxReceivedAt por patientDni
            // join con patient_markdown.updatedAt
            // filter maxReceivedAt > updatedAt
            // limit 50
        );
        var pacientes = mongoTemplate.aggregate(pipeline, "game_sessions", PendienteDto.class)
            .getMappedResults();
        for (var p : pacientes) {
            try {
                markdownService.regenerar(p.patientDni());
            } catch (Exception e) {
                log.error("Fallo regenerando MD para {}", p.patientToken(), e);
            }
        }
    }
}
```

### 6.6 Tests

`MdGeneratorServiceTest` con golden file:
- Insertar dataset conocido (2 tratamientos, 5 sesiones).
- Generar MD.
- Comparar con `src/test/resources/golden/markdown-paciente-test.md` (whitespace-tolerant).

---

## PHASE 7 — OBSERVABILITY

### 7.1 Counters

```java
@Component
public class DataMetrics {
    private final Counter ingestCounter;
    private final Counter mdRegeneratedCounter;

    public DataMetrics(MeterRegistry registry) {
        this.ingestCounter = Counter.builder("rehabiapp.data.ingest.sessions")
            .description("Sesiones de juego ingestadas")
            .register(registry);
        this.mdRegeneratedCounter = Counter.builder("rehabiapp.data.markdown.regenerations")
            .description("Markdowns regenerados")
            .register(registry);
    }

    public void incIngest() { ingestCounter.increment(); }
    public void incMd() { mdRegeneratedCounter.increment(); }
}
```

Inyectar en `IngestService.ingestSession(...)` y `MarkdownService.regenerar(...)`.

### 7.2 Pipeline timer

```java
Timer.Sample sample = Timer.start(registry);
// ... ejecutar pipeline ...
sample.stop(Timer.builder("rehabiapp.data.pipeline.duration")
    .tag("pipeline", "treatment-progress")
    .register(registry));
```

### 7.3 Logs estructurados

En `MdGeneratorService.generar(...)`, al final:
```java
log.info("md_regenerated dni={} sessionCount={} durationMs={}",
    pseudonymUtil.tokenize(dni), sessions.size(), durationMs);
```

---

## ORDER OF EXECUTION

1. Phase 5.1 (resolver) + 5.2 (pipeline) — fundacion.
2. Phase 5.3-5.5 (servicios + DTOs + endpoints).
3. Phase 5.6-5.7 (endpoints internos para API).
4. Phase 5.8 (tests).
5. Phase 6.1-6.4 (MD generator + service + endpoints).
6. Phase 6.5 (scheduler).
7. Phase 6.6 (tests con golden files).
8. Phase 7 (metricas).

---

## DEPENDENCIES

- API debe enriquecer ingest con `codTrat` y `parteCuerpo`. Coordinar con `api/PLAN.md` Phase 8 (TelemetriaService).
- Mientras la API no envie `codTrat`, las sesiones existentes NO aparecen en treatment-progress (filter `codTrat != null`). Aceptable para MVP.

---

## NON-NEGOTIABLES

- Spring Boot 4 + Java 24. Records para todos los DTOs.
- Agregaciones server-side MongoDB. Nunca cargar todo en memoria.
- Anonimizacion: respuestas con patientToken, dni solo en path/storage (CSFLE).
- Logs estructurados JSON.
- Tests por cada nuevo pipeline y endpoint.

---

*End of plan.*
