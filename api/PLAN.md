# PLAN.md — API iteration 2026-04-29

> **Branch:** stats-implementation
> **Author:** Agent 0/1 Thinker (Opus) — PRESCRIPTIVE. Doer (Sonnet) MUST follow step by step.
> **Language:** All code/comments in Spanish (root `CLAUDE.md` §4.5). This plan in English.
> **Scope:** Phases 4-10 del checklist `/api/CLAUDE.md` §5.

---

## 0. CONTEXTO OBLIGATORIO

Antes de tocar codigo, leer:

1. `/CLAUDE.md` raiz — §4.5 estilo, §4.6 seguridad, §10 TestSprite.
2. `/api/CLAUDE.md` — §5 checklist Phase 4-10.
3. `/api/.claude/skills/springboot4-postgresql/SKILL.md` — TODAS las reglas (CSFLE no aplica aqui, AES-256-GCM si).
4. `/data/PLAN.md` Phase 5-6 — endpoints que el API consumira.
5. `/desktop/PLAN.md` Phase B-E — consumidores de los endpoints nuevos.
6. `/mobile/backend/PLAN.md` — consumidores del dashboard endpoint.

---

## PHASE 4 — H2 TEST COMPATIBILITY

### 4.1 Causa raiz

`V11__fix_protesis_boolean.sql` usa PL/pgSQL `DO $$ DECLARE...BEGIN...END $$`. H2 (test DB) NO soporta este bloque, lanza `JdbcSQLSyntaxErrorException` y aborta el contexto.

### 4.2 Fix prescriptivo (opcion A — recomendada)

**Crear migraciones H2-friendly bajo `src/test/resources/db/migration/` y configurar perfil test para usarlas en lugar de las de produccion.**

1. Crear `src/test/resources/db/migration/V11__fix_protesis_boolean.sql` con SQL ANSI-compatible:
```sql
-- V11 (test/H2): convertir protesis de INTEGER a BOOLEAN sin DO block.
-- H2 ejecuta cada sentencia secuencialmente; los ALTER son idempotentes
-- gracias a IF EXISTS / IF DEFINED.
ALTER TABLE paciente ALTER COLUMN protesis DROP DEFAULT;
ALTER TABLE paciente ALTER COLUMN protesis SET DATA TYPE BOOLEAN USING (protesis <> 0);
ALTER TABLE paciente ALTER COLUMN protesis SET DEFAULT FALSE;
```

2. Crear `src/test/resources/db/migration/V12__fix_protesis_default.sql` (vacio o NO-OP en H2):
```sql
-- V12 (test/H2): NO-OP. La logica de V12 prod solo aplica si V11 dejo
-- la columna como INTEGER en una BD legacy; en H2 la BD es siempre fresca.
SELECT 1;
```

3. Configurar `src/test/resources/application-test.yml`:
```yaml
spring:
  flyway:
    locations: classpath:db/migration
    # Spring Boot agrega ambos classpath de main y test al classpath unificado.
    # H2 ejecutara la version de test (mismo path) por orden de classpath:
    # los recursos de test/ tienen prioridad en runtime de tests.
```

> **Verificar empiricamente:** si Spring Boot mezcla las V11 de main y test (mismo nombre, mismo path), Maven Surefire usa `test/resources` con prioridad. Si NO se respeta la prioridad, alternativa B abajo.

### 4.3 Fix prescriptivo (opcion B — fallback)

Si la opcion A no funciona, usar perfiles Flyway distintos:

1. Mover scripts de produccion a `src/main/resources/db/migration/postgres/`.
2. Crear `src/main/resources/db/migration/h2/` con las versiones H2-friendly de V11 y V12.
3. En `application-local.yml`, `application-aws.yml`, `application-production.yml`:
```yaml
spring.flyway.locations: classpath:db/migration/postgres
```
4. En `application-test.yml`:
```yaml
spring.flyway.locations: classpath:db/migration/h2
```

### 4.4 Verificacion

```bash
cd api && ./mvnw clean test
```

Salida esperada: `BUILD SUCCESS`, `Tests run: N, Failures: 0, Errors: 0`.

---

## PHASE 5 — PATIENT PROGRESS INTEGRATION

### 5.1 DataPipelineClient (infrastructure)

`src/main/java/com/rehabiapp/api/infrastructure/client/DataPipelineClient.java`:

