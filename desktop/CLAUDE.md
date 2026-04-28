# CLAUDE.md - RehabiAPP Desktop (SGE)

> **File:** `/desktop/CLAUDE.md`
> **Agent:** Agent 3 (Desktop Developer)
> **Role:** Thinker (Opus) + Doer (Sonnet)

---

## 1. PROJECT DEFINITION

This directory contains the Desktop ERP (SGE - Sistema de Gestion de Expedientes) of RehabiAPP. JavaFX client used by healthcare practitioners (specialists and nurses) to manage patients, practitioners, appointments, disability-linked treatments organized by clinical progression levels, treatment-game associations, and to visualize patient rehabilitation progress charts (data sourced from MongoDB via the API).

El SGE consume la REST API central (`/api`) sin acceso directo a la base de datos. La conexion JDBC legacy fue eliminada en marzo-abril de 2026.

---

## 2. OPERATING RULES

1. **Global context:** Read root `/CLAUDE.md` before any cross-domain decision. Local file precedence for desktop-specific decisions.
2. **Skills are mandatory:** Read `.claude/skills/` before any architectural change. Skills override default behavior.
3. **Maintain this file:** Mark `[x]` when complete. Remove resolved items that no longer provide useful context.
4. **Testing requirement:** DAO/Service refactors require JUnit 5 + Mockito tests. Run `./gradlew test` before marking `[x]`.
5. **No inline styles:** ZERO `setStyle(...)` in Java/FXML. All styling lives in `tema_claro.css` / `tema_oscuro.css`.

---

## 3. LOCAL STACK

- Java 24, JavaFX 23 (FXML via SceneBuilder), CSS (light/dark themes).
- Conexion via REST API central (`/api` en `localhost:8080` por defecto, configurable via `api.properties` o `REHABIAPP_API_URL`).
- ControlsFX 11.x (visual field validation).
- JasperReports 6.20+ (PDF and HTML report generation).
- PDFBox 3.0+ (PDF preview/extraction in treatment editor — NEW dependency, see PLAN.md).
- Gradle (build system).

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
    |-- Clases/        Main, ApiClient, Paciente, Sanitario, Cita, SesionUsuario, Videojuego (NEW), ProgresoTratamiento (NEW)
    |-- Interface/     Controladores JavaFX (controladorSesion, controladorMenuPrincipal, controladorVentanaProgresoPaciente NEW, etc.)
    |-- DAO/           PacienteDAO, SanitarioDAO, CitaDAO, CatalogoDAO, ProgresoDAO (NEW), VideojuegoDAO (NEW)
    |-- service/       PacienteService, CatalogoService, ProgresoService (NEW), SyncProgresoService (NEW)
    |-- util/          CifradoUtil, VentanaUtil, AnimacionUtil, ValidacionUtil, PaginacionUtil, VentanaHelper, ConstantesApp, GraficoUtil (NEW)
    |-- excepcion/     RehabiAppException, ConexionException, ValidacionException, AutenticacionException, PermisoException

src/main/resources/
    |-- fxml/          FXML files (incluido VentanaProgresoPaciente.fxml NEW)
    |-- css/           tema_claro.css, tema_oscuro.css
    |-- config/        api.properties, preferencias.properties
    |-- imagenes/      Icons and images
