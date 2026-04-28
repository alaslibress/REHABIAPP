# PLAN.md — Desktop iteration 2026-04-28

> **Branch:** stats-implementation
> **Author:** Agent 0/3 Thinker (Opus) — PRESCRIPTIVE. Doer (Sonnet) MUST follow step by step.
> **Language:** All code/comments in Spanish (root `CLAUDE.md` §4.5).
> **Scope:** Phases A-E del checklist `/desktop/CLAUDE.md` §7.

---

## 0. CONTEXTO OBLIGATORIO

Antes de tocar codigo, leer:

1. `/CLAUDE.md` raiz — §4.5 estilo, §4.6 seguridad, §10 TestSprite.
2. `/desktop/CLAUDE.md` — §7 checklist Phase A-E.
3. `/desktop/.claude/skills/javafx-java24/` — todas las reglas.
4. `/api/PLAN.md` Phase 5-7 — endpoints nuevos que el desktop consumira.
5. `/data/PLAN.md` Phase 5-6 — pipeline de progreso y MD.
6. `/desktop/src/main/resources/css/tema_claro.css` y `tema_oscuro.css` — completos.
7. Controladores y FXML afectados (listados en cada fase).

---

## PHASE A — UI FIXES

### A.1 — Homologar botones de busqueda y "Anadir"

**Diagnostico:** En `VentanaSanitarios.fxml` los botones tienen estilo `boton-primario` con icono `imagenes/anadir.png` 16x16 y la SearchBar de ControlsFX. En `VentanaDiscapacidades.fxml` y `VentanaTratamientos.fxml` se usaron clases CSS distintas y un TextField simple en lugar de SearchBar.

**Fix:**

1. Abrir `VentanaSanitarios.fxml` y copiar el HBox cabecera (TextField busqueda + Button "Anadir") al portapapeles como referencia.
2. En `VentanaDiscapacidades.fxml`: reemplazar el HBox cabecera actual con la misma estructura, ajustando ids (`txfBuscarDiscapacidades`, `btnAnadirDiscapacidad`) y los handlers FXML.
3. Mismo paso para `VentanaTratamientos.fxml` (`txfBuscarTratamientos`, `btnAnadirTratamiento`).
4. Verificar que las clases CSS aplicadas son IDENTICAS — `cabecera-listado`, `boton-primario`, `campo-busqueda`. Si alguna no existe en discapacidad/tratamiento, anadirla SIN duplicar definiciones CSS.
5. Confirmar visualmente que los tres listados (sanitarios, discapacidades, tratamientos) son indistinguibles en cabecera.

### A.2 — Botones Aceptar/Cancelar en filtro de tratamientos

**Diagnostico:** `VentanaFiltroTratamientos.fxml` carece de la barra inferior con botones presente en `VentanaFiltroPacientes.fxml`.

**Fix:**

1. Anadir `<HBox styleClass="modal-footer">` al final de `VentanaFiltroTratamientos.fxml`:
```xml
<HBox styleClass="modal-footer" spacing="10" alignment="CENTER_RIGHT">
    <Button fx:id="btnCancelar" text="Cancelar" styleClass="boton-secundario" onAction="#cancelar"/>
    <Button fx:id="btnAceptar"  text="Aceptar"  styleClass="boton-primario"  onAction="#aceptar"/>
</HBox>
```
2. En `controladorFiltroTratamientos.java` anadir `@FXML private void cancelar()` (cerrar sin aplicar) y `@FXML private void aceptar()` (aplicar filtros y cerrar). Si los metodos ya existen con otro nombre, NO renombrar — apuntar el `onAction` al existente.

### A.3 — Centrado de textos en ventanas emergentes

**Fix:**