```java
@Component
public class DataPipelineClient {

    private final RestClient restClient;

    public DataPipelineClient(@Value("${rehabiapp.data.url:http://localhost:8081}") String baseUrl,
                              RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public CheckProgresoResponse checkNuevosDatos(String dni, Instant since) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/patient/{dni}/check-new-data")
                        .queryParam("since", since)
                        .build(dni))
                .retrieve()
                .body(CheckProgresoResponse.class);
    }

    public List<ProgresoTratamientoDto> obtenerProgreso(String dni) {
        return restClient.get()
                .uri("/analytics/patient/{dni}/treatment-progress", dni)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public String obtenerMarkdown(String dni) {
        return restClient.get()
                .uri("/analytics/patient/{dni}/markdown", dni)
                .accept(MediaType.parseMediaType("text/markdown"))
                .retrieve()
                .body(String.class);
    }

    public void regenerarMarkdown(String dni) {
        restClient.post()
                .uri("/analytics/patient/{dni}/markdown/regenerar", dni)
                .retrieve()
                .toBodilessEntity();
    }
}
```

### 5.2 ProgresoController (presentation)

```java
@RestController
@RequestMapping("/api/pacientes/{dni}/progreso")
public class ProgresoController {

    private final ProgresoService progresoService;
    // ...

    @GetMapping("/check")
    @PreAuthorize("hasAnyRole('SPECIALIST','NURSE')")
    public ResponseEntity<CheckProgresoResponse> check(
            @PathVariable String dni,
            @RequestParam(required = false) Instant since) {
        return ResponseEntity.ok(progresoService.checkNuevosDatos(dni, since));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SPECIALIST','NURSE')")
    public ResponseEntity<List<ProgresoTratamientoResponse>> obtener(@PathVariable String dni) {
        return ResponseEntity.ok(progresoService.obtenerProgreso(dni));
    }

    @GetMapping(value = "/markdown", produces = "text/markdown;charset=UTF-8")
    @PreAuthorize("hasAnyRole('SPECIALIST','NURSE')")
    public ResponseEntity<String> markdown(@PathVariable String dni) {
        return ResponseEntity.ok(progresoService.obtenerMarkdown(dni));
    }

    @PostMapping("/markdown/regenerar")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<Void> regenerar(@PathVariable String dni) {
        progresoService.regenerarMarkdown(dni);
        return ResponseEntity.accepted().build();
    }
}
```

### 5.3 ProgresoService (application)

- `checkNuevosDatos(dni, since)` → llama a `DataPipelineClient` y registra READ en audit_log.
- `obtenerProgreso(dni)` → registra READ + verifica que el paciente existe + maneja excepciones.
- `obtenerMarkdown(dni)` → llama al cliente, ACTUALIZA `paciente.archivo_progreso_md` con el contenido devuelto, devuelve string. Cache transparente.
- `regenerarMarkdown(dni)` → llama al cliente.

### 5.4 DTOs

```java
public record CheckProgresoResponse(boolean hasNewData, Instant lastSessionAt, int count) {}

public record ProgresoTratamientoResponse(
    String codTrat,
    String tratamientoNombre,
    String parteCuerpo,
    String metricaNombre,
    Double baselineValor,
    Instant baselineFecha,
    Double currentValor,
    Instant currentFecha,
    Double deltaPorcentaje,
    List<ProgresoEntradaResponse> entradas
) {}

public record ProgresoEntradaResponse(Instant fecha, Double valor) {}
```

### 5.5 Tests

`ProgresoControllerIT` con `MockRestServiceServer`:
- check con datos → 200 + `hasNewData=true`.
- check sin datos → 200 + `hasNewData=false`.
- obtener con paciente inexistente → 404.
- markdown como nurse → 200.
- regenerar como nurse → 403.
- regenerar como specialist → 202.

---

## PHASE 6 — TREATMENT-GAME ASSOCIATION

### 6.1 Migracion V13

`src/main/resources/db/migration/V13__videojuego_pdf_md.sql`:

```sql
-- V13: Tabla de videojuegos terapeuticos, asociacion con tratamientos,
-- almacenamiento de PDF en tratamiento, y cache de progreso MD en paciente.

CREATE TABLE videojuego (
    id_videojuego   BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(50)  NOT NULL UNIQUE,
    nombre          VARCHAR(200) NOT NULL,
    descripcion     TEXT,
    cod_dis         VARCHAR(50)  NOT NULL,
    parte_cuerpo    VARCHAR(100) NOT NULL,
    url_unity       VARCHAR(500),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_videojuego_dis
        FOREIGN KEY (cod_dis) REFERENCES discapacidad(cod_dis) ON DELETE RESTRICT
);
CREATE INDEX idx_videojuego_cod_dis ON videojuego(cod_dis);
CREATE INDEX idx_videojuego_activo ON videojuego(activo);

CREATE TABLE tratamiento_videojuego (
    cod_trat        VARCHAR(50) NOT NULL,
    id_videojuego   BIGINT      NOT NULL,
    fecha_vinculo   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cod_trat, id_videojuego),
    CONSTRAINT fk_tv_trat FOREIGN KEY (cod_trat) REFERENCES tratamiento(cod_trat) ON DELETE CASCADE,
    CONSTRAINT fk_tv_jue  FOREIGN KEY (id_videojuego) REFERENCES videojuego(id_videojuego) ON DELETE CASCADE
);

ALTER TABLE tratamiento
    ADD COLUMN archivo_pdf BYTEA,
    ADD COLUMN nombre_archivo_pdf VARCHAR(255),
    ADD COLUMN tamano_pdf_bytes BIGINT,
    ADD CONSTRAINT chk_tamano_pdf CHECK (tamano_pdf_bytes IS NULL OR tamano_pdf_bytes <= 10485760);

ALTER TABLE paciente
    ADD COLUMN archivo_progreso_md TEXT,
    ADD COLUMN progreso_md_actualizado_en TIMESTAMP;

-- Audit tables (Envers)
CREATE TABLE videojuego_audit (
    id_videojuego BIGINT NOT NULL,
    rev INTEGER NOT NULL,
    rev_type SMALLINT,
    codigo VARCHAR(50),
    nombre VARCHAR(200),
    descripcion TEXT,
    cod_dis VARCHAR(50),
    parte_cuerpo VARCHAR(100),
    url_unity VARCHAR(500),
    activo BOOLEAN,
    fecha_creacion TIMESTAMP,
    PRIMARY KEY (id_videojuego, rev),
    CONSTRAINT fk_videojuego_audit_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE TABLE tratamiento_videojuego_audit (
    cod_trat VARCHAR(50) NOT NULL,
    id_videojuego BIGINT NOT NULL,
    rev INTEGER NOT NULL,
    rev_type SMALLINT,
    fecha_vinculo TIMESTAMP,
    PRIMARY KEY (cod_trat, id_videojuego, rev),
    CONSTRAINT fk_tv_audit_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)
);
```

> Verificar nombre exacto de la tabla de revisiones (`revinfo` o `rehabi_revision`) en V3 / V9. Adaptar FK.

### 6.2-6.3 Entidades + repositorios

`Videojuego.java` (entity, `@Audited`):
```java
@Entity
@Audited
@Table(name = "videojuego")
public class Videojuego {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idVideojuego;

    @Column(unique = true, nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cod_dis", nullable = false)
    @NotAudited
    private Discapacidad discapacidad;

    @Column(name = "parte_cuerpo", nullable = false, length = 100)
    private String parteCuerpo;

    @Column(name = "url_unity", length = 500)
    private String urlUnity;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion = Instant.now();
    // getters/setters
}
```

`TratamientoVideojuego.java` con `@EmbeddedId TratamientoVideojuegoId` (codTrat + idVideojuego).

Repositorios Spring Data:
```java
public interface VideojuegoRepository extends JpaRepository<Videojuego, Long> {
    @EntityGraph(attributePaths = "discapacidad")
    List<Videojuego> findByActivoTrueOrderByNombreAsc();

    @EntityGraph(attributePaths = "discapacidad")
    List<Videojuego> findByDiscapacidadCodDisAndActivoTrueOrderByNombreAsc(String codDis);

    Optional<Videojuego> findByCodigo(String codigo);
}
```

### 6.4 DTOs y mappers MapStruct

```java
public record VideojuegoRequest(
    @NotBlank String codigo,
    @NotBlank String nombre,
    String descripcion,
    @NotBlank String codDis,
    @NotBlank String parteCuerpo,
    String urlUnity
) {}

public record VideojuegoResponse(
    Long idVideojuego,
    String codigo,
    String nombre,
    String descripcion,
    String codDis,
    String discapacidadNombre,
    String parteCuerpo,
    String urlUnity,
    boolean activo
) {}
```

```java
@Mapper(componentModel = "spring")
public interface VideojuegoMapper {
    @Mapping(source = "discapacidad.codDis", target = "codDis")
    @Mapping(source = "discapacidad.nombreDis", target = "discapacidadNombre")
    VideojuegoResponse toResponse(Videojuego entity);

    List<VideojuegoResponse> toResponseList(List<Videojuego> entities);
}
```

### 6.5 VideojuegoController

