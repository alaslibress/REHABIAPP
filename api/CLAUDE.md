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

- [ ] Initialize Spring Boot 3 skeleton with Maven.
- [ ] Configure PostgreSQL connection (application.yml).
- [ ] Configure Flyway and create initial migration scripts mirroring the existing desktop DB schema.
- [ ] Set up project layer structure (domain, application, infrastructure, presentation).
- [ ] Configure MapStruct for DTO mapping.

### Phase 2: Security

- [ ] Implement JWT authentication (login endpoint, token generation, token validation filter).
- [ ] Implement BCrypt password hashing utility (compatible with desktop legacy hashes).
- [ ] Implement AES-256-GCM encryption utility for clinical fields (compatible with desktop CifradoService).
- [ ] Configure role-based authorization (SPECIALIST full access, NURSE restricted).
- [ ] Implement audit logging interceptor for all data operations.

### Phase 3: Core CRUD endpoints

- [ ] Patient endpoints (GET list, GET by DNI, POST create, PUT update, DELETE soft-delete).
- [ ] Practitioner endpoints (GET list, GET by DNI, POST create, PUT update, DELETE soft-delete).
- [ ] Appointment endpoints (GET by date, GET by practitioner, POST create, PUT update, DELETE).
- [ ] Disability catalog endpoints (GET list, GET by code).
- [ ] Treatment catalog endpoints (GET list, GET by code, GET by disability and progression level).
- [ ] Progression level endpoints (GET list).
- [ ] Patient-disability assignment endpoints (GET, POST assign, PUT update level).
- [ ] Patient-treatment visibility endpoints (GET, PUT toggle visibility).

### Phase 4: Ecosystem integration

- [ ] Game telemetry ingestion endpoint (POST, routes data to /data pipeline).
- [ ] Mobile-specific endpoints (patient dashboard data, treatment visibility, appointment booking).
- [ ] API documentation with Swagger/OpenAPI.
- [ ] Rate limiting and request validation.

---

## 6. DATABASE REFERENCE

This API shares the same PostgreSQL database as the desktop ERP. The schema is defined and migrated via Flyway. Refer to `/desktop/CLAUDE.md` section 8 for the current schema. All Flyway migration scripts in this project must be compatible with the existing desktop schema.

---

*This file is the single source of truth for the API domain. Update it as tasks are completed.*

## Memory

You have access to Engram persistent memory via MCP tools (mem_save, mem_search, mem_session_summary, etc.).
- Save proactively after significant work — don't wait to be asked.
- After any compaction or context reset, call `mem_context` to recover session state before continuing.