1. En `tema_claro.css` y `tema_oscuro.css`, anadir al bloque "MEJORAS VISUALES v2":
```css
.modal-texto-centrado, .alert .content, .dialog-pane .content {
    -fx-text-alignment: center;
    -fx-alignment: center;
}
```
2. En `VentanaUtil.mostrarVentanaInformativa(...)` y `mostrarConfirmacion(...)` anadir `dialogPane.getStyleClass().add("modal-texto-centrado");` antes de `showAndWait()`.
3. Auditoria de cada FXML modal (`VentanaAgregarPaciente`, `VentanaAgregarSanitario`, `VentanaAgregarTratamiento`, `VentanaAgregarDiscapacidad`, `VentanaFiltroPacientes`, `VentanaFiltroTratamientos`) — anadir `styleClass="modal-texto-centrado"` al Label de titulo y a cualquier Label informativo.

### A.4 — Fondo blanco en tema oscuro (CRITICO — auditoria CSS profunda)

**Diagnostico previo:** Intentos anteriores fallaron probablemente por:
- AnchorPane / VBox raiz del FXML SIN `styleClass`, heredando default blanco de JavaFX.
- Reglas CSS especificas de `.text-field` o `.scroll-pane` que sobreescriben el fondo del padre.
- Selector demasiado generico (`*`) sin `!important` o con menor especificidad que el style en linea de SceneBuilder.

**Fix prescriptivo (3 pasos):**

1. **Auditoria con SceneBuilder (manual):**
   - Abrir `VentanaAgregarPaciente.fxml` y `VentanaAgregarSanitario.fxml`.
   - Inspeccionar la jerarquia: el ROOT (probablemente `AnchorPane` o `VBox`) DEBE tener `styleClass="modal-root"`.
   - Eliminar cualquier `style="-fx-background-color: ..."` o `style="..."` inline en el root y descendientes.

2. **CSS — anadir reglas con especificidad explicita y herencia controlada:**

En `tema_oscuro.css`, en el bloque "MEJORAS VISUALES v2", anadir AL FINAL (mayor prioridad por orden):
```css
/* Forzar fondo correcto en TODOS los modales del tema oscuro */
.root .modal-root,
.root .modal-root > * ,
.modal-root,
.modal-root > AnchorPane,
.modal-root > VBox,
.modal-root > ScrollPane,
.modal-root > ScrollPane > .viewport {
    -fx-background-color: -color-fondo-panel;
}
.modal-root .scroll-pane,
.modal-root .scroll-pane > .viewport,
.modal-root .scroll-pane .content {
    -fx-background-color: transparent;
}
```

En `tema_claro.css` mantener el mismo selector pero con el color claro correspondiente.

3. **Verificacion E2E:**
   - Compilar y arrancar con tema oscuro activo.
   - Abrir VentanaAgregarPaciente y VentanaAgregarSanitario.
   - Verificar que NINGUN area se ve blanca (incluido el ScrollPane interno).
   - Repetir con tema claro y comprobar que sigue funcionando.

**No-go:** No usar `!important`. JavaFX CSS no soporta `!important` y romperia el cascade.

---

## PHASE B — TREATMENT PDF IMPORT

### B.1 — FileChooser en VentanaAgregarTratamiento

1. En FXML anadir HBox tras los campos de definicion:
```xml
<HBox spacing="10" alignment="CENTER_LEFT">
    <Label text="PDF del tratamiento:" styleClass="label-formulario"/>
    <Button fx:id="btnImportarPdf" text="Importar PDF" styleClass="boton-secundario" onAction="#importarPdf"/>
    <Label fx:id="lblNombrePdf" text="(Sin archivo)" styleClass="label-secundario"/>
    <Button fx:id="btnEliminarPdf" text="Eliminar" styleClass="boton-peligro" onAction="#eliminarPdf" visible="false"/>
</HBox>
```

