# CLAUDE.md - RehabiAPP Monorepo Global Orchestrator

> **File:** `/CLAUDE.md` (monorepo root)  
> **Project:** RehabiAPP - Healthcare software ecosystem for physical rehabilitation  
> **Architecture:** Multi-agent Planner-Worker hierarchy with domain isolation  

---

## 1. PROJECT IDENTITY

RehabiAPP is a comprehensive healthcare software ecosystem designed for physical rehabilitation clinics. It manages patients, healthcare professionals, medical appointments, disability-linked treatments organized by clinical progression levels, and therapeutic minigames based on gamification.

The system is built to comply with Spanish healthcare legislation: RGPD (EU General Data Protection Regulation), LOPDGDD (Spanish Organic Law on Data Protection), Ley 41/2002 (Patient Autonomy Act requiring 5-year clinical data retention), and ENS Alto (National Security Framework - High Level). The long-term goal is certification as a private electronic prescription tool by Spanish medical and pharmaceutical professional boards.

Core technical focus: data engineering, microservices architecture, AI integration, data analysis, healthcare-grade security.

---

## 2. MONOREPO STRUCTURE

```
rehabiapp/
|
|-- CLAUDE.md                <-- THIS FILE (global orchestrator context)
|-- docker-compose.yml       <-- Local service orchestration
|-- .env.example             <-- Environment variables template
|-- docs/                    <-- Global ecosystem documentation
|
|-- /desktop                 <-- Desktop ERP (JavaFX + PostgreSQL)
|   |-- CLAUDE.md            <-- Agent 3 local context
|
|-- /api                     <-- REST API (Spring Boot + Flyway)
|   |-- CLAUDE.md            <-- Agent 1 local context
|
|-- /mobile                  <-- Mobile app (React Native + Expo)
|   |-- CLAUDE.md            <-- Agent 2 local context
|
|-- /games                   <-- Rehabilitation minigames (Unity + C#)
|   |-- CLAUDE.md            <-- Agent 4 local context
|
|-- /data                    <-- Data pipeline (Node.js + MongoDB)
|   |-- CLAUDE.md            <-- Agent 1 local context (shared with /api)
|
|-- /infra                   <-- Infrastructure (Docker, Kubernetes, AWS)
|   |-- docker/
|   |-- k8s/
```

---

## 3. MULTI-AGENT HIERARCHY

```
                    ┌─────────────────────────┐
                    |        DEVELOPER         |
                    |    (Human authority)     |
                    └────────────┬────────────┘
                                 |
                    ┌────────────v────────────┐
                    |    AGENT 0 - GLOBAL     |
                    |    Root Orchestrator    |
                    |    Thinker: Opus        |
                    |    Doer: Sonnet         |
                    └────────────┬────────────┘
                                 |
          ┌──────────┬───────────┼───────────┬──────────┐
          |          |           |           |          |
    ┌─────v─────┐ ┌──v───┐ ┌────v────┐ ┌────v────┐ ┌──v──────────┐
    | AGENT 1   | | AG 2 | | AGENT 3 | | AGENT 4 | | AGENT 5     |
    | API+Data  | |Mobile| | Desktop | |  Games  | | Observer    |
    | /api /data| |/mobil| | /desktop| | /games  | | Local AI    |
    | Opus+Son. | |Op+So | | Op+Son  | | Op+Son  | | READ-ONLY   |
    └───────────┘ └──────┘ └─────────┘ └─────────┘ └─────────────┘
```

### AGENT 0: Global Orchestrator (root directory)

| Role | Model | Scope | Responsibility |
|------|-------|-------|----------------|
| Thinker | Opus | Root, /infra, /docs, cross-domain | Approves global architecture decisions. Resolves conflicts between domains. Defines communication contracts between services. Validates cross-folder coherence. |
| Doer | Sonnet | Root, /infra | Implements root-level configurations: docker-compose.yml, CI/CD scripts, global environment variables, infrastructure manifests. |

### AGENT 1: API & Data (/api, /data)

