# CLAUDE.md - RehabiAPP Monorepo Global Orchestrator

> **File:** `/CLAUDE.md` (monorepo root)
> **Project:** RehabiAPP - Healthcare software ecosystem for physical rehabilitation
> **Architecture:** Multi-agent Planner-Worker hierarchy with domain isolation and AI-driven QA

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
|-- /api                     <-- Central REST API (Spring Boot + Flyway)
|   |-- CLAUDE.md            <-- Agent 1 local context
|
|-- /mobile
|   |-- /frontend            <-- Patient mobile app (React Native + Expo)
|   |   |-- CLAUDE.md        <-- Agent 2 local context (frontend)
|   |-- /backend             <-- Mobile BFF (Node.js)
|   |   |-- CLAUDE.md        <-- Agent 2 local context (backend)
|
|-- /data                    <-- Data pipeline (Node.js + MongoDB)
|   |-- CLAUDE.md            <-- Agent 1 local context
|
|-- /infra                   <-- Infrastructure (Docker, Kubernetes, AWS)
|   |-- docker/
|   |-- k8s/
```

**External systems (hosted on AWS, not in this repository):**

```
AWS Cloud
|
|-- Unity WebGL Games        <-- Therapeutic minigames (S3/CloudFront)
|   Communicates with /api via HTTPS REST endpoints.
|   Not stored in this monorepo due to repository size constraints.
```

---

## 3. MULTI-AGENT HIERARCHY

```
                    ┌─────────────────────────┐
                    |        DEVELOPER         |
                    |    (Human authority)      |
                    └────────────┬────────────┘
                                 |
                    ┌────────────v────────────┐
                    |    AGENT 0 - GLOBAL     |
                    |    Root Orchestrator     |
                    |    Thinker: Opus         |
                    |    Doer: Sonnet          |
                    └────────────┬────────────┘
                                 |
          ┌──────────┬───────────┼───────────┬──────────┐
          |          |           |           |          |
    ┌─────v─────┐ ┌──v──────┐ ┌─v────────┐ ┌v────────┐ ┌v────────────┐
    | AGENT 1   | | AGENT 2 | | AGENT 3  | | AGENT 4 | | AGENT 5     |
    | API+Data  | | Mobile  | | Desktop  | | Games   | | Observer    |
    | /api /data| | /mobile | | /desktop | | External| | Qwen Local  |
    | Opus+Son. | | Op+Son  | | Op+Son   | | Op+Son  | | READ-ONLY   |
    └───────────┘ └─────────┘ └──────────┘ └─────────┘ └─────────────┘
