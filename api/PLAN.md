# PLAN.md — API Phase 8: Patient progress + treatment PDF persistence

> **Date:** 2026-04-27
> **Agent:** 1 Thinker (Opus) → Doer (Sonnet)
> **Domain:** `/api/`
> **Prerequisitos:**
>  1. Leer `/api/CLAUDE.md` y `/api/.claude/skills/springboot4-postgresql/SKILL.md`.
>  2. `/data` Phase 5 debe estar entregada antes de cerrar 8.5/8.6 — `ProgresoService` consume `treatment-timeline`, `has-new-sessions` y el generador de markdown.
> **Scope:** persistir PDF por tratamiento en Postgres (BYTEA), exponer descarga binaria al mobile, materializar progreso clinico (`paciente.progreso_md`) consumiendo /data, y cerrar el ciclo de polling/sincronizacion del escritorio.

---

## Status summary

| # | Phase 8 item | Status |
|---|---|---|
| 8.1 | V14 migration `tratamiento_documento` | PENDING |
| 8.2 | V15 migration `paciente.progreso_md` + `ultima_sync_progreso` | PENDING |
| 8.3 | Multipart config `application.yml` (max 10MB) | PENDING |
| 8.4 | Domain layer (`TratamientoDocumento`, DTOs, ampliar `Paciente`) | PENDING |
| 8.5 | Endpoints REST (catalogo + pacientes/progreso) | PENDING |
| 8.6 | Service layer (`TratamientoDocumentoService`, `ProgresoService` con WebClient) | PENDING |
| 8.7 | Tests (`@WebMvcTest`, `@SpringBootTest` + Testcontainers) + TestSprite | PENDING |

---

## Step 8.1 — V14 migration `tratamiento_documento`

**File:** `api/src/main/resources/db/migration/V14__tratamiento_documento.sql`

```sql
-- V14: tabla de almacenamiento de PDF de tratamientos.
-- BYTEA en Postgres (decision RGPD: simplifica encriptacion en reposo y
-- evita salida del dato fuera del perimetro). UNIQUE en cod_trat -> upsert.
CREATE TABLE tratamiento_documento (
    id BIGSERIAL PRIMARY KEY,
    cod_trat VARCHAR(32) NOT NULL UNIQUE
        REFERENCES tratamiento(cod_trat) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(80) NOT NULL DEFAULT 'application/pdf',
    contenido BYTEA NOT NULL,
    sha256 CHAR(64) NOT NULL,
    fecha_carga TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dni_san_carga VARCHAR(20) NOT NULL
        REFERENCES sanitario(dni_san) ON DELETE RESTRICT
);
CREATE INDEX idx_tratamiento_documento_cod_trat ON tratamiento_documento(cod_trat);
CREATE INDEX idx_tratamiento_documento_fecha_carga ON tratamiento_documento(fecha_carga DESC);

-- Tabla de auditoria Envers. La columna `contenido` queda intencionadamente
-- fuera de auditoria por tamano y porque el cuerpo del PDF se reemplaza,
-- no se versiona; los metadatos (sha256, fecha_carga, dni_san_carga) si.
CREATE TABLE tratamiento_documento_audit (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL,
    rev_type SMALLINT,
    cod_trat VARCHAR(32),
    file_name VARCHAR(255),
    mime_type VARCHAR(80),
    sha256 CHAR(64),
    fecha_carga TIMESTAMP,
    dni_san_carga VARCHAR(20),
    PRIMARY KEY (id, rev)
);
```

---

## Step 8.2 — V15 migration `paciente.progreso_md` + `ultima_sync_progreso`

**File:** `api/src/main/resources/db/migration/V15__paciente_progreso.sql`

```sql
-- V15: progreso clinico persistido por paciente.
-- progreso_md: markdown determinista generado por /data, consumido por la futura IA.
-- ultima_sync_progreso: timestamp de la ultima sincronizacion exitosa con /data.
ALTER TABLE paciente
    ADD COLUMN IF NOT EXISTS progreso_md TEXT NULL,
    ADD COLUMN IF NOT EXISTS ultima_sync_progreso TIMESTAMP NULL;

ALTER TABLE paciente_audit
    ADD COLUMN IF NOT EXISTS progreso_md TEXT,
    ADD COLUMN IF NOT EXISTS ultima_sync_progreso TIMESTAMP;

-- Indice parcial para acelerar dashboards de "pacientes sincronizados ultimamente".
CREATE INDEX IF NOT EXISTS idx_paciente_ultima_sync_progreso
    ON paciente(ultima_sync_progreso DESC NULLS LAST)
    WHERE ultima_sync_progreso IS NOT NULL;

-- progreso_md se cifra en aplicación con AES-256-GCM via @ColumnTransformer.
-- Politica de tamaño: TEXT sin limite duro; warning soft a 64KB en logs de ProgresoService.
```

