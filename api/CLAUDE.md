# CLAUDE.md - RehabiAPP API (REST Backend)

> **File:** `/api/CLAUDE.md`
> **Agent:** Agent 1 (Backend and Data Engineer)
> **Role:** Thinker (Opus) + Doer (Sonnet)

---

## 1. PROJECT DEFINITION

This directory contains the core RESTful API that connects the entire RehabiAPP ecosystem. It is the central communication hub consumed by the mobile app (/mobile), the rehabilitation games (/games), and eventually the desktop ERP (/desktop) once its legacy JDBC connection is migrated.

The API exposes all data operations, handles JWT authentication, role-based authorization, and routes game telemetry data to the data pipeline (/data) for MongoDB storage.

Architecture: Clean Architecture with four layers (Domain, Application, Infrastructure, Presentation). No business logic in controllers.

---

## 2. OPERATING RULES

1. **Global context:** Read and respect the root `/CLAUDE.md` before any cross-domain decision. This local file takes precedence for API-specific decisions only.

2. **Skills are mandatory:** Before any architectural change or implementation, read and follow the manuals in `.claude/skills/` of this directory. Skills override default behavior.

3. **Maintain this file:** When you complete a task, change `[ ]` to `[x]`. Remove resolved items that no longer provide useful context to keep this file short and token-efficient.

4. **Testing requirement:** Any new endpoint, service, or repository must include integration tests (Spring Boot Test) or unit tests (JUnit 5 + Mockito). Run `./mvnw test` before marking any task complete.

5. **No God Classes:** Controllers handle HTTP mapping only. All business logic lives in the Application layer (services/use cases). Never return `@Entity` directly from controllers; use DTOs mapped with MapStruct.

6. **Security by default:** All passwords hashed with BCrypt. Clinical data fields (allergies, medical history, current medication) encrypted with AES-256-GCM before database insertion. JWT tokens for all authenticated endpoints.

---

## 3. LOCAL STACK

- Java 24, Spring Boot 3, Spring Data JPA, Spring Security (JWT).
- PostgreSQL 15+ (relational data).
- Flyway (database migrations, versioned SQL scripts).
- MapStruct (entity-to-DTO mapping).
- Maven (build system).

### Build commands

```
./mvnw spring-boot:run    # Run
./mvnw compile            # Compile
./mvnw test               # Run tests
./mvnw clean              # Clean
./mvnw flyway:migrate     # Apply DB migrations
```

---

## 4. ARCHITECTURE

```
src/main/java/com/rehabiapp/api/
    |-- domain/            Entities, value objects, repository interfaces
    |-- application/       Use cases, services, DTOs, mappers
    |-- infrastructure/    JPA repositories, security config, encryption, Flyway
    |-- presentation/      REST controllers, exception handlers, request/response models
```

No circular dependencies between layers. Domain has zero framework imports.

---

## 5. IMPLEMENTATION CHECKLIST

### Phase 1: Project setup

- [x] Initialize Spring Boot 3 skeleton with Maven.
- [x] Configure PostgreSQL connection (application.yml).
- [x] Configure Flyway and create initial migration scripts mirroring the existing desktop DB schema.
- [x] Set up project layer structure (domain, application, infrastructure, presentation).
- [ ] Configure MapStruct for DTO mapping.

### Phase 2: Security

- [x] Implement JWT authentication (login endpoint, token generation, token validation filter).
- [x] Implement BCrypt password hashing utility (compatible with desktop legacy hashes).
- [x] Implement AES-256-GCM encryption utility for clinical fields (compatible with desktop CifradoService).
- [x] Configure role-based authorization (SPECIALIST full access, NURSE restricted).
- [x] Implement audit logging interceptor for all data operations.

### Phase 3: Core CRUD endpoints