```java
@RestController
@RequestMapping("/api/videojuegos")
public class VideojuegoController {
    @GetMapping public Page<VideojuegoResponse> listar(Pageable pageable) {...}
    @GetMapping("/{id}") public VideojuegoResponse obtener(@PathVariable Long id) {...}
    @GetMapping("/discapacidad/{codDis}") public List<VideojuegoResponse> porDiscapacidad(@PathVariable String codDis) {...}

    @PostMapping
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<VideojuegoResponse> crear(@Valid @RequestBody VideojuegoRequest req) {...}

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<VideojuegoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody VideojuegoRequest req) {...}

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) { /* soft delete activo=false */ }
}
```

### 6.6 Asociacion en CatalogoController

```java
@GetMapping("/api/tratamientos/{cod}/videojuegos")
public List<VideojuegoResponse> juegosDeTratamiento(@PathVariable String cod) {...}

@PostMapping("/api/tratamientos/{cod}/videojuegos/{id}")
@PreAuthorize("hasRole('SPECIALIST')")
public ResponseEntity<Void> vincular(@PathVariable String cod, @PathVariable Long id) {...}

@DeleteMapping("/api/tratamientos/{cod}/videojuegos/{id}")
@PreAuthorize("hasRole('SPECIALIST')")
public ResponseEntity<Void> desvincular(@PathVariable String cod, @PathVariable Long id) {...}
```

---

## PHASE 7 — TREATMENT PDF

### 7.1 Endpoints

```java
@PostMapping(value = "/api/tratamientos/{cod}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@PreAuthorize("hasRole('SPECIALIST')")
public ResponseEntity<Void> subirPdf(
        @PathVariable String cod,
        @RequestPart("file") MultipartFile file
) throws IOException {
    if (file.getSize() > 10 * 1024 * 1024) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
    }
    byte[] bytes = file.getBytes();
    if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F') {
        throw new ValidacionException("El archivo no es un PDF valido.");
    }
    tratamientoService.guardarPdf(cod, bytes, file.getOriginalFilename(), file.getSize());
    return ResponseEntity.created(URI.create("/api/tratamientos/" + cod + "/pdf")).build();
}

@GetMapping(value = "/api/tratamientos/{cod}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
public ResponseEntity<byte[]> descargarPdf(@PathVariable String cod) {
    var pdf = tratamientoService.obtenerPdf(cod);
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + pdf.nombre() + "\"")
            .body(pdf.bytes());
}

@GetMapping("/api/tratamientos/{cod}/pdf/metadatos")
public ResponseEntity<PdfMetadatosResponse> metadatos(@PathVariable String cod) {...}

@DeleteMapping("/api/tratamientos/{cod}/pdf")
@PreAuthorize("hasRole('SPECIALIST')")
public ResponseEntity<Void> eliminarPdf(@PathVariable String cod) {...}
```

### 7.2 TratamientoService extension

```java
public void guardarPdf(String cod, byte[] bytes, String filename, long size) {
    Tratamiento t = repo.findByCodTrat(cod).orElseThrow(() -> new RecursoNoEncontradoException(...));
    t.setArchivoPdf(bytes);
    t.setNombreArchivoPdf(filename);
    t.setTamanoPdfBytes(size);
    repo.save(t);
    auditService.registrar(AccionAuditoria.UPDATE, "tratamiento", cod,
        "PDF subido: " + filename + " (" + size + " bytes)");
}
```

### 7.3 Tests

`PdfControllerIT`:
- Subir PDF 5MB valido → 201.
- Subir 11MB → 413.
- Subir .docx → 400 con mensaje claro.
- Descargar tras subir → bytes identicos.
- Metadatos tras subir → nombre + tamano correctos.
- Eliminar como nurse → 403.

---

## PHASE 8 — GAME TELEMETRY ROUTING

### 8.1-8.2 TelemetriaController + Service

```java
@RestController
@RequestMapping("/api/telemetria")
public class TelemetriaController {

    private final TelemetriaService telemetriaService;

    @PostMapping("/sesion-juego")
    @PreAuthorize("hasAuthority('SCOPE_GAMES_TELEMETRY') or hasRole('SPECIALIST')")
    public ResponseEntity<Map<String, String>> ingestar(@Valid @RequestBody TelemetriaSesionRequest req) {
        var resultado = telemetriaService.ingestar(req);
        return ResponseEntity.accepted().body(Map.of("dataId", resultado.dataId()));
    }
}
```

