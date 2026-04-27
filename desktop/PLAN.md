# IMPLEMENTATION PLAN — Therapeutic Game Link + UI Parity + Cancel/Dark Bugfixes

> Date: 2026-04-24
> Branch: desktop-final
> Author: Agent 3 Thinker (Opus) — PRESCRIPTIVE. Doer (Sonnet) MUST follow step by step without re-architecting.
> Language: All code/FXML/CSS comments in Spanish (root `CLAUDE.md` §4.5). This plan itself is English.
> Scope:
>   Phase P0 — Cross-domain prerequisites (Agent 1: API + schema for `juego` and `articulacion`)
>   Phase P1 — Bugfix: `Cancelar` in `Añadir tratamiento` / `Añadir discapacidad` triggers spurious error popup
>   Phase P2 — Bugfix: `Añadir paciente` / `Añadir sanitario` modal background stays white in dark theme
>   Phase P3 — CSS parity: search buttons blue like other primary buttons; add buttons on Tratamientos/Discapacidades identical to Sanitarios
>   Phase P4 — Feature: `Juego terapeutico` checkbox on treatment form → post-save `Seleccionar juego` modal filtered by articulacion
>   Phase P5 — Acceptance checklist + test requirement

---

## 0. CONTEXT THE DOER MUST READ FIRST (MANDATORY)

Before touching code, open and read:

1. `/CLAUDE.md` — §4.5 (style), §4.6 (security), §10 (TestSprite loop), §6 (inter-service contracts).
2. `desktop/CLAUDE.md` — §2, §3, §4, §6, §7 completed items (do NOT regress them).
3. `desktop/.claude/skills/javafx-java24/` — apply all rules. Fail hard if missing.
4. `desktop/src/main/resources/VentanaTratamientos.fxml`, `VentanaDiscapacidades.fxml`, `VentanaSanitarios.fxml` — toolbar structure reference.
5. `desktop/src/main/resources/VentanaAgregarTratamiento.fxml`, `VentanaAgregarDiscapacidad.fxml`, `VentanaAgregarPaciente.fxml`, `VentanaAgregarSanitario.fxml` — modal roots.
6. `desktop/src/main/java/com/javafx/Interface/controladorVentanaTratamientos.java` lines 142-176 (`cargarTratamientos`) and lines 320-352 (`abrirFormularioNuevoTratamiento`).
7. `desktop/src/main/java/com/javafx/Interface/controladorVentanaDiscapacidades.java` lines 196-260 (reload logic) and lines 240-300 (`abrirFormularioNuevaDiscapacidad`).
8. `desktop/src/main/java/com/javafx/Interface/controladorAgregarTratamiento.java` — full file (`initialize`, `cargarComboBoxes`, `cerrarVentana`).
9. `desktop/src/main/java/com/javafx/Interface/controladorAgregarDiscapacidad.java` — full file.
10. `desktop/src/main/java/com/javafx/Interface/controladorVentanaOpciones.aplicarConfiguracionAScene` — CSS application.
11. `desktop/src/main/resources/tema_claro.css` (lines 180-230, 780-810) and `tema_oscuro.css` (lines 190-230, 975-990).
12. `desktop/src/main/java/com/javafx/Clases/ApiClient.java` — how `get/post/put/delete` serialize JSON.
13. `desktop/src/main/java/com/javafx/service/CatalogoService.java` and `desktop/src/main/java/com/javafx/DAO/CatalogoDAO.java` — extension pattern.
14. `api/src/main/resources/db/migration/V4__tratamiento_nivel_progresion.sql` — migration style reference.

DO NOT start coding until all 14 sources are read.

---

## PHASE P0 — CROSS-DOMAIN PREREQUISITES (Agent 1 scope — MUST COMPLETE BEFORE P4)

> This phase is NOT for Agent 3. It must be delegated to Agent 1 (API + Data). Desktop Doer MUST NOT implement P0. If Agent 1 has not delivered P0, P4 is BLOCKED.
> Rationale: desktop consumes the REST API exclusively (`desktop/CLAUDE.md` §1). There is no schema for games, no link from `tratamiento` to game, and no body-part concept in the database.

### P0.1 — Schema migration `V13__juego_articulacion.sql`

Create a new Flyway migration at `api/src/main/resources/db/migration/V13__juego_articulacion.sql` with:

```sql
-- Articulacion: taxonomia de partes del cuerpo asociables a una discapacidad.
CREATE TABLE articulacion (
    id_articulacion SERIAL PRIMARY KEY,
    codigo VARCHAR(32) UNIQUE NOT NULL,
    nombre VARCHAR(80) NOT NULL
);

-- Seed alineado con BodyPartId de mobile (progress.ts):
-- HEAD, NECK, TORSO, LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_ARM, RIGHT_ARM,
-- LEFT_HAND, RIGHT_HAND, LEFT_HIP, RIGHT_HIP, LEFT_LEG, RIGHT_LEG, LEFT_FOOT, RIGHT_FOOT.
INSERT INTO articulacion(codigo, nombre) VALUES
  ('HEAD','Cabeza'), ('NECK','Cuello'), ('TORSO','Torso'),
  ('LEFT_SHOULDER','Hombro izquierdo'), ('RIGHT_SHOULDER','Hombro derecho'),
  ('LEFT_ARM','Brazo izquierdo'), ('RIGHT_ARM','Brazo derecho'),
  ('LEFT_HAND','Mano izquierda'), ('RIGHT_HAND','Mano derecha'),
  ('LEFT_HIP','Cadera izquierda'), ('RIGHT_HIP','Cadera derecha'),
  ('LEFT_LEG','Pierna izquierda'), ('RIGHT_LEG','Pierna derecha'),
  ('LEFT_FOOT','Pie izquierdo'), ('RIGHT_FOOT','Pie derecho');

-- FK nullable en discapacidad para asociarla a una articulacion.
ALTER TABLE discapacidad
    ADD COLUMN id_articulacion INT NULL REFERENCES articulacion(id_articulacion) ON DELETE SET NULL;

-- Catalogo de juegos Unity hospedados en AWS.
CREATE TABLE juego (
    cod_juego VARCHAR(32) PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    descripcion TEXT,
    url_juego VARCHAR(400) NOT NULL,
    id_articulacion INT NOT NULL REFERENCES articulacion(id_articulacion) ON DELETE RESTRICT,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Relacion N:1 tratamiento -> juego (un tratamiento a un juego terapeutico; juego puede reutilizarse).
-- Se usa columna directa en tratamiento para simplificar el UPDATE unico desde el formulario.
ALTER TABLE tratamiento
    ADD COLUMN cod_juego VARCHAR(32) NULL REFERENCES juego(cod_juego) ON DELETE SET NULL;

CREATE INDEX idx_juego_articulacion ON juego(id_articulacion);
CREATE INDEX idx_tratamiento_juego ON tratamiento(cod_juego);
```

### P0.2 — REST endpoints (Spring Boot)

Agent 1 MUST expose:

| Verbo | Ruta | Cuerpo / Parametros | Respuesta |
|-------|------|---------------------|-----------|
| GET | `/api/catalogo/articulaciones` | — | `[{idArticulacion,codigo,nombre}]` |
| GET | `/api/catalogo/juegos` | query `idArticulacion` opcional | `[{codJuego,nombre,descripcion,urlJuego,idArticulacion,activo}]` |
| POST | `/api/catalogo/juegos` | `JuegoRequest` | 201 + objeto creado |
| PUT | `/api/catalogo/juegos/{cod}` | `JuegoRequest` | 200 + objeto |
| DELETE | `/api/catalogo/juegos/{cod}` | — | 204 / 409 si esta vinculado |
| PUT | `/api/catalogo/tratamientos/{codTrat}/juego` | `{"codJuego":"<cod>"}` o `{"codJuego":null}` | 200 + tratamiento |
| GET | `/api/catalogo/tratamientos/{codTrat}` | — | tratamiento extendido incluyendo `codJuego`, `codArticulacionDiscapacidad` |

Rules:
- Reject (400) si `codJuego` apunta a un juego cuya `id_articulacion` no coincide con la de la discapacidad vinculada al tratamiento.
- Endpoints bajo RBAC `SPECIALIST` (crear/editar/borrar juego + set tratamiento.juego). Listados accesibles a `SPECIALIST` y `NURSE`.
- Validaciones `@NotBlank` en cod/nombre/urlJuego; `url_juego` debe ser HTTPS.

### P0.3 — DTOs Java (compartidos desktop ↔ API)

Desktop debe duplicar los records DTO en `desktop/src/main/java/com/javafx/dto/`:

```java
public record Articulacion(Integer idArticulacion, String codigo, String nombre) {}
public record Juego(String codJuego, String nombre, String descripcion,
                    String urlJuego, Integer idArticulacion, boolean activo) {}
```

### P0.4 — Seed de desarrollo

