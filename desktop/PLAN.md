# IMPLEMENTATION PLAN — Bugfixes + CSS Visual Enhancement

> Date: 2026-04-15
> Branch: desktop-final
> Author: Agent 3 Thinker (Opus) — PRESCRIPTIVE. Doer (Sonnet) MUST follow step by step without re-architecting.
> Language: All code comments inside Java/FXML/CSS MUST be in Spanish (root `CLAUDE.md` §4.5). This plan itself is English.
> Scope:
>   Phase A — Two bugfixes (DNI filter on "Ver ficha paciente" + misleading "conexion BD" error on register)
>   Phase B — 12 CSS visual enhancement checklist items from `desktop/CLAUDE.md` §7

---

## 0. CONTEXT THE DOER MUST READ FIRST (MANDATORY)

Before touching code, open and read:

1. `/CLAUDE.md` — §4.5 (style), §4.6 (security), §10 (TestSprite loop).
2. `desktop/CLAUDE.md` — §2, §3, §4, §7 checklist pending items.
3. `desktop/.claude/skills/javafx-java24/` — apply all rules.
4. `desktop/src/main/java/com/javafx/Interface/controladorVentanaPrincipal.java` lines 230-470 (cargarPestania, cargarPestaniaCitasConFiltro, busquedaRapida) and lines 597-650 (abrirFichaPacienteDesdeCita, limpiarCachePestania).
5. `desktop/src/main/java/com/javafx/Interface/controladorVentanaPacientes.java` lines 190-230 (busqueda logic), lines 440-520 (CRUD entry points).
6. `desktop/src/main/java/com/javafx/Interface/controladorVentanaCitas.java` — how §4.6 `verFichaPaciente` calls `abrirFichaPacienteDesdeCita(dniPac)`.
7. `desktop/src/main/java/com/javafx/Clases/ApiClient.java` lines 170-450 — how ConexionException is thrown and the exact status codes it covers.
8. `desktop/src/main/java/com/javafx/Interface/controladorAgregarPaciente.java`, `controladorAgregarSanitario.java`, `controladorAgregarTratamiento.java`, `controladorAgregarDiscapacidad.java` — every `catch (ConexionException)` block.
9. `desktop/src/main/resources/tema_claro.css` and `tema_oscuro.css` — full files (existing tokens, button styles, table styles).
10. `desktop/src/main/resources/VentanaPrincipal.fxml` — sidebar structure (`btnPestania*` ids).

DO NOT start coding until all 10 sources are read.

---

## PHASE A — BUGFIXES

### A.1 BUG 1 — "Ver ficha paciente" does NOT filter by DNI

#### A.1.1 Root cause

`controladorVentanaPrincipal.abrirFichaPacienteDesdeCita(String dniPac)` (line 603) receives `dniPac` but NEVER uses it. It only navigates to Pacientes tab. The filter path that already works for Citas (`cargarPestaniaCitasConFiltro` → `setTextoBusquedaPendiente`) is NOT mirrored for Pacientes.

#### A.1.2 Fix — mirror the Citas pattern for Pacientes

All four changes atomic. Doer does NOT redesign the cache system.

##### A.1.2.1 Modify `controladorVentanaPacientes.java`

Add field + public setter + consume-on-load, identical pattern to `controladorVentanaCitas.setTextoBusquedaPendiente`:

```java
private String textoBusquedaPendiente;

public void setTextoBusquedaPendiente(String texto) {
    this.textoBusquedaPendiente = texto;
}
```

Inside the existing `initialize()` (or the first method guaranteed to run AFTER `cargarPacientes()`), at the END add:

```java
if (textoBusquedaPendiente != null && !textoBusquedaPendiente.isBlank()) {
    txfBuscarPacientes.setText(textoBusquedaPendiente);
    buscarPacientes(null);
    textoBusquedaPendiente = null;
}
```

If `initialize()` triggers `cargarPacientes()` asynchronously (Task), wrap the block above in `Platform.runLater(...)` placed AFTER the Task's `setOnSucceeded` callback. Keep the method `buscarPacientes(ActionEvent)` UNCHANGED — Doer only invokes it.

If `buscarPacientes` is not `public`/`@FXML` accessible, call the existing `@FXML` search action instead; do NOT rename methods.

