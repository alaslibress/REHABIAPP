# PLAN.md — Desktop iteracion 2026-05-07 (stats-implementation)

> **Branch:** stats-implementation
> **Autor:** Agente 3 Thinker (Opus) — PRESCRIPTIVO. Doer (Sonnet) implementa paso a paso sin reinterpretar.
> **Idioma:** Codigo y comentarios en castellano (root `/CLAUDE.md` §4.5).
> **Scope:** Iteracion final. Tres tareas nuevas (G, H, I) + test (J).
> **Estado API:** TODOS los endpoints ya estan operativos en `/api`. Doer NO toca `/api`.
>
> Las fases 0/A/B/C/D/E del plan anterior estan IMPLEMENTADAS y se conservan como referencia en la seccion final (§REFERENCIA — fases ya completadas). NO retocar nada de esas fases salvo que la fase G/H/I lo exija explicitamente.

---

## 0. CONTEXTO OBLIGATORIO (LEER ANTES DE TOCAR CODIGO)

1. `/CLAUDE.md` raiz — §4.5 estilo, §4.6 seguridad, §10 TestSprite.
2. `/desktop/CLAUDE.md` — §7 checklist Phase A-E (todas ya estan tachadas excepto los items que esta iteracion va a tachar).
3. `/desktop/.claude/skills/javafx-java24/SKILL.md` — todas las reglas (threading, MVC, CSS, no setStyle).
4. `desktop/src/main/resources/tema_claro.css` y `tema_oscuro.css` — completos.
5. `desktop/src/main/java/com/javafx/Clases/ApiClient.java` — helpers HTTP (`get`, `getBytes`, `post`, `delete`, `uploadFile`).

### 0BIS. CONTRATOS DE LA API (referencia rapida — NO inventar paths)

| Operacion | Metodo + Path | Auth | Response |
|-----------|---------------|------|----------|
| Listar progreso por tratamiento | `GET /api/pacientes/{dni}/progreso` | Bearer | `List<ProgresoTratamientoResponse>` |
| Check nuevos datos progreso | `GET /api/pacientes/{dni}/progreso/check?since=<Instant ISO>` | Bearer | `CheckProgresoResponse{hasNewData,lastSessionAt,count}` |
| Markdown progreso | `GET /api/pacientes/{dni}/progreso/markdown` | Bearer | `text/markdown;charset=UTF-8` |
| Forzar regeneracion MD | `POST /api/pacientes/{dni}/progreso/markdown/regenerar` | Bearer SPECIALIST | 202 Accepted |
| Listar videojuegos activos | `GET /api/videojuegos` | Bearer | `List<VideojuegoResponse>` |
| Videojuegos por discapacidad | `GET /api/videojuegos/discapacidad/{codDis}` | Bearer | `List<VideojuegoResponse>` |
| Juegos vinculados a tratamiento | `GET /api/catalogo/tratamientos/{cod}/videojuegos` | Bearer | `List<VideojuegoResponse>` |
| Vincular juego | `POST /api/catalogo/tratamientos/{cod}/videojuegos/{id}` | SPECIALIST | 200 OK |
| Desvincular juego | `DELETE /api/catalogo/tratamientos/{cod}/videojuegos/{id}` | SPECIALIST | 204 |
| Subir PDF tratamiento | `POST /api/tratamientos/{cod}/pdf` (multipart `file`) | SPECIALIST | 200 |
| Descargar PDF tratamiento | `GET /api/tratamientos/{cod}/pdf` | Bearer | `application/pdf` (binario) |
| Metadatos PDF | `GET /api/tratamientos/{cod}/pdf/metadatos` | Bearer | `PdfMetadatosResponse` |
| Eliminar PDF | `DELETE /api/tratamientos/{cod}/pdf` | SPECIALIST | 204 |

DTOs ya existentes en desktop (`com.javafx.Clases`):
`ProgresoEntrada(Instant fecha, Double valor)`,
`ProgresoTratamiento(...)`, `CheckProgreso(...)`, `PdfMetadato(...)`, `Videojuego(...)`.

NO crear nuevos records. NO renombrar campos.

---

## DECISIONES DEL THINKER PARA ESTA ITERACION

| Tema | Opciones evaluadas | Decision | Justificacion |
|------|--------------------|----------|---------------|
| Vista de tratamiento read-only | a) FXML nuevo `VentanaVerTratamiento.fxml` con controlador propio · b) Reutilizar `VentanaAgregarTratamiento.fxml` con un metodo `cargarDatosParaVer(...)` que deshabilita controles | **b) Reutilizar el FXML existente** | Cero duplicacion de UI. La FXML actual ya tiene los campos, tabla de juegos y boton "Descargar PDF". Solo hay que anadir un modo "ver" que: deshabilita inputs, oculta `btnImportarPdf`/`btnEliminarPdf`/`btnGuardar`, muestra `btnDescargarPdf`, cambia `btnCancelar` a "Cerrar" y el titulo a "Ver Tratamiento". Mantener un solo controlador con tres modos (`crear`, `editar`, `ver`) es mas barato que duplicar 200 lineas de FXML + un controlador nuevo. |
| Visualizacion de progreso del paciente | a) Previsualizacion JavaFX `LineChart` en modal · b) Generar y descargar PDF JasperReports con grafica | **a) Previsualizacion JavaFX `LineChart`** | Toda la infra ya existe (`VentanaProgresoPaciente.fxml`, `controladorVentanaProgresoPaciente`, `GraficoUtil.crearLineChart`, `ProgresoService`, `SyncProgresoService`, `ProgresoDAO`). Jasper exigiria: nuevo `.jrxml` con grafica de linea (configuracion compleja en JasperSoft Studio o XML manual), compilar `.jasper`, `JasperFillManager.fillReport`, `JasperExportManager.exportReportToPdfFile`, file-chooser, manejo de fuentes, y mantener un dataset paralelo. Coste de desarrollo > 10x. La vista interactiva ya cubre el caso de uso. La generacion en PDF queda en BACKLOG (Phase F del checklist) si el cliente lo pide. |
| Pestaña Pacientes rota | Diagnostico antes de fix | **Reproducir y aislar el stack trace** | El plan G impone que el Doer arranque la app, capture la traza en consola y la pegue en el commit. NO intentar fix a ciegas. Hay >5 hipotesis posibles (FXML, DAO, RBAC, NPE en cache de pestanias, error 500 del API). Sin la traza no se puede prescribir un fix. |

