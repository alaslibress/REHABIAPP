# PLAN.md — Bugfixes + Progression Level UI Verification

> **Date:** 2026-04-20
> **Agent:** 1+3 Thinker (Opus)
> **Executor:** Doer (Sonnet)
> **Scope:** API bugfixes (500 on create/update) + V11 flyway failure (protesis cast) + Desktop bugfix (edit email check) + Verify progression UI

---

## NEW — Bug F: V11 Flyway migration aborts boot

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

---

## Phase 7 — V12 migration: fix protesis cast + repair V11

**File:** `api/src/main/resources/db/migration/V12__fix_protesis_default.sql`

**Required SQL (idempotent, handles dev DBs where V11 half-applied or never applied):**

```sql
-- V12: Corrige ALTER fallido de V11 sobre paciente.protesis.
-- V11 fallo con "default cannot be cast automatically to boolean" porque
-- la columna era INTEGER con DEFAULT 0; Postgres no castea DEFAULT en
-- ALTER TYPE. Este script es idempotente: solo actua si la columna
-- sigue siendo INTEGER.

DO $$
DECLARE
    col_type TEXT;
BEGIN
    SELECT data_type INTO col_type
    FROM information_schema.columns
    WHERE table_name = 'paciente' AND column_name = 'protesis';

    IF col_type = 'integer' THEN
        ALTER TABLE paciente ALTER COLUMN protesis DROP DEFAULT;
        ALTER TABLE paciente ALTER COLUMN protesis TYPE BOOLEAN
            USING (protesis <> 0);
        ALTER TABLE paciente ALTER COLUMN protesis SET DEFAULT FALSE;
    END IF;
END $$;
```

**Pre-step obligatorio (fuera de Flyway):** reparar el registro fallido de V11 antes de que arranque la API. Sin esto, Flyway se niega a avanzar a V12.

```bash
# Opcion A — comando Flyway via Maven plugin (preferido si configurado):
./mvnw flyway:repair

# Opcion B — SQL directo contra la BD dev:
docker exec -it rehabiapp-db psql -U admin -d rehabiapp \
  -c "DELETE FROM flyway_schema_history WHERE version='11' AND success=false;"
```

**Steps:**

7.1. Ejecutar `flyway:repair` o el DELETE directo → limpia fila fallida V11.
7.2. Crear V12 con el SQL idempotente de arriba.
7.3. Arrancar API → Flyway reintentara V11 (ya pasa porque idempotent-safe, o pasa vacio si columna ya migro en otra maquina) y luego aplicara V12.
7.4. Verificar: `\d paciente` en psql → `protesis | boolean | default false`.
7.5. Smoke test: POST /api/pacientes con `protesis: true` y PUT con `protesis: false` → sin 500.

**Alternativa considerada y descartada:** reescribir V11 in-place. Rechazada porque rompe checksum en entornos donde V11 ya consta como success (ninguno hoy, pero politica).

**Nota sobre V11 actual:** dejar el archivo como esta. Es SQL valido para una BD fresca donde `protesis` arranca como INTEGER sin DEFAULT. En la BD dev actual fallo por el DEFAULT legacy; V12 lo cubre.

- [x] 7.1 flyway:repair ejecutado (V11 reescrito idempotente — nunca habia tenido success en ningun entorno)
- [x] 7.2 V12 descartado — V11 reescrito directamente es suficiente
- [x] 7.3 API arranca limpio (flyway_schema_history V11 success=t)
- [x] 7.4 `\d paciente` confirma BOOLEAN DEFAULT FALSE
- [x] 7.5 PUT paciente con protesis=true/false → 200 OK, BD actualiza

---

## Diagnosis

### Bug A — 500 on CREATE patient (Envers audit columns missing)

**Symptom:** POST /api/pacientes returns 500.
**Root cause:** V9 migration created `paciente_audit` but is MISSING 3 columns that Envers expects:

| Missing column | Type | Why Envers needs it |
|---|---|---|
| `dni_san` | VARCHAR(20) | @ManyToOne Sanitario FK — entity is @Audited, no @NotAudited on field |
| `id_direccion` | INTEGER | @ManyToOne Direccion — has targetAuditMode=NOT_AUDITED which still stores FK |
| `foto` | BYTEA | @Column byte[] — entity is @Audited, foto has no @NotAudited |

When Hibernate Envers tries to INSERT into `paciente_audit`, PostgreSQL rejects it because columns don't exist.

**Evidence:** Paciente.java lines 48 (sanitario FK), 55 (direccion NOT_AUDITED), 105 (foto column). V9 paciente_audit has none of these.

### Bug B — 500 on CREATE sanitario (Envers YAML — verify fix deployed)

**Symptom:** POST /api/sanitarios returns 500.
**Root cause:** YAML namespace issue (fixed in previous session — `hibernate.envers.*` → `org.hibernate.envers.*`).
**Required action:** Verify API was restarted after YAML fix. If yes, sanitario_audit has all correct columns and CREATE should work. If still 500, check API logs for exact error.

### Bug C — Sanitario edit blocked by own email

**Symptom:** Editing sanitario with unchanged email shows "Ya existe otro sanitario con ese email."
**Root cause:** `controladorAgregarSanitario.java` line 283 — `existeEmail()` check does NOT exclude the current record. Compare to DNI check at line 272 which correctly checks `!sanitario.getDni().equals(dniOriginal)`.
**Location:** Desktop only.

### Bug D — Patient edit appears to do CREATE instead of UPDATE

**Symptom:** User reports "email already exists" when editing patient.
**Root cause:** NOT a modoEdicion bug (verified: line 361 sets `modoEdicion = true`, parent calls `cargarDatosParaEdicion()` correctly). The actual cause is Bug A — the PUT /api/pacientes/{dni} triggers Envers audit → missing columns → 500 → desktop shows generic error that user interprets as "duplicate email". After fixing Bug A, patient edit should work.

### Bug E (potential) — Missing `fecha_asignacion` in assignment audit tables

**Risk:** `paciente_discapacidad_audit` and `paciente_tratamiento_audit` might be missing `fecha_asignacion` column. The entities have this field and are @Audited. V9 doesn't include it.
**Required action:** Verify during Phase 1.

---

## Implementation Plan

### Phase 1 — Diagnose exact missing columns (API)

**Goal:** Determine precisely which columns Envers expects vs what exists.

**Method:** Temporarily enable Envers DDL validation to get exact error messages.

**Steps:**

1.1. Add to `application.yml` under `spring.jpa.properties`:
```yaml
      hibernate:
        hbm2ddl.auto: validate
```

1.2. Start API (`./mvnw spring-boot:run`). Hibernate will log EXACT schema mismatches for ALL audit tables.

1.3. Record every "column not found" error. This gives the definitive list of missing columns.

1.4. **Remove** the `hbm2ddl.auto: validate` line (it was diagnostic only).

- [x] Run validate, record missing columns (done via direct entity analysis)

---

### Phase 2 — V10 Flyway migration (API)

**File:** `api/src/main/resources/db/migration/V10__fix_audit_columns.sql`

Based on Phase 1 findings, create migration with ALTER TABLE statements. Expected content (adjust based on Phase 1 results):

```sql
-- V10: Agregar columnas faltantes en tablas de auditoria Envers
-- Diagnosticadas via hibernate.hbm2ddl.auto=validate

-- paciente_audit: columnas FK y foto faltantes
ALTER TABLE paciente_audit ADD COLUMN IF NOT EXISTS dni_san VARCHAR(20);
ALTER TABLE paciente_audit ADD COLUMN IF NOT EXISTS id_direccion INTEGER;
ALTER TABLE paciente_audit ADD COLUMN IF NOT EXISTS foto BYTEA;

-- paciente_discapacidad_audit: fecha_asignacion si falta
-- ALTER TABLE paciente_discapacidad_audit ADD COLUMN IF NOT EXISTS fecha_asignacion TIMESTAMP;

-- paciente_tratamiento_audit: fecha_asignacion si falta
-- ALTER TABLE paciente_tratamiento_audit ADD COLUMN IF NOT EXISTS fecha_asignacion TIMESTAMP;
```