Actualizar `desktop/scripts/reseed-dev.sql` (o equivalente) para añadir al menos 2 juegos por articulacion comun (`LEFT_HAND`, `RIGHT_HAND`, `LEFT_KNEE` si aplica, etc.) de forma que las QA visuales de P4 tengan datos.

### P0.5 — Verificacion P0

Antes de liberar P4:
- `curl -s http://localhost:8080/api/catalogo/articulaciones | jq length` > 0.
- `curl -s "http://localhost:8080/api/catalogo/juegos?idArticulacion=5" | jq length` > 0.
- `./gradlew test` verde en `/api`.

---

## PHASE P1 — BUG A: `Cancelar` en `Añadir tratamiento` / `Añadir discapacidad` muestra error

### P1.1 Sintoma

1. Abrir pestana Tratamientos o Discapacidades.
2. Pulsar `btnAnadirTratamiento` / `btnAnadirDiscapacidad`.
3. Pulsar `Cancelar` en la ventana modal sin rellenar nada.
4. Aparece un `VentanaInformativa` con mensaje de error.

En Sanitarios y Pacientes NO ocurre — patron identico pero sin fallo.

### P1.2 Root cause (hipotesis prioritarias — validar con debug antes de fix)

El Doer debe ejecutar con `./gradlew run` y capturar la traza de stderr al cancelar. Una de estas causas es la real:

- **H1 (probable):** `controladorVentanaTratamientos.abrirFormularioNuevoTratamiento` (line 335) hace `showAndWait()` y despues llama a `cargarTratamientos()` + `aplicarFiltros()` **sin distinguir** si el usuario creo algo o cancelo. Si `cargarTratamientos()` propaga cualquier `Exception`, el bloque `catch (Exception e)` en la linea 344 dispara `VentanaUtil.mostrarVentanaInformativa("Error al abrir el formulario ...", ERROR)` — mensaje ENGANOSO que dice "Error al abrir" aunque el fallo sea posterior al cierre.
- **H2:** `controladorAgregarTratamiento.cargarComboBoxes` puede lanzar una excepcion silenciosa (rama `catch (Exception)` line 112 solo imprime stack) cuando se dispara antes de tiempo desde `initialize()`; al cancelar, la tabla padre se recarga llamando `listarDiscapacidadesDeTratamiento` y el mismo fallo transitorio reaparece.
- **H3:** En dark theme, `aplicarConfiguracionAScene` carga CSS con `@import` o ruta relativa incorrecta que emite `javafx.scene.control.Dialog` warning promovido a error por el manejador global.

### P1.3 Fix (orden obligatorio)

1. **Primero reproducir y capturar traza.** No tocar codigo hasta tener el stack real.
2. Si traza confirma H1:
   - Envolver `cargarTratamientos()` + `aplicarFiltros()` en su propio `try/catch` LOCAL con mensaje honesto (`"Error al refrescar la tabla de tratamientos"`), y hacer el mismo tratamiento en `controladorVentanaDiscapacidades`.
   - Eliminar del `catch (Exception e)` externo en `abrirFormularioNuevoTratamiento` el `mostrarVentanaInformativa` cuando la excepcion provenga del post-refresh (usar `try/catch` separado tal como esta hecho en Sanitarios line 401-405: reload en su propio try, error solo a stderr).
3. Si traza confirma H2:
   - En `controladorAgregarTratamiento.cargarComboBoxes`, reemplazar el `catch (Exception)` mudo por `catch (ConexionException e) { mostrar advertencia y dejar combos vacios; }` `catch (Exception e) { log y dejar combos vacios; }`. Evitar propagar.
4. Si traza confirma H3:
   - Ajustar `aplicarConfiguracionAScene` para usar `scene.getStylesheets().setAll(getClass().getResource(cssPath).toExternalForm())` y eliminar cualquier `add()` acumulativo.
5. Replicar el fix en `controladorVentanaDiscapacidades.abrirFormularioNuevaDiscapacidad` y en `controladorAgregarDiscapacidad` (mismo patron).

### P1.4 Verificacion

- Abrir tratamiento → cancelar → NO popup de error. Logs limpios en stderr.
- Abrir discapacidad → cancelar → NO popup.
- Crear uno real → popup EXITO correcto.
- Editar uno real → popup EXITO correcto.
- Repetir en tema claro y en tema oscuro.

---

## PHASE P2 — BUG B: `Añadir paciente` / `Añadir sanitario` fondo blanco en tema oscuro

### P2.1 Sintoma

Con tema oscuro activo (`config.properties` → `tema=oscuro`), abrir `Añadir paciente` o `Añadir sanitario`: el area de contenido aparece blanca en lugar del gris oscuro del tema.