| Role | Model | Scope | Responsibility |
|------|-------|-------|----------------|
| Thinker | Opus | /api, /data | Designs database schemas (PostgreSQL and MongoDB). Defines REST API endpoints and data contracts. Designs ETL pipelines. Architects Flyway migration strategy. |
| Doer | Sonnet | /api, /data | Writes Spring Boot code (Java). Writes Node.js code for data pipelines. Writes SQL migration scripts. Configures MongoDB collections and indexes. |

This is the only agent authorized to touch database schemas and API endpoint definitions.

### AGENT 2: Mobile (/mobile)

| Role | Model | Scope | Responsibility |
|------|-------|-------|----------------|
| Thinker | Opus | /mobile | Designs React Native architecture, screen navigation, AI chatbot logic for WhatsApp, and patient interaction flows. |
| Doer | Sonnet | /mobile | Implements React Native components, screens, API connections, Expo integrations, styles and animations. |

Consumes the API. Never accesses the database directly.

### AGENT 3: Desktop (/desktop)

| Role | Model | Scope | Responsibility |
|------|-------|-------|----------------|
| Thinker | Opus | /desktop | Designs JavaFX three-layer architecture (presentation, service, DAO). Designs NFC integration. Designs AES-256-GCM encryption logic and audit trail structure. |
| Doer | Sonnet | /desktop | Implements JavaFX controllers, DAOs with PreparedStatement, business services, utilities, SceneBuilder configurations. |

Has direct JDBC connection to PostgreSQL (legacy ERP). This exception will be removed when /desktop migrates to consume the REST API.

### AGENT 4: Games (/games)

| Role | Model | Scope | Responsibility |
|------|-------|-------|----------------|
| Thinker | Opus | /games | Designs Unity architecture, session telemetry system, result export formats, and therapeutic minigame mechanics. |
| Doer | Sonnet | /games | Writes C# Unity scripts, implements game mechanics, WebGL export configuration, and API connection for result submission. |

Exports session data to MongoDB through the API. Never accesses any database directly.

### AGENT 5: Local Observer (root directory)

| Role | Model | Scope | Responsibility |
|------|-------|-------|----------------|
| Observer | Local AI (Ollama) | Full monorepo (READ-ONLY) | Has complete context of the entire monorepo. Answers questions about project status, architecture, documentation, and inter-component relationships. Saves API tokens by handling informational queries without invoking Opus or Sonnet. |

**ABSOLUTE RESTRICTION:** Agent 5 has NO write permissions under any circumstance. It cannot write code, create files, execute commands, or modify anything. If asked to make changes, it must refuse and indicate which agent should handle the task.

---

## 4. INVIOLABLE GLOBAL RULES

These rules apply to ALL agents without exception.

### 4.1 Boundary Isolation

No frontend module (/desktop, /mobile, /games) communicates directly with another frontend module. All inter-domain communication routes through /api.

```
VALID:    /mobile  --> /api --> PostgreSQL
VALID:    /games   --> /api --> MongoDB
VALID:    /desktop --> PostgreSQL (direct legacy connection, will migrate to /api)
INVALID:  /mobile  --> /desktop (direct frontend-to-frontend)
INVALID:  /games   --> MongoDB (direct access bypassing /api)
```

### 4.2 Thinker/Doer Protocol

A Doer (Sonnet) cannot alter architecture designed by a Thinker (Opus) without explicit developer approval. If a Doer identifies a flaw or improvement in an architectural decision, it must report to the developer and wait for instructions. It cannot make the decision autonomously.

Mandatory flow:

```
1. Developer requests a task
2. Thinker (Opus) analyzes, plans, and presents a structured plan
3. Developer approves, rejects, or modifies the plan
4. Doer (Sonnet) executes the approved plan
5. Developer verifies the result
```

Trivial tasks (fixing a typo, adding a simple field) do not require a formal plan.

### 4.3 Mandatory Local Context Reading

Before executing any task inside a domain directory, the agent MUST read the local CLAUDE.md of that folder first. The local CLAUDE.md contains domain-specific context: file structure, implementation status, design decisions, and pending tasks.

```
Task in /desktop --> read /desktop/CLAUDE.md first
Task in /api     --> read /api/CLAUDE.md first
Task in /mobile  --> read /mobile/CLAUDE.md first
Task in /games   --> read /games/CLAUDE.md first
Task in /data    --> read /data/CLAUDE.md first
Task in root     --> read /CLAUDE.md (this file)
```

