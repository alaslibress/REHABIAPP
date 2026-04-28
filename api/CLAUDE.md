# CLAUDE.md - RehabiAPP API (REST Backend)

> **File:** `/api/CLAUDE.md`
> **Agent:** Agent 1 (Backend and Data Engineer)
> **Role:** Thinker (Opus) + Doer (Sonnet)

---

## 1. PROJECT DEFINITION

Core RESTful API que conecta el ecosistema RehabiAPP. Hub central consumido por `/mobile` (via BFF), `/games` (Unity WebGL externos) y `/desktop`. Expone operaciones de datos, gestiona JWT + RBAC, y rutea telemetria de juegos al pipeline de datos `/data` (MongoDB).

**Arquitectura:** Clean Architecture en cuatro capas (Domain, Application, Infrastructure, Presentation). Sin logica de negocio en controladores.

---

## 2. OPERATING RULES

1. **Global context:** Leer raiz `/CLAUDE.md` antes de cualquier decision cross-domain. Local file precedente para decisiones API-especificas.
2. **Skills are mandatory:** Leer `.claude/skills/springboot4-postgresql/SKILL.md` antes de tocar JPA, Envers, encryption, o queries.
3. **Maintain this file:** Marcar `[x]` al completar. Eliminar items resueltos que no aporten contexto.
4. **Testing requirement:** Cada nuevo endpoint, servicio o repository requiere integration tests (Spring Boot Test) o unit tests (JUnit 5 + Mockito). Ejecutar `./mvnw test` antes de marcar `[x]`.
5. **No God Classes:** Controladores solo HTTP mapping. Logica en Application layer. NUNCA devolver `@Entity` directamente — usar DTOs MapStruct.
6. **Security by default:** BCrypt para passwords, AES-256-GCM para campos clinicos, JWT en endpoints autenticados, audit_log en CRUD + READ de pacientes.

---

## 3. LOCAL STACK

- Spring Boot 4.0.5, Java 24, Maven.
- Spring Data JPA + Hibernate 7 + Envers (auditoria).
- PostgreSQL 18 (driver 42.7.2).
- Flyway (migraciones versionadas).
- MapStruct 1.6 (entity-to-DTO mapping, `componentModel = "spring"`).
- Spring Security + jjwt 0.12 (JWT).
- Spring Boot Actuator + Micrometer Prometheus.

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

Sin dependencias circulares entre capas. Domain con cero imports de framework.

---

## 5. IMPLEMENTATION CHECKLIST

> Phases 1-3 (project setup, security, core CRUD) y bugfixes Envers/protesis YA completados. Eliminados de este checklist.

### Phase 4 — H2 test compatibility (current iteration)

- [ ] 4.1 Resolver fallo de tests con H2: `V11__fix_protesis_boolean.sql` usa PL/pgSQL `DO $$ ... $$` no soportado por H2. Crear `src/test/resources/db/migration/` con override H2-compatible (PostgreSQL Compatibility Mode + script H2-friendly) O configurar Flyway con `locations` distintos por perfil para excluir V11/V12 en tests y usar versiones H2-friendly.
- [ ] 4.2 Restablecer suite verde: `./mvnw test` debe pasar sin errores en `ApiApplicationTests` y `AuthControllerIT`.

### Phase 5 — Patient progress integration (current iteration)

> Endpoints consumidos por `/desktop` (visualizacion de progreso). Detalles en `api/PLAN.md` Phase 5.

- [ ] 5.1 Crear `ProgresoController` (presentation) con endpoints:
  - `GET /api/pacientes/{dni}/progreso/check?since=<Instant>` → `{ hasNewData: boolean, lastSessionAt: Instant, count: int }`. Llama a `/data` via `DataPipelineClient`.
  - `GET /api/pacientes/{dni}/progreso` → `List<ProgresoTratamientoResponse>`. Proxy a `/data` `GET /analytics/patient/{dni}/treatment-progress`.
  - `GET /api/pacientes/{dni}/progreso/markdown` (Content-Type: `text/markdown`). Proxy a `/data` + cache en `paciente.archivo_progreso_md`.
  - `POST /api/pacientes/{dni}/progreso/markdown/regenerar` → fuerza regeneracion en `/data`.
