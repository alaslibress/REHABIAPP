# CLAUDE.md - RehabiAPP Desktop (SGE)

> **File:** `/desktop/CLAUDE.md`
> **Agent:** Agent 3 (Desktop Developer)
> **Role:** Thinker (Opus) + Doer (Sonnet)

---

## 1. PROJECT DEFINITION

This directory contains the Desktop ERP (SGE - Sistema de Gestion de Expedientes) of RehabiAPP. It is a JavaFX client application used by healthcare practitioners (specialists and nurses) to manage patients, practitioners, appointments, disability-linked treatments organized by clinical progression levels, and to visualize patient rehabilitation progress.

El SGE consume la REST API central (`/api`) sin acceso directo a la base de datos. La conexion JDBC legacy fue eliminada en la migracion completada en marzo-abril de 2026.

---

## 2. OPERATING RULES

1. **Global context:** You are part of a larger ecosystem. Read and respect the root `/CLAUDE.md` before any cross-domain decision. This local file takes precedence for desktop-specific decisions only.

2. **Skills are mandatory:** Before any architectural change, design pattern implementation, or technical task in Java/JavaFX, you MUST read and follow the manuals in `.claude/skills/` of this directory. Skills override default behavior.

3. **Maintain this file:** This document is the living state of the project. When you complete a task, change `[ ]` to `[x]`. When a resolved item no longer provides useful context, remove it to keep this file short and token-efficient.

4. **Testing requirement:** Any refactoring of DAOs, Services, or database connections (such as the HikariCP migration or the custom exceptions refactor) MUST be accompanied by unit tests (JUnit 5) or mocked tests (Mockito) that verify the refactored code does not break existing logic. Do not mark a refactoring task as `[x]` without passing tests. Run `./gradlew test` before considering any refactoring task complete.

---

## 3. LOCAL STACK

- Java 24, JavaFX 23 (FXML via SceneBuilder), CSS (light/dark themes).
- Conexion via REST API central (`/api` en `localhost:8080` por defecto, configurable via `api.properties` o variable `REHABIAPP_API_URL`).
- AES-256-GCM y BCrypt delegados a la API (ya no se ejecutan en el desktop).
- ControlsFX 11.x (visual field validation).
- CalendarFX 11.12+ (monthly calendar view for appointments).
- JasperReports 6.20+ (PDF and HTML report generation).
- Gradle (build system).

### Build commands

```
./gradlew compileJava     # Compile
./gradlew run             # Run
./gradlew clean           # Clean
./gradlew test            # Run tests
```

---

## 4. PACKAGE STRUCTURE

```
src/main/java/com/javafx/
    |-- Clases/        Main, ApiClient (HTTP singleton), Paciente, Sanitario, Cita, SesionUsuario
    |-- Interface/     All JavaFX controllers (controladorSesion, controladorMenuPrincipal, etc.)
    |-- DAO/           PacienteDAO, SanitarioDAO, CitaDAO, DireccionDAO, AuditLogDAO
    |-- service/       PacienteService, SanitarioService, AuditService, CifradoService
    |-- util/          CifradoUtil, VentanaUtil, AnimacionUtil, ValidacionUtil, PaginacionUtil, VentanaHelper, ConstantesApp
    |-- excepcion/     RehabiAppException, ConexionException, ValidacionException, AutenticacionException, PermisoException

src/main/resources/
    |-- fxml/          All FXML files (designed in SceneBuilder)
    |-- css/           tema-claro.css, tema-oscuro.css
    |-- config/        api.properties (url base + timeout), preferencias.properties
    |-- imagenes/      Icons and images
```

Root package is `com.javafx`. Respect this structure in all operations.

---

## 5. ROLES AND PERMISSIONS (RBAC)

Two user types, both healthcare practitioners:

**Specialist (medico especialista):** Full CRUD on patients and practitioners. Appointment management. Report generation. Profile editing.

**Nurse (enfermero):** Read-only access to patients (cannot create, edit or delete). No access to practitioner management (tab hidden). Appointment management. Profile editing.

---

## 6. SECURITY RULES