---

## PHASE G — FIX PESTAÑA PACIENTES ROTA

> **Sintoma reportado:** al pulsar el boton de la pestana "Pacientes" en `VentanaPrincipal`, la pestana no se abre (queda en blanco o se queda en la pestana anterior).
> **Hipotesis del Thinker (NO asumir cual es real hasta tener traza):**
> 1. `cargarPacientes()` en `controladorVentanaPacientes#initialize()` lanza excepcion (API caida, 401, deserializacion fallida, NPE en `paginacion`).
> 2. `cacheControladores` mantiene un controlador roto de una sesion anterior y al reusarlo `configurarPermisos()` revienta.
> 3. Mismatch FXML/controlador (algun `fx:id` declarado en FXML no existe en el controlador o viceversa).
> 4. El BorderPane principal recibe `null` porque `cargarPestania` lo absorbe en su `try/catch` general (`controladorVentanaPrincipal.java:279-282`).

### G.0 — Reproducir el bug

1. Levantar el stack completo (ver `/desktop/CLAUDE.md` §9):
   ```bash
   docker compose -f /home/alaslibres/DAM/RehabiAPP/infra/docker-compose.yml up -d postgres mongodb
   cd /home/alaslibres/DAM/RehabiAPP/api && set -a && source .env.local && set +a && ./mvnw spring-boot:run
   # nueva terminal
   cd /home/alaslibres/DAM/RehabiAPP/data && ./mvnw spring-boot:run
   # nueva terminal
   cd /home/alaslibres/DAM/RehabiAPP/desktop && ./gradlew run 2>&1 | tee /tmp/desktop-run.log
   ```
2. Login con `ADMIN0000 / admin`. La app entra en VentanaPrincipal (que segun `controladorVentanaPrincipal#initialize` ya carga "Pacientes" como primera pestana).
3. Anotar lo que se ve en pantalla:
   - Si la pestana esta en blanco → leer `/tmp/desktop-run.log` y copiar la PRIMERA traza que mencione `Error al cargar pestaña Pacientes:` o cualquier `Exception in thread`/`Caused by`.
   - Si la pestana inicial NO es Pacientes (otra pestana se carga primero), pulsar "Pacientes" y repetir.
4. Anadir la traza completa al PR description bajo `## Stack trace observado`.

### G.1 — Diagnostico segun traza

**Caso 1 — `ConexionException`/`AutenticacionException` en `pacienteDAO.listarTodos()` durante initialize:**

`controladorVentanaPacientes#cargarPacientes()` no captura excepciones. Cualquier fallo en `listarTodos()` aborta `initialize` y JavaFX devuelve `null` como contenido. Fix:

En `controladorVentanaPacientes.java`, refactorizar `cargarPacientes()` y enriquecer `initialize()`:

```java
private void cargarPacientes() {
    try {
        todosPacientes = pacienteDAO.listarTodos();
        if (todosPacientes == null) todosPacientes = new java.util.ArrayList<>();
        paginacion.setDatos(todosPacientes);
        System.out.println("Pacientes cargados: " + todosPacientes.size()
                + " (mostrando " + listaPacientes.size() + " por pagina)");
    } catch (com.javafx.excepcion.ConexionException e) {
        todosPacientes = new java.util.ArrayList<>();
        paginacion.setDatos(todosPacientes);
        VentanaUtil.mostrarVentanaInformativa(
            "No se pudo cargar la lista de pacientes (sin conexion con la API).",
            TipoMensaje.ERROR);
    } catch (Exception e) {
        todosPacientes = new java.util.ArrayList<>();
        paginacion.setDatos(todosPacientes);
        System.err.println("Error al cargar pacientes: " + e.getMessage());
        e.printStackTrace();
        VentanaUtil.mostrarVentanaInformativa(
            "Error al cargar pacientes: " + e.getMessage(), TipoMensaje.ERROR);
    }
}
```

Aplicar el mismo patron defensivo en `buscarPacientes(...)`, `realizarBusqueda()` y `aplicarFiltros(...)` — toda llamada a `pacienteDAO` debe ir envuelta en `try/catch` sin propagar a JavaFX.

**Caso 2 — Cache de controladores corrupto (`cacheControladores` reusa instancia tras login distinto):**

Si tras logout/login la pestana revienta porque el cache aun tiene un controlador de la sesion anterior:

En `controladorVentanaPrincipal#cargarPestania`, ya existe `limpiarCachePestania(...)` (linea 636) y `cacheControladores.clear()` (linea 650). Asegurar que se invoca al hacer logout:

Buscar el handler de logout existente. Si NO limpia el cache, anadir antes de mostrar la pantalla de inicio:
```java
cachePestanias.clear();
cacheControladores.clear();
```

**Caso 3 — `fx:id` declarado en FXML pero no en controlador (o al reves):**