### P2.2 Root cause

- `VentanaAgregarPaciente.fxml` raiz es `<ScrollPane styleClass="root">` que contiene un `<VBox styleClass="root modal-root">`. El `ScrollPane` NO hereda el color de su `viewport` a menos que exista la regla `.scroll-pane, .scroll-pane .viewport { -fx-background-color: -color-fondo-panel; }` en `tema_oscuro.css`. En el CSS actual no hay regla especifica para `.scroll-pane .viewport` que fuerce el fondo oscuro → viewport blanco default.
- `VentanaAgregarSanitario.fxml` raiz es un `<VBox styleClass="root modal-root">`. Si el usuario ve blanco aqui tambien es porque el `Stage` se crea con `Scene` vacio y el CSS se anade tarde; o la clase `.modal-root` en dark define `-color-fondo-panel` pero `root` global tiene `-fx-background: #101520` que no aplica a hijos si no hay cascada.

### P2.3 Fix

#### P2.3.1 `tema_oscuro.css` — reglas que FALTAN

Insertar (si no existen ya) tras la seccion `====== B.12 VENTANAS MODALES MEJORADAS ======` (line 974):

```css
/* Forzar fondo oscuro en ScrollPane de formularios modales */
.modal-root .scroll-pane,
.modal-root .scroll-pane .viewport,
.root > .scroll-pane,
.root > .scroll-pane .viewport {
    -fx-background-color: -color-fondo-panel;
    -fx-background: -color-fondo-panel;
}

/* Fondo de VBox raiz del formulario cuando es el hijo directo del ScrollPane */
.scroll-pane > .viewport > .modal-root {
    -fx-background-color: -color-fondo-panel;
}
```

Reglas equivalentes para `tema_claro.css` solo si visualmente se nota un viewport azulado en claro; en ese caso duplicar con `-color-fondo-panel-claro`. Si el tema claro esta correcto, NO tocar.

#### P2.3.2 Revisar FXMLs

- `VentanaAgregarPaciente.fxml` linea 24: verificar que el `<ScrollPane>` raiz tenga `styleClass="root modal-root"` (ahora solo `"root"`). Cambiar a `styleClass="root modal-root"` para que la regla CSS anterior aplique.
- `VentanaAgregarSanitario.fxml`: ya tiene `"root modal-root"` en la VBox raiz — no cambia.

#### P2.3.3 Verificar CSS unicamente aplicado una vez

En `aplicarConfiguracionAScene`, usar `scene.getStylesheets().setAll(...)` (NO `add()`) y confirmar que no queda un stylesheet residual del tema opuesto.

### P2.4 Verificacion

- Cambiar tema a oscuro en `VentanaOpciones` → abrir `Añadir paciente` → fondo oscuro uniforme incluyendo area scrollable.
- Abrir `Añadir sanitario` → fondo oscuro uniforme.
- Revertir a tema claro → ambos modales muestran fondo claro correcto.
- Campos de texto legibles en ambos temas.

---

## PHASE P3 — UI PARITY (botones de busqueda y de anadir)

### P3.1 Botones de busqueda azules

**Objetivo:** el icono-boton `btnBuscarSanitarios` / `btnBuscarTratamientos` / `btnBuscarDiscapacidades` / `btnBuscarPacientes` debe compartir el azul primario del resto de botones principales.

**Fix A (recomendado — minimiza delta CSS):** anadir la clase `button-primario` junto a `button-icono` en los 4 FXML:

```xml
<!-- ANTES -->
<Button fx:id="btnBuscarTratamientos" ... styleClass="button-icono">

<!-- DESPUES -->
<Button fx:id="btnBuscarTratamientos" ... styleClass="button-icono button-primario-icon">
```

Archivos a editar:
- `VentanaTratamientos.fxml` line 38
- `VentanaDiscapacidades.fxml` line 37
- `VentanaSanitarios.fxml` line 37
- `VentanaPacientes.fxml` (busqueda rapida — buscar `btnBuscarPacientes`)

**Fix B (CSS):** crear nueva clase `.button-primario-icon` en AMBOS temas, tras `.button-icono`:

`tema_claro.css`:
```css
.button-primario-icon {
    -fx-background-color: #5DADE2;
    -fx-text-fill: #FFFFFF;
    -fx-background-radius: 8px;
    -fx-padding: 8px;
}
.button-primario-icon:hover { -fx-background-color: #4A90E2; }
```