- Passwords: BCrypt with cost factor 12. Lazy migration of legacy plain-text passwords on successful login.
- Clinical fields (allergies, medical history, current medication): AES-256-GCM with random 96-bit IV per operation. Key stored in cifrado.properties (excluded from Git via .gitignore).
- Audit: Every CRUD operation and every READ access to patient records logged in audit_log (immutable, INSERT only).
- Deletion: Soft delete only (active=FALSE, deactivation_date). Physical deletion prohibited. 5-year retention (Ley 41/2002).
- SSL/TLS: Responsabilidad de la API en produccion. El desktop usa HTTPS hacia la API cuando la variable `REHABIAPP_API_URL` apunta a un endpoint seguro.

---

## 7. IMPLEMENTATION CHECKLIST

### API prerequisites (Agent 1 scope — must be completed before desktop CRUD tabs)

- [x] API: POST /api/catalogo/discapacidades — create disability (codDis, nombreDis, descripcionDis, necesitaProtesis).
- [x] API: PUT /api/catalogo/discapacidades/{cod} — update disability fields.
- [x] API: DELETE /api/catalogo/discapacidades/{cod} — delete disability (reject if patients are assigned).
- [x] API: POST /api/catalogo/tratamientos — create treatment (codTrat, nombreTrat, definicionTrat, idNivel, codDis).
- [x] API: PUT /api/catalogo/tratamientos/{cod} — update treatment fields.
- [x] API: DELETE /api/catalogo/tratamientos/{cod} — delete treatment (reject if patients are assigned).
- [x] API: POST /api/catalogo/tratamientos/{codTrat}/discapacidades/{codDis} — link treatment to disability.
- [x] API: DELETE /api/catalogo/tratamientos/{codTrat}/discapacidades/{codDis} — unlink treatment from disability.
- [x] API: GET /api/catalogo/tratamientos/{codTrat}/discapacidades — list disabilities linked to a treatment.
- [x] API: Add validation — unique codDis/codTrat, unique nombreDis/nombreTrat, required fields, valid FKs.

### Disability CRUD tab (desktop)

- [x] Extend CatalogoDAO with crearDiscapacidad(), actualizarDiscapacidad(), eliminarDiscapacidad() calling new API endpoints.
- [x] Extend CatalogoService to wrap new CatalogoDAO CRUD methods with error handling.
- [x] Create DTO DiscapacidadRequest record (codDis, nombreDis, descripcionDis, necesitaProtesis) for POST/PUT body.
- [x] Create VentanaDiscapacidades.fxml following VentanaPacientes/VentanaSanitarios layout pattern (VBox root, header with title + search/add/filter bar, TableView with columns: Codigo, Nombre, Descripcion, Protesis, footer with Eliminar + Editar buttons).
- [x] Create controladorVentanaDiscapacidades.java (load from CatalogoService, search filter, CRUD button wiring, RBAC specialist-only for write operations).
- [x] Create VentanaAgregarDiscapacidad.fxml modal form (fields: codDis, nombreDis, descripcionDis, necesitaProtesis checkbox; create/edit mode support).
- [x] Create controladorAgregarDiscapacidad.java (validate required fields, handle DuplicadoException, refresh parent table on success).
- [x] Verify VentanaDiscapacidades loads correctly in tab cache system (cargarPestania("Discapacidades") already wired in controladorVentanaPrincipal line 420).

### Treatment CRUD tab (desktop)

- [x] Extend CatalogoDAO with crearTratamiento(), actualizarTratamiento(), eliminarTratamiento(), vincularDiscapacidad(), desvincularDiscapacidad(), listarDiscapacidadesDeTratamiento().
- [x] Extend CatalogoService to wrap new treatment CRUD methods.
- [x] Create DTO TratamientoRequest record (codTrat, nombreTrat, definicionTrat, idNivel, codDis).
- [x] Create VentanaTratamientos.fxml following same pattern (header + filter bar with ComboBox by disability and by progression level + TableView with columns: Codigo, Nombre, Definicion, Discapacidad, Nivel + footer with Eliminar + Editar buttons).
- [x] Create controladorVentanaTratamientos.java (load treatments, populate filter ComboBoxes from catalog, wire filters and CRUD buttons, RBAC specialist-only).
- [x] Create VentanaAgregarTratamiento.fxml modal form (fields: codTrat, nombreTrat, definicionTrat, ComboBox discapacidad required, ComboBox nivel progresion required; create/edit mode support).
- [x] Create controladorAgregarTratamiento.java (validate required fields including disability + level, handle DuplicadoException, refresh parent table on success).
- [x] Verify VentanaTratamientos loads correctly in tab cache system (cargarPestania("Tratamientos") already wired in controladorVentanaPrincipal line 428).