Comparar `VentanaPacientes.fxml` (lineas 15-124) con `controladorVentanaPacientes.java` (lineas 40-94). Hoy ambos coinciden. Si la traza dice `IllegalArgumentException: ... not a valid type for FXML class` o `LoadException: ... fx:id`, identificar el fx:id y o bien anadir el campo al controlador o eliminarlo del FXML. NO renombrar.

**Caso 4 — `manejarDobleClicTabla` u otro handler nullable:**

Si la traza dice `NullPointerException` en `manejarDobleClicTabla`, asegurar que `tblPacientes != null` antes de `setOnMouseClicked`. Defensivo:
```java
if (tblPacientes != null) tblPacientes.setOnMouseClicked(this::manejarDobleClicTabla);
```

**Caso 5 — `cargarPestania` devuelve sin actualizar `bdpPrincipal.setCenter(contenido)` por excepcion silenciosa:**

En `controladorVentanaPrincipal#cargarPestania` (linea 230), la captura `catch (Exception e)` (linea 279) suprime errores. Modificar para que tras logear el error muestre un mensaje y deje un placeholder en el centro (NO dejar `bdpPrincipal` en estado inconsistente):

```java
} catch (Exception e) {
    System.err.println("Error al cargar pestaña " + nombrePestania + ": " + e.getMessage());
    e.printStackTrace();
    // Limpiar cache corrupto y mostrar placeholder
    cachePestanias.remove(nombrePestania);
    cacheControladores.remove(nombrePestania);
    Label errorLabel = new Label("No se pudo abrir la pestana \"" + nombrePestania
        + "\".\nRevise la conexion con el servidor o reinicie la aplicacion.");
    errorLabel.getStyleClass().add("label-error-pestania");
    VBox placeholder = new VBox(errorLabel);
    placeholder.setAlignment(javafx.geometry.Pos.CENTER);
    placeholder.getStyleClass().add("panel-card");
    bdpPrincipal.setCenter(placeholder);
    pestaniaActual = nombrePestania;
    VentanaUtil.mostrarVentanaInformativa(
        "Error al abrir la pestana de " + nombrePestania + ": " + e.getMessage(),
        VentanaUtil.TipoMensaje.ERROR);
}
```

Anadir en `tema_claro.css` y `tema_oscuro.css` al final:
```css
.label-error-pestania {
    -fx-text-fill: -color-peligro;
    -fx-font-size: 14px;
    -fx-text-alignment: center;
    -fx-wrap-text: true;
    -fx-padding: 30 30 30 30;
}
```

### G.2 — Validacion del fix

1. Reiniciar la API y la app.
2. Login y abrir la pestana de Pacientes → la tabla debe poblarse SIN excepciones en consola.
3. Apagar la API a proposito y volver a abrir la pestana → debe mostrar el modal `"Error al cargar pacientes: ..."` y dejar la tabla vacia (NO bloquear la app).
4. Levantar la API otra vez, cerrar y reabrir la pestana → la tabla se puebla.
5. `./gradlew test` debe seguir verde.

### G.3 — Tests

`desktop/src/test/java/com/javafx/Interface/ControladorVentanaPacientesTest.java` (test de integracion ligera con Mockito):
- `initialize_concapaApiCaida_noLanzaExcepcion`. Mockear `PacienteDAO#listarTodos` para que lance `ConexionException` y verificar que `tblPacientes` quede inicializada (no null) y la tabla este vacia.
- `cargarPacientes_concapaListaVacia_noRevienta`. `listarTodos` devuelve `Collections.emptyList()`.

Si la creacion de un controlador JavaFX en test es complicada (FXMLLoader requiere toolkit), usar `TestFx` solo si ya esta en `build.gradle`. Si NO esta, probar el metodo via instancia directa con campos `@FXML` inyectados manualmente con reflection, o mover la logica de carga a un metodo packagee-private testeable y dejar el test al nivel del DAO/Service.

---

## PHASE H — VISTA "VER TRATAMIENTO" (READ-ONLY)

> **Objetivo:** al pulsar (doble-click o boton "Ver") un tratamiento de la tabla, abrir la ficha del tratamiento en modo solo lectura. Reutilizar `VentanaAgregarTratamiento.fxml` mediante un nuevo modo `ver`. NO crear FXML nuevo.

### H.1 — Renombrar y anadir boton en `VentanaTratamientos.fxml`

El FXML actual tiene en la barra inferior los botones `Eliminar` y `Editar`. Cambiar el flujo:
- El boton "Editar" pasa a llamarse "Ver" y abre la ventana en modo `ver`.
- Anadir un nuevo boton "Editar" para los SPECIALIST que abre la ventana en modo `editar` (logica actual).
- El doble-click en la tabla pasa a abrir en modo `ver` (NO editar).

Editar `desktop/src/main/resources/VentanaTratamientos.fxml`, sustituir el HBox de botones inferior (lineas 110-115):

```xml
<HBox alignment="CENTER_RIGHT" prefHeight="50.0" prefWidth="200.0" spacing="10.0">
    <children>
        <Button fx:id="btnEliminarTratamiento" mnemonicParsing="false"
                onAction="#eliminarTratamientoSeleccionado"
                prefHeight="26.0" prefWidth="126.0"
                styleClass="button-peligro" text="Eliminar" textAlignment="CENTER" />
        <Button fx:id="btnEditarTratamiento" mnemonicParsing="false"
                onAction="#editarTratamientoSeleccionado"
                prefHeight="26.0" prefWidth="126.0"
                styleClass="button-secundario" text="Editar" />
        <Button fx:id="btnVerTratamiento" mnemonicParsing="false"
                onAction="#verTratamientoSeleccionado"
                prefHeight="26.0" prefWidth="126.0"
                styleClass="button-primario" text="Ver" />
    </children>
</HBox>
```