`tema_oscuro.css`:
```css
.button-primario-icon {
    -fx-background-color: #5499C7;
    -fx-text-fill: #FFFFFF;
    -fx-background-radius: 8px;
    -fx-padding: 8px;
}
.button-primario-icon:hover { -fx-background-color: #6B8EC4; }
```

Rationale: no destruir `.button-icono` (usado tambien por `btnFiltrar*`). Crear clase nueva que combina tinte primario + padding de icono.

### P3.2 Paridad del boton `Añadir`

Comparativa actual:

| Pantalla | id | prefH | prefW | styleClass | margin right |
|----------|----|-------|-------|------------|--------------|
| Sanitarios | btnAnadirSanitario | 40 | 40 | button-primario | 20 |
| Discapacidades | btnAnadirDiscapacidad | 40 | 40 | button-primario | 20 |
| Tratamientos | btnAnadirTratamiento | 40 | 40 | button-primario | 10 |

Tamano y clase CSS coinciden. Diferencia REAL: el `HBox.margin right` de Tratamientos es `10.0` (deberia ser `20.0` como los otros), y la ImageView interior puede tener `fitWidth` inconsistente. La diferencia visual percibida viene del margen.

**Fix:**
- `VentanaTratamientos.fxml` line 69-71: cambiar `<Insets right="10.0" />` por `<Insets right="20.0" />` en el `HBox.margin` de `btnAnadirTratamiento`.
- Confirmar que los tres FXML usen `fitHeight="18.0" fitWidth="74.0"` (ya lo hacen segun lectura de contexto §5). Si no coinciden, alinear con Sanitarios.
- NO cambiar `styleClass`. Ya es `button-primario` en los tres.

### P3.3 Verificacion

- Capturar screenshot de las 3 pestanas: lupa azul, lupa azul, lupa azul (todas identicas al iconoAgregar).
- Boton `+` visualmente identico en tamano y color en las 3 pestanas.
- Hover en cada uno produce transicion al tono azul mas oscuro.
- Tema claro y tema oscuro: repetir verificacion.

---

## PHASE P4 — FEATURE: `Juego terapeutico` en formulario de tratamiento

> DEPENDE de P0 completado por Agent 1. NO iniciar si `GET /api/catalogo/articulaciones` devuelve 404.
> Flujo UX segun briefing del developer:
> 1. Formulario `Nuevo tratamiento` contiene checkbox `Juego terapeutico`.
> 2. Al pulsar `Crear`/`Guardar`, si el checkbox esta marcado, se abre un modal secundario `Seleccionar juego`.
> 3. El modal muestra un `ComboBox` con los juegos filtrados por la `articulacion` asociada a la discapacidad seleccionada.
> 4. Al pulsar `Guardar` en el modal secundario, el juego queda vinculado al tratamiento recien creado (`PUT /api/catalogo/tratamientos/{cod}/juego`).
> 5. Si el checkbox esta desmarcado, el tratamiento se guarda como antes (sin juego) y no aparece el modal.

### P4.1 Extender `CatalogoDAO` y `CatalogoService` (desktop)

**Archivo:** `desktop/src/main/java/com/javafx/DAO/CatalogoDAO.java`

Anadir metodos:

```java
public List<Articulacion> listarArticulaciones() throws ConexionException { ... }
public List<Juego> listarJuegos() throws ConexionException { ... }
public List<Juego> listarJuegosPorArticulacion(int idArticulacion) throws ConexionException { ... }
public void asociarJuegoATratamiento(String codTrat, String codJuego) throws ConexionException, ValidacionException { ... }
public void desasociarJuegoDeTratamiento(String codTrat) throws ConexionException { ... }
```

Usar `ApiClient.get/put` existentes. JSON → record via `ObjectMapper` ya usado en `listarDiscapacidades`.

**Archivo:** `desktop/src/main/java/com/javafx/service/CatalogoService.java`

Espejo trivial 1:1 sobre `CatalogoDAO`. No anadir logica adicional — servicio solo propaga excepciones domain.

### P4.2 Extender DTO `TratamientoRequest` y modelo `Tratamiento`

**Archivo:** `desktop/src/main/java/com/javafx/dto/TratamientoRequest.java`

Dejar el record SIN `codJuego` (el juego se vincula en segunda llamada, no en `POST /tratamientos`). Motivo: simplifica error handling y mantiene compatibilidad con la API existente.

**Archivo:** `desktop/src/main/java/com/javafx/Clases/Tratamiento.java`

Anadir campo opcional `private String codJuego;` con getter/setter. Inicializar a `null`. Deserializar si la API lo devuelve.