`TelemetriaService.ingestar(req)`:
1. Validar paciente existe y esta activo.
2. Si `req.disabilityId()` es null, inferir desde `paciente_discapacidad` (la mas reciente del paciente).
3. Construir payload para `/data` POST `/ingest/game-session`.
4. Llamar a `DataPipelineClient.ingestar(payload)`.
5. Publicar `RegenerarMdEvent` (Spring `ApplicationEvent`).
6. Devolver dataId al cliente.

### 8.3 Scope JWT

Anadir scope `GAMES_TELEMETRY` al JwtService — emitido cuando un servicio de juegos se autentica con credenciales especiales (NO un usuario humano). Por simplicidad inicial, aceptar tambien role SPECIALIST.

### 8.4 EventListener

```java
@Component
public class RegenerarMdListener {
    private final DataPipelineClient client;

    @EventListener
    @Async
    public void handle(RegenerarMdEvent event) {
        try {
            client.regenerarMarkdown(event.dniPac());
        } catch (Exception e) {
            log.error("Fallo al regenerar MD para paciente {}", event.dniPac(), e);
        }
    }
}
```

Habilitar `@EnableAsync` en `ApiApplication` o en un `@Configuration`.

### 8.5 Tests

MockMvc + MockRestServiceServer:
- POST sesion valida → 202 + dataId.
- POST sin disabilityId → infiere desde BD → llamada exitosa a /data.
- POST con paciente inexistente → 404.
- POST con paciente inactivo → 403.

---

## PHASE 9 — MOBILE DASHBOARD

### 9.1-9.2 DashboardController + DTO

```java
@RestController
@RequestMapping("/api/pacientes/{dni}/dashboard")
public class DashboardController {
    @GetMapping
    @PreAuthorize("hasAnyRole('SPECIALIST','NURSE','PATIENT')")
    public ResponseEntity<DashboardResponse> obtener(@PathVariable String dni) {
        return ResponseEntity.ok(dashboardService.obtener(dni));
    }
}

public record DashboardResponse(
    PacienteResumenDto paciente,
    List<DiscapacidadActivaDto> discapacidadesActivas,
    List<TratamientoVisibleDto> tratamientosVisibles,
    List<JuegoDesbloqueadoDto> juegosDesbloqueados,
    UltimaSesionDto ultimaSesionJuego,
    ProximaCitaDto proximaCita
) {}
```

### 9.3 Service

`DashboardService.obtener(dni)`:
- Carga paciente.
- Carga `paciente_discapacidad` con FETCH JOIN a `discapacidad` y `nivel_progresion`.
- Para cada discapacidad activa, busca tratamientos visibles (`paciente_tratamiento` con `visible=true`).
- Para cada tratamiento, busca juegos asociados → marca como desbloqueado si el nivel del paciente >= nivel del tratamiento.
- Carga ultima sesion via `DataPipelineClient.ultimaSesion(dni)` (NUEVO endpoint en `/data`, ver `data/PLAN.md` Phase 5.7).
- Carga proxima cita via `CitaRepository.findProximaByPaciente(dni)`.
- Audit READ.

### 9.4 Tests

- Dashboard con paciente sin asignaciones → respuesta con listas vacias pero NO 404.
- Dashboard con paciente completo → estructura correcta y juegos correctamente filtrados por nivel.

---

## PHASE 10 — DOCS + RATE LIMIT

### 10.1 springdoc-openapi

Pom.xml (version 2.8.13 — Spring Boot 4 compatible; ver Phase 11 §11.3):
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.13</version>
</dependency>
<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-annotations-jakarta</artifactId>
    <version>2.2.30</version>