```

---

## 5. ROLES AND PERMISSIONS (RBAC)

**Specialist (medico especialista):** Full CRUD on patients/practitioners. Appointments. Reports. Treatment-game association. Patient progress visualization.

**Nurse (enfermero):** Read-only patients. Appointments. NO practitioner management. NO treatment-game association. Patient progress visualization (read-only).

---

## 6. SECURITY RULES

- Passwords: BCrypt cost 12 (delegado a la API).
- AES-256-GCM (delegado a la API) — campos clinicos.
- Audit: Every CRUD + READ access logged in audit_log via API (immutable INSERT only).
- Soft delete only. 5-year retention (Ley 41/2002).

---

## 7. IMPLEMENTATION CHECKLIST

### Phase A — UI fixes (current iteration)

> Razones detalladas y diagnostico CSS en `desktop/PLAN.md` Phase A.

- [ ] A.1 Homologar botones de busqueda y "Anadir" en VentanaDiscapacidades.fxml y VentanaTratamientos.fxml para que sean visualmente identicos a los de VentanaSanitarios.fxml (mismas clases CSS, mismos iconos, misma jerarquia HBox).
- [ ] A.2 Anadir botones "Aceptar" y "Cancelar" en VentanaFiltroTratamientos.fxml siguiendo el patron de VentanaFiltroPacientes.fxml.
- [ ] A.3 Centrar todos los textos de ventanas emergentes (modales, alertas, dialogs) — anadir clase CSS global `.modal-texto-centrado` aplicada en VentanaUtil + actualizar todos los modales.
- [ ] A.4 Solucionar fondo blanco en tema oscuro al abrir VentanaAgregarPaciente y VentanaAgregarSanitario — auditoria CSS profunda (resolver problema de prioridad de selectores), documentar en `desktop/PLAN.md` causa raiz y solucion permanente.

### Phase B — Treatment PDF import (current iteration)

- [ ] B.1 Anadir FileChooser de PDF en VentanaAgregarTratamiento.fxml (boton "Importar PDF" + Label "Sin archivo" / nombre + tamano).
- [ ] B.2 Validacion: max 10 MB, MIME `application/pdf` (verificar magic bytes), rechazar otros tipos.
- [ ] B.3 Extender CatalogoDAO con `subirPdfTratamiento(codTrat, byte[], filename)` y `descargarPdfTratamiento(codTrat)` (multipart upload + download).
- [ ] B.4 En modo edicion mostrar PDF actual con boton "Reemplazar" y "Eliminar".
- [ ] B.5 Tests JUnit5 mock para subir/descargar PDF (MockWebServer si esta en build.gradle).

### Phase C — Treatment-Game association (current iteration)

- [ ] C.1 Crear `Videojuego.java` (record) en `Clases/`: id, codigo, nombre, descripcion, codDis, parteCuerpo, urlUnity, activo.
- [ ] C.2 Crear `VideojuegoDAO.java` con `listarPorDiscapacidad(codDis)`, `listarTodos()`.
- [ ] C.3 Extender CatalogoDAO con `vincularJuego(codTrat, idJuego)`, `desvincularJuego(codTrat, idJuego)`, `listarJuegosDeTratamiento(codTrat)`.
- [ ] C.4 En VentanaAgregarTratamiento.fxml anadir un TableView de juegos (filtrado automaticamente por la discapacidad seleccionada en el ComboBox) con columna CheckBox para multi-seleccion.
- [ ] C.5 Al guardar tratamiento, sincronizar las asociaciones (alta/baja diff con el estado original).
- [ ] C.6 RBAC: solo specialist puede vincular/desvincular juegos.

### Phase D — Patient progress visualization (current iteration)

- [ ] D.1 Crear `VentanaProgresoPaciente.fxml`: ScrollPane con un VBox que contiene un LineChart por cada (tratamiento, parteCuerpo) del paciente. Eje X = valor de la metrica, eje Y = fecha del registro (CategoryAxis con strings formateados o NumberAxis con epoch tick formatter).
- [ ] D.2 Crear `controladorVentanaProgresoPaciente.java` con carga asincrona via Task (no bloquear UI), placeholder mientras carga, mensaje claro si no hay datos.
- [ ] D.3 Anadir boton "Progreso" en la cabecera de VentanaListarPaciente.fxml (ficha del paciente, abierta con doble click) — abre VentanaProgresoPaciente del DNI actual.
- [ ] D.4 Anadir boton "Progreso" en VentanaPacientes.fxml junto al boton "Generar listado PDF". Si no hay paciente seleccionado: error con TipoMensaje.ADVERTENCIA `"Selecciona un paciente para ver su progreso."` (reutilizar VentanaUtil).
- [ ] D.5 Crear `ProgresoDAO.java` con `obtenerProgresoPaciente(dni)` y `obtenerMarkdownPaciente(dni)`. Llama a los nuevos endpoints del API (ver `api/PLAN.md`).
- [ ] D.6 Crear `ProgresoService.java` que envuelve ProgresoDAO con manejo de errores y caching local de 30 segundos.
- [ ] D.7 Crear `GraficoUtil.java` con factory de LineChart configurado segun los requisitos (estilo, axes, formateo de fechas, tooltips por punto).
- [ ] D.8 Tests JUnit5 mock de ProgresoService.

### Phase E — Background sync of new game sessions (current iteration)

- [ ] E.1 Crear `SyncProgresoService.java` que ejecuta `GET /api/pacientes/{dni}/progreso/check` al abrir VentanaProgresoPaciente y cada 30 segundos mientras la pestana este activa.
- [ ] E.2 Si la respuesta es `hasNewData=true`, refrescar automaticamente los charts y mostrar Toast `"Datos actualizados"` (reutilizar VentanaUtil).
- [ ] E.3 Manejo de fallo: si el API no responde, mantener los charts antiguos y mostrar indicador discreto `"Sin conexion — datos en cache"`.
- [ ] E.4 Detener el polling al cerrar la pestana (cleanup en `onCleanup` o equivalente).

### Phase F — Future integrations (parked)

- [ ] OpenAI API integration: analizar el archivo `.md` de progreso del paciente y emitir opinion clinica automatizada (requiere endpoint `/api/pacientes/{dni}/progreso/analisis-ia`).
- [ ] NFC scanner integration for Spanish health card reading (auto-fill patient forms).
- [ ] Activar y probar HTTPS hacia la API en produccion (AWS).

---

## 8. DATABASE SCHEMA REFERENCE

> Solo lectura. Cambios al schema se hacen via Flyway en `/api/src/main/resources/db/migration/`.

### Existentes (post V12)

```
sanitario, sanitario_agrega_sanitario, telefono_sanitario,
localidad, cp, direccion,
discapacidad, tratamiento, discapacidad_tratamiento,
paciente (con protesis BOOLEAN tras V11/V12),
telefono_paciente, cita, audit_log,
nivel_progresion, paciente_discapacidad, paciente_tratamiento,
[entidades]_audit (Envers)
```

### Cambios pendientes (V13 — ver `api/PLAN.md`)

```
videojuego (id_videojuego SERIAL PK, codigo UNIQUE, nombre, descripcion,
            cod_dis FK discapacidad, parte_cuerpo, url_unity, activo, fecha_creacion)