2. En `controladorAgregarTratamiento.java` anadir:
```java
private byte[] pdfBytes;
private String pdfNombre;

@FXML
private void importarPdf() {
    FileChooser fc = new FileChooser();
    fc.setTitle("Seleccionar PDF del tratamiento");
    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
    File f = fc.showOpenDialog(btnImportarPdf.getScene().getWindow());
    if (f == null) return;
    try {
        byte[] bytes = Files.readAllBytes(f.toPath());
        if (bytes.length > 10 * 1024 * 1024) {
            VentanaUtil.mostrarVentanaInformativa("El PDF supera 10 MB.", TipoMensaje.ADVERTENCIA);
            return;
        }
        // Verificar magic bytes "%PDF-"
        if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F') {
            VentanaUtil.mostrarVentanaInformativa("El archivo no es un PDF valido.", TipoMensaje.ERROR);
            return;
        }
        this.pdfBytes = bytes;
        this.pdfNombre = f.getName();
        lblNombrePdf.setText(pdfNombre + " (" + (bytes.length / 1024) + " KB)");
        btnEliminarPdf.setVisible(true);
    } catch (IOException e) {
        VentanaUtil.mostrarVentanaInformativa("No se pudo leer el archivo: " + e.getMessage(), TipoMensaje.ERROR);
    }
}

@FXML
private void eliminarPdf() {
    pdfBytes = null;
    pdfNombre = null;
    lblNombrePdf.setText("(Sin archivo)");
    btnEliminarPdf.setVisible(false);
}
```

3. En el handler "Guardar", si `pdfBytes != null`, llamar tras el POST/PUT del tratamiento a `catalogoService.subirPdfTratamiento(codTrat, pdfBytes, pdfNombre)`.

### B.2 — Modo edicion: cargar PDF existente

En `cargarDatosParaEdicion(Tratamiento t)`:
- Llamar a `catalogoService.consultarMetadatosPdf(codTrat)` (devuelve `{nombre, tamano}` o null si no hay PDF).
- Si hay PDF: setear `lblNombrePdf` con el nombre y mostrar `btnEliminarPdf`.
- NO descargar bytes hasta que el usuario pulse "Reemplazar" (cambia el boton a "Importar PDF" + un boton "Descargar para revisar").

### B.3 — Extender CatalogoDAO

Anadir en `CatalogoDAO.java`:
```java
public void subirPdfTratamiento(String codTrat, byte[] bytes, String filename) throws RehabiAppException;
public PdfMetadato consultarMetadatosPdf(String codTrat) throws RehabiAppException;
public byte[] descargarPdfTratamiento(String codTrat) throws RehabiAppException;
public void eliminarPdfTratamiento(String codTrat) throws RehabiAppException;
```

`PdfMetadato` es un `record(String nombre, long tamano)`.

Implementacion: usar el `ApiClient.postMultipart(...)` (anadir helper si no existe — multipart es nuevo en el desktop).

---

## PHASE C — TREATMENT-GAME ASSOCIATION

### C.1 — Modelo Videojuego

Crear `desktop/src/main/java/com/javafx/Clases/Videojuego.java`:
```java
public record Videojuego(
    Long idVideojuego,
    String codigo,
    String nombre,
    String descripcion,
    String codDis,
    String parteCuerpo,
    String urlUnity,
    boolean activo
) {}
```

### C.2 — VideojuegoDAO

Crear `desktop/src/main/java/com/javafx/DAO/VideojuegoDAO.java` con `listarTodos()` y `listarPorDiscapacidad(String codDis)` llamando a los endpoints `/api/videojuegos` (ver `api/PLAN.md` Phase 6).

### C.3 — Extender CatalogoDAO

Anadir:
```java
public List<Videojuego> listarJuegosDeTratamiento(String codTrat) throws RehabiAppException;
public void vincularJuego(String codTrat, long idVideojuego) throws RehabiAppException;
public void desvincularJuego(String codTrat, long idVideojuego) throws RehabiAppException;
```

### C.4 — UI en VentanaAgregarTratamiento