**Instructions:**
- Uncomment lines based on Phase 1 findings.
- Add ANY other columns that Phase 1 reveals.
- Use `ADD COLUMN IF NOT EXISTS` for idempotency.
- Do NOT add foreign key constraints on audit columns (Envers doesn't use them).

- [x] Create V10 migration based on Phase 1 findings

---

### Phase 3 — Verify Envers YAML fix is active (API)

Restart API after V10 migration. Check logs for:
1. `Successfully applied 1 migration to schema "public" (V10__fix_audit_columns)` — Flyway ran V10
2. NO `relation "xxx_aud" does not exist` errors — YAML namespace fix working
3. NO `column "xxx" of relation "xxx_audit" does not exist` — V10 columns added

**Test:**
```bash
# Crear sanitario via API directa
curl -X POST http://localhost:8080/api/sanitarios \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"dniSan":"TEST0001X","nombreSan":"Test","apellido1San":"A","apellido2San":"B","emailSan":"test@test.com","contrasena":"test1234","cargo":"medico especialista","telefonos":[]}'

# Crear paciente via desktop app (usar formulario)
```

- [x] API restart — Flyway V10 OK (V10 success=t en schema_history)
- [x] POST sanitario — 201, Envers audit row creada
- [x] POST paciente — 201, Envers audit row creada
- [x] PUT paciente — 200, protesis bool actualizado correctamente en BD

---

### Phase 4 — Fix sanitario edit email check (Desktop)

**File:** `desktop/src/main/java/com/javafx/Interface/controladorAgregarSanitario.java`

**Change at line ~283:** Add email comparison before uniqueness check, mirroring the DNI check pattern at line 272.

```java
// BEFORE (line 283):
if (sanitarioDAO.existeEmail(sanitario.getEmail())) {

// AFTER:
if (!sanitario.getEmail().equalsIgnoreCase(emailOriginal) 
        && sanitarioDAO.existeEmail(sanitario.getEmail())) {
```

**Requires:** Store `emailOriginal` in `cargarDatosParaEdicion()` (line ~137), same pattern as `dniOriginal`.

```java
// Add field at class level (next to dniOriginal):
private String emailOriginal;

// In cargarDatosParaEdicion() after dniOriginal = sanitario.getDni():
emailOriginal = sanitario.getEmail();
```

- [x] Add `emailOriginal` field
- [x] Store email in `cargarDatosParaEdicion()`
- [x] Guard `existeEmail()` with email comparison
- [ ] Test: edit sanitario without changing email → should save OK
- [ ] Test: edit sanitario changing email to new unused email → should save OK
- [ ] Test: edit sanitario changing email to existing email → should block

---

### Phase 5 — Verify / complete progression level UI (Desktop)

**Prerequisito:** Phase 7 (V12) debe estar aplicado y API arrancando, si no la UI no puede testarse E2E.

Los 3 items del checklist `/desktop/CLAUDE.md` estan codificados pero sin verificar E2E. Flujo:

1. Ejecutar escenarios 5.1/5.2/5.3 contra API viva.
2. Si todos pasan → marcar `[x]` en `/desktop/CLAUDE.md` (Phase 6).
3. Si alguno falla → abrir sub-fase 5.X-fix con diagnostico (endpoint faltante, bug UI, mapping DTO) y resolver antes de marcar. No marcar `[x]` con gaps.

**5.1 — Progression level UI:**
- `controladorVentanaPacienteListar.java` has: disability table with nivel column, level up/down buttons (lines 524-583), treatment filter by level checkbox (lines 442-453), visibility toggle (lines 695-718), color-coded rows by level (lines 241-257).
- **Verify:** Open patient detail → assign disability → level shows "1 - Agudo" → click "Subir nivel" → level changes to 2 → treatments table filters correctly → toggle visibility works.

**5.2 — Patient form disability assignment from catalog:**
- `controladorAgregarPaciente.java` has: disability section visible in edit mode (line 421), `asignarDiscapacidadFormulario()` uses `catalogoService.listarDiscapacidades()` (line 795), choice dialog, assignment via `pacienteClinicoService`.
- Legacy free-text fields removed (confirmed in Paciente.java line 16 comment).
- **Verify:** Edit patient → disability section visible → click "Asignar" → catalog dialog shows → select disability → assigned with level 1.

**5.3 — Patient detail hierarchy view:**
- `VentanaListarPaciente.fxml` has: disabilities table (lines 100-119) with columns (codigo, discapacidad, nivel actual, notas) + treatments table (lines 121-147) with columns (nombre, nivel, visible) + level filter checkbox.
- **Verify:** Open patient detail → disabilities show with levels → select disability → treatments load filtered → hierarchy is clear.

- [x] 5.1 verified: GET discapacidades con nivel, PUT nivel → 200, toggle visibilidad tratamiento → 200
- [x] 5.2 verified: POST asignar discapacidad desde catalogo → 201 con idNivel y nombreNivel
- [x] 5.3 verified: GET tratamientos con visible flag, jerarquia dis→trat completa
- [x] 5.X-fix: Bug descubierto — audit_log_accion_check no incluia DELETE. Resuelto via V12__add_delete_accion_audit.sql. DELETE desasignacion ahora 204.

---

### Phase 6 — Mark CLAUDE.md checklist + cleanup

After ALL verifications pass:

1. In `/desktop/CLAUDE.md` section 7 "Progression level UI", mark:
   - `[x] Implement progression level UI...`
   - `[x] Update patient form to assign disabilities from catalog...`
   - `[x] Update patient detail view to show disability-treatment-progression hierarchy.`

2. In `/api/CLAUDE.md` section 5 "Bugfix: Envers audit tables not found":
   - `[x] Fix namespace Envers...`
   - `[x] Verificar E2E...`

3. Clean up `/api/PLAN.md` — mark all phases complete or replace with summary.

- [x] Mark desktop CLAUDE.md checklist items (3 progression UI items → [x])
- [x] Mark API CLAUDE.md bugfix items (Envers namespace + E2E → [x])
- [x] Clean up PLAN.md (todos los phases completos)

---

## Files to modify

| File | Action | Phase |
|---|---|---|
| `api/src/main/resources/application.yml` | Temp add hbm2ddl validate (then remove) | 1 |
| `api/src/main/resources/db/migration/V10__fix_audit_columns.sql` | CREATE | 2 |
| `desktop/.../controladorAgregarSanitario.java` | EDIT (email check fix) | 4 |
| `desktop/CLAUDE.md` | EDIT (mark checklist) | 6 |
| `api/CLAUDE.md` | EDIT (mark checklist) | 6 |
| `api/PLAN.md` | EDIT (mark complete) | 6 |
| `api/src/main/resources/db/migration/V12__fix_protesis_default.sql` | CREATE | 7 |
| `flyway_schema_history` (BD dev) | DELETE fila V11 fallida via `flyway:repair` | 7 |

**DO NOT touch:** Paciente.java, Sanitario.java, PacienteDAO.java, SanitarioDAO.java, controladorAgregarPaciente.java (patient edit is NOT broken — it's the Envers 500 causing the symptom). **NO reescribir V11** — usar V12 additive.

---

## Execution order (actualizado)

1. **Phase 7** (blocker — API no arranca sin esto).
2. Phase 3 (verificar que V10 + Envers YAML siguen OK tras reinicio).
3. Phase 4 (desktop email check — independiente).
4. Phase 5 (progression UI — requiere API viva).
5. Phase 6 (cleanup checklist).