- [ ] 5.2 Crear `DataPipelineClient` (infrastructure) — `RestClient` configurado con URL `${rehabiapp.data.url:http://localhost:8081}` y timeout 5s.
- [ ] 5.3 RBAC: solo SPECIALIST y NURSE pueden leer progreso del paciente. NURSE no puede regenerar.
- [ ] 5.4 Audit: cada GET de progreso/markdown registra READ en audit_log (paciente, sanitario, timestamp).
- [ ] 5.5 Tests: `ProgresoControllerIT` con MockRestServiceServer simulando respuestas de `/data`.

### Phase 6 — Treatment-Game association (current iteration)

> Detalles en `api/PLAN.md` Phase 6.

- [ ] 6.1 Migracion Flyway `V13__videojuego_y_tratamiento_pdf.sql`:
  - Tabla `videojuego` (id_videojuego BIGSERIAL PK, codigo VARCHAR(50) UNIQUE, nombre, descripcion, cod_dis FK discapacidad, parte_cuerpo, url_unity, activo BOOLEAN DEFAULT TRUE, fecha_creacion).
  - Tabla `tratamiento_videojuego` (cod_trat FK, id_videojuego FK — composite PK).
  - Anadir columnas a tratamiento: `archivo_pdf BYTEA`, `nombre_archivo_pdf VARCHAR(255)`, `tamano_pdf_bytes BIGINT`.
  - Anadir columnas a paciente: `archivo_progreso_md TEXT`, `progreso_md_actualizado_en TIMESTAMP`.
- [ ] 6.2 Entidades JPA: `Videojuego` (`@Audited`), `TratamientoVideojuego` (composite key, `@Audited`).
- [ ] 6.3 Repositorios: `VideojuegoRepository.findByCodDis(String)`, `findByActivoTrue()`. `TratamientoVideojuegoRepository`.
- [ ] 6.4 DTOs y mappers (MapStruct): `VideojuegoRequest`, `VideojuegoResponse`.
- [ ] 6.5 `VideojuegoController`:
  - `GET /api/videojuegos` (lista todos los activos, paginado).
  - `GET /api/videojuegos/{id}`.
  - `GET /api/videojuegos/discapacidad/{codDis}`.
  - `POST /api/videojuegos` (crear, solo SPECIALIST).
  - `PUT /api/videojuegos/{id}`.
  - `DELETE /api/videojuegos/{id}` (soft delete: `activo=false`).
- [ ] 6.6 Endpoints en `CatalogoController` (asociacion):
  - `GET /api/tratamientos/{cod}/videojuegos`.
  - `POST /api/tratamientos/{cod}/videojuegos/{id}` (vincular).
  - `DELETE /api/tratamientos/{cod}/videojuegos/{id}` (desvincular).
- [ ] 6.7 Tests integration por endpoint.

### Phase 7 — Treatment PDF (current iteration)

- [ ] 7.1 Endpoints en `CatalogoController`:
  - `POST /api/tratamientos/{cod}/pdf` (multipart `file`, max 10MB, valida MIME). Verifica magic bytes `%PDF-`.
  - `GET /api/tratamientos/{cod}/pdf` (Content-Type: `application/pdf`, Content-Disposition: attachment).
  - `GET /api/tratamientos/{cod}/pdf/metadatos` → `{ nombre, tamano }`.
  - `DELETE /api/tratamientos/{cod}/pdf` (solo SPECIALIST).
- [ ] 7.2 Validacion: si `file.size > 10 * 1024 * 1024` → 413 Payload Too Large.
- [ ] 7.3 Audit: cada upload registra accion `UPDATE` en audit_log con detalle `"PDF: {nombre} ({tamano} bytes)"`.
- [ ] 7.4 Tests integration con `MockMultipartFile`.

### Phase 8 — Game telemetry routing (current iteration)

> Endpoint consumido por Unity WebGL (juegos externos en AWS).