### H.2 — Cambios en `controladorVentanaTratamientos.java`

1. Anadir el campo:
   ```java
   @FXML
   private Button btnVerTratamiento;
   ```

2. Cambiar el doble-click para que abra en modo `ver` (linea 469):
   ```java
   private void manejarDobleClicTabla(MouseEvent event) {
       if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
           Tratamiento seleccionado = tblTratamientos.getSelectionModel().getSelectedItem();
           if (seleccionado != null) {
               verTratamientoSeleccionado(null);
           }
       }
   }
   ```

3. Anadir el handler `verTratamientoSeleccionado`:
   ```java
   /**
    * Abre el formulario en modo solo lectura para el tratamiento seleccionado.
    * Reutiliza VentanaAgregarTratamiento.fxml con la nueva flag "ver".
    */
   @FXML
   void verTratamientoSeleccionado(ActionEvent event) {
       Tratamiento seleccionado = tblTratamientos.getSelectionModel().getSelectedItem();
       if (seleccionado == null) {
           VentanaUtil.mostrarVentanaInformativa(
               "Debe seleccionar un tratamiento de la lista.",
               TipoMensaje.ADVERTENCIA);
           return;
       }
       try {
           FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarTratamiento.fxml"));
           Parent root = loader.load();
           controladorAgregarTratamiento controlador = loader.getController();
           controlador.cargarDatosParaVer(seleccionado);

           Scene scene = new Scene(root);
           controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

           Stage stage = new Stage();
           stage.setTitle("Ver Tratamiento");
           stage.setScene(scene);
           stage.initModality(Modality.APPLICATION_MODAL);
           stage.setResizable(false);
           VentanaUtil.establecerIconoVentana(stage);
           stage.showAndWait();
       } catch (Exception e) {
           System.err.println("Error al abrir vista de tratamiento: " + e.getMessage());
           e.printStackTrace();
           VentanaUtil.mostrarVentanaInformativa(
               "Error al abrir la vista del tratamiento.", TipoMensaje.ERROR);
       }
   }
   ```

4. RBAC en `configurarPermisos()`: el boton "Ver" SIEMPRE habilitado (incluso enfermero); "Editar"/"Eliminar"/"Anadir" siguen restringidos a SPECIALIST.

   Localizar el bloque `if (!sesion.esEspecialista()) { ... btnEditarTratamiento.setDisable(true); ... }` (lineas 289-309). Anadir DESPUES del `}` final, dentro del metodo:
   ```java
   // El boton "Ver" esta siempre disponible para todos los roles autorizados a esta pestana
   if (btnVerTratamiento != null) {
       btnVerTratamiento.setDisable(false);
       btnVerTratamiento.setOpacity(1.0);
   }
   ```

### H.3 — Anadir modo `ver` en `controladorAgregarTratamiento.java`

> Toda la logica de PDF y juegos ya esta en este controlador. Solo anadimos un modo nuevo que deshabilita inputs y oculta botones de mutacion.

1. Anadir un `enum` privado y un campo de estado al inicio de la clase (junto a `modoEdicion`):
   ```java
   /** Modos de uso del formulario. */
   private enum Modo { CREAR, EDITAR, VER }
   private Modo modo = Modo.CREAR;
   ```

   Mantener `modoEdicion` por compatibilidad con el codigo existente (`modoEdicion = (modo == Modo.EDITAR)`).

2. Anadir el metodo publico `cargarDatosParaVer(Tratamiento)`:
   ```java
   /**
    * Carga los datos del tratamiento en modo solo lectura.
    * Deshabilita todos los inputs, oculta los botones de mutacion (Importar/Eliminar PDF, Guardar)
    * y deja visible el boton Descargar PDF si existe PDF asociado.
    */
   public void cargarDatosParaVer(Tratamiento tratamiento) {
       // Reusar el cargado de datos del modo edicion
       cargarDatosParaEdicion(tratamiento);
       // Pero conmutar el modo y aplicar la mascara de solo lectura
       this.modo = Modo.VER;
       this.modoEdicion = false; // las validaciones de "guardar" no se ejecutan
       aplicarModoSoloLectura();
   }

   /**
    * Aplica la mascara de solo lectura: deshabilita inputs, oculta botones de mutacion
    * y reetiqueta el boton de cancelar a "Cerrar".
    */
   private void aplicarModoSoloLectura() {
       lblTituloVentana.setText("Ver Tratamiento");

       // Inputs: bloquear edicion (no usar setDisable para mantener legibilidad del texto)
       txtCodigo.setEditable(false);
       txtNombre.setEditable(false);
       txtAreaDefinicion.setEditable(false);
       cmbDiscapacidad.setDisable(true);
       cmbNivelProgresion.setDisable(true);

       // Tabla de juegos: solo lectura (mantener visible el check para que se vea cuales estan vinculados)
       tablaJuegos.setEditable(false);
       // Deshabilitar interaccion sobre la columna de seleccion: el usuario ve los marcados pero no puede tocar
       colJuegoSel.setEditable(false);

       // Botones de PDF: solo descargar
       btnImportarPdf.setVisible(false);
       btnImportarPdf.setManaged(false);
       btnEliminarPdf.setVisible(false);
       btnEliminarPdf.setManaged(false);
       // btnDescargarPdf permanece como esta — visible solo si hay PDF (ya gestionado por cargarMetadatosPdfDeTratamiento)

       // Botones del pie: ocultar Guardar, mantener Cancelar reetiquetado a "Cerrar"
       btnGuardar.setVisible(false);
       btnGuardar.setManaged(false);
       btnCancelar.setText("Cerrar");
       btnCancelar.getStyleClass().removeAll("button-peligro");
       if (!btnCancelar.getStyleClass().contains("button-secundario")) {
           btnCancelar.getStyleClass().add("button-secundario");
       }
   }
   ```