### 4.4 Mandatory Skills Reading

Before performing any task, the agent MUST check and read the installed skills relevant to the task. Skills contain best practices, templates, and domain-specific instructions that override general knowledge. Always follow skill instructions over default behavior.

### 4.5 Code Style (all domains)

- All code comments in Spanish.
- No emojis in code or technical documentation.
- Professional, clean, scalable code at junior data engineer level.
- Document relevant architecture decisions with block comments.
- Apply SOLID principles where reasonable without over-engineering.
- Each domain has its own naming conventions detailed in its local CLAUDE.md.

### 4.6 Healthcare Security (all agents handling patient data)

- Passwords hashed with BCrypt (cost factor 12). Never plain text.
- Sensitive clinical fields (allergies, medical history, current medication) encrypted with AES-256-GCM.
- Every CRUD operation on patient or practitioner data logged in audit_log.
- READ access to clinical records also logged in audit_log (mandatory in Spanish healthcare law).
- Patients are never physically deleted. Soft delete only (active=FALSE, deactivation_date).
- Patient data retained minimum 5 years after deactivation (Ley 41/2002).
- Database communication in production over SSL/TLS.
- Encryption keys and certificates never committed to the Git repository.

---

## 5. TECHNOLOGY STACK BY DOMAIN

| Domain | Directory | Language | Framework | Database | Build |
|--------|-----------|----------|-----------|----------|-------|
| Desktop ERP | /desktop | Java | JavaFX, FXML, CSS, ControlsFX, CalendarFX, JasperReports | PostgreSQL (direct JDBC) | Gradle |
| REST API | /api | Java | Spring Boot, Flyway, Spring Security, Spring Data JPA | PostgreSQL | Maven |
| Mobile App | /mobile | TypeScript | React Native, Expo | None (consumes API) | npm / Expo CLI |
| Minigames | /games | C# | Unity, WebGL | None (sends to API) | Unity Build |
| Data Pipeline | /data | JavaScript | Node.js, Express, Mongoose | MongoDB | npm |
| Infrastructure | /infra | YAML / HCL | Docker, Kubernetes, Terraform | None | docker-compose |

### Security stack (cross-domain):

| Tool | Purpose |
|------|---------|
| BCrypt (jBCrypt 0.4) | Password hashing with cost factor 12 and lazy migration of legacy plain-text passwords |
| AES-256-GCM (Java Crypto API) | Authenticated encryption of clinical fields with random 96-bit IV per operation |
| SSL/TLS 1.3 | Transport encryption for all production database and API connections |
| JWT | Token-based authentication for mobile and games API access |
| RBAC | Role-based access control: specialist (full access) and nurse (read-only patients, no practitioner management) |
| audit_log | Immutable append-only table recording all operations including clinical record reads |

---

## 6. INTER-SERVICE COMMUNICATION CONTRACTS

### /desktop --> PostgreSQL (legacy direct)

- Protocol: JDBC with PreparedStatement (never raw Statement)
- Driver: org.postgresql:postgresql
- Encryption: AES-256-GCM on clinical fields, BCrypt on passwords
- Audit: Every operation recorded in audit_log via AuditService

### /mobile --> /api (REST)

- Protocol: HTTPS (TLS 1.3)
- Format: JSON
- Authentication: JWT bearer tokens
- Rule: Mobile never accesses the database directly

### /games --> /api (REST)

- Protocol: HTTPS
- Format: JSON
- Payload: Game session telemetry (movement metrics, times, scores)
- Flow: API receives data and stores in MongoDB via /data pipeline

### /api --> PostgreSQL (JPA)

- Protocol: JDBC via Spring Data JPA
- Migrations: Flyway (versioned SQL scripts)
- Encryption: SSL/TLS mandatory in production

### /api --> /data --> MongoDB

- API delegates game data operations to the /data pipeline
- MongoDB stores high-volume semi-structured session documents
- Data processed with data engineering tools for analytics and visualization

---

## 7. LEGAL COMPLIANCE