```

### AGENT 0: Global Orchestrator (root directory)

| Role | Model | Scope | Responsibility |
|------|-------|-------|----------------|
| Thinker | Opus | Root, /infra, /docs, cross-domain | Approves global architecture decisions. Resolves conflicts between domains. Defines communication contracts between services. Validates cross-folder coherence. |
| Doer | Sonnet | Root, /infra | Implements root-level configurations: docker-compose.yml, CI/CD scripts, global environment variables, infrastructure manifests. |

### AGENT 1: API and Data (/api, /data)

| Role | Model | Scope | Responsibility |
|------|-------|-------|----------------|
| Thinker | Opus | /api, /data | Designs database schemas (PostgreSQL and MongoDB). Defines REST API endpoints and data contracts. Designs ETL pipelines. Architects Flyway migration strategy. |
| Doer | Sonnet | /api, /data | Writes Spring Boot code (Java). Writes Node.js code for data pipelines. Writes SQL migration scripts. Configures MongoDB collections and indexes. |

This is the only agent authorized to define database schemas and API endpoint contracts.

### AGENT 2: Mobile (/mobile/frontend, /mobile/backend)

| Role | Model | Scope | Responsibility |
|------|-------|-------|----------------|
| Thinker | Opus | /mobile/frontend, /mobile/backend | Designs React Native architecture, screen navigation, Node.js BFF logic, AI chatbot flows, and patient interaction patterns. |
| Doer | Sonnet | /mobile/frontend, /mobile/backend | Implements React Native components, screens, Expo integrations, Node.js backend routes, and API connections. |

The mobile frontend NEVER communicates with the central API (/api) directly. All traffic from /mobile/frontend routes through /mobile/backend (BFF pattern), which then calls /api.

### AGENT 3: Desktop (/desktop)

| Role | Model | Scope | Responsibility |
|------|-------|-------|----------------|
| Thinker | Opus | /desktop | Designs JavaFX three-layer architecture (presentation, service, DAO). Designs NFC integration. Designs AES-256-GCM encryption logic and audit trail structure. |
| Doer | Sonnet | /desktop | Implements JavaFX controllers, DAOs with PreparedStatement, business services, utilities, SceneBuilder configurations. |

Has direct JDBC connection to PostgreSQL (legacy ERP). This exception will be removed when /desktop migrates to consume the REST API.

### AGENT 4: Games (external, AWS Cloud)

| Role | Model | Scope | Responsibility |
|------|-------|-------|----------------|
| Thinker | Opus | External Unity project | Designs Unity architecture, session telemetry system, result export formats, and therapeutic minigame mechanics. |
| Doer | Sonnet | External Unity project | Writes C# Unity scripts, implements game mechanics, WebGL export configuration, and API connection for result submission. |

Games are hosted externally on AWS (S3/CloudFront). They communicate with /api via HTTPS REST endpoints. The Unity project is NOT stored in this monorepo due to repository size constraints. Agent 4 operates on the external Unity repository when needed.

### AGENT 5: Local Observer (root directory, local AI)

| Role | Model | Scope | Responsibility |
|------|-------|-------|----------------|
| Observer | Ollama running **Qwen 2.5 Coder** | Full monorepo (READ-ONLY) | Answers developer questions about project status, architecture, documentation, and inter-component relationships. Saves API tokens by handling informational queries locally without invoking Opus or Sonnet. |

**ABSOLUTE RESTRICTION:** Agent 5 has NO write permissions under any circumstance. It cannot write code, create files, execute commands, or modify anything. If asked to make changes, it must refuse and indicate which agent should handle the task.

**Context loading protocol (RAG via Repomix):**

Agent 5 cannot hold the entire monorepo in its context window simultaneously. To avoid overflowing the context window and to keep the working disk clean, context is generated dynamically by isolating a single domain at a time and storing the packed output in the Linux temporary directory.

Mandatory commands to interrogate Agent 5:

```bash
# Step 1: Pack the target domain into a single context file
repomix [domain]/ --output /tmp/[domain]-context.xml

# Step 2: Pipe the context into Ollama with your query
cat /tmp/[domain]-context.xml | ollama run qwen2.5-coder "Your query here"
```

Examples:

```bash
repomix desktop/ --output /tmp/desktop-context.xml
cat /tmp/desktop-context.xml | ollama run qwen2.5-coder "What DAOs exist and what exceptions do they throw?"

repomix api/ --output /tmp/api-context.xml
cat /tmp/api-context.xml | ollama run qwen2.5-coder "List all REST endpoints and their HTTP methods"

