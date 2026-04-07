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

Completed items provide context of what already exists. Uncompleted items are the current work focus. When a task is completed, mark it `[x]`. When a completed task no longer needs explanation, remove it entirely.

### Database and schema

- [x] Relational schema (12 core tables) in PostgreSQL.
- [x] Immutable audit_log table with performance indexes.
- [x] Clinical fields added to patient table: date_of_birth, sex, allergies, medical_history, current_medication, rgpd_consent, consent_date, active, deactivation_date.
- [x] Soft delete fields (active, deactivation_date) on both patient and practitioner tables.
- [x] Disability and treatment catalog tables with N:M relationship (discapacidad, tratamiento, discapacidad_tratamiento).
- [ ] Progression level catalog table (nivel_progresion) with 4 clinical phases (acute, subacute, strengthening, functional).
- [ ] Patient-disability assignment table (paciente_discapacidad) with per-disability progression level tracking.
- [ ] Patient-treatment visibility table (paciente_tratamiento) with practitioner-controlled visibility flag.
- [ ] Add id_nivel foreign key to tratamiento table linking treatments to progression levels.
- [ ] Migration script for progression level integration.
- [ ] Deprecate and eventually remove legacy text fields discapacidad_pac and tratamiento_pac from patient table.

### Security and encryption

- [x] BCrypt password hashing with lazy migration of plain-text passwords.
- [x] AES-256-GCM encryption/decryption of clinical fields in PacienteDAO.
- [x] Encryption key management via cifrado.properties (excluded from Git).
- [x] Fallback mode: app functions without encryption for development if key is missing.
- [x] Audit logging for all CRUD operations via AuditService (fire-and-forget pattern).
- [x] READ audit logging when opening patient records (consultaSensible).
- [x] SSL/TLS delegado a la API. El desktop usa HTTPS apuntando a URL segura via REHABIAPP_API_URL.
- [x] Custom exception hierarchy created (RehabiAppException, ConexionException, ValidacionException, AutenticacionException, PermisoException).
- [x] DAOs ya lanzan excepciones custom (migracion a REST API completada).

### CRUD operations

- [x] Full CRUD for patients with AES encryption, soft delete, active=TRUE filtering.
- [x] Full CRUD for practitioners with BCrypt, soft delete, active=TRUE filtering.
- [x] Appointment management with calendar view, conflict detection, async loading.
- [x] PacienteService and SanitarioService wrappers with automatic audit logging.
- [ ] Implement atomic transactions (commit/rollback) in PacienteService for compound operations (patient + phones + address + photo).
- [ ] Implement atomic transactions in SanitarioService for compound operations (practitioner + role + phones).
- [ ] Integrate photo upload within the same transaction as patient INSERT.

### User interface

- [x] Login screen with database connection indicator.
- [x] Main window with sidebar navigation and dynamic tab loading.
- [x] Role-based UI (practitioner tab hidden for nurses).
- [x] Paginated tables (50 records per page) for patients and practitioners.
- [x] Text search filtering by DNI, name, surnames, email, social security number.
- [x] Advanced filters for patients (prosthesis, age range, sorting) and practitioners (role, assigned patients, sorting).
- [x] Patient form with all clinical fields (sex combo, date picker, allergy/history/medication text areas, RGPD consent checkbox).
- [x] Patient detail view with clinical data section (read-only).
- [x] Sex column added to patient table.
- [x] Patient photo management (BYTEA storage).
- [x] Light and dark CSS themes (switchable).
- [x] Configurable font size (12px, 14px, 16px, 18px).
- [x] Window animations (open/close scale + fade, tab transitions, shake on validation error, hover effects).
- [x] Informational and confirmation dialog windows.
- [x] PDF report generation with JasperReports (individual patient, practitioner list).
- [x] Practitioner agenda in HTML rendered via WebView.
- [x] Help tab with integrated documentation.
- [x] User profile view and edit.
- [x] Quick search bar in header for fast patient report access.
- [ ] Implement progression level UI: display patient disabilities with current level, show treatments filtered by matching level, toggle treatment visibility.
- [ ] Update patient form to assign disabilities from catalog instead of free text.
- [ ] Update patient detail view to show disability-treatment-progression hierarchy.

### Advanced integrations (future phases)

- [ ] OpenAI API integration for automated clinical text processing and chart interpretation.
- [ ] NFC scanner integration for Spanish health card reading (auto-fill patient forms).
- [ ] Activar y probar HTTPS hacia la API en produccion (AWS).
- [x] Architectural migration: JDBC eliminado, desktop consume REST API de /api.

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