3. Localizar `cargarDatosParaEdicion(Tratamiento tratamiento)` (lineas 184+). Al inicio del metodo, antes de `lblTituloVentana.setText("Editar Tratamiento");`, anadir:
   ```java
   this.modo = Modo.EDITAR;
   ```
   Y al inicio de `initialize()` dejar implicito `this.modo = Modo.CREAR;` (es el default del campo).

4. Localizar `guardarTratamiento(...)` (buscar el metodo @FXML). Al inicio del metodo anadir:
   ```java
   if (modo == Modo.VER) {
       // Defensa en profundidad: btnGuardar esta oculto en modo ver, pero por si se llama por accion-key
       return;
   }
   ```

5. Verificar que `cargarMetadatosPdfDeTratamiento(...)` ya hace visible `btnDescargarPdf` cuando hay PDF (ya implementado). En modo `ver` con PDF presente el flujo es: usuario pulsa "Descargar" → `descargarPdf()` (ya existente) → FileChooser → guarda. NO requiere cambios.

### H.4 — Validacion manual

| # | Caso | Esperado |
|---|------|----------|
| H.4.1 | SPECIALIST: doble-click en un tratamiento | Abre en modo VER (titulo "Ver Tratamiento", inputs deshabilitados, botones Importar/Eliminar PDF/Guardar ocultos, "Cerrar" visible) |
| H.4.2 | SPECIALIST: pulsar boton "Ver" | Idem H.4.1 |
| H.4.3 | SPECIALIST: pulsar boton "Editar" | Abre en modo EDITAR (comportamiento previo, sin regresion) |
| H.4.4 | NURSE: la pestana de tratamientos es invisible (mantenido) o si por error es visible, los botones Editar/Eliminar/Anadir estan disabled y solo "Ver" funciona | Sin regresion |
| H.4.5 | Tratamiento con PDF: en modo ver, boton "Descargar" visible y funcional, "Importar"/"Quitar" ocultos | OK |
| H.4.6 | Tratamiento con 2 juegos vinculados: en modo ver, los checks aparecen marcados pero no se pueden cambiar | OK |
| H.4.7 | Pulsar "Cerrar" en modo ver | Cierra sin tocar nada |

### H.5 — Tests

`desktop/src/test/java/com/javafx/Interface/ControladorAgregarTratamientoModoVerTest.java`:
- `cargarDatosParaVer_ocultaBotonesDeMutacion`. Mockear `CatalogoService` y `VideojuegoDAO`. Cargar el FXML en TestFx (si esta en build) o instanciar el controlador a mano e inyectar campos `@FXML` por reflection. Verificar que `btnGuardar.isVisible() == false`, `btnImportarPdf.isVisible() == false`, `btnCancelar.getText().equals("Cerrar")`, `txtCodigo.isEditable() == false`.
- `guardarTratamiento_enModoVer_noLlamaServicio`. Forzar `modo = VER` por reflection y verificar que tras invocar `guardarTratamiento(null)` no se llamo a `catalogoService.crearTratamiento(...)` ni `actualizarTratamiento(...)`.

Si la suite de tests no soporta TestFx, contemplar el test al nivel de comportamiento del campo `modo` y la lista de visibilidad de botones (tests de invariantes).

---

## PHASE I — VISUALIZACION DE PROGRESO DEL PACIENTE (PREVIEW JAVAFX LINECHART)

> **Decision tomada:** previsualizacion JavaFX con `LineChart`. La generacion en JasperReports queda PARKED en backlog (`/desktop/CLAUDE.md` §7 Phase F).
>
> **Estado actual:** la infraestructura ya existe (`VentanaProgresoPaciente.fxml`, `controladorVentanaProgresoPaciente`, `GraficoUtil`, `ProgresoService`, `SyncProgresoService`, `ProgresoDAO`). Esta fase verifica el flujo end-to-end y completa los detalles que falten.

### I.0 — Verificacion del estado actual

Doer ejecuta antes de tocar nada:

1. `grep -n "abrirProgresoSeleccionado\|btnProgresoPaciente" /home/alaslibres/DAM/RehabiAPP/desktop/src/main/resources/VentanaPacientes.fxml /home/alaslibres/DAM/RehabiAPP/desktop/src/main/java/com/javafx/Interface/controladorVentanaPacientes.java`
2. `grep -rn "GraficoUtil.crearLineChart\|VentanaProgresoPaciente" /home/alaslibres/DAM/RehabiAPP/desktop/src/main`
3. Anotar en el PR cuales de los siguientes elementos YA estan presentes (`[x]`) o faltan (`[ ]`):
   - [x] `Videojuego.java`, `ProgresoEntrada.java`, `ProgresoTratamiento.java`, `CheckProgreso.java`, `PdfMetadato.java`
   - [x] `ProgresoDAO.java`, `VideojuegoDAO.java`
   - [x] `ProgresoService.java`, `SyncProgresoService.java`
   - [x] `GraficoUtil.java`
   - [x] `VentanaProgresoPaciente.fxml`, `controladorVentanaProgresoPaciente.java`
   - [x] Boton "Progreso" en `VentanaPacientes.fxml` (linea 114)
   - [x] Handler `abrirProgresoSeleccionado(...)` en `controladorVentanaPacientes` (linea 608)
   - [ ] Boton "Progreso" en `VentanaListarPaciente.fxml` (ficha del paciente — verificar)
   - [ ] Handler `abrirProgreso(...)` en `controladorVentanaPacienteListar.java`