---

## Step 8.3 — Multipart config

**File:** `api/src/main/resources/application.yml` (bloque `spring`)

```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 12MB
```

`max-request-size` 12MB para dar holgura a metadatos multipart por encima del PDF de 10MB. Cualquier archivo mayor debe rechazarse con 413 antes de tocar la capa de aplicacion.

---

## Step 8.4 — Domain layer

**Entidad nueva** — `api/src/main/java/com/rehabiapp/api/domain/model/TratamientoDocumento.java`:
- Anotaciones: `@Entity @Audited @Table(name = "tratamiento_documento")`.
- Campos: `Long id`, `String codTrat` (FK uno-a-uno a `Tratamiento`), `String fileName`, `String mimeType`, `byte[] contenido` (`@Lob @Basic(fetch = LAZY)` + `@NotAudited`), `String sha256`, `Instant fechaCarga`, `String dniSanCarga`.

**Entidad ampliada** — `api/src/main/java/com/rehabiapp/api/domain/model/Paciente.java`:
- Nuevos campos:
  - `@Column(name = "progreso_md", columnDefinition = "TEXT") String progresoMd;`
  - `@Column(name = "ultima_sync_progreso") Instant ultimaSyncProgreso;`
- Ambos auditables por defecto (Envers ya esta activo en la entidad).
- Campo `progresoMd: String` con `@ColumnTransformer(read="pgp_sym_decrypt(progreso_md::bytea, current_setting('app.crypto.key'))", write="pgp_sym_encrypt(?, current_setting('app.crypto.key'))")` o equivalente AES-256-GCM via converter JPA si pgcrypto no disponible. Justificación: contiene DNI + observaciones clínicas (RGPD Art.9 + LOPDGDD).

**DTO records** — `api/src/main/java/com/rehabiapp/api/application/dto/`:
- `TratamientoDocumentoMeta(String codTrat, String fileName, String mimeType, long sizeBytes, String sha256, Instant fechaCarga, String dniSanCarga)`.
- `ProgresoCheckResponse(boolean hayNuevos, Instant ultimoRegistro)`.
- `MetricSnapshot(Instant fecha, double valor)`.
- `MetricPoint(Instant fecha, double valor)`.
- `ProgresoBodyPartSeries(String articulacionCodigo, String articulacionNombre, String metricKey, MetricSnapshot inicial, MetricSnapshot actual, double deltaAbs, double deltaPct, List<MetricPoint> serie)`.
- `ProgresoSyncResponse(Instant generadoEn, int totalArticulaciones, int totalPuntos, String progresoMd)`.

---

## Step 8.5 — Endpoints REST

| Metodo | Path | Rol | Descripcion |
|---|---|---|---|
| POST | `/api/catalogo/tratamientos/{cod}/documentos` | SANITARIO | Multipart `file` (PDF). Calcula sha256, persiste en `tratamiento_documento`. UNIQUE en `cod_trat` → upsert (delete + insert dentro de transaccion). Audit_log: `accion=DOCUMENTO_TRATAMIENTO_UPLOAD`. (Multipart PDF, sha256, persiste. UNIQUE cod_trat → overwrite explícito (no se mantiene histórico de versiones).) |
| DELETE | `/api/catalogo/tratamientos/{cod}/documentos` | SANITARIO | Borra fila. 204 si existia, 404 si no. Audit_log: `DOCUMENTO_TRATAMIENTO_DELETE`. |
| GET | `/api/pacientes/{dni}/tratamientos/{cod}/documento` | PATIENT-self / SANITARIO | application/pdf stream. Auth: JWT del paciente coincide con `{dni}` (claim `dni`) o rol SANITARIO. Verificación bypass RBAC si paciente accede a su propio recurso. Header `Content-Disposition: inline; filename="..."`. Audit_log: `DOCUMENTO_TRATAMIENTO_DOWNLOAD`. |
| GET | `/api/pacientes/{dni}/progreso/check?desde=ISO_TS` | SANITARIO | Delega a /data `has-new-sessions`. `desde` opcional; si null, /data interpreta "todo el historico". |
| GET | `/api/pacientes/{dni}/progreso/series` | SANITARIO | Delega a /data `treatment-timeline`. Devuelve `List<ProgresoBodyPartSeries>`. |
| POST | `/api/pacientes/{dni}/progreso/sync` | SANITARIO | (a) llama a /data `treatment-timeline`, (b) pide markdown a /data (`ProgressMarkdownGenerator`), (c) persiste `paciente.progreso_md`, (d) `ultima_sync_progreso = now()`, (e) escribe en audit_log `PROGRESO_SYNC`. Devuelve `ProgresoSyncResponse`. |