### P4.3 Checkbox en `VentanaAgregarTratamiento.fxml`

Insertar entre el bloque de `Nivel progresion` (line 50-55) y el `<Separator />` (line 57):

```xml
<HBox alignment="CENTER_LEFT" spacing="10.0">
    <children>
        <Label prefWidth="140.0" text="Juego terapeutico:" />
        <CheckBox fx:id="chkJuegoTerapeutico" text="Asociar juego tras guardar" />
    </children>
</HBox>
```

Import FXML: anadir `<?import javafx.scene.control.CheckBox?>` tras `<?import javafx.scene.control.Button?>` (line 4).

### P4.4 Controlador `controladorAgregarTratamiento.java`

1. Anadir `@FXML private CheckBox chkJuegoTerapeutico;` (line 45 aprox).
2. En `cargarDatosParaEdicion(Tratamiento t)`: `chkJuegoTerapeutico.setSelected(t.getCodJuego() != null);`.
3. En `guardarTratamiento(ActionEvent event)` tras el `mostrarVentanaInformativa(EXITO)` pero ANTES de `cerrarVentana(event)`:

```java
if (chkJuegoTerapeutico.isSelected()) {
    Discapacidad dis = cmbDiscapacidad.getValue();
    Integer idArt = (dis != null) ? dis.getIdArticulacion() : null;
    String codTrat = modoEdicion ? codigoOriginal : txtCodigo.getText().trim();
    abrirModalSeleccionJuego(codTrat, idArt);
} else if (modoEdicion && codigoOriginal != null) {
    // Si se desmarca en edicion, desvincular juego previo.
    try {
        catalogoService.desasociarJuegoDeTratamiento(codigoOriginal);
    } catch (Exception e) {
        System.err.println("Aviso: no se pudo desvincular juego previo: " + e.getMessage());
    }
}
cerrarVentana(event);
```

4. Metodo nuevo `private void abrirModalSeleccionJuego(String codTrat, Integer idArticulacion)`:

```java
private void abrirModalSeleccionJuego(String codTrat, Integer idArticulacion) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaSeleccionarJuego.fxml"));
        Parent root = loader.load();
        controladorSeleccionarJuego ctrl = loader.getController();
        ctrl.configurar(codTrat, idArticulacion);

        Scene scene = new Scene(root);
        controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

        Stage stage = new Stage();
        stage.setTitle("Seleccionar juego terapeutico");
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        VentanaUtil.establecerIconoVentana(stage);
        stage.showAndWait();
    } catch (Exception e) {
        e.printStackTrace();
        VentanaUtil.mostrarVentanaInformativa(
            "No se pudo abrir la seleccion de juego. El tratamiento se creo sin juego asociado.",
            TipoMensaje.ADVERTENCIA
        );
    }
}
```

### P4.5 Nuevo FXML `VentanaSeleccionarJuego.fxml`

Crear en `desktop/src/main/resources/VentanaSeleccionarJuego.fxml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.ComboBox?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.Separator?>
<?import javafx.scene.control.TextArea?>
<?import javafx.scene.layout.HBox?>
<?import javafx.scene.layout.VBox?>

<VBox alignment="TOP_CENTER" prefWidth="480.0" spacing="12.0" styleClass="root modal-root"
      xmlns="http://javafx.com/javafx/23.0.1" xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.javafx.Interface.controladorSeleccionarJuego">
    <padding><Insets bottom="20.0" left="20.0" right="20.0" top="20.0" /></padding>
    <children>
        <Label styleClass="label-titulo" text="Seleccionar juego terapeutico" />
        <Separator />
        <Label fx:id="lblArticulacion" text="Articulacion: -" />
        <HBox alignment="CENTER_LEFT" spacing="10.0">
            <children>
                <Label prefWidth="110.0" text="Juego:" />
                <ComboBox fx:id="cmbJuego" prefWidth="320.0" promptText="Seleccione un juego" />
            </children>
        </HBox>
        <VBox spacing="3.0">
            <children>
                <Label text="Descripcion:" />
                <TextArea fx:id="txtDescripcionJuego" editable="false" prefHeight="80.0" wrapText="true" />
            </children>
        </VBox>
        <Separator />
        <HBox alignment="CENTER" spacing="10.0">
            <children>
                <Button fx:id="btnCancelarJuego" onAction="#cerrarSinGuardar" prefWidth="120.0" styleClass="button-peligro" text="Cancelar" />
                <Button fx:id="btnGuardarJuego" onAction="#guardarJuego" prefWidth="120.0" styleClass="button-primario" text="Guardar" />
            </children>
        </HBox>
    </children>
</VBox>
```