### I.1 — Anadir boton "Progreso" en la ficha del paciente

Si el paso I.0 confirma que falta, anadir en `desktop/src/main/resources/VentanaListarPaciente.fxml` un `Button` en la cabecera de acciones de la ficha (junto a "Editar"/"Eliminar"):

```xml
<Button fx:id="btnProgresoPaciente" mnemonicParsing="false"
        onAction="#abrirProgreso"
        prefHeight="26.0" prefWidth="126.0"
        styleClass="button-secundario" text="Progreso" />
```

> Nota: localizar el HBox de botones existente (parecido al de `VentanaPacientes.fxml` lineas 107-122) y anadir el boton manteniendo el patron. NO romper la alineacion.

En `desktop/src/main/java/com/javafx/Interface/controladorVentanaPacienteListar.java` anadir:

```java
@FXML
private Button btnProgresoPaciente;

/** Abre la ventana de progreso clinico del paciente actual. */
@FXML
private void abrirProgreso(ActionEvent event) {
    if (dniPacienteActual == null || dniPacienteActual.isBlank()) {
        VentanaUtil.mostrarVentanaInformativa(
            "No se puede abrir el progreso: paciente no inicializado.",
            VentanaUtil.TipoMensaje.ADVERTENCIA);
        return;
    }
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaProgresoPaciente.fxml"));
        Parent root = loader.load();
        controladorVentanaProgresoPaciente ctrl = loader.getController();

        Scene scene = new Scene(root);
        controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

        Stage stage = new Stage();
        stage.setTitle("Progreso — " + dniPacienteActual);
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(true);
        VentanaUtil.establecerIconoVentana(stage);

        ctrl.inicializarConDni(dniPacienteActual);
        stage.showAndWait();
    } catch (Exception e) {
        System.err.println("Error al abrir progreso desde ficha: " + e.getMessage());
        e.printStackTrace();
        VentanaUtil.mostrarVentanaInformativa(
            "Error al abrir la ventana de progreso.",
            VentanaUtil.TipoMensaje.ERROR);
    }
}
```

> Si el campo que guarda el DNI del paciente actual NO se llama `dniPacienteActual`, usar el nombre real (probablemente `paciente.getDni()` o `dniPaciente`). NO renombrar campos existentes.

### I.2 — Endurecer `controladorVentanaProgresoPaciente`

Verificar las siguientes invariantes en el codigo actual (`controladorVentanaProgresoPaciente.java`). Aplicar fix solo donde NO se cumplan:

1. `inicializarConDni` debe llamarse SIEMPRE antes de `stage.showAndWait()`. Hoy se llama tras `stage.setScene(...)` y antes de `showAndWait()` (correcto en `controladorVentanaPacientes#abrirProgresoSeleccionado` linea 630). Verificar idem para el nuevo handler `abrirProgreso` de la ficha.

2. `cargarProgresoAsync` (linea 63) ya usa `Task` y `Platform.runLater` implicito via `setOnSucceeded`. Sin cambios.

3. `renderizarGraficos` (linea 90) limpia `contenedorGraficos` antes de redibujar. Sin cambios.

4. `cerrar` (linea 122) detiene el `syncService`. Verificar tambien que el `setOnCloseRequest` registrado en `inicializarConDni` (linea 53) detenga el sync al cerrar con la X. Hoy lo hace.

5. **Mejora opcional (NO bloqueante):** anadir un placeholder visual antes de cargar — un `ProgressIndicator` centrado en el contenedor mientras llega la respuesta. Hoy ya existe `indicadorCarga` (linea 30). Sin cambios.

### I.3 — `GraficoUtil.crearLineChart` — micro-revisiones

`GraficoUtil.java` actual instala los puntos baseline (primer punto) y actual (ultimo punto). Verificar:

1. Si `p.entradas()` esta vacia, NO romper. El metodo debe devolver un `LineChart` vacio con el titulo, sin lanzar `IndexOutOfBoundsException`. Si la condicion `if (!serie.getData().isEmpty())` ya cubre esto, sin cambios.

2. La fecha en el eje Y se formatea como `yyyy-MM-dd` (ya hecho). Si hay varias entradas en el mismo dia, JavaFX `CategoryAxis` los pinta superpuestos. **No es bug** — el caso real (1 sesion al dia) lo cubre. Si en pruebas reales aparecen sesiones agrupadas, escalar al Thinker.

3. Anadir tooltip con texto formateado claro (ya hecho con `Tooltip.install`).

4. CSS: las clases `.punto-baseline` y `.punto-actual` ya estan declaradas en el plan original (Phase D.4). Verificar que existen al final de `tema_claro.css` y `tema_oscuro.css`. Si no, anadir:
   ```css
   .grafico-progreso .chart-line-symbol.punto-baseline {
       -fx-background-color: -color-info, white;
       -fx-background-radius: 6px;
       -fx-padding: 4px;
   }
   .grafico-progreso .chart-line-symbol.punto-actual {
       -fx-background-color: -color-exito, white;
       -fx-background-radius: 6px;
       -fx-padding: 4px;
   }
   ```
   (Sustituir `-color-info`/`-color-exito` por el token CSS real ya definido al principio del tema.)

### I.4 — Mensaje de "no datos"

Si el paciente no tiene sesiones en MongoDB, `obtenerProgreso` devuelve lista vacia. `renderizarGraficos` ya muestra `"No hay datos de progreso para este paciente."` (linea 93). Sin cambios.