### Custom calendar (replace CalendarFX)

- [x] Create CalendarioPersonalizado.java — custom GridPane-based monthly calendar (7 columns L-D x 6 rows, cell as VBox with day number + appointment count label, navigation bar with month/year + prev/next buttons, today highlight, weekend distinct style, adjacent month days muted).
- [x] Implement cell appointment count rendering: show "{n} cita(s)" badge below day number when appointments exist.
- [x] Implement cell tooltip on hover: show patient initials list (e.g., "J.G., M.R., A.L.") for days with appointments.
- [x] Implement single-click day selection: highlight cell, load day's appointments in table below, sync with DatePicker.
- [x] Implement multi-day selection: Ctrl+Click toggle, Shift+Click range, distinct visual indicator, enable batch deletion.
- [x] Implement appointment interaction: double-click day to expand in table, double-click table row for appointment context, "Ver ficha paciente" button to navigate to patient tab.
- [x] Fix appointment creation flow: verify ComboBox patient search + DatePicker + Spinners correctly POST to /api/citas, debug if broken.
- [x] Remove CalendarFX dependency from build.gradle and all CalendarFX imports from controladorVentanaCitas.java.
- [x] Update controladorVentanaCitas.java: replace MonthView initialization with CalendarioPersonalizado, replace CalendarSource with direct appointment Map, keep existing table/form/button logic and async loading.
- [x] Remove emoji from btnVerAgenda text in VentanaCitas.fxml (change to "Ver Mi Agenda" without emoji prefix).
- [x] Add CSS classes for custom calendar in both tema_claro.css and tema_oscuro.css (.calendario-grid, .calendario-celda, .calendario-celda-hoy, .calendario-celda-fin-semana, .calendario-celda-seleccionada, .calendario-celda-otro-mes, .calendario-badge-citas, .calendario-header, .calendario-nav-button).

### CSS visual enhancement (both themes)

- [x] Enhanced sidebar navigation: left accent border (3px) on active/hover tab, subtle gradient background, smooth color transition between selected/unselected states.
- [x] Card-based content panels: wrap content areas in card containers with border-radius 8-12px, soft shadow, slight background elevation, darker header strip.
- [x] Improved table styling: left border indicator on hover row, increased row height 36-40px, smooth hover transition 150ms, accent left border on selected row, improved header bottom border.
- [x] Enhanced form inputs: inner shadow on focus, left color accent bar 3px on focus, lighter italic placeholder text, validation state borders (green valid, red error).
- [x] Button refinements: subtle gradient on primary buttons, deeper shadow + scale 0.98 on press, improved disabled state opacity 0.5.
- [x] Enhanced separators: gradient fade (transparent-color-transparent) instead of solid lines, improved spacing.
- [x] Typography improvements: letter-spacing 0.3px on titles, section titles 15px semi-bold, text-shadow on dark theme titles.
- [x] Tooltip and popover polish: deeper softer shadow, improved styling.
- [x] Scrollbar refinement: thinner track 6px, rounded thumb with hover expansion 6px-8px, subtle color transition.
- [x] Status indicators and badges: CSS classes .badge-activo, .badge-inactivo, .badge-nivel-* with color-coded progression levels (agudo=red, subagudo=orange, fortalecimiento=blue, funcional=green), pill shape border-radius 12px.
- [x] Login screen polish: subtle background gradient, improved connection indicator styling.
- [x] Modal window improvements: accent top border on modal header, smooth open animation.

### Progression level UI (existing pending tasks)

- [ ] Implement progression level UI: display patient disabilities with current level, show treatments filtered by matching level, toggle treatment visibility.
- [ ] Update patient form to assign disabilities from catalog instead of free text.
- [ ] Update patient detail view to show disability-treatment-progression hierarchy.

### Advanced integrations (future phases)

- [ ] OpenAI API integration for automated clinical text processing and chart interpretation.
- [ ] NFC scanner integration for Spanish health card reading (auto-fill patient forms).
- [ ] Activar y probar HTTPS hacia la API en produccion (AWS).

---

## 8. DATABASE SCHEMA REFERENCE

Current schema (quick reference for query writing):