Anadir tras el ComboBox de discapacidad:
```xml
<Label text="Videojuegos terapeuticos asociados:" styleClass="label-formulario"/>
<TableView fx:id="tablaJuegos" prefHeight="180" styleClass="tabla-secundaria">
    <columns>
        <TableColumn fx:id="colJuegoSel" prefWidth="40"  text="Sel"/>
        <TableColumn fx:id="colJuegoCod" prefWidth="100" text="Codigo"/>
        <TableColumn fx:id="colJuegoNom" prefWidth="220" text="Nombre"/>
        <TableColumn fx:id="colJuegoCue" prefWidth="120" text="Parte cuerpo"/>
    </columns>
</TableView>
```

`colJuegoSel` es `TableColumn<Videojuego, Boolean>` con `CheckBoxTableCell`.

### C.5 — Wiring en controlador

En `controladorAgregarTratamiento.java`:
- Listener del ComboBox discapacidad: al cambiar, recarga `tablaJuegos` con `videojuegoDAO.listarPorDiscapacidad(codDis)`.
- En modo edicion, marcar como seleccionados los juegos que devuelve `catalogoService.listarJuegosDeTratamiento(codTrat)`.
- Al guardar: calcular diff entre seleccion original y actual → llamar a `vincularJuego` / `desvincularJuego` por cada cambio.

### C.6 — RBAC

Si el usuario es nurse, deshabilitar `tablaJuegos` (`setDisable(true)`) — solo visualizacion.

---

## PHASE D — PATIENT PROGRESS VISUALIZATION

### D.1 — VentanaProgresoPaciente.fxml

Estructura:
```xml
<VBox styleClass="modal-root, panel-card" prefWidth="900" prefHeight="700">
    <HBox styleClass="modal-header">
        <Label fx:id="lblTituloProgreso" text="Progreso del paciente" styleClass="label-titulo"/>
        <Region HBox.hgrow="ALWAYS"/>
        <Label fx:id="lblUltimaActualizacion" text="" styleClass="label-secundario"/>
        <Button fx:id="btnRecargar" text="Actualizar" onAction="#recargar" styleClass="boton-secundario"/>
        <Button fx:id="btnCerrar"   text="Cerrar"    onAction="#cerrar"    styleClass="boton-secundario"/>
    </HBox>
    <ScrollPane fx:id="scrollGraficos" fitToWidth="true" styleClass="scroll-progreso">
        <VBox fx:id="contenedorGraficos" spacing="20" styleClass="contenedor-graficos"/>
    </ScrollPane>
    <Label fx:id="lblEstado" text="" styleClass="label-estado"/>
</VBox>
```

### D.2 — controladorVentanaProgresoPaciente

```java
public class controladorVentanaProgresoPaciente {
    private String dniPac;
    private final ProgresoService progresoService = new ProgresoService();
    private SyncProgresoService syncService;

    public void inicializarConDni(String dniPac) {
        this.dniPac = dniPac;
        cargarProgreso();
        // Phase E: arrancar polling
        syncService = new SyncProgresoService(dniPac, this::onNuevosDatos);
        syncService.iniciar();
    }

    private void cargarProgreso() {
        Task<List<ProgresoTratamiento>> task = new Task<>() {
            @Override protected List<ProgresoTratamiento> call() throws Exception {
                return progresoService.obtenerProgreso(dniPac);
            }
        };
        task.setOnSucceeded(e -> renderizarGraficos(task.getValue()));
        task.setOnFailed(e -> mostrarError(task.getException()));
        new Thread(task).start();
    }

    private void renderizarGraficos(List<ProgresoTratamiento> datos) {
        contenedorGraficos.getChildren().clear();
        if (datos.isEmpty()) {
            lblEstado.setText("No hay datos de progreso para este paciente.");
            return;
        }
        for (ProgresoTratamiento p : datos) {
            contenedorGraficos.getChildren().add(GraficoUtil.crearLineChart(p));
        }
        lblUltimaActualizacion.setText("Ultima actualizacion: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }
    // ... onNuevosDatos, recargar, cerrar, mostrarError
}
```