##### A.1.2.2 Modify `controladorVentanaPrincipal.java`

Add a new helper mirroring `cargarPestaniaCitasConFiltro` (reference: lines 350-372). Place it directly AFTER `cargarPestaniaCitasConFiltro`:

```java
private void cargarPestaniaPacientesConFiltro(String textoBusqueda) {
    limpiarCachePestania("Pacientes");
    pestaniaActual = "";
    cargarPestania("Pacientes");
    marcarPestaniaSeleccionada(btnPestaniaPacientes);

    Object ctrl = cacheControladores.get("Pacientes");
    if (ctrl instanceof controladorVentanaPacientes cvp && textoBusqueda != null && !textoBusqueda.isBlank()) {
        cvp.setTextoBusquedaPendiente(textoBusqueda);
    }
}
```

##### A.1.2.3 Replace body of `abrirFichaPacienteDesdeCita(String dniPac)` (line 603)

Replace the current 4-line body with:

```java
public void abrirFichaPacienteDesdeCita(String dniPac) {
    if (dniPac == null || dniPac.isBlank()) {
        cargarPestania("Pacientes");
        marcarPestaniaSeleccionada(btnPestaniaPacientes);
        return;
    }
    cargarPestaniaPacientesConFiltro(dniPac);
}
```

##### A.1.2.4 Verify the call site in `controladorVentanaCitas.java`

`verFichaPaciente(ActionEvent)` must already pass `cita.getDniPaciente()`. If not, fix it. Do NOT change anything else in that method.

#### A.1.3 Acceptance test (manual)

1. Login as ADMIN0000/admin.
2. Go to Citas tab.
3. Select any row in the appointments table.
4. Click "Ver ficha paciente".
5. Expected: Pacientes tab opens AND `txfBuscarPacientes` is prefilled with the DNI AND the table shows ONLY that patient.

#### A.1.4 Unit test (mandatory per §10 of root CLAUDE.md)

Create `desktop/src/test/java/com/javafx/Interface/NavegacionFichaPacienteTest.java`. Two cases, both with JFXPanel bootstrap:

- `testSetTextoBusquedaPendienteAplicaFiltroAlIniciar` — instanciate controller, call setter, invoke initialize, assert that `txfBuscarPacientes.getText()` equals the DNI.
- `testAbrirFichaConDniVacioNoRompeFlujo` — call `abrirFichaPacienteDesdeCita(null)` → no exception, tab loads without filter.

---

### A.2 BUG 2 — "No se ha podido conectar a la base de datos" on register

#### A.2.1 Root cause (two orthogonal issues — fix BOTH)

**Issue 2A (message correctness):** `ApiClient.execute...` throws `ConexionException` for EVERY non-2xx response AND for transport errors. In the register controllers the `catch (ConexionException e)` branch shows a literal `"Error de conexion con la base de datos."` even when the real cause is a 400/409/500 from the API. The user sees "no se ha podido conectar a la base de datos" even though HTTP transport is OK.

**Issue 2B (underlying HTTP failure):** `ApiClient.java:525` maps every non-specific server status to `ConexionException("Error de servidor (...)")`. For 500-class errors coming from `POST /api/pacientes`, `POST /api/sanitarios`, `POST /api/catalogo/discapacidades` and `POST /api/catalogo/tratamientos`, Doer MUST verify the REQUEST BODY, not the error message.

#### A.2.2 Fix Issue 2A — correct the error text (minimal, deterministic)

For EACH of the four register controllers (`controladorAgregarPaciente.java`, `controladorAgregarSanitario.java`, `controladorAgregarTratamiento.java`, `controladorAgregarDiscapacidad.java`) do ONLY these edits:

1. Locate every `catch (ConexionException e)` block.
2. Replace the literal message `"Error de conexion con la base de datos."` (or `"Error al conectar con la base de datos"` or the constant `ConstantesApp.MSG_ERROR_BD`) with:

```java
"No se pudo comunicar con el servidor: " + e.getMessage()
```

3. Add a new `catch (ValidacionException ev)` branch RIGHT AFTER the `ConexionException` catch (and before `RehabiAppException`). Display `ev.getMessage()` as `TipoMensaje.ADVERTENCIA`. If `ValidacionException` is not thrown today for register flows, STILL add the catch — harmless and future-proof.