Tests manuales con un paciente que SI tiene sesiones (insertar manualmente en Mongo via `data` pipeline o usar el seed de `V14__datos_prueba_desarrollo.sql` si esta poblado): verificar que se pintan N graficos uno por (`tratamiento`, `parteCuerpo`).

### I.5 — Validacion E2E

| # | Caso | Esperado |
|---|------|----------|
| I.5.1 | VentanaPacientes + Progreso sin seleccion | Modal `"Selecciona un paciente para ver su progreso."` |
| I.5.2 | VentanaPacientes + Progreso con seleccion | Abre `VentanaProgresoPaciente` con titulo "Progreso — <DNI>" |
| I.5.3 | Ficha paciente (doble-click) + boton Progreso | Abre `VentanaProgresoPaciente` con titulo "Progreso — <DNI>" |
| I.5.4 | Paciente sin sesiones | Texto `"No hay datos de progreso para este paciente."` y sin graficos |
| I.5.5 | Paciente con N tratamientos y M sesiones | N `LineChart` apilados, scroll vertical, primer punto azul, ultimo verde |
| I.5.6 | Polling de 30s | Logs de la API muestran `GET /api/pacientes/<dni>/progreso/check` cada ~30s mientras la ventana este abierta |
| I.5.7 | Cerrar la ventana (X o boton Cerrar) | Logs muestran `shutdown` del executor del polling |
| I.5.8 | Apagar la API durante el polling | `lblEstado` queda en estado neutro, charts antiguos persisten, sin spam de modales |

### I.6 — Tests

Ya cubiertos por las pruebas existentes mencionadas en Phase D.8 / E.3 del plan original. Verificar que `./gradlew test` pasa al final de la fase.

---

## PHASE J — VERIFICACION GLOBAL Y TESTING

### J.1 — Compilacion y tests

```bash
cd /home/alaslibres/DAM/RehabiAPP/desktop
./gradlew clean compileJava test
```

Sin warnings nuevos. Sin tests rotos. Si MapStruct/Annotation processors generan warnings preexistentes, dejarlos.

### J.2 — Smoke test manual

| # | Caso | Esperado |
|---|------|----------|
| J.2.1 | Login ADMIN0000/admin → app abre Pacientes | Tabla poblada (Phase G OK) |
| J.2.2 | Apagar API y abrir Pacientes | Modal de error claro, app no se cuelga |
| J.2.3 | Pestana Tratamientos → boton Ver | Abre modo VER (Phase H OK) |
| J.2.4 | Pestana Tratamientos → doble-click | Abre modo VER (NO editar) |
| J.2.5 | Pestana Tratamientos → boton Editar | Abre modo EDITAR (sin regresion) |
| J.2.6 | Modo VER con PDF → Descargar | FileChooser → fichero PDF correcto |
| J.2.7 | Modo VER → Cerrar | Cierra modal sin cambios |
| J.2.8 | VentanaPacientes → seleccionar y pulsar Progreso | Modal con LineCharts (Phase I OK) |
| J.2.9 | Doble-click paciente → ficha → boton Progreso | Modal con LineCharts |
| J.2.10 | Cambiar tema (claro/oscuro) y repetir J.2.3..J.2.9 | Sin areas blancas en oscuro, sin regresiones |

### J.3 — TestSprite MCP

Tras smoke OK, delegar a TestSprite (root `/CLAUDE.md` §10). Marcar `[x]` en `/desktop/CLAUDE.md` §7 SOLO cuando TestSprite devuelva 100% en los items afectados.

### J.4 — Actualizar `/desktop/CLAUDE.md`

Una vez TestSprite verde, actualizar el checklist §7:
- Phase A.1 .. A.4 → `[x]` (ya lo estaban en el plan anterior, confirmar)
- Phase B.1 .. B.5 → `[x]` (ya implementado, confirmar)
- Phase C.1 .. C.6 → `[x]`
- Phase D.1 .. D.8 → `[x]`
- Phase E.1 .. E.4 → `[x]`
- Anadir entradas nuevas:
  - `[x] G.1 Pestana Pacientes resiliente a fallos del API.`
  - `[x] H.1 Vista "Ver Tratamiento" en modo solo lectura.`
  - `[x] I.1 Boton Progreso en ficha paciente y verificacion E2E del flujo de progreso.`

---

## K. ORDEN DE EJECUCION

```
Phase G (Pacientes fix)  --> independiente
Phase H (Ver Tratamiento) --> independiente
Phase I (Progreso preview) --> independiente
       |
       +--> Phase J (Testing al final)
```

Recomendacion: G → H → I → J. Las tres son ortogonales pero G corrige un bug bloqueante en la pantalla mas usada, asi que va primero.

---

## L. NON-NEGOTIABLES

- Comentarios y mensajes de UI en castellano. Nada en ingles en Java/FXML/CSS.
- Sin emojis.
- Sin `setStyle(...)` inline en Java.
- Sin nuevas dependencias en `build.gradle` (NO anadir Jasper para esta iteracion — es una decision explicita del Thinker).
- Toda I/O fuera del hilo JavaFX (`Task` / `ScheduledExecutorService`).
- Toda actualizacion UI desde background → `Platform.runLater`.
- TestSprite 100% antes de marcar `[x]` (root `/CLAUDE.md` §10.4).
- No tocar codigo fuera de los archivos listados en este plan sin aprobacion del Thinker.
- No renombrar metodos existentes; si hay conflicto, apuntar el `onAction` al metodo real ya presente.
- NO crear `VentanaVerTratamiento.fxml` separado. La decision es REUSAR `VentanaAgregarTratamiento.fxml` con un modo nuevo (ver §H).

---