- [x] Patient endpoints (GET list, GET by DNI, POST create, PUT update, DELETE soft-delete).
- [x] Practitioner endpoints (GET list, GET by DNI, POST create, PUT update, DELETE soft-delete).
- [x] Appointment endpoints (GET by date, GET by practitioner, POST create, PUT update, DELETE).
- [x] Disability catalog endpoints (GET list, GET by code).
- [x] Treatment catalog endpoints (GET list, GET by code, GET by disability and progression level).
- [x] Progression level endpoints (GET list).
- [x] Patient-disability assignment endpoints (GET, POST assign, PUT update level).
- [x] Patient-treatment visibility endpoints (GET, PUT toggle visibility).

### Bugfix: Envers audit tables not found (cita_aud does not exist)

**Diagnostico:** `application.yml` lineas 27-35 declaran propiedades Envers bajo `properties.hibernate.envers.*`, que genera claves como `hibernate.envers.audit_table_suffix`. Pero Hibernate Envers requiere namespace completo `org.hibernate.envers.*`. Al ignorarse la config, Envers usa defaults (`_aud` suffix, `revtype` columna) y no encuentra las tablas V9 creadas con `_audit` suffix y `rev_type` columna.

**Fix prescriptivo (1 archivo, 1 cambio):**

En `application.yml`, cambiar el bloque bajo `spring.jpa.properties`:

```yaml
# ANTES (INCORRECTO — Envers ignora estas propiedades):
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        envers:
          audit_table_suffix: _audit
          revision_field_name: rev
          revision_type_field_name: rev_type
          store_data_at_delete: true

# DESPUES (CORRECTO — namespace completo org.hibernate.envers):
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
      org.hibernate.envers:
        audit_table_suffix: _audit
        revision_field_name: rev
        revision_type_field_name: rev_type
        store_data_at_delete: true
```

- [x] Fix namespace Envers en `application.yml` (`hibernate.envers.*` → `org.hibernate.envers.*`)
- [x] Verificar E2E: POST/PUT paciente y POST sanitario → audit rows creadas sin error

### Phase 4: Ecosystem integration

- [ ] API documentation with Swagger/OpenAPI.
- [ ] Rate limiting and request validation.

### Phase 8 — Patient progress + treatment PDF persistence (2026-04-27)

- [ ] V14 migration `tratamiento_documento`.
- [ ] V15 migration `paciente.progreso_md` + `ultima_sync_progreso`.
- [ ] Multipart config (`spring.servlet.multipart.max-file-size: 10MB`).
- [ ] `TratamientoDocumento` entity + DTOs + add `progresoMd`/`ultimaSyncProgreso` to `Paciente`.
- [ ] POST `/api/catalogo/tratamientos/{cod}/documentos` (multipart upload PDF).
- [ ] DELETE `/api/catalogo/tratamientos/{cod}/documentos`.
- [ ] GET `/api/pacientes/{dni}/tratamientos/{cod}/documento` (binary stream).
- [ ] GET `/api/pacientes/{dni}/progreso/check?desde=ISO_TS`.
- [ ] GET `/api/pacientes/{dni}/progreso/series`.
- [ ] POST `/api/pacientes/{dni}/progreso/sync` (regenera markdown + actualiza ultima_sync_progreso).
- [ ] `@WebMvcTest` + `@SpringBootTest` con Testcontainers + TestSprite backend_test_plan.
- [ ] Encriptar `paciente.progreso_md` con AES-256-GCM (`@ColumnTransformer` o `AttributeConverter`).

---

## 6. DATABASE REFERENCE

This API shares the same PostgreSQL database as the desktop ERP. The schema is defined and migrated via Flyway. Refer to `/desktop/CLAUDE.md` section 8 for the current schema. All Flyway migration scripts in this project must be compatible with the existing desktop schema.

---

*This file is the single source of truth for the API domain. Update it as tasks are completed.*

## Memory

You have access to Engram persistent memory via MCP tools (mem_save, mem_search, mem_session_summary, etc.).
- Save proactively after significant work — don't wait to be asked.
- After any compaction or context reset, call `mem_context` to recover session state before continuing.