</dependency>
```

Application.yml:
```yaml
springdoc:
  api-docs.path: /v3/api-docs
  swagger-ui.path: /swagger-ui.html
  swagger-ui.operationsSorter: method
  packages-to-scan: com.rehabiapp.api.presentation
  paths-to-exclude: /internal/**
```

### 10.2 Anotaciones

Cada controller con `@Tag(name = "...", description = "...")`. Cada metodo con `@Operation(summary = "...")`. DTOs con `@Schema(description = "...", example = "...")`.

### 10.3 Bucket4j

Pom.xml (version 8.14.0 — ver Phase 11 §11.3):
```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j_jdk17-core</artifactId>
    <version>8.14.0</version>
</dependency>
```

Crear `RateLimitFilter` (Spring Filter) que:
- Identifica request por IP en `/api/auth/login` o por JWT `sub` en el resto.
- Mantiene un `ConcurrentHashMap<String, Bucket>` con buckets de Bucket4j.
- Limites por path:
  - `/api/auth/login`: 10/min/IP.
  - `/api/telemetria/**`: 60/min/JWT.
  - Resto: 300/min/JWT.
- Si bucket vacio, devuelve 429 con header `Retry-After`.

### 10.5 Limite de payload

En `application.yml`:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 12MB
  http:
    max-http-request-header-size: 16KB
```

Filtro custom `PayloadSizeFilter` para JSON: si `Content-Length > 1MB` y `Content-Type=application/json` → 413.

### 10.6 Tests rate limit

```java
@Test
void login_11Veces_undecimaDevuelve429() {
    for (int i = 0; i < 10; i++) {
        mvc.perform(post("/api/auth/login").content(...)).andExpect(status().isUnauthorized());
    }
    mvc.perform(post("/api/auth/login").content(...)).andExpect(status().isTooManyRequests());
}
```

---

## PHASE 11 — BUILD DEPENDENCY RESOLUTION FIX (BLOCKING — DO FIRST)

> **Status:** URGENT. Build fails with **79 compile errors** in `api` module on `stats-implementation` branch (build run 2026-05-04 23:38, javac 26, Spring Boot 4.0.5 parent).
> **Owner:** Agent 1 Doer (Sonnet).
> **Scope:** Pure dependency / classpath plumbing in `api/pom.xml` + IDE re-import. NO source code rewrite required. NO new features.
> **Pass condition:** `cd api && ./mvnw clean compile` returns BUILD SUCCESS with 0 errors. Then `./mvnw test` still passes the suite from Phase 4.

### 11.1 Symptom inventory (verbatim from build log)

The 79 errors fall into exactly **two missing-package families**. The Doer MUST NOT touch any source file: the source already imports the right classes — only the dependencies fail to resolve them.

**Family A — springdoc / OpenAPI annotations missing (74 errors)**

Missing packages on classpath:
- `io.swagger.v3.oas.annotations`
- `io.swagger.v3.oas.annotations.tags`
- `io.swagger.v3.oas.annotations.media`

Missing symbols and the files that reference them:
- `class Tag` and `class Operation` in:
  - `com.rehabiapp.api.presentation.controller.DashboardController`
  - `com.rehabiapp.api.presentation.controller.TratamientoPdfController`
  - `com.rehabiapp.api.presentation.controller.TelemetriaController`
  - `com.rehabiapp.api.presentation.controller.ProgresoController`
  - `com.rehabiapp.api.presentation.controller.VideojuegoController`
- `class Schema` in:
  - `com.rehabiapp.api.application.dto.DashboardResponse`
  - `com.rehabiapp.api.application.dto.CheckProgresoResponse`
  - `com.rehabiapp.api.application.dto.TelemetriaSesionRequest`
  - `com.rehabiapp.api.application.dto.PdfMetadatosResponse`
  - `com.rehabiapp.api.application.dto.VideojuegoRequest`

**Family B — Bucket4j missing (5 errors)**

Missing package: `io.github.bucket4j`.
Missing symbol: `class Bucket` in `com.rehabiapp.api.infrastructure.ratelimit.RateLimitFilter`.

### 11.2 Root cause hypothesis

Both families share the same shape: the artifact is declared in `pom.xml` (lines 118-132 of current `api/pom.xml`) but its classes are not visible to javac. Two real causes are possible and the Doer MUST handle both because either alone explains the failure:

1. **Version incompatibility with Spring Boot 4.0.5 BOM.**
   - `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0` (currently in pom) was built for Spring Boot 3.3 / Spring Framework 6.1 / Jakarta EE 10. Spring Boot 4.0.5 ships Spring Framework 7 + Jakarta EE 11 and may BOM-override or exclude transitive Swagger annotation jars, leaving `io.swagger.v3.oas.annotations.*` off the compile classpath.
   - For Spring Boot 4 the supported springdoc line is **2.8.x or newer**. Use the latest GA in the 2.8 series (see step 11.3).
   - `com.bucket4j:bucket4j_jdk17-core:8.14.0` is the correct coordinate but its JAR is published as a Multi-Release JAR; under unusual classpath ordering (e.g., when the dep is overridden by a transitive BOM-managed older version) the `io.github.bucket4j` package can be missing. We mitigate by **pinning the version explicitly outside the BOM scope and forcing dependency resolution** (no exclusions needed; just guarantee the jar lands on the compile classpath).

2. **Stale IntelliJ Maven import / Maven local cache.**
   - The build log shows the IDE-driven incremental compile (“Executing pre-compile tasks…”, “Updating dependency information…”). After any pom edit the IDE caches the previous classpath. Until a forced **Reload All Maven Projects** + `clean install`, the new deps are invisible to javac.

The fix below is idempotent and addresses both causes.

### 11.3 Prescriptive pom.xml edits (exact diffs)

> File: `api/pom.xml`. Doer applies these EXACT edits, no rewording.

**Edit A — replace the springdoc block (currently lines 118-124).**

OLD:
```xml
        <!-- ======================== DOCUMENTACION OPENAPI ======================== -->
        <!-- springdoc-openapi — genera /v3/api-docs y /swagger-ui.html automaticamente -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.6.0</version>
        </dependency>
```

NEW:
```xml
        <!-- ======================== DOCUMENTACION OPENAPI ======================== -->
        <!--
            springdoc-openapi 2.8.x es la primera linea compatible con Spring Boot 4.0.x
            (Spring Framework 7, Jakarta EE 11). 2.6.0 fue construido contra Spring 6 y
            su starter no expone io.swagger.v3.oas.annotations.* en el classpath bajo
            el BOM de Spring Boot 4.
        -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.8.13</version>
        </dependency>

        <!--
            Cinturon de seguridad: declaramos explicitamente swagger-annotations-jakarta
            para garantizar que io.swagger.v3.oas.annotations.{Operation,tags.Tag,
            media.Schema} estan SIEMPRE en el compile classpath aunque el BOM de Spring
            Boot 4 modifique el grafo transitivo del starter en el futuro.
        -->
        <dependency>
            <groupId>io.swagger.core.v3</groupId>
            <artifactId>swagger-annotations-jakarta</artifactId>
            <version>2.2.30</version>
        </dependency>
```

**Edit B — replace the Bucket4j block (currently lines 126-132).**

OLD:
```xml
        <!-- ======================== RATE LIMIT ======================== -->
        <!-- Bucket4j — token bucket en memoria para rate limiting por IP/JWT -->
        <dependency>
            <groupId>com.bucket4j</groupId>
            <artifactId>bucket4j_jdk17-core</artifactId>
            <version>8.14.0</version>
        </dependency>
```

NEW:
```xml
        <!-- ======================== RATE LIMIT ======================== -->
        <!--
            Bucket4j 8.x publica el modulo principal como `bucket4j_jdk17-core` (paquete
            `io.github.bucket4j`). Pinned a 8.14.0 GA. Si tras Reload Maven el paquete
            `io.github.bucket4j` siguiera ausente, sustituir por `bucket4j-core:8.7.0`
            (linea legacy publicada en groupId `com.github.vladimir-bukhtoyarov`) — pero
            primero agotar 11.4 paso 5 (purgar cache local de Maven).
        -->
        <dependency>
            <groupId>com.bucket4j</groupId>
            <artifactId>bucket4j_jdk17-core</artifactId>
            <version>8.14.0</version>
        </dependency>
```

> **DO NOT** add Maven `<exclusions>`. Both deps must resolve cleanly with their default transitive graph.

### 11.4 Mandatory post-edit procedure

Execute every step in this exact order. Do NOT skip even if a step looks redundant — they cover the two root causes from §11.2 jointly.

1. **Save `pom.xml`** with the two edits from §11.3.

2. **Purge previous classes from the build dir** (avoids stale `.class` shadowing the new deps):
   ```bash
   cd /home/alaslibres/DAM/RehabiAPP/api
   ./mvnw clean
   ```
   Confirm the directory `api/target/classes/` is gone.

3. **Force Maven to refresh the dependency graph** (downloads new artifacts, ignores cached `_remote.repositories` entries):
   ```bash
   cd /home/alaslibres/DAM/RehabiAPP/api
   ./mvnw -U dependency:purge-local-repository -DmanualInclude="org.springdoc:springdoc-openapi-starter-webmvc-ui,io.swagger.core.v3:swagger-annotations-jakarta,com.bucket4j:bucket4j_jdk17-core" -DreResolve=true -DactTransitively=false
   ```
   Then:
   ```bash
   ./mvnw -U dependency:resolve
   ```

4. **Verify the dependency tree shows the three artifacts** (and their packages):
   ```bash
   ./mvnw dependency:tree -Dincludes=org.springdoc:*,io.swagger.core.v3:*,com.bucket4j:*
   ```
   Expected output MUST contain (versions as above):
   ```
   [INFO] +- org.springdoc:springdoc-openapi-starter-webmvc-ui:jar:2.8.13:compile
   [INFO] +- io.swagger.core.v3:swagger-annotations-jakarta:jar:2.2.30:compile
   [INFO] +- com.bucket4j:bucket4j_jdk17-core:jar:8.14.0:compile
   ```
   If `swagger-annotations-jakarta` is missing OR resolves to a different version, STOP and report — likely a corporate proxy issue, not a code issue.

5. **Compile from the command line first** (NOT from IntelliJ):
   ```bash
   ./mvnw -DskipTests clean compile
   ```
   This must end with `BUILD SUCCESS`. If 79 errors persist:
   - Re-read §11.3 — confirm both `<dependency>` blocks are EXACTLY as specified (no typos in groupId/artifactId).
   - Rerun §11.4 step 3.
   - As last resort, switch the bucket4j coordinate per the comment in Edit B and rerun from step 2.

6. **Reload IntelliJ Maven** (only after step 5 is green):
   - Right-click `api/pom.xml` → **Maven** → **Reload Project**.
   - File → **Invalidate Caches… → Invalidate and Restart**.
   - After restart, build from inside the IDE — must end with 0 errors.

7. **Run the test suite** to ensure no regression:
   ```bash
   cd /home/alaslibres/DAM/RehabiAPP/api
   ./mvnw test
   ```
   Phase 4 already left this green. If now red, the regression is caused by Phase 11 — investigate before continuing.

### 11.5 Files the Doer is FORBIDDEN to touch in Phase 11

Phase 11 is dependency-only. The Doer MUST NOT modify any of:

- `src/main/java/**` (especially the controllers and DTOs listed in §11.1).
- `src/test/**`.
- `application*.yml`.
- Flyway migrations.
- Any other `.xml` except `pom.xml`.

If a source file genuinely needs an edit to compile, that means §11.3 was applied incorrectly — STOP and re-read §11.3.

### 11.6 TestSprite gate (mandatory per root §10)

After §11.4 step 7 is green, delegate verification to TestSprite MCP:

1. TestSprite re-runs `./mvnw test` in its sandbox.
2. TestSprite re-runs the integration tests previously written for Phases 5-10 (`ProgresoControllerIT`, `VideojuegoControllerIT`, `TratamientoPdfControllerIT`, `TelemetriaControllerIT`, `DashboardControllerIT`, rate-limit `AuthLoginRateLimitIT`).
3. 100% pass → mark Phase 11 `[x]` in `/api/CLAUDE.md` §5 (add a new line `### Phase 11 — Build dependency resolution fix` with one item `- [x] 11.1 Resolved 79 compile errors caused by stale springdoc 2.6.0 + bucket4j classpath`).
4. Any failure → enter Self-Healing Protocol (root §10.3). NEVER stop with a red bar.

### 11.7 Sanity check before reporting done

The Doer reports Phase 11 complete ONLY when all of the following are simultaneously true:

- [ ] `./mvnw clean compile` — 0 errors.
- [ ] `./mvnw test` — 0 failures, 0 errors.
- [ ] `./mvnw spring-boot:run` boots the app and `curl http://localhost:8080/swagger-ui.html` returns HTTP 200 (proves springdoc 2.8.13 wired up correctly under Spring Boot 4).
- [ ] `curl -i http://localhost:8080/api/auth/login` (11 times in <60s) — last request returns HTTP 429 (proves bucket4j is on the classpath at runtime, not just compile time).
- [ ] TestSprite returned 100% on the suites in §11.6.
- [ ] `/api/CLAUDE.md` §5 updated with the Phase 11 checklist line marked `[x]`.

---

## ORDER OF EXECUTION

0. **Phase 11 (BUILD FIX)** — BLOCKING. Until compile is green nothing else can be tested or merged.
1. Phase 4 (H2) — DESBLOQUEA TODO. Sin tests verdes no se puede iterar con confianza.
2. Phase 6 (V13 schema) — fundacion para PDF (Phase 7), juegos (Phase 6), MD cache (Phase 5).
3. Phase 7 (PDF) — independiente.
4. Phase 5 (Progress) — depende de `/data` Phase 5 ya implementado.
5. Phase 8 (Telemetria) — depende de `/data` ingest existente (Phase 2 data).
6. Phase 9 (Dashboard) — agrega; depende de 5+6+8.
7. Phase 10 (Docs + rate limit) — al final.

---

## NON-NEGOTIABLES

- Spring Boot 4.0.5 + Java 24. NO downgrade.
- Records Java 24 para DTOs. Sealed interfaces si aplica.
- Sin comentarios en ingles. Sin emojis.
- Sin entidades retornadas por controllers.
- Cada nuevo endpoint con test integration.
- Cada nueva tabla con `@Audited` + audit table en V13.
- Audit log READ obligatorio en `/api/pacientes/**` segun §4.6 raiz.
- TestSprite 100% antes de marcar checklist `[x]`.

---

*End of plan.*