```
sanitario(dni_san PK, nombre_san, apellido1_san, apellido2_san, email_san UNIQUE,
          num_de_pacientes, contrasena_san, activo, fecha_baja)

sanitario_agrega_sanitario(dni_san PK/FK CASCADE, cargo CHECK)

telefono_sanitario(id_telefono SERIAL PK, dni_san FK CASCADE, telefono)

localidad(nombre_localidad PK, provincia)
cp(cp PK, nombre_localidad FK)
direccion(id_direccion SERIAL PK, calle, numero, piso, cp FK)

discapacidad(cod_dis PK, nombre_dis UNIQUE, descripcion_dis, necesita_protesis)
tratamiento(cod_trat PK, nombre_trat UNIQUE, definicion_trat)
discapacidad_tratamiento(cod_dis FK, cod_trat FK -- composite PK)

paciente(dni_pac PK, dni_san FK RESTRICT, nombre_pac, apellido1_pac, apellido2_pac,
         edad_pac, email_pac UNIQUE, num_ss UNIQUE, id_direccion FK,
         discapacidad_pac, tratamiento_pac, estado_tratamiento, protesis, foto BYTEA,
         fecha_nacimiento, sexo, alergias, antecedentes, medicacion_actual,
         consentimiento_rgpd, fecha_consentimiento, activo, fecha_baja)

telefono_paciente(id_telefono SERIAL PK, dni_pac FK CASCADE, telefono)

cita(dni_pac FK CASCADE, dni_san FK CASCADE, fecha_cita, hora_cita -- composite PK)

audit_log(id_audit BIGSERIAL PK, fecha_hora, dni_usuario, nombre_usuario,
          accion CHECK, entidad, id_entidad, detalle, ip_origen)
```

Tables pending creation: nivel_progresion, paciente_discapacidad, paciente_tratamiento.

---

*This file is the single source of truth for the desktop SGE domain. Update it as tasks are completed. Remove resolved items that no longer provide useful context.*

---

## RUNBOOK LOCAL

### Levantar el stack completo (orden obligatorio)

1. **PostgreSQL** (terminal 1):
   ```
   docker compose -f /home/alaslibres/DAM/RehabiAPP/infra/docker-compose.yml up postgres
   ```

2. **API Spring Boot** (terminal 2):
   ```
   cd /home/alaslibres/DAM/RehabiAPP/api
   set -a && source .env.local && set +a
   ./mvnw spring-boot:run
   ```
   Esperar a `Started ApiApplication`.

3. **Desktop JavaFX** (terminal 3):
   ```
   cd /home/alaslibres/DAM/RehabiAPP/desktop
   ./gradlew run
   ```

### Variables de entorno necesarias (`api/.env.local`, NO commitear)

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/rehabiapp
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=admin
SPRING_PROFILES_ACTIVE=local
```

### Credenciales de prueba (seed en `desktop/scripts/reseed-dev.sql`)

| DNI | Contrasena | Rol |
|-----|------------|-----|
| ADMIN0000 | admin | SPECIALIST |
| 00000001R | medico1234 | SPECIALIST |
| 00000002W | enfermero1234 | NURSE |

### Health checks

```bash
# PG
docker exec rehabiapp-db psql -U admin -d rehabiapp -c "SELECT 1;"
# API
curl http://localhost:8080/actuator/health
# Desktop: pulsar el indicador del login -> debe pintarse verde
```

### Errores comunes

| Sintoma | Causa | Solucion |
|---------|-------|----------|
| `"JavaFX runtime components are missing"` | JVM sin JavaFX | Ver skill `javafx-java24` |
| `"password authentication failed"` en logs API | `.env.local` mal configurado | Revisar credenciales; deben coincidir con docker-compose.yml |
| Indicador de login en rojo | API no arrancada o inaccesible | Ver trazas SLF4J `Conexion API fallida` en terminal 3 |
| 403 al login con credenciales correctas | DNI en minusculas en BD | El DNI se envia en mayusculas; BD debe tenerlo en mayusculas |
| 500 en GET /api/pacientes | Mismatch de columna o enum en API | Revisar logs de la API con DEBUG habilitado |
| 401 en GET /api/pacientes despues de login OK | JWT no se envia | Revisar `ApiClient.get()` y header Authorization |

---

## Memory

You have access to Engram persistent memory via MCP tools (mem_save, mem_search, mem_session_summary, etc.).
- Save proactively after significant work — don't wait to be asked.
- After any compaction or context reset, call `mem_context` to recover session state before continuing.