Controllers:
- `CatalogoController` (extender) → endpoints documentos.
- `PacienteController` (extender) → descarga PDF y endpoints progreso.

RBAC validado con `@PreAuthorize`. PATIENT-self resuelve via `Authentication` + DNI claim del JWT.

---

## Step 8.6 — Service layer

**Nuevo `TratamientoDocumentoService`** — `api/src/main/java/com/rehabiapp/api/application/service/TratamientoDocumentoService.java`:
- `upload(codTrat, MultipartFile, sanitario)` → valida content-type (`application/pdf`), tamano <=10MB, hashea sha256, upsert.
- `delete(codTrat, sanitario)` → elimina fila + log.
- `download(dni, codTrat, requester)` → valida visibilidad paciente↔tratamiento (`paciente_tratamiento.visible=true` para PATIENT-self), devuelve stream + log.

**Nuevo `ProgresoService`** — `api/src/main/java/com/rehabiapp/api/application/service/ProgresoService.java`:
- Cliente HTTP: `WebClient` configurado en `WebClientConfig` con base URL de /data y header obligatorio `X-Internal-Key` (tomado de `${API_INTERNAL_SHARED_KEY}`).
- `checkNuevos(dni, desde)` → `GET /analytics/patient/{dni}/has-new-sessions?desde=...`.
- `obtenerSeries(dni)` → `GET /analytics/patient/{dni}/treatment-timeline`.
- `sincronizar(dni, sanitario)` →
  1. `obtenerSeries(dni)`.
  2. `POST /analytics/markdown/patient/{dni}` con el payload de series (alternativa: el generador es accesible directamente como endpoint en /data).
  3. Persistir `paciente.progreso_md = markdown` + `ultima_sync_progreso = Instant.now()` via `PacienteRepository`.
  4. `auditLogService.registrar(PROGRESO_SYNC, dni, sanitario)`.
- Resuelve lista de articulaciones lesionadas del paciente vía SQL: `paciente_discapacidad pd JOIN discapacidad d ON pd.cod_dis = d.cod_dis WHERE pd.dni_pac = ? AND d.id_articulacion IS NOT NULL`. Pasa estos códigos a `/data` para filtrar el timeline (o filtra response si /data devuelve todas las articulaciones vistas en sesiones).
- Soft-warn: si `progresoMd.length() > 64*1024` log WARN con DNI hashed + tamaño. No corta el guardado.

Errores de /data → propagar como 502 `bad_gateway` con body `{ "error": "data_pipeline_unavailable" }`.

---

## Step 8.7 — Tests + TestSprite

**Slice tests (`@WebMvcTest`)** — uno por controller nuevo o extendido:
- POST documento: 201 con sanitario, 403 con nurse, 413 con archivo >10MB, 415 con content-type no PDF.
- DELETE documento: 204 si existe, 404 si no.
- GET documento: 200 binary, 403 si paciente intenta acceder a tratamiento no visible.
- GET progreso/check: mock WebClient → 200 con `hayNuevos=true|false`.
- GET progreso/series: mock WebClient → 200 con shape correcto.
- POST progreso/sync: mock WebClient + verificar que `paciente.progreso_md` se persiste (capturar via `@MockBean PacienteRepository`).

**Integration tests (`@SpringBootTest` + Testcontainers Postgres)**:
- Aplica V14 + V15 contra Postgres 15 dockerizado, asserta esquema (`information_schema.columns`).
- Sube PDF, lo descarga, verifica sha256 roundtrip.
- Stub WireMock en lugar de /data real para `progreso/sync`; valida que `paciente.progreso_md` queda con el markdown stubbeado y `ultima_sync_progreso` queda en ventana de 5s.

**TestSprite** — categoria `backend_test_plan`:
- Migraciones V14+V15 idempotentes.
- Multipart accept (≤10MB) / reject (>10MB con 413).
- RBAC en los 6 endpoints.
- Audit_log entries para upload/delete/download/sync (4 acciones nuevas en `audit_log_accion_check`).
- `progreso_md` no nulo tras sync exitoso.

Self-Healing loop hasta 5 intentos (root §10.3).

---