tratamiento_videojuego (cod_trat FK, id_videojuego FK -- composite PK)

tratamiento.archivo_pdf BYTEA NULL
tratamiento.nombre_archivo_pdf VARCHAR(255) NULL
tratamiento.tamano_pdf_bytes BIGINT NULL

paciente.archivo_progreso_md TEXT NULL          -- cache del MD generado por /data
paciente.progreso_md_actualizado_en TIMESTAMP   -- ultima actualizacion del MD
```

---

## 9. RUNBOOK LOCAL

### Stack completo (orden obligatorio)

1. **PostgreSQL + MongoDB**: `docker compose -f /home/alaslibres/DAM/RehabiAPP/infra/docker-compose.yml up postgres mongodb`
2. **API Spring Boot**: `cd api && set -a && source .env.local && set +a && ./mvnw spring-boot:run` — esperar `Started ApiApplication`.
3. **Data pipeline Spring Boot**: `cd data && ./mvnw spring-boot:run` — esperar puerto 8081.
4. **Desktop JavaFX**: `cd desktop && ./gradlew run`.

### Variables de entorno (`api/.env.local`, NO commitear)

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/rehabiapp
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=admin
SPRING_PROFILES_ACTIVE=local
DATA_PIPELINE_URL=http://localhost:8081
```

### Credenciales de prueba

| DNI | Contrasena | Rol |
|-----|------------|-----|
| ADMIN0000 | admin | SPECIALIST |
| 00000001R | medico1234 | SPECIALIST |
| 00000002W | enfermero1234 | NURSE |

### Health checks

```bash
docker exec rehabiapp-db psql -U admin -d rehabiapp -c "SELECT 1;"
curl http://localhost:8080/actuator/health        # API
curl http://localhost:8081/actuator/health        # Data
```

---

## Memory

You have access to Engram persistent memory via MCP tools (mem_save, mem_search, mem_session_summary, etc.).
- Save proactively after significant work — don't wait to be asked.
- After any compaction or context reset, call `mem_context` to recover session state before continuing.