4. Make sure the generic `catch (RehabiAppException e)` remains LAST and shows `"Error: " + e.getMessage()` with `TipoMensaje.ERROR`.

5. `ConstantesApp.MSG_ERROR_BD` (util/ConstantesApp.java:107): rewrite its literal to `"No se pudo comunicar con el servidor"`. Do NOT rename the constant.

These edits are surgical. Doer does NOT refactor the exception hierarchy.

#### A.2.3 Fix Issue 2B — verify and repair the failing POST

Run this diagnostic BEFORE editing any DAO:

1. Start stack per `desktop/CLAUDE.md` Runbook.
2. Enable DEBUG on API: `SPRING_PROFILES_ACTIVE=local` already sets debug. Tail the Spring Boot console.
3. From desktop, attempt ONE register per entity:
   - New Paciente with all required fields + a photo.
   - New Sanitario.
   - New Discapacidad (codDis="DIS-TEST", nombreDis="Prueba", descripcion, protesis=false).
   - New Tratamiento (codTrat="TRAT-TEST", nombreTrat="Prueba", definicion, idNivel=1, codDis="DIS-TEST").
4. For each failure, record HTTP status + API-side stack trace.

Apply the matrix below STRICTLY based on what the logs show:

| API log says | Root cause | Exact fix |
|--------------|-----------|-----------|
| `Field 'xxx' is required` or 400 | DTO field name mismatch between desktop Request record and API expected JSON | Align record field names in `desktop/src/main/java/com/javafx/dto/*Request.java` to EXACTLY match API DTO. API is source of truth. |
| `duplicate key value violates unique constraint` / 409 | Seed or prior data | NOT a bug — UI already rejects. If it doesn't, add a `catch (DuplicadoException)` in the register controller showing `TipoMensaje.ADVERTENCIA`. |
| `cannot deserialize value of type LocalDate/LocalTime/byte[]` | Jackson config missing JSR310 or base64 for foto | In `ApiClient` ensure `ObjectMapper` has `registerModule(new JavaTimeModule())` and `disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)`. For `foto` (byte[]) verify the DTO serializes as base64 automatically (default Jackson behavior). If API expects multipart, switch the photo POST to multipart per §A.2.4. |
| `null value in column "xxx" violates not-null` (API 500) | Desktop sends null for a column that cannot be null | Check the desktop form — a required field is empty. Add `ValidacionUtil` check BEFORE the POST, showing a clear field-level message. Do NOT silently default. |
| `FK violation paciente_dni_san_fkey` | Selected sanitario DNI is invalid | Ensure the ComboBox of sanitarios is populated from the API (not from stale cache). |
| `Connection refused` / transport error | API not running | Not a bug — improved error message A.2.2 already covers this. |

If the log shows something NOT in this matrix, STOP and report to the developer. Do NOT guess.

#### A.2.4 Photo upload special case (paciente only)

If paciente register fails with `base64` or `payload too large`:

1. Check current API contract: open `/api/src/main/java/.../controller/PacienteController.java` POST endpoint. Confirm whether it expects `foto` as a base64 string inside JSON OR as multipart `@RequestPart MultipartFile`.
2. If base64 JSON (desktop default): keep as is. Make sure photo is resized BEFORE encoding. Add a guard in `controladorAgregarPaciente`:

```java
if (fotoBytes != null && fotoBytes.length > 2 * 1024 * 1024) {
    VentanaUtil.mostrarVentanaInformativa("La foto supera 2 MB. Reducela o elige otra.", TipoMensaje.ADVERTENCIA);
    return;
}
```

3. If multipart: add a new helper `ApiClient.postMultipart(String path, Map<String,String> parts, byte[] fileBytes, String filename)` (Doer writes it following existing `post` signature). ONLY implement if the API actually requires multipart.

DO NOT migrate from base64 to multipart unless the API truly demands it.

#### A.2.5 Acceptance tests

1. Manual: register ONE entity per type with valid data → success toast.
2. Manual: register with ONE required field empty → clear validation message (not "BD error").
3. Manual: register with duplicate codTrat → `TipoMensaje.ADVERTENCIA` with the API's duplicate message.
4. Manual: stop the API → register attempt → toast `"No se pudo comunicar con el servidor: ..."` (confirms A.2.2 message fix).