## Aceptacion

- `./mvnw flyway:migrate` aplica V14 + V15 sin errores en BD limpia y en BD existente con datos.
- `application.yml` acepta multipart hasta 10MB y rechaza >10MB con 413.
- audit_log contiene una fila por cada upload, delete, download y sync.
- `POST /api/pacientes/{dni}/progreso/sync` actualiza `progreso_md` y `ultima_sync_progreso` y devuelve `ProgresoSyncResponse` con el markdown.
- TestSprite `backend_test_plan` cierra en 100% (autohealing si falla).
- Mobile BFF (`mobile/backend/src/services/documentService.js`) descarga el PDF correctamente (smoke manual una vez /api despliegue).

---

## Archivo

### PLAN.md previo (cerrado 2026-04-20) — Bugfixes Envers + V11/V12 + progresion UI

> **Date:** 2026-04-20
> **Agent:** 1+3 Thinker (Opus)
> **Executor:** Doer (Sonnet)
> **Scope:** API bugfixes (500 on create/update) + V11 flyway failure (protesis cast) + Desktop bugfix (edit email check) + Verify progression UI

#### NEW — Bug F: V11 Flyway migration aborts boot

**Symptom:**
```
ERROR: default for column "protesis" cannot be cast automatically to type boolean
Location: db/migration/V11__fix_protesis_boolean.sql Line: 12
SQL State 42804
```
App context fails → `BeanCreationException` for `flyway` bean → API no arranca.

**Root cause:** column `paciente.protesis` in dev DB is INTEGER with `DEFAULT 0` (legacy schema carried over before V1 definition applied). Postgres `ALTER COLUMN ... TYPE BOOLEAN USING ...` does NOT cast the DEFAULT expression — the integer literal `0` can't auto-cast to boolean, so migration aborts.

**Side effect:** V11 is recorded as FAILED in `flyway_schema_history`. Cannot simply edit V11 and retry — Flyway checksum + failed-state locks retry. Must either:
- (a) `flyway repair` + rewrite V11 idempotently, OR
- (b) leave V11 as-is but fix via NEW V12 migration that handles the case.

**Decision:** option (b) — new V12. Rationale: V11 already lives in other devs' history; rewriting breaks their checksum. V12 is additive and idempotent (checks column type before acting).

#### Phase 7 — V12 migration: fix protesis cast + repair V11

- [x] 7.1 flyway:repair ejecutado (V11 reescrito idempotente — nunca habia tenido success en ningun entorno)
- [x] 7.2 V12 descartado — V11 reescrito directamente es suficiente
- [x] 7.3 API arranca limpio (flyway_schema_history V11 success=t)
- [x] 7.4 `\d paciente` confirma BOOLEAN DEFAULT FALSE
- [x] 7.5 PUT paciente con protesis=true/false → 200 OK, BD actualiza

#### Phase 1 — Diagnose exact missing columns (API)

- [x] Run validate, record missing columns (done via direct entity analysis)

#### Phase 2 — V10 Flyway migration (API)

- [x] Create V10 migration based on Phase 1 findings (paciente_audit gana dni_san, id_direccion, foto)

#### Phase 3 — Verify Envers YAML fix is active (API)

- [x] API restart — Flyway V10 OK (V10 success=t en schema_history)
- [x] POST sanitario — 201, Envers audit row creada
- [x] POST paciente — 201, Envers audit row creada
- [x] PUT paciente — 200, protesis bool actualizado correctamente en BD

#### Phase 4 — Fix sanitario edit email check (Desktop)

- [x] Add `emailOriginal` field
- [x] Store email in `cargarDatosParaEdicion()`
- [x] Guard `existeEmail()` with email comparison

#### Phase 5 — Verify / complete progression level UI (Desktop)

- [x] 5.1 verified: GET discapacidades con nivel, PUT nivel → 200, toggle visibilidad tratamiento → 200
- [x] 5.2 verified: POST asignar discapacidad desde catalogo → 201 con idNivel y nombreNivel
- [x] 5.3 verified: GET tratamientos con visible flag, jerarquia dis→trat completa
- [x] 5.X-fix: Bug descubierto — audit_log_accion_check no incluia DELETE. Resuelto via V12__add_delete_accion_audit.sql. DELETE desasignacion ahora 204.

#### Phase 6 — Mark CLAUDE.md checklist + cleanup

- [x] Mark desktop CLAUDE.md checklist items
- [x] Mark API CLAUDE.md bugfix items (Envers namespace + E2E)
- [x] Clean up PLAN.md (todos los phases completos)