repomix data/ --output /tmp/data-context.xml
cat /tmp/data-context.xml | ollama run qwen2.5-coder "Describe the MongoDB aggregation pipelines"
```

The /tmp/ files are ephemeral and cleaned automatically by the OS. Do not store context files anywhere else.

---

## 4. INVIOLABLE GLOBAL RULES

These rules apply to ALL agents without exception.

### 4.1 Boundary Isolation

No frontend module communicates directly with another frontend module or with a database it does not own. All inter-domain communication routes through the appropriate backend layer.

```
VALID:    /mobile/frontend --> /mobile/backend --> /api --> PostgreSQL
VALID:    Unity Games (external) --> /api --> /data --> MongoDB
VALID:    /desktop --> PostgreSQL (direct legacy connection, will migrate to /api)
INVALID:  /mobile/frontend --> /api (bypassing mobile backend)
INVALID:  /mobile/frontend --> /desktop (direct frontend-to-frontend)
INVALID:  Unity Games --> MongoDB (direct access bypassing /api)
```

### 4.2 Thinker/Doer Protocol

A Doer (Sonnet) cannot alter architecture designed by a Thinker (Opus) without explicit developer approval.

Mandatory flow:

```
1. Developer requests a task
2. Thinker (Opus) analyzes, plans, and presents a structured plan
3. Developer approves, rejects, or modifies the plan
4. Doer (Sonnet) executes the approved plan
5. Doer delegates verification to TestSprite MCP (see Section 10)
6. Task marked complete only after TestSprite returns 100% success
```

Trivial tasks (fixing a typo, adding a simple field) do not require a formal plan but still require TestSprite verification if they touch code.

### 4.3 Mandatory Local Context Reading

Before executing any task inside a domain directory, the agent MUST read the local CLAUDE.md of that folder first.

```
Task in /desktop          --> read /desktop/CLAUDE.md
Task in /api              --> read /api/CLAUDE.md
Task in /mobile/frontend  --> read /mobile/frontend/CLAUDE.md
Task in /mobile/backend   --> read /mobile/backend/CLAUDE.md
Task in /data             --> read /data/CLAUDE.md
Task in root              --> read /CLAUDE.md (this file)
```

### 4.4 Mandatory Skills Reading

Before performing any task, the agent MUST check and read the installed skills relevant to the task. Skills are located in `.claude/skills/` within each domain directory. Skills override general knowledge and default behavior.

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
- READ access to clinical records also logged in audit_log.
- Patients are never physically deleted. Soft delete only (active=FALSE, deactivation_date).
- Patient data retained minimum 5 years after deactivation (Ley 41/2002).
- Database communication in production over SSL/TLS.
- Encryption keys and certificates never committed to the Git repository.
- Nueva columna `paciente.progreso_md TEXT` + `paciente.ultima_sync_progreso TIMESTAMP` (V15) para materializar el resumen de progreso clinico consumido por la futura IA.

---

## 5. TECHNOLOGY STACK BY DOMAIN

| Domain | Directory | Language | Framework | Database | Build |
|--------|-----------|----------|-----------|----------|-------|
| Desktop ERP | /desktop | Java | JavaFX, FXML, CSS, ControlsFX, CalendarFX, JasperReports | PostgreSQL (direct JDBC) | Gradle |
| Central API | /api | Java | Spring Boot 3, Flyway, Spring Security, Spring Data JPA | PostgreSQL | Maven |
| Mobile Frontend | /mobile/frontend | TypeScript | React Native, Expo | None (calls mobile backend) | npm / Expo CLI |
| Mobile Backend (BFF) | /mobile/backend | JavaScript | Node.js, Express | None (calls central API) | npm |
| Data Pipeline | /data | JavaScript | Node.js, Express, Mongoose | MongoDB | npm |
| Games | External (AWS) | C# | Unity, WebGL | None (calls central API) | Unity Build |
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

### AI stack:

| Tool | Purpose |
|------|---------|
| Ollama + Qwen 2.5 Coder | Local read-only observer (Agent 5) for project queries without API token consumption |
| OpenAI API | Integrated in desktop ERP for automated clinical text processing and chart interpretation |
| TestSprite MCP | Automated QA, test generation, and self-healing verification for all Doer agents |

---

## 6. INTER-SERVICE COMMUNICATION CONTRACTS

### /desktop --> PostgreSQL (legacy direct)

- Protocol: JDBC with PreparedStatement
- Encryption: AES-256-GCM on clinical fields, BCrypt on passwords
- Audit: Every operation recorded in audit_log via AuditService

### /mobile/frontend --> /mobile/backend --> /api (BFF pattern)

- /mobile/frontend --> /mobile/backend: HTTPS, JSON, session management
- /mobile/backend --> /api: HTTPS, JSON, JWT bearer tokens
- Rule: Mobile frontend NEVER calls /api directly

### Unity Games (external) --> /api (REST)

- Protocol: HTTPS, JSON
- Payload: Game session telemetry (movement metrics, times, scores)
- Flow: Games POST to /api, which routes to /data for MongoDB storage

### /api --> PostgreSQL (JPA)

- Protocol: JDBC via Spring Data JPA
- Migrations: Flyway (versioned SQL scripts)
- Encryption: SSL/TLS mandatory in production

### /api --> /data --> MongoDB

- API delegates game data operations to /data pipeline
- MongoDB stores high-volume semi-structured session documents
- Data processed with aggregation pipelines for analytics

### Desktop <--> /api (NEW - Sprint Progreso 2026-04-27)

- Polling de progreso: el escritorio consulta `GET /api/pacientes/{dni}/progreso/check?desde=ISO_TS` y, si hay nuevos registros de juego, ejecuta `POST /api/pacientes/{dni}/progreso/sync` para regenerar `paciente.progreso_md` y actualizar `ultima_sync_progreso`.
- Subida de PDF de tratamiento: el escritorio sube el documento clinico vía `POST /api/catalogo/tratamientos/{cod}/documentos` (multipart). La app mobile descarga ese mismo binario reutilizando el endpoint existente `GET /api/pacientes/{dni}/tratamientos/{cod}/documento`.

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

Core management application for healthcare practitioners. JavaFX. Full patient/practitioner/appointment lifecycle, disability-treatment-progression management, AES-256-GCM encryption, BCrypt hashing, immutable audit logging, soft deletes, JasperReports, dual CSS themes, ControlsFX validation.

### /api - Central REST API (Agent 1)

Central communication hub. Spring Boot. Exposes all data operations as RESTful endpoints consumed by /mobile/backend and external Unity games. PostgreSQL via JPA with Flyway migrations. JWT authentication, RBAC, telemetry routing to /data.

### /mobile/frontend - Patient Mobile App (Agent 2)

Patient-facing application. React Native + Expo. Clinical profile, treatments by progression level, game history, appointment scheduling. Communicates exclusively with /mobile/backend.

### /mobile/backend - Mobile BFF (Agent 2)

Backend-for-Frontend. Node.js + Express. Session management, request aggregation, data transformation for mobile client needs. All requests from /mobile/frontend pass through here before reaching /api.

### /data - Data Pipeline (Agent 1)

Node.js service for game telemetry ingestion and processing. MongoDB storage. ETL operations, aggregation pipelines, analytics-ready datasets for practitioner visualization.

### External: Unity Games (Agent 4)

Therapeutic minigames. Unity + WebGL. Hosted on AWS S3/CloudFront. Session telemetry POST to /api. Not in this monorepo.

### /infra - Infrastructure (Agent 0)

Docker Compose (local), Kubernetes (production), Terraform (AWS provisioning). EU-hosted for RGPD compliance.

---

## 9. AGENT OPERATING PROTOCOL

When you receive a task:

1. Identify which domain the task belongs to.
2. Read this global CLAUDE.md for hierarchy and global rules.
3. Read the local CLAUDE.md of your assigned domain for current state.
4. Check and read installed skills in `.claude/skills/`.
5. If Thinker: analyze, plan, present for approval.
6. If Doer: execute only approved plans.
7. Stay within domain boundaries. Report cross-domain needs.
8. After implementation, delegate verification to TestSprite MCP.
9. Mark task complete only after TestSprite returns 100% success.
10. Never bypass Thinker/Doer protocol for non-trivial tasks.
11. Never compromise healthcare security rules.

---

## 10. QA AND AI SELF-HEALING PROTOCOL (VIA TESTSPRITE MCP)

This section defines the mandatory quality assurance protocol for ALL Doer agents (Sonnet) across every domain. This is the most critical operational rule after healthcare security.

### 10.1 TestSprite MCP Integration

All Doer agents are equipped with the TestSprite MCP server. TestSprite provides automated test generation, execution, vulnerability scanning, and HTTP contract validation in a sandboxed environment. It is the final gate before any task can be marked as complete.

### 10.2 Mandatory Testing Loop

Before finalizing any implementation, refactoring, or code change, the Doer agent MUST execute:

```
1. Doer completes the implementation
2. Doer delegates verification to TestSprite MCP
3. TestSprite runs in its sandbox:
   - Unit tests for modified classes/functions
   - Integration tests for modified endpoints or database operations
   - Vulnerability scan on security-sensitive changes
   - HTTP contract validation for new or modified API endpoints