### D.3, D.4 — Botones "Progreso"

- En `VentanaListarPaciente.fxml`: anadir `Button text="Progreso"` en la cabecera de acciones, con handler `abrirProgreso(ActionEvent)` que invoca `VentanaHelper.abrirModal("VentanaProgresoPaciente.fxml", ctrl -> ctrl.inicializarConDni(this.paciente.getDniPac()))`.
- En `VentanaPacientes.fxml`: anadir `Button text="Progreso"` junto a "Generar listado PDF". Handler:
```java
@FXML
private void abrirProgresoPacienteSeleccionado() {
    Paciente sel = tablaPacientes.getSelectionModel().getSelectedItem();
    if (sel == null) {
        VentanaUtil.mostrarVentanaInformativa("Selecciona un paciente para ver su progreso.", TipoMensaje.ADVERTENCIA);
        return;
    }
    VentanaHelper.abrirModal("VentanaProgresoPaciente.fxml",
        (controladorVentanaProgresoPaciente ctrl) -> ctrl.inicializarConDni(sel.getDniPac()));
}
```

### D.5 — ProgresoDAO

```java
public class ProgresoDAO {
    public List<ProgresoTratamiento> obtenerProgreso(String dni) throws RehabiAppException {
        // GET /api/pacientes/{dni}/progreso
    }
    public CheckProgresoResponse comprobarNuevosDatos(String dni, Instant desde) throws RehabiAppException {
        // GET /api/pacientes/{dni}/progreso/check?since=...
    }
    public String obtenerMarkdown(String dni) throws RehabiAppException {
        // GET /api/pacientes/{dni}/progreso/markdown (Content-Type: text/markdown)
    }
}
```

### D.6 — ProgresoService

Wraps el DAO con cache de 30s en memoria por DNI. Limpia cache cuando `comprobarNuevosDatos` devuelve true.

### D.7 — GraficoUtil

```java
public static LineChart<Number, String> crearLineChart(ProgresoTratamiento p) {
    NumberAxis ejeX = new NumberAxis();
    ejeX.setLabel(p.metricaNombre()); // ej. "Rango de movimiento (grados)"
    CategoryAxis ejeY = new CategoryAxis();
    ejeY.setLabel("Fecha");
    LineChart<Number, String> chart = new LineChart<>(ejeX, ejeY);
    chart.setTitle(p.tratamientoNombre() + " — " + p.parteCuerpo());

    XYChart.Series<Number, String> serie = new XYChart.Series<>();
    serie.setName("Progreso");
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    for (ProgresoEntrada e : p.entradas()) {
        serie.getData().add(new XYChart.Data<>(e.valor(), e.fecha().atZone(ZoneOffset.UTC).format(fmt)));
    }
    chart.getData().add(serie);

    // Marcar baseline (primer registro) y current (ultimo) en color destacado
    if (!serie.getData().isEmpty()) {
        serie.getData().getFirst().getNode().getStyleClass().add("punto-baseline");
        serie.getData().getLast().getNode().getStyleClass().add("punto-actual");
    }
    return chart;
}
```

### D.8 — Tests

`desktop/src/test/java/com/javafx/service/ProgresoServiceTest.java` — mock ProgresoDAO con MockWebServer (si esta en build.gradle, si no skip).

---

## PHASE E — BACKGROUND SYNC

### E.1 — SyncProgresoService