### P4.6 Controlador `controladorSeleccionarJuego.java`

Crear en `desktop/src/main/java/com/javafx/Interface/controladorSeleccionarJuego.java`:

```java
package com.javafx.Interface;

// imports minimos: FXML, ActionEvent, Stage, ComboBox, Label, TextArea, StringConverter.
// imports de dominio: Juego, CatalogoService, excepciones.

public class controladorSeleccionarJuego {
    @FXML private Label lblArticulacion;
    @FXML private ComboBox<Juego> cmbJuego;
    @FXML private TextArea txtDescripcionJuego;
    @FXML private Button btnCancelarJuego;
    @FXML private Button btnGuardarJuego;

    private CatalogoService service;
    private String codTrat;
    private Integer idArticulacion;

    @FXML
    public void initialize() {
        service = new CatalogoService();
        cmbJuego.setConverter(new StringConverter<Juego>() {
            @Override public String toString(Juego j) { return j == null ? "" : j.nombre(); }
            @Override public Juego fromString(String s) { return null; }
        });
        cmbJuego.valueProperty().addListener((obs, old, nu) -> {
            txtDescripcionJuego.setText(nu == null ? "" : (nu.descripcion() == null ? "" : nu.descripcion()));
        });
    }

    // Inyeccion externa desde controladorAgregarTratamiento.
    public void configurar(String codTrat, Integer idArticulacion) {
        this.codTrat = codTrat;
        this.idArticulacion = idArticulacion;
        cargarJuegos();
    }

    private void cargarJuegos() {
        try {
            List<Juego> juegos;
            if (idArticulacion != null) {
                juegos = service.listarJuegosPorArticulacion(idArticulacion);
                lblArticulacion.setText("Articulacion ID: " + idArticulacion);
            } else {
                juegos = service.listarJuegos();
                lblArticulacion.setText("Articulacion: (no definida — mostrando todos)");
            }
            cmbJuego.getItems().setAll(juegos);
            if (juegos.isEmpty()) {
                VentanaUtil.mostrarVentanaInformativa(
                    "No hay juegos disponibles para esta articulacion.",
                    TipoMensaje.ADVERTENCIA
                );
            }
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                "Error al cargar juegos: " + e.getMessage(),
                TipoMensaje.ERROR
            );
        }
    }

    @FXML
    void guardarJuego(ActionEvent event) {
        Juego sel = cmbJuego.getValue();
        if (sel == null) {
            VentanaUtil.mostrarVentanaInformativa("Selecciona un juego antes de guardar.", TipoMensaje.ADVERTENCIA);
            return;
        }
        try {
            service.asociarJuegoATratamiento(codTrat, sel.codJuego());
            VentanaUtil.mostrarVentanaInformativa("Juego asociado correctamente.", TipoMensaje.EXITO);
            cerrarSinGuardar(event);
        } catch (ValidacionException e) {
            VentanaUtil.mostrarVentanaInformativa("Validacion: " + e.getMessage(), TipoMensaje.ERROR);
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa("Error de conexion: " + e.getMessage(), TipoMensaje.ERROR);
        }
    }

    @FXML
    void cerrarSinGuardar(ActionEvent event) {
        Stage stage = (Stage) btnCancelarJuego.getScene().getWindow();
        stage.close();
    }
}
```

### P4.7 Disparo del modal en edicion

En el controlador padre `controladorVentanaTratamientos`, el handler `editarTratamientoSeleccionado` NO cambia: el `chkJuegoTerapeutico` estara precargado y el usuario puede pulsar `Guardar` para volver a disparar el modal si lo desea. El codigo en P4.4 ya gestiona ambos modos.

### P4.8 Reflejo en la tabla padre

Anadir columna opcional a `VentanaTratamientos.fxml`: tras la columna `colNivel` (line 102) anadir:

```xml
<TableColumn fx:id="colJuego" prefWidth="140.0" text="Juego" />
```

En `controladorVentanaTratamientos.initialize()` cablear `colJuego.setCellValueFactory(new PropertyValueFactory<>("codJuego"));`. Mostrar el `codJuego` o `-` si es null.

### P4.9 Verificacion P4

1. Crear tratamiento con checkbox MARCADO → aparece modal → seleccionar juego → guardar → tabla refleja juego.
2. Crear tratamiento con checkbox DESMARCADO → NO aparece modal → tabla sin juego.
3. Editar tratamiento con juego existente → checkbox aparece marcado → desmarcar y guardar → tratamiento queda sin juego (verificar en BD: `SELECT cod_juego FROM tratamiento WHERE cod_trat=?` devuelve NULL).
4. Si la discapacidad del tratamiento no tiene `id_articulacion`, el modal debe cargar TODOS los juegos (fallback de P4.6).
5. Si no hay juegos para esa articulacion, aparece aviso y el ComboBox queda vacio.