- [ ] 8.1 `TelemetriaController`:
  - `POST /api/telemetria/sesion-juego` — recibe payload de Unity, valida JWT, enriquece con `disabilityId` desde la asignacion del paciente, reenvia a `/data` `POST /ingest/game-session`.
- [ ] 8.2 `TelemetriaService` con logica de enriquecimiento (lookup en `paciente_discapacidad` para inferir `disabilityId` si Unity no lo manda).
- [ ] 8.3 RBAC: tokens JWT con scope `GAMES_TELEMETRY` (un nuevo rol/scope).
- [ ] 8.4 Tras ingestion exitosa, disparar `RegenerarMdEvent` (Spring `ApplicationEvent`) que invoca `POST /data/analytics/patient/{dni}/markdown/regenerar` en background.
- [ ] 8.5 Tests con MockMvc + MockRestServiceServer.

### Phase 9 — Mobile dashboard endpoint (current iteration)

> Consumido por el BFF mobile (`/mobile/backend`).

- [ ] 9.1 `DashboardController`:
  - `GET /api/pacientes/{dni}/dashboard` — agregado: paciente + discapacidades con nivel actual + tratamientos visibles + juegos desbloqueados (basado en niveles + asociaciones tratamiento-juego) + ultima sesion de juego + proxima cita.
- [ ] 9.2 Respuesta unificada con DTO `DashboardResponse` (record con sub-records).
- [ ] 9.3 Audit: registrar READ del paciente.
- [ ] 9.4 Tests integration.

### Phase 10 — API documentation + rate limit (current iteration)

- [ ] 10.1 Anadir `springdoc-openapi-starter-webmvc-ui` a pom.xml. Verificar que `/swagger-ui.html` y `/v3/api-docs` se exponen correctamente.
- [ ] 10.2 Anotar todos los controllers con `@Tag` y endpoints con `@Operation`. DTOs con `@Schema`.
- [ ] 10.3 Excluir endpoints internos (`/internal/*`) de la documentacion publica.
- [ ] 10.4 Rate limit a nivel aplicacion: anadir Bucket4j (`com.bucket4j:bucket4j-core` + `bucket4j-spring-boot-starter`). Configurar limits:
  - `/api/auth/login`: 10 req/min por IP.
  - `/api/telemetria/*`: 60 req/min por JWT.
  - Resto: 300 req/min por JWT.
- [ ] 10.5 Filtro de validacion de tamano de payload — rechazar bodies > 1 MB excepto multipart upload de PDF.
- [ ] 10.6 Tests: 11 logins consecutivos en menos de 60s → respuestas 11 = 429 Too Many Requests.

---

## 6. DATABASE REFERENCE

> El schema lo define este modulo via Flyway. Las migraciones V1-V12 estan aplicadas. V13 es la nueva.

```
sanitario, sanitario_agrega_sanitario, telefono_sanitario,
localidad, cp, direccion,
discapacidad, tratamiento (+ archivo_pdf, nombre_archivo_pdf, tamano_pdf_bytes en V13),
discapacidad_tratamiento,
videojuego (V13), tratamiento_videojuego (V13),
paciente (+ archivo_progreso_md, progreso_md_actualizado_en en V13),
telefono_paciente, cita,
audit_log,
nivel_progresion, paciente_discapacidad, paciente_tratamiento,
[entidades]_audit (Envers).
```

---

## 7. RUNBOOK

```bash
# Local stack
docker compose -f infra/docker-compose.yml up postgres mongodb
cd api && set -a && source .env.local && set +a && ./mvnw spring-boot:run
cd data && ./mvnw spring-boot:run

# Health
curl http://localhost:8080/actuator/health     # API
curl http://localhost:8081/actuator/health     # Data

# Swagger (tras Phase 10)
open http://localhost:8080/swagger-ui.html
```

---

## Memory

You have access to Engram persistent memory via MCP tools (mem_save, mem_search, mem_session_summary, etc.).
- Save proactively after significant work — don't wait to be asked.
- After any compaction or context reset, call `mem_context` to recover session state before continuing.