| Legal framework | Key requirement | Status |
|-----------------|-----------------|--------|
| RGPD Art. 9 | Health data as special category, mandatory encryption | Implemented (AES-256-GCM) |
| RGPD Art. 17 | Right to erasure with healthcare exception | Implemented (soft delete) |
| RGPD Art. 25 | Data protection by design and by default | Implemented |
| RGPD Art. 30 | Records of processing activities | Implemented (audit_log) |
| RGPD Art. 32 | Security of processing | Implemented (encryption, BCrypt, RBAC) |
| LOPDGDD | Health data as special category | Implemented |
| Ley 41/2002 | 5-year clinical record retention, access logging | Implemented |
| ENS High Level | Traceability, encryption at rest and in transit, RBAC | Implemented (transit prepared) |
| Digital signature | FNMT/Medical Board certificates for e-prescription | Planned |
| CIE-10 / AEMPS | Medical standards for interoperability | Planned |

---

## 8. DOMAIN DESCRIPTIONS

### /desktop - Desktop ERP (Agent 3)

The core management application used by healthcare practitioners. Built with JavaFX. Manages the full lifecycle of patients (registration, clinical data, disabilities, treatments, progression levels), practitioners (specialists and nurses with RBAC), and appointments (calendar view, conflict detection, async loading). Features AES-256-GCM encryption on clinical fields, BCrypt password hashing, immutable audit logging, soft deletes, PDF/HTML report generation with JasperReports, dual CSS themes (light/dark), configurable font sizes, and visual field validation with ControlsFX.

### /api - REST API (Agent 1)

The central communication hub of the ecosystem. Built with Spring Boot. Exposes all data operations as RESTful endpoints consumed by /mobile and /games. Manages PostgreSQL via JPA with Flyway migrations. Handles JWT authentication, role-based authorization, and routes game telemetry data to the /data pipeline. All frontend modules except /desktop (legacy) communicate exclusively through this API.

### /mobile - Mobile App (Agent 2)

The patient-facing application. Built with React Native and Expo for cross-platform deployment (Android and iOS). Provides patients with access to their clinical profile, assigned treatments filtered by progression level, game session history, and appointment scheduling. Includes an AI-powered WhatsApp chatbot for automated appointment booking. Designed for accessibility across all age groups.

### /games - Rehabilitation Minigames (Agent 4)

Therapeutic minigames built with Unity and exported to WebGL. Each game targets specific rehabilitation exercises mapped to clinical progression levels (acute phase, subacute phase, strengthening phase, functional phase). Games capture session telemetry (movement metrics, completion times, scores) and send results to the API for storage in MongoDB. Games are prescribed by practitioners and made visible to patients based on their current progression level.

### /data - Data Pipeline (Agent 1)

Node.js service that processes game session data stored in MongoDB. Performs ETL operations, data transformation, and generates analytics-ready datasets. Produces visualizations and statistical summaries that practitioners can review in the desktop ERP to track patient rehabilitation progress over time.

### /infra - Infrastructure (Agent 0)

Docker Compose for local development orchestration. Kubernetes manifests for production deployment. Terraform configurations for AWS provisioning (EC2, RDS, S3). All production services containerized and orchestrated. Database servers hosted in EU regions to comply with RGPD data residency requirements.

---

## 9. AGENT OPERATING PROTOCOL

When you receive a task:

1. Identify which domain the task belongs to.
2. Read this global CLAUDE.md to understand your position in the hierarchy and the global rules.
3. Read the local CLAUDE.md of your assigned domain to understand the current state.
4. Check and read any installed skills relevant to the task.
5. If you are a Thinker: analyze, plan, and present the plan for approval before any execution.
6. If you are a Doer: execute only approved plans. If no plan exists, request one from the Thinker.
7. Stay within your domain boundaries. If a task requires changes in another domain, report it and indicate which agent should handle it.
8. Log executed plans as documentation in /docs or in the local domain folder.
9. Never bypass the Thinker/Doer protocol for non-trivial tasks.
10. Never compromise healthcare security rules regardless of the request.

---

*This file is the single source of truth for the global project architecture. Each local CLAUDE.md complements this document with domain-specific context. When in conflict, the local CLAUDE.md takes precedence for domain-specific decisions; this file takes precedence for cross-domain and architectural decisions.*