---

## PHASE P5 — ACCEPTANCE + TESTING

### P5.1 Checklist final

- [ ] P0.1 Migration `V13__juego_articulacion.sql` aplicada; `SELECT count(*) FROM articulacion;` >= 15.
- [ ] P0.2 Endpoints `articulaciones`, `juegos`, `tratamientos/{cod}/juego` responden 200/201 esperado.
- [ ] P1 Cancelar en `Añadir tratamiento` y `Añadir discapacidad` NO muestra popup de error. Logs limpios.
- [ ] P2 Tema oscuro → `Añadir paciente` y `Añadir sanitario` con fondo oscuro uniforme.
- [ ] P3.1 Botones de busqueda azules en Sanitarios, Pacientes, Tratamientos, Discapacidades.
- [ ] P3.2 Boton `Añadir` identico visualmente en las 3 pestanas (tamano + color + margen).
- [ ] P4 Crear tratamiento con `Juego terapeutico` → modal con juegos filtrados por articulacion → guardar → vinculo persistido.
- [ ] P4 Edicion de tratamiento respeta el estado del checkbox (marca/desmarca => vincula/desvincula).
- [ ] Verificaciones en tema claro Y tema oscuro.

### P5.2 Tests unitarios (mandato `desktop/CLAUDE.md` §2.4)

Anadir JUnit 5 + Mockito en `desktop/src/test/java/`:

- `CatalogoServiceJuegoTest` — stub de `CatalogoDAO`, verifica que `asociarJuegoATratamiento` propaga `ValidacionException` cuando la articulacion no coincide.
- `controladorSeleccionarJuegoTest` — mock `CatalogoService`, verifica `configurar()` con `idArticulacion=null` carga todos los juegos; con id valido llama `listarJuegosPorArticulacion`.

Ejecutar: `./gradlew test`. Todas las suites verdes antes de marcar P4 completo.

### P5.3 TestSprite (mandato `/CLAUDE.md` §10)

Tras cada fase, delegar verificacion a TestSprite MCP. Bucle de self-healing hasta 100%. Si tras 5 intentos consecutivos TestSprite no pasa en la misma fase, notificar al developer.

### P5.4 Actualizar `desktop/CLAUDE.md` §7

Anadir tras el bloque de `Advanced integrations (future phases)`:

```
### Therapeutic game linking (this sprint)
- [ ] P0 API + schema entregado por Agent 1.
- [ ] P1 Bug cancelar en Tratamientos/Discapacidades corregido.
- [ ] P2 Bug fondo blanco en dark mode (Añadir paciente/sanitario) corregido.
- [ ] P3 Paridad visual botones busqueda + anadir.
- [ ] P4 Checkbox juego terapeutico + modal de seleccion + vinculo API.
- [ ] P5 Suite JUnit + TestSprite verdes.
```

Al completar cada item, cambiar `[ ]` a `[x]`. No borrar entradas hasta cierre de sprint.

---

## PHASE GUARDRAILS — cosas que el Doer NO debe hacer

- NO bypassar P0: implementar P4 contra una API ausente romperia el desktop entero con NPEs y es obligatoria la coordinacion con Agent 1.
- NO almacenar una lista local de juegos hardcoded como fallback permanente. El fallback "cargar todos los juegos si idArticulacion=null" es SOLO para el caso de discapacidad sin articulacion, no para sustituir la API.
- NO modificar la pestana de pacientes ni la de sanitarios fuera de P2.
- NO tocar `controladorVentanaOpciones.aplicarConfiguracionAScene` fuera de P2.3.3.
- NO refactorizar FXMLs que funcionan: solo aplicar las ediciones prescritas linea a linea.
- NO introducir comentarios en ingles en el codigo (regla `/CLAUDE.md` §4.5).
- NO marcar una fase como `[x]` en `desktop/CLAUDE.md` sin TestSprite al 100%.
- Si surge cualquier ambiguedad tecnica (por ejemplo, que el `id_articulacion` de `discapacidad` no este en el DTO actual porque Agent 1 no lo expuso), DETENER y consultar con el developer antes de improvisar un fallback.

---

*Fin del plan. Doer: abre issues separados por fase. No mezclar commits de distintas fases.*