## M. ARCHIVOS QUE EL DOER VA A CREAR / MODIFICAR

### Crear (nuevos)

```
desktop/src/test/java/com/javafx/Interface/ControladorVentanaPacientesTest.java
desktop/src/test/java/com/javafx/Interface/ControladorAgregarTratamientoModoVerTest.java
```

### Modificar

```
desktop/src/main/java/com/javafx/Interface/controladorVentanaPacientes.java     (Phase G — try/catch defensivo)
desktop/src/main/java/com/javafx/Interface/controladorVentanaPrincipal.java     (Phase G — placeholder en pestana fallida)
desktop/src/main/java/com/javafx/Interface/controladorVentanaTratamientos.java  (Phase H — boton Ver + handler + doble-click cambia a ver)
desktop/src/main/java/com/javafx/Interface/controladorAgregarTratamiento.java   (Phase H — modo VER + cargarDatosParaVer + aplicarModoSoloLectura)
desktop/src/main/java/com/javafx/Interface/controladorVentanaPacienteListar.java (Phase I — boton Progreso si falta)
desktop/src/main/resources/VentanaTratamientos.fxml                              (Phase H — anadir btnVerTratamiento)
desktop/src/main/resources/VentanaListarPaciente.fxml                            (Phase I — boton Progreso si falta)
desktop/src/main/resources/tema_claro.css                                        (Phase G + I — label-error-pestania, refuerzo punto-baseline/actual si falta)
desktop/src/main/resources/tema_oscuro.css                                        (idem)
```

### NO TOCAR

```
desktop/src/main/java/com/javafx/Clases/ApiClient.java                  (estable)
desktop/src/main/java/com/javafx/Clases/Paciente.java                   (estable)
desktop/src/main/java/com/javafx/Clases/Tratamiento.java                (estable)
desktop/src/main/java/com/javafx/Clases/Videojuego.java                 (estable)
desktop/src/main/java/com/javafx/Clases/ProgresoTratamiento.java        (estable)
desktop/src/main/java/com/javafx/Clases/ProgresoEntrada.java            (estable)
desktop/src/main/java/com/javafx/Clases/CheckProgreso.java              (estable)
desktop/src/main/java/com/javafx/Clases/PdfMetadato.java                (estable)
desktop/src/main/java/com/javafx/DAO/*.java                              (estables)
desktop/src/main/java/com/javafx/service/*.java                          (estables)
desktop/src/main/java/com/javafx/util/GraficoUtil.java                   (cambios solo si I.3 lo exige)
desktop/src/main/resources/VentanaProgresoPaciente.fxml                  (estable)
desktop/src/main/resources/VentanaAgregarTratamiento.fxml                (estable — el modo VER es solo logica del controlador, NO cambia el FXML)
api/**                                                                    (NUNCA — Doer 3 no toca la API)
data/**                                                                   (NUNCA)
```

---

## N. RESUMEN EJECUTIVO PARA EL DOER

1. **Bug bloqueante:** Phase G arregla la pestana Pacientes que no abre. Reproducir → leer traza → aplicar el fix correspondiente (caso 1 a 5). Endurecer `cargarPestania` para que un fallo nunca rompa la UI silenciosamente.
2. **Vista de tratamiento:** Phase H anade un nuevo modo VER al controlador `controladorAgregarTratamiento` reutilizando el FXML existente. Boton nuevo "Ver" en la pestana de tratamientos; doble-click abre VER (no editar). Sin nuevo FXML.
3. **Progreso del paciente:** Phase I confirma el flujo end-to-end (la mayor parte ya esta) y anade el boton "Progreso" en la ficha del paciente si faltara. Decision firme: previsualizacion JavaFX `LineChart` (NO Jasper).
4. **Testing:** Phase J pasa `./gradlew test`, smoke matrix manual, TestSprite 100%, y actualiza `/desktop/CLAUDE.md`.

Si algo no aparece arriba, NO inventar — escalar al Thinker.

---

## REFERENCIA — FASES YA COMPLETADAS (no tocar salvo necesidad)

> Las fases 0/A/B/C/D/E del plan anterior estan implementadas. Esta seccion existe solo para que el Doer pueda volver a leerlas si necesita el detalle exacto de un metodo o un FXML que ya existe.

### Phase 0 — Extender ApiClient
- `getBytes(String path)` y `uploadFile(...)` — IMPLEMENTADO en `ApiClient.java`.

### Phase A — UI fixes
- A.1 Cabeceras Discapacidades/Tratamientos homologadas con Sanitarios — IMPLEMENTADO.
- A.2 Botones Aceptar/Cancelar en `VentanaFiltroTratamientos.fxml` — IMPLEMENTADO.
- A.3 Centrar textos en modales — IMPLEMENTADO via clase `modal-texto-centrado`.
- A.4 Fondo blanco en tema oscuro corregido con clase `modal-root` — IMPLEMENTADO.

### Phase B — PDF tratamiento
- DAO + Service + UI + RBAC + tests — IMPLEMENTADO.

### Phase C — Asociacion tratamiento-juego
- DAO + Service + tabla en formulario + diff alta/baja al guardar + RBAC — IMPLEMENTADO.

### Phase D — Visualizacion progreso
- DTOs + DAO + Service + GraficoUtil + FXML + controlador — IMPLEMENTADO. La fase I de esta iteracion solo verifica end-to-end y anade el boton en la ficha del paciente si faltara.

### Phase E — Polling 30s
- `SyncProgresoService` + cleanup en cierre — IMPLEMENTADO.

---

*Fin del plan. Doer: ejecuta G → H → I → J en orden. Si algo no aparece arriba, NO inventar — escalar al Thinker.*