#### A.2.6 Unit test

`desktop/src/test/java/com/javafx/Clases/ApiClientTest.java` — extend existing test file with one case per CRUD entity that uses `MockWebServer` to:
- 201 → expect no exception.
- 400 with JSON body `{"message":"campo dni requerido"}` → expect `ValidacionException` (if wired) or `RehabiAppException` with the server message embedded.
- 500 → expect `ConexionException` with `"Error de servidor (500)"`.

If `MockWebServer` is not in `build.gradle`, SKIP this test (do NOT add new dependency) and rely on manual verification.

---

## PHASE B — CSS VISUAL ENHANCEMENT (12 ITEMS)

### B.0 GLOBAL RULES FOR PHASE B

- Every new rule lives in BOTH `tema_claro.css` AND `tema_oscuro.css`. Mirror structurally; only hex values differ.
- Reuse existing palette tokens. Do NOT invent new hex codes unless unavoidable. If unavoidable, add comment `/* Revisar paleta con UI */` above the rule.
- NO emojis. NO English comments. NO inline `setStyle(...)` in Java/FXML.
- Append all new rules at the END of each CSS file, inside a clearly marked block:

```css
/* =========================================================== */
/* MEJORAS VISUALES v2 — 2026-04-15                            */
/* Phase B de PLAN.md. No mover estas reglas a otro archivo.    */
/* =========================================================== */
```

- After writing every rule, run `./gradlew run`, visually confirm the change, then mark the checklist item.

### B.1 Enhanced sidebar navigation

Target selectors (verify exact names in `VentanaPrincipal.fxml`): `.boton-pestania`, `.boton-pestania:hover`, `.boton-pestania.pestania-activa` (or equivalent). If the active tab style class is named differently (e.g. `pestania-seleccionada`), use the real one.

```css
.boton-pestania {
    -fx-background-color: transparent;
    -fx-border-color: transparent;
    -fx-border-width: 0 0 0 3;
    -fx-padding: 10 16 10 16;
    -fx-transition: all 180ms ease-in-out;
}
.boton-pestania:hover {
    -fx-border-color: derive(-color-primario, 10%);
    -fx-background-color: linear-gradient(to right, derive(-color-fondo, -3%), transparent);
}
.boton-pestania.pestania-activa {
    -fx-border-color: -color-primario;
    -fx-background-color: linear-gradient(to right, derive(-color-primario, 85%), transparent);
    -fx-text-fill: -color-primario;
    -fx-font-weight: bold;
}
```

Dark theme: swap `derive(-color-fondo, -3%)` → `derive(-color-fondo, 8%)` and `derive(-color-primario, 85%)` → `derive(-color-primario, -60%)`.

Verify `.pestania-activa` is toggled in `marcarPestaniaSeleccionada` of `controladorVentanaPrincipal`. If not, add `btn.getStyleClass().add("pestania-activa")` and remove from previous button. DO NOT rename existing classes.

### B.2 Card-based content panels

Target: `VBox vboxContenedorPrin*` and the header `HBox` of every main tab (Pacientes, Sanitarios, Citas, Discapacidades, Tratamientos). Add style class `panel-card` to each in the FXML — zero Java changes. Header `HBox` in each FXML: add `panel-card-header`.

```css
.panel-card {
    -fx-background-color: -color-fondo-panel;
    -fx-background-radius: 10;
    -fx-border-color: -color-borde-suave;
    -fx-border-width: 1;
    -fx-border-radius: 10;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0.15, 0, 2);
    -fx-padding: 0;
}
.panel-card-header {
    -fx-background-color: derive(-color-fondo-panel, -4%);
    -fx-background-radius: 10 10 0 0;
    -fx-padding: 12 16 12 16;
    -fx-border-color: transparent transparent -color-borde-suave transparent;
    -fx-border-width: 0 0 1 0;
}
```

Dark theme: `derive(...-4%)` → `derive(...+6%)`, shadow alpha 0.35.

If `-color-fondo-panel` or `-color-borde-suave` tokens do not exist in the theme, define them in the root `*{}` section at the TOP of each theme file:

```css
-color-fondo-panel: #ffffff;        /* claro */
-color-borde-suave: #e0e4ea;
```

```css
-color-fondo-panel: #1e222b;        /* oscuro */
-color-borde-suave: #2f3540;
```

### B.3 Improved table styling

Target: `.table-view`, `.table-row-cell`, `.table-row-cell:hover`, `.table-row-cell:selected`, `.table-view .column-header`.

```css
.table-row-cell {
    -fx-cell-size: 38;
    -fx-border-width: 0 0 0 3;
    -fx-border-color: transparent;
    -fx-transition: all 150ms ease-in-out;
}
.table-row-cell:hover {
    -fx-background-color: derive(-color-fondo-panel, -5%);
    -fx-border-color: derive(-color-primario, 40%);
}
.table-row-cell:selected {
    -fx-background-color: derive(-color-primario, 85%);
    -fx-border-color: -color-primario;
    -fx-text-fill: -color-texto;
}
.table-view .column-header {
    -fx-border-color: transparent transparent -color-primario transparent;
    -fx-border-width: 0 0 2 0;
    -fx-background-color: derive(-color-fondo-panel, -3%);
    -fx-padding: 8 10 8 10;
}
```

Dark theme: hover derive `-color-fondo-panel, 6%`; selected bg `derive(-color-primario, -60%)`.

### B.4 Enhanced form inputs

Target: `.text-field`, `.text-field:focused`, `.combo-box`, `.date-picker`, `.spinner`.

```css
.text-field, .combo-box, .date-picker, .spinner {
    -fx-background-radius: 6;
    -fx-border-radius: 6;
    -fx-border-color: -color-borde-suave;
    -fx-border-width: 1;
    -fx-padding: 6 10 6 10;
}
.text-field:focused, .combo-box:focused, .date-picker:focused, .spinner:focused {
    -fx-effect: innershadow(gaussian, rgba(0,0,0,0.10), 4, 0, 0, 1);
    -fx-border-color: -color-primario;
    -fx-border-width: 0 0 0 3, 1 1 1 1;
    -fx-border-insets: 0 0 0 0, 0 0 0 3;
}
.text-field {
    -fx-prompt-text-fill: derive(-color-texto, 55%);
    -fx-font-style: normal;
}
.text-field > .prompt-text {
    -fx-font-style: italic;
}
.input-valido { -fx-border-color: -color-exito; }
.input-error  { -fx-border-color: -color-error; }
```

If `-color-exito`/`-color-error` tokens missing, define:
- Claro: `-color-exito: #2a9d52; -color-error: #c0392b;`
- Oscuro: `-color-exito: #3cc073; -color-error: #e05a4b;`

### B.5 Button refinements

Target `.button-primario`, `.button-secundario`, `.button-peligro`, `.button:disabled`.

```css
.button-primario {
    -fx-background-color: linear-gradient(to bottom, derive(-color-primario, 10%), -color-primario);
    -fx-background-radius: 6;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.14), 4, 0, 0, 1);
    -fx-transition: all 120ms;
}
.button-primario:pressed {
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 8, 0, 0, 2);
    -fx-scale-x: 0.98;
    -fx-scale-y: 0.98;
}
.button:disabled {
    -fx-opacity: 0.5;
}
```

Mirror for `.button-secundario` and `.button-peligro` with their own palette tokens.

### B.6 Enhanced separators

Target `.separator`, `.separator *.line`.

```css
.separator *.line {
    -fx-background-color: linear-gradient(to right, transparent, -color-borde-suave 50%, transparent);
    -fx-border-color: transparent;
    -fx-padding: 0;
    -fx-pref-height: 1;
}
.separator { -fx-padding: 8 0 8 0; }
```

### B.7 Typography improvements

```css
.label-titulo {
    -fx-letter-spacing: 0.03em;     /* JavaFX 23+ */
    -fx-font-weight: bold;
}
.label-seccion {
    -fx-font-size: 15px;
    -fx-font-weight: 600;
}
```

Dark theme ONLY, add:

```css
.label-titulo { -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 2, 0, 0, 1); }
```

If `-fx-letter-spacing` is not supported in the target JavaFX version, remove that single property. All other rules remain.