```java
public class SyncProgresoService {
    private final String dniPac;
    private final Consumer<Boolean> callback;
    private ScheduledExecutorService executor;
    private Instant ultimoCheck = Instant.EPOCH;

    public SyncProgresoService(String dniPac, Consumer<Boolean> callback) {
        this.dniPac = dniPac;
        this.callback = callback;
    }

    public void iniciar() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sync-progreso-" + dniPac);
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::comprobar, 0, 30, TimeUnit.SECONDS);
    }

    private void comprobar() {
        try {
            CheckProgresoResponse res = new ProgresoDAO().comprobarNuevosDatos(dniPac, ultimoCheck);
            ultimoCheck = Instant.now();
            if (res.hasNewData()) Platform.runLater(() -> callback.accept(true));
        } catch (Exception e) {
            // log y silencioso — no spam
        }
    }

    public void detener() {
        if (executor != null) executor.shutdownNow();
    }
}
```

### E.2-E.4 — Integracion

- `controladorVentanaProgresoPaciente.onCleanup()` invoca `syncService.detener()`.
- `onNuevosDatos(true)` recarga charts y muestra Toast.
- Si el check falla, no spam: marcar `lblEstado` con "Sin conexion — datos en cache" y reintentar al siguiente tick.

---

## PHASE F — TESTING (mandatory)

### F.1 Static check

```
./gradlew clean compileJava test
```

Sin warnings. Fix cualquier import.

### F.2 Manual verification matrix

| Check | Expected |
|-------|----------|
| A.1 Sanitario/Discapacidad/Tratamiento cabeceras identicas | Confirmacion visual lado a lado |
| A.2 Filtro tratamientos tiene Aceptar/Cancelar funcionales | Aplicar filtro cierra modal y filtra tabla |
| A.3 Todas las alertas/dialogs centradas | Confirmacion visual en 6+ modales |
| A.4 Tema oscuro: agregar paciente/sanitario sin fondo blanco | Confirmacion visual en ambas |
| B.1 Subir PDF 5MB → exito; 11MB → rechazo; .docx → rechazo | Mensajes claros |
| B.2 Editar tratamiento con PDF existente → muestra metadatos | Filename + KB visibles |
| C.4 Cambiar discapacidad en tratamiento → tabla de juegos refresca | Solo aparecen los de la nueva disc. |
| C.5 Marcar 2 juegos + guardar → POST a /api/tratamientos/{c}/videojuegos/{id} x2 | Verificar via logs API |
| D.3 Doble-click paciente → ficha con boton Progreso | Visible y clickable |
| D.4 Pacientes tab + Progreso sin seleccion → ADVERTENCIA | Mensaje correcto |
| D.4 Pacientes tab + Progreso con seleccion → abre VentanaProgreso | Abre con DNI correcto |
| D.7 Charts renderizados con baseline/current destacados | Visual |
| E.1 Polling cada 30s mientras tab abierta | Logs del SyncProgresoService |
| E.2 Si llega nuevo dato → Toast + recarga | Probar inyectando sesion en MongoDB |
| E.4 Cerrar tab → polling para | Logs muestran shutdown |

### F.3 TestSprite MCP

Tras matriz manual OK, delegar a TestSprite por root `CLAUDE.md` §10.2. NO marcar `[x]` hasta TestSprite 100%.

---

## G. ORDER OF EXECUTION

1. Phase A (UI fixes) — independiente, no requiere API/Data nuevos.
2. Phase B (PDF import) — requiere endpoints API Phase 7 (api/PLAN.md). Si API no listo, pausar y arrancar Phase A.
3. Phase C (Game association) — requiere API Phase 6 + tabla videojuego.
4. Phase D (Progress visualization) — requiere API Phase 5 + Data Phase 5.
5. Phase E (Background sync) — incremental sobre D.
6. Phase F (Tests) — al final de cada fase.

---

## H. NON-NEGOTIABLES

- Comentarios en castellano. Sin ingles en Java/FXML/CSS.
- Sin emojis.
- Sin inline `setStyle(...)`.
- Sin nuevas dependencias en `build.gradle` excepto PDFBox (justificado en Phase B si se necesita preview).
- Sin tocar codigo fuera del scope del plan.
- TestSprite 100% antes de marcar `[x]` (root `CLAUDE.md` §10.4).

---

*End of plan.*