4. TestSprite returns results
5. If 100% success --> mark task complete
6. If any failure --> enter Self-Healing Protocol (10.3)
```

### 10.3 Self-Healing Protocol

If TestSprite detects a failure, vulnerability, or HTTP contract violation, the Doer agent is FORBIDDEN from:

- Stopping execution and waiting for the developer.
- Marking the task as complete or partially complete.
- Ignoring the failure and moving to the next task.

The Doer agent MUST autonomously:

```
1. Read the full error trace returned by TestSprite MCP
2. Identify the root cause of the failure
3. Correct the code that caused the failure
4. Request TestSprite to re-run the verification
5. Repeat steps 1-4 until TestSprite returns 100% success
```

The developer is only notified if the agent exhausts 5 consecutive failed attempts on the same issue and genuinely cannot resolve the problem.

### 10.4 Pass Condition

A task is only marked as "Done" (changing `[ ]` to `[x]` in the local CLAUDE.md checklist) when TestSprite returns a 100% success rate on all applicable test categories for that task. No exceptions.

---

*This file is the single source of truth for the global project architecture. Each local CLAUDE.md complements this document with domain-specific context. When in conflict, the local CLAUDE.md takes precedence for domain-specific decisions; this file takes precedence for cross-domain and architectural decisions.*

## Memory

You have access to Engram persistent memory via MCP tools (mem_save, mem_search, mem_session_summary, etc.).
- Save proactively after significant work — don't wait to be asked.
- After any compaction or context reset, call `mem_context` to recover session state before continuing.