### B.8 Tooltip and popover polish

```css
.tooltip {
    -fx-background-color: -color-fondo-panel;
    -fx-text-fill: -color-texto;
    -fx-background-radius: 6;
    -fx-padding: 6 10 6 10;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 10, 0.2, 0, 3);
    -fx-font-size: 12px;
}
```

### B.9 Scrollbar refinement

```css
.scroll-bar:horizontal, .scroll-bar:vertical {
    -fx-background-color: transparent;
    -fx-pref-width: 6;
    -fx-pref-height: 6;
    -fx-transition: all 180ms;
}
.scroll-bar:hover {
    -fx-pref-width: 8;
    -fx-pref-height: 8;
}
.scroll-bar .thumb {
    -fx-background-color: derive(-color-borde-suave, -20%);
    -fx-background-radius: 6;
}
.scroll-bar .thumb:hover {
    -fx-background-color: -color-primario;
}
.scroll-bar .increment-button,
.scroll-bar .decrement-button,
.scroll-bar .increment-arrow,
.scroll-bar .decrement-arrow {
    -fx-background-color: transparent;
    -fx-padding: 0;
    -fx-shape: "";
}
```

### B.10 Status indicators and badges

Paleta fija de niveles (agregar como tokens o inline):

- agudo: `#d14b4b`
- subagudo: `#e08a2c`
- fortalecimiento: `#2b6fd4`
- funcional: `#2e9d58`

```css
.badge-activo, .badge-inactivo,
.badge-nivel-agudo, .badge-nivel-subagudo,
.badge-nivel-fortalecimiento, .badge-nivel-funcional {
    -fx-background-radius: 12;
    -fx-padding: 2 10 2 10;
    -fx-font-size: 11px;
    -fx-font-weight: bold;
    -fx-text-fill: white;
}
.badge-activo   { -fx-background-color: #2e9d58; }
.badge-inactivo { -fx-background-color: #888a91; }
.badge-nivel-agudo          { -fx-background-color: #d14b4b; }
.badge-nivel-subagudo       { -fx-background-color: #e08a2c; }
.badge-nivel-fortalecimiento{ -fx-background-color: #2b6fd4; }
.badge-nivel-funcional      { -fx-background-color: #2e9d58; }
```

Doer does NOT wire these badges in controllers for this plan — CSS only. Follow-up task (Progression level UI) will apply them.

### B.11 Login screen polish

Target `VentanaSesion.fxml` root and `indicadorConexion`. Add via CSS:

```css
.login-root {
    -fx-background-color: linear-gradient(to bottom right, derive(-color-fondo, 5%), derive(-color-fondo, -5%));
}
.indicador-conexion {
    -fx-background-radius: 50%;
    -fx-min-width: 10;
    -fx-min-height: 10;
    -fx-max-width: 10;
    -fx-max-height: 10;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 3, 0.3, 0, 1);
}
.indicador-conexion-ok  { -fx-background-color: #2e9d58; }
.indicador-conexion-ko  { -fx-background-color: #d14b4b; }
.indicador-conexion-wait{ -fx-background-color: #e0b02c; }
```

In `VentanaSesion.fxml`: add `styleClass="login-root"` on the root and `styleClass="indicador-conexion"` on the circle/Pane. In `controladorSesion.java`, toggle `indicador-conexion-ok/ko/wait` — ZERO inline styles.

### B.12 Modal window improvements

Apply to every modal FXML root (`VentanaAgregarPaciente`, `VentanaAgregarSanitario`, `VentanaAgregarTratamiento`, `VentanaAgregarDiscapacidad`, `VentanaFiltroPacientes`, `VentanaFiltroTratamientos`, etc.):

1. Add `styleClass="modal-root"` on the root VBox/AnchorPane.
2. Add `styleClass="modal-header"` on the title HBox.

```css
.modal-root {
    -fx-background-color: -color-fondo-panel;
    -fx-background-radius: 10;
    -fx-border-radius: 10;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0.25, 0, 6);
}
.modal-header {
    -fx-border-color: -color-primario transparent transparent transparent;
    -fx-border-width: 3 0 0 0;
    -fx-background-radius: 10 10 0 0;
    -fx-padding: 14 18 12 18;
}
```

Open animation — use a single util, no per-modal code. Add in `VentanaUtil`:

```java
public static void animarAperturaModal(javafx.scene.Parent root) {
    root.setOpacity(0);
    root.setScaleX(0.97);
    root.setScaleY(0.97);
    javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(
        new javafx.animation.FadeTransition(javafx.util.Duration.millis(160), root),
        new javafx.animation.ScaleTransition(javafx.util.Duration.millis(160), root)
    );
    ((javafx.animation.FadeTransition) pt.getChildren().get(0)).setToValue(1);
    ((javafx.animation.ScaleTransition) pt.getChildren().get(1)).setToX(1);
    ((javafx.animation.ScaleTransition) pt.getChildren().get(1)).setToY(1);
    pt.play();
}
```

Call `VentanaUtil.animarAperturaModal(scene.getRoot())` from the existing `VentanaHelper.abrirModal(...)` helper (single call site). If `VentanaHelper` does not have a single entry point, add one — do NOT duplicate the call in every controller.

---

## PHASE C — TESTING (mandatory)

### C.1 Static check

```
./gradlew clean compileJava test
```

Must pass without warnings. Fix any residual imports.

### C.2 Manual verification matrix

| Check | Expected |
|-------|----------|
| A.1 — Ver ficha paciente from a Cita | Pacientes tab opens with DNI filter applied |
| A.2 — Register paciente with all fields valid | Success toast, paciente appears in table |
| A.2 — Register paciente with API stopped | Toast: "No se pudo comunicar con el servidor: ..." |
| A.2 — Register discapacidad with duplicated codDis | Advertencia toast with API message |
| B.1..B.12 | Visual changes match descriptions in both themes |

### C.3 TestSprite MCP

After manual matrix passes, delegate to TestSprite per root `CLAUDE.md` §10.2. Do NOT mark any checklist item `[x]` until TestSprite returns 100%.

---

## D. CHECKLIST UPDATE (after TestSprite = 100%)

In `desktop/CLAUDE.md` §7, flip these items, ONE commit per flip:

Phase A bugs (add two NEW items under "Progression level UI" or a new "Bugfixes" subsection):
- [x] Fix: "Ver ficha paciente" now filters Pacientes tab by DNI.
- [x] Fix: misleading "conexion BD" error replaced by accurate server-communication message.

CSS visual enhancement block — flip B.1..B.12 in order:
- [x] Enhanced sidebar navigation
- [x] Card-based content panels
- [x] Improved table styling
- [x] Enhanced form inputs
- [x] Button refinements
- [x] Enhanced separators
- [x] Typography improvements
- [x] Tooltip and popover polish
- [x] Scrollbar refinement
- [x] Status indicators and badges
- [x] Login screen polish
- [x] Modal window improvements

---

## E. ORDER OF EXECUTION

1. A.1 (Bug 1) — mirror Citas filter for Pacientes. Test.
2. A.2.2 (message correctness) — 5 minutes edit + manual check.
3. A.2.3 (diagnostic run) — record logs.
4. A.2.3 matrix — apply ONLY the fix that matches the log.
5. B.0 — define missing palette tokens in both themes.
6. B.2, B.3, B.1 in that order (card wrap first so hover/active states have ground to stand on).
7. B.4, B.5 — form & buttons.
8. B.6, B.7, B.8, B.9 — separators, typography, tooltip, scrollbar.
9. B.10 — badges CSS only.
10. B.11 — login screen.
11. B.12 — modal polish + VentanaUtil animation.
12. C — static + manual + TestSprite.
13. D — flip checklist, one commit per step.

---

## F. NON-NEGOTIABLES

- Comments in Spanish. No English in Java/FXML/CSS.
- No emojis anywhere.
- No inline `setStyle(...)`.
- No new dependencies in `build.gradle`.
- No refactor outside §2-scope files. Touch only what this plan names.
- No partial commits that leave the app uncompilable.
- Do NOT mark checklist items `[x]` until TestSprite 100% per root `CLAUDE.md` §10.4.
- If a step's diagnostic (A.2.3) reveals something outside the matrix, STOP and escalate to developer. No guessing.

---

*End of plan.*
