# Plan: Pestana CRUD Discapacidades (Desktop)

> **Agente:** 3 (Desktop) - Pensador Opus
> **Ejecutor:** Sonnet (Doer)
> **Fecha:** 2026-04-13
> **Checklist:** 5 items de "Disability CRUD tab (desktop)" en CLAUDE.md

## Contexto

Los prerequisitos API estan completados (10/10 endpoints CRUD en `/api/catalogo`). Los prerequisitos desktop tambien (CatalogoDAO +9 metodos, CatalogoService +9 wrappers, DTOs creados). Falta la capa de presentacion: 2 FXML + 2 controladores + integracion en el tab cache.

La pestana "Discapacidades" ya esta conectada en `controladorVentanaPrincipal.java`:
- Boton sidebar: `btnPestaniaDiscapacidades` (VentanaPrincipal.fxml)
- Handler: `abrirPestaniaDiscapacidades()` -> `cargarPestania("Discapacidades")` (linea 420)
- RBAC: oculto para enfermeros en `inicializarSesion()` (lineas 209-215)
- Tab cache: `cargarPestania()` busca `/VentanaDiscapacidades.fxml` automaticamente

**PERO** hay un detalle: `cargarPestania()` tiene una cadena `instanceof` en lineas 254-258 para llamar `configurarPermisos()`. Solo tiene checks para `controladorVentanaSanitarios` y `controladorVentanaPacientes`. Hay que anadir el check para `controladorVentanaDiscapacidades`.

---

## FASE 1: Crear VentanaDiscapacidades.fxml

**Archivo a CREAR**: `/home/alaslibres/DAM/RehabiAPP/desktop/src/main/resources/VentanaDiscapacidades.fxml`

Replica EXACTA del patron de `VentanaPacientes.fxml` / `VentanaSanitarios.fxml` con estos cambios:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.TableColumn?>
<?import javafx.scene.control.TableView?>
<?import javafx.scene.control.TextField?>
<?import javafx.scene.control.Tooltip?>
<?import javafx.scene.image.Image?>
<?import javafx.scene.image.ImageView?>
<?import javafx.scene.layout.HBox?>
<?import javafx.scene.layout.VBox?>

<VBox fx:id="vboxContenedorPrinDiscapacidades" maxHeight="Infinity" maxWidth="Infinity" spacing="15.0" xmlns="http://javafx.com/javafx/23.0.1" xmlns:fx="http://javafx.com/fxml/1" fx:controller="com.javafx.Interface.controladorVentanaDiscapacidades">
    <padding>
        <Insets bottom="20.0" left="20.0" right="20.0" top="20.0" />
    </padding>
    <children>
        <HBox prefHeight="100.0" prefWidth="200.0">
            <children>
                <Label fx:id="lblTituloPestaniaDiscapacidades" prefHeight="18.0" prefWidth="200.0" styleClass="label-titulo" text="Discapacidades" />
                <VBox prefHeight="100.0" prefWidth="802.0" HBox.hgrow="ALWAYS">
                    <children>
                        <Button fx:id="btnSeleccionarTodoDiscapacidades" mnemonicParsing="false" onAction="#seleccionarTodasDiscapacidades" text="Seleccionar todo">
                            <VBox.margin>
                                <Insets bottom="20.0" left="150.0" />
                            </VBox.margin>
                        </Button>
                        <HBox alignment="CENTER_LEFT" prefHeight="74.0" prefWidth="103.0">
                            <children>
                                <Label fx:id="lblBuscarDiscapacidades" text="Buscar:">
                                    <HBox.margin>
                                        <Insets right="10.0" />
                                    </HBox.margin>
                                </Label>
                                <Button fx:id="btnBuscarDiscapacidades" mnemonicParsing="false" onAction="#buscarDiscapacidades" prefHeight="30.0" prefWidth="30.0" styleClass="button-icono">
                                    <graphic>
                                        <ImageView fitHeight="18.0" fitWidth="25.0" pickOnBounds="true" preserveRatio="true">
                                            <image>
                                                <Image url="@buscar.png" />
                                            </image>
                                        </ImageView>
                                    </graphic>
                                    <tooltip>
                                        <Tooltip text="Buscar" />
                                    </tooltip>
                                </Button>
                                <TextField fx:id="txfBuscarDiscapacidades" onAction="#buscarDiscapacidades" prefHeight="30.0" prefWidth="250.0" promptText="Buscar:">
                                    <padding>
                                        <Insets left="5.0" right="10.0" />
                                    </padding>
                                    <HBox.margin>
                                        <Insets right="50.0" />
                                    </HBox.margin>
                                </TextField>
                                <Button fx:id="btnAnadirDiscapacidad" mnemonicParsing="false" onAction="#abrirFormularioNuevaDiscapacidad" prefHeight="40.0" prefWidth="40.0" styleClass="button-primario">
                                    <graphic>
                                        <ImageView fitHeight="18.0" fitWidth="74.0" pickOnBounds="true" preserveRatio="true">
                                            <image>
                                                <Image url="@iconoAgregar.png" />
                                            </image>
                                        </ImageView>
                                    </graphic>
                                    <tooltip>
                                        <Tooltip text="Anadir nueva discapacidad" />
                                    </tooltip>
                                    <HBox.margin>
                                        <Insets right="20.0" />
                                    </HBox.margin>
                                </Button>
                            </children>
                        </HBox>
                    </children>
                </VBox>
            </children>
        </HBox>
        <HBox alignment="TOP_CENTER" spacing="10.0" VBox.vgrow="ALWAYS">
            <children>
                <TableView fx:id="tblDiscapacidades" maxHeight="Infinity" maxWidth="Infinity" prefHeight="479.0" HBox.hgrow="ALWAYS">
                    <columns>
                        <TableColumn fx:id="colCodigo" prefWidth="120.0" text="Codigo" />
                        <TableColumn fx:id="colNombre" prefWidth="200.0" text="Nombre" />
                        <TableColumn fx:id="colDescripcion" prefWidth="300.0" text="Descripcion" />
                        <TableColumn fx:id="colProtesis" prefWidth="120.0" text="Protesis" />
                    </columns>
                    <columnResizePolicy>
                        <TableView fx:constant="CONSTRAINED_RESIZE_POLICY" />
                    </columnResizePolicy>
                </TableView>
            </children>
        </HBox>
        <HBox alignment="CENTER_RIGHT" prefHeight="50.0" prefWidth="200.0" spacing="10.0">
            <children>
                <Button fx:id="btnEliminarDiscapacidad" mnemonicParsing="false" onAction="#eliminarDiscapacidadSeleccionada" prefHeight="26.0" prefWidth="126.0" styleClass="button-peligro" text="Eliminar" textAlignment="CENTER" />
                <Button fx:id="btnEditarDiscapacidad" mnemonicParsing="false" onAction="#editarDiscapacidadSeleccionada" prefHeight="26.0" prefWidth="126.0" styleClass="button-primario" text="Editar" />
            </children>
        </HBox>
    </children>
</VBox>
```

**Diferencias respecto a VentanaPacientes:**
- Sin boton de filtro (las discapacidades son un catalogo pequeno, no necesita filtros avanzados)
- Sin boton "Generar PDF" (no aplica a un catalogo)
- Footer: "Eliminar" + "Editar" (en vez de "Generar PDF" + "Eliminar" + "Abrir")
- 4 columnas: Codigo, Nombre, Descripcion, Protesis
- Todos los fx:id y handlers renombrados a "Discapacidad(es)"

---

## FASE 2: Crear controladorVentanaDiscapacidades.java

**Archivo a CREAR**: `/home/alaslibres/DAM/RehabiAPP/desktop/src/main/java/com/javafx/Interface/controladorVentanaDiscapacidades.java`

Sigue EXACTAMENTE el patron de `controladorVentanaPacientes.java` adaptado a Discapacidad:

```java
package com.javafx.Interface;

import com.javafx.Clases.Discapacidad;
import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.dto.DiscapacidadRequest;
import com.javafx.service.CatalogoService;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.RehabiAppException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controlador para la ventana de gestion de discapacidades del catalogo clinico.
 * Solo accesible para SPECIALIST (enfermeros no ven esta pestana).
 */
public class controladorVentanaDiscapacidades {

    @FXML
    private Button btnAnadirDiscapacidad;

    @FXML
    private Button btnBuscarDiscapacidades;

    @FXML
    private Button btnEditarDiscapacidad;

    @FXML
    private Button btnEliminarDiscapacidad;

    @FXML
    private Button btnSeleccionarTodoDiscapacidades;

    @FXML
    private TableColumn<Discapacidad, String> colCodigo;

    @FXML
    private TableColumn<Discapacidad, String> colDescripcion;

    @FXML
    private TableColumn<Discapacidad, String> colNombre;

    @FXML
    private TableColumn<Discapacidad, Boolean> colProtesis;

    @FXML
    private Label lblBuscarDiscapacidades;

    @FXML
    private Label lblTituloPestaniaDiscapacidades;

    @FXML
    private TableView<Discapacidad> tblDiscapacidades;

    @FXML
    private TextField txfBuscarDiscapacidades;

    @FXML
    private VBox vboxContenedorPrinDiscapacidades;

    // Lista observable de discapacidades
    private ObservableList<Discapacidad> listaDiscapacidades;

    // Servicio del catalogo
    private CatalogoService catalogoService;

    // Cache de todas las discapacidades
    private List<Discapacidad> todasDiscapacidades;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        catalogoService = new CatalogoService();
        listaDiscapacidades = FXCollections.observableArrayList();

        configurarTabla();

        // Permitir seleccion multiple
        tblDiscapacidades.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Doble clic para editar
        tblDiscapacidades.setOnMouseClicked(this::manejarDobleClicTabla);

        cargarDiscapacidades();
    }

    /**
     * Maneja el evento de doble clic en la tabla.
     * Si es doble clic, abre el formulario de edicion.
     */
    private void manejarDobleClicTabla(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            Discapacidad seleccionada = tblDiscapacidades.getSelectionModel().getSelectedItem();
            if (seleccionada != null) {
                editarDiscapacidadSeleccionada(null);
            }
        }
    }

    /**
     * Configura los permisos segun el rol del usuario.
     * Esta pestana solo es visible para SPECIALIST, pero se aplica RBAC
     * igualmente como medida de defensa en profundidad.
     */
    public void configurarPermisos() {
        SesionUsuario sesion = SesionUsuario.getInstancia();

        if (!sesion.esEspecialista()) {
            // Deshabilitar boton de anadir
            btnAnadirDiscapacidad.setDisable(true);
            btnAnadirDiscapacidad.setOpacity(0.5);
            btnAnadirDiscapacidad.setTooltip(new Tooltip(
                    "No tienes permisos para anadir discapacidades"));

            // Deshabilitar boton de eliminar
            btnEliminarDiscapacidad.setDisable(true);
            btnEliminarDiscapacidad.setOpacity(0.5);
            btnEliminarDiscapacidad.setTooltip(new Tooltip(
                    "No tienes permisos para eliminar discapacidades"));

            // Deshabilitar boton de editar
            btnEditarDiscapacidad.setDisable(true);
            btnEditarDiscapacidad.setOpacity(0.5);
            btnEditarDiscapacidad.setTooltip(new Tooltip(
                    "No tienes permisos para editar discapacidades"));

            System.out.println("Permisos de solo lectura aplicados para discapacidades: " + sesion.getCargo());
        } else {
            System.out.println("Permisos completos aplicados para discapacidades: " + sesion.getCargo());
        }
    }

    /**
     * Configura las columnas de la tabla
     */
    private void configurarTabla() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codDis"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreDis"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcionDis"));
        // Para protesis: mostrar "Si"/"No" en vez de true/false
        colProtesis.setCellValueFactory(new PropertyValueFactory<>("necesitaProtesis"));
        colProtesis.setCellFactory(column -> new javafx.scene.control.TableCell<Discapacidad, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "Si" : "No");
                }
            }
        });

        tblDiscapacidades.setItems(listaDiscapacidades);
    }

    /**
     * Carga todas las discapacidades desde el servicio del catalogo
     */
    private void cargarDiscapacidades() {
        try {
            todasDiscapacidades = catalogoService.listarDiscapacidades();
            listaDiscapacidades.clear();
            listaDiscapacidades.addAll(todasDiscapacidades);
            System.out.println("Discapacidades cargadas: " + todasDiscapacidades.size());
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de conexion al cargar discapacidades.",
                    TipoMensaje.ERROR
            );
        } catch (Exception e) {
            System.err.println("Error al cargar discapacidades: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al cargar las discapacidades: " + e.getMessage(),
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Busca discapacidades por texto (codigo o nombre)
     */
    @FXML
    void buscarDiscapacidades(ActionEvent event) {
        String textoBusqueda = txfBuscarDiscapacidades.getText().trim().toLowerCase();

        listaDiscapacidades.clear();

        if (textoBusqueda.isEmpty()) {
            // Si no hay texto, mostrar todas
            if (todasDiscapacidades != null) {
                listaDiscapacidades.addAll(todasDiscapacidades);
            } else {
                cargarDiscapacidades();
            }
        } else {
            // Filtrar localmente por codigo o nombre
            List<Discapacidad> filtradas = todasDiscapacidades.stream()
                    .filter(d -> d.getCodDis().toLowerCase().contains(textoBusqueda)
                              || d.getNombreDis().toLowerCase().contains(textoBusqueda))
                    .toList();
            listaDiscapacidades.addAll(filtradas);
        }
    }

    /**
     * Abre el formulario para crear una nueva discapacidad
     */
    @FXML
    void abrirFormularioNuevaDiscapacidad(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarDiscapacidad.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Nueva Discapacidad");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            VentanaUtil.establecerIconoVentana(stage);
            stage.showAndWait();

            // Recargar la lista
            cargarDiscapacidades();

        } catch (Exception e) {
            System.err.println("Error al abrir formulario de nueva discapacidad: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir el formulario de nueva discapacidad.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Abre el formulario de edicion para la discapacidad seleccionada
     */
    @FXML
    void editarDiscapacidadSeleccionada(ActionEvent event) {
        Discapacidad seleccionada = tblDiscapacidades.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar una discapacidad de la lista.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarDiscapacidad.fxml"));
            Parent root = loader.load();

            // Obtener controlador y cargar datos para edicion
            controladorAgregarDiscapacidad controlador = loader.getController();
            controlador.cargarDatosParaEdicion(seleccionada);

            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Editar Discapacidad");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            VentanaUtil.establecerIconoVentana(stage);
            stage.showAndWait();

            // Recargar la lista
            cargarDiscapacidades();

        } catch (Exception e) {
            System.err.println("Error al abrir formulario de edicion: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir la edicion de la discapacidad.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Elimina la discapacidad seleccionada con confirmacion previa.
     * La API rechaza con 409 si hay pacientes asignados.
     */
    @FXML
    void eliminarDiscapacidadSeleccionada(ActionEvent event) {
        Discapacidad seleccionada = tblDiscapacidades.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar una discapacidad para eliminar.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        String mensaje = "¿Esta seguro de que desea eliminar la discapacidad '"
                + seleccionada.getNombreDis() + "' (" + seleccionada.getCodDis() + ")?\n\n"
                + "Se eliminaran tambien sus vinculos con tratamientos.";

        boolean confirmado = VentanaUtil.mostrarVentanaPregunta(mensaje);

        if (confirmado) {
            try {
                catalogoService.eliminarDiscapacidad(seleccionada.getCodDis());
                VentanaUtil.mostrarVentanaInformativa(
                        "La discapacidad ha sido eliminada correctamente.",
                        TipoMensaje.EXITO
                );
                cargarDiscapacidades();

            } catch (DuplicadoException e) {
                // 409: hay pacientes asignados
                VentanaUtil.mostrarVentanaInformativa(
                        "No se puede eliminar: " + e.getMessage(),
                        TipoMensaje.ADVERTENCIA
                );
            } catch (ConexionException e) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Error de conexion con el servidor.",
                        TipoMensaje.ERROR
                );
            } catch (RehabiAppException e) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Error: " + e.getMessage(),
                        TipoMensaje.ERROR
                );
            }
        }
    }

    /**
     * Selecciona todas las discapacidades de la tabla
     */
    @FXML
    void seleccionarTodasDiscapacidades(ActionEvent event) {
        tblDiscapacidades.getSelectionModel().selectAll();
    }
}
```

**Notas para el Doer:**
- NO usa `PaginacionUtil` (el catalogo de discapacidades es pequeno, <100 registros)
- Busqueda es LOCAL (filtro en memoria sobre `todasDiscapacidades`), no llama al DAO por busqueda
- El `ApiClient` mapea HTTP 409 -> `DuplicadoException`. Usamos ese catch para el caso "no se puede eliminar porque hay pacientes asignados"
- El boton "Editar" abre el mismo FXML (`VentanaAgregarDiscapacidad.fxml`) pero llamando `cargarDatosParaEdicion()`
- `colProtesis` necesita un CellFactory custom para mostrar "Si"/"No" en vez de true/false

---

## FASE 3: Crear VentanaAgregarDiscapacidad.fxml

**Archivo a CREAR**: `/home/alaslibres/DAM/RehabiAPP/desktop/src/main/resources/VentanaAgregarDiscapacidad.fxml`

Formulario modal sencillo. Patron simplificado de `VentanaAgregarPaciente.fxml` (sin ScrollPane — el formulario es corto):

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.CheckBox?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.Separator?>
<?import javafx.scene.control.TextArea?>
<?import javafx.scene.control.TextField?>
<?import javafx.scene.layout.HBox?>
<?import javafx.scene.layout.VBox?>

<VBox alignment="TOP_CENTER" prefWidth="500.0" spacing="10.0" styleClass="root" xmlns="http://javafx.com/javafx/23.0.1" xmlns:fx="http://javafx.com/fxml/1" fx:controller="com.javafx.Interface.controladorAgregarDiscapacidad">
    <padding>
        <Insets bottom="20.0" left="20.0" right="20.0" top="20.0" />
    </padding>
    <children>
        <Label fx:id="lblTituloVentana" styleClass="label-titulo" text="Nueva Discapacidad" />

        <Separator />

        <HBox alignment="CENTER_LEFT" spacing="10.0">
            <children>
                <Label prefWidth="140.0" text="Codigo:" />
                <TextField fx:id="txtCodigo" prefWidth="280.0" promptText="Codigo de la discapacidad" />
            </children>
        </HBox>

        <HBox alignment="CENTER_LEFT" spacing="10.0">
            <children>
                <Label prefWidth="140.0" text="Nombre:" />
                <TextField fx:id="txtNombre" prefWidth="280.0" promptText="Nombre de la discapacidad" />
            </children>
        </HBox>

        <VBox spacing="3.0">
            <children>
                <Label text="Descripcion:" />
                <TextArea fx:id="txtAreaDescripcion" prefHeight="80.0" prefWidth="460.0" promptText="Descripcion de la discapacidad" wrapText="true" />
            </children>
        </VBox>

        <CheckBox fx:id="chkNecesitaProtesis" text="Necesita protesis" />

        <Separator />

        <HBox alignment="CENTER" spacing="10.0">
            <children>
                <Button fx:id="btnCancelar" onAction="#cerrarVentana" prefWidth="120.0" styleClass="button-peligro" text="Cancelar" />
                <Button fx:id="btnGuardar" onAction="#guardarDiscapacidad" prefWidth="120.0" styleClass="button-primario" text="Crear" />
            </children>
        </HBox>
    </children>
</VBox>
```

**Detalles de diseno:**
- Sin ScrollPane (solo 4 campos, cabe en pantalla)
- VBox raiz con `styleClass="root"` para que apliquen los estilos CSS del tema
- Patron identico al formulario de pacientes: Label de ancho fijo 140px + campo de entrada 280px
- TextArea para descripcion (multilinea)
- CheckBox para protesis (boolean)
- Footer: Cancelar (button-peligro) + Crear/Guardar (button-primario)

---

## FASE 4: Crear controladorAgregarDiscapacidad.java

**Archivo a CREAR**: `/home/alaslibres/DAM/RehabiAPP/desktop/src/main/java/com/javafx/Interface/controladorAgregarDiscapacidad.java`

Patron de `controladorAgregarPaciente.java` simplificado:

```java
package com.javafx.Interface;

import com.javafx.Clases.Discapacidad;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.dto.DiscapacidadRequest;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.RehabiAppException;
import com.javafx.excepcion.ValidacionException;
import com.javafx.service.CatalogoService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controlador del formulario modal para crear/editar discapacidades.
 * Modo creacion: campos vacios, boton "Crear", codigo editable.
 * Modo edicion: campos rellenados, boton "Guardar", codigo deshabilitado (PK no editable).
 */
public class controladorAgregarDiscapacidad {

    @FXML
    private Label lblTituloVentana;

    @FXML
    private TextField txtCodigo;

    @FXML
    private TextField txtNombre;

    @FXML
    private TextArea txtAreaDescripcion;

    @FXML
    private CheckBox chkNecesitaProtesis;

    @FXML
    private Button btnCancelar;

    @FXML
    private Button btnGuardar;

    // Servicio del catalogo
    private CatalogoService catalogoService;

    // Modo edicion
    private boolean modoEdicion = false;
    private String codigoOriginal;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        catalogoService = new CatalogoService();
    }

    /**
     * Carga los datos de una discapacidad existente para edicion.
     * Cambia el titulo, el texto del boton, y deshabilita el campo codigo.
     */
    public void cargarDatosParaEdicion(Discapacidad discapacidad) {
        modoEdicion = true;
        codigoOriginal = discapacidad.getCodDis();

        lblTituloVentana.setText("Editar Discapacidad");
        btnGuardar.setText("Guardar");

        // Codigo no editable en modo edicion (es la PK)
        txtCodigo.setText(discapacidad.getCodDis());
        txtCodigo.setDisable(true);

        txtNombre.setText(discapacidad.getNombreDis());
        txtAreaDescripcion.setText(discapacidad.getDescripcionDis());
        chkNecesitaProtesis.setSelected(discapacidad.isNecesitaProtesis());
    }

    /**
     * Valida los campos obligatorios del formulario.
     * @return true si todos los campos obligatorios estan rellenos
     */
    private boolean validarCampos() {
        if (txtCodigo.getText().trim().isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El codigo es obligatorio.",
                    TipoMensaje.ADVERTENCIA
            );
            txtCodigo.requestFocus();
            return false;
        }

        if (txtNombre.getText().trim().isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El nombre es obligatorio.",
                    TipoMensaje.ADVERTENCIA
            );
            txtNombre.requestFocus();
            return false;
        }

        if (txtCodigo.getText().trim().length() > 20) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El codigo no puede superar 20 caracteres.",
                    TipoMensaje.ADVERTENCIA
            );
            txtCodigo.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Guarda la discapacidad (creacion o actualizacion segun modo).
     */
    @FXML
    void guardarDiscapacidad(ActionEvent event) {
        if (!validarCampos()) {
            return;
        }

        DiscapacidadRequest request = new DiscapacidadRequest(
                txtCodigo.getText().trim(),
                txtNombre.getText().trim(),
                txtAreaDescripcion.getText().trim(),
                chkNecesitaProtesis.isSelected()
        );

        try {
            if (modoEdicion) {
                catalogoService.actualizarDiscapacidad(codigoOriginal, request);
                VentanaUtil.mostrarVentanaInformativa(
                        "La discapacidad ha sido actualizada correctamente.",
                        TipoMensaje.EXITO
                );
            } else {
                catalogoService.crearDiscapacidad(request);
                VentanaUtil.mostrarVentanaInformativa(
                        "La discapacidad ha sido creada correctamente.",
                        TipoMensaje.EXITO
                );
            }
            cerrarVentana(event);

        } catch (DuplicadoException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Dato duplicado: " + e.getMessage(),
                    TipoMensaje.ERROR
            );
        } catch (ValidacionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de validacion: " + e.getMessage(),
                    TipoMensaje.ERROR
            );
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de conexion con el servidor.",
                    TipoMensaje.ERROR
            );
        } catch (RehabiAppException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error: " + e.getMessage(),
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Cierra la ventana modal
     */
    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
}
```

**Notas para el Doer:**
- `DuplicadoException` viene del `ApiClient` cuando la API devuelve 409 (codigo o nombre duplicado)
- `ValidacionException` viene del `ApiClient` cuando la API devuelve 400 (campos vacios)
- En modo edicion: `txtCodigo.setDisable(true)` porque el codigo es la PK y no se puede cambiar
- `cargarDatosParaEdicion()` es publico para que el controlador padre lo llame despues de `loader.getController()`
- `cerrarVentana()` cierra el Stage del modal. Al volver al controlador padre, este llama `cargarDiscapacidades()`

---

## FASE 5: Integrar en el tab cache de controladorVentanaPrincipal

**Archivo a MODIFICAR**: `/home/alaslibres/DAM/RehabiAPP/desktop/src/main/java/com/javafx/Interface/controladorVentanaPrincipal.java`

### 5.1 Anadir instanceof check en cargarPestania()

Localizar la cadena `instanceof` en lineas 254-258:

```java
            // Configurar permisos segun el controlador
            if (controlador instanceof controladorVentanaSanitarios) {
                ((controladorVentanaSanitarios) controlador).configurarPermisos();
            } else if (controlador instanceof controladorVentanaPacientes) {
                ((controladorVentanaPacientes) controlador).configurarPermisos();
            }
```

Reemplazar con:

```java
            // Configurar permisos segun el controlador
            if (controlador instanceof controladorVentanaSanitarios) {
                ((controladorVentanaSanitarios) controlador).configurarPermisos();
            } else if (controlador instanceof controladorVentanaPacientes) {
                ((controladorVentanaPacientes) controlador).configurarPermisos();
            } else if (controlador instanceof controladorVentanaDiscapacidades) {
                ((controladorVentanaDiscapacidades) controlador).configurarPermisos();
            }
```

### 5.2 Anadir import

Al principio del archivo, anadir con los demas imports de Interface (si no existe ya):

```java
// NO es necesario: controladorVentanaDiscapacidades esta en el mismo paquete (com.javafx.Interface)
```

**NOTA**: Como `controladorVentanaDiscapacidades` esta en el mismo paquete `com.javafx.Interface`, NO necesita import. Solo hay que anadir el `else if`.

### 5.3 Verificacion

Despues de los 5 cambios, ejecutar:

```bash
cd /home/alaslibres/DAM/RehabiAPP/desktop && ./gradlew compileJava
```

Si compila OK, la verificacion final es:
1. `./gradlew run`
2. Login como ADMIN0000 / admin (SPECIALIST)
3. Click en "Discapacidades" en el sidebar
4. La tabla debe cargarse con las discapacidades del catalogo
5. Click "+" para crear nueva discapacidad (debe abrir modal)
6. Crear discapacidad de prueba -> debe aparecer en la tabla
7. Doble clic en la discapacidad -> modal de edicion (codigo deshabilitado)
8. Editar nombre -> guardar -> tabla actualizada
9. Seleccionar discapacidad -> "Eliminar" -> confirmar -> eliminada
10. Login como 00000002W / enfermero1234 (NURSE) -> pestana "Discapacidades" NO debe aparecer en sidebar

---

## RESUMEN DE ARCHIVOS

### Archivos a CREAR (4):

| # | Archivo | Descripcion |
|---|---------|-------------|
| 1 | `desktop/src/main/resources/VentanaDiscapacidades.fxml` | Pestana con tabla y botones CRUD |
| 2 | `desktop/src/main/java/com/javafx/Interface/controladorVentanaDiscapacidades.java` | Controlador de la pestana (listar, buscar, eliminar, abrir modal) |
| 3 | `desktop/src/main/resources/VentanaAgregarDiscapacidad.fxml` | Modal formulario crear/editar |
| 4 | `desktop/src/main/java/com/javafx/Interface/controladorAgregarDiscapacidad.java` | Controlador del modal (validar, crear, actualizar) |

### Archivos a MODIFICAR (1):

| # | Archivo | Cambio |
|---|---------|--------|
| 1 | `desktop/src/main/java/com/javafx/Interface/controladorVentanaPrincipal.java` | Anadir `else if (controlador instanceof controladorVentanaDiscapacidades)` en linea ~258 |

### Dependencias ya existentes (NO tocar):

- `CatalogoService.java` — ya tiene `crearDiscapacidad()`, `actualizarDiscapacidad()`, `eliminarDiscapacidad()`
- `CatalogoDAO.java` — ya tiene los 3 metodos CRUD de discapacidades
- `DiscapacidadRequest.java` — DTO ya creado
- `Discapacidad.java` — modelo JavaFX ya creado con factory `desdeDiscapacidadResponse()`
- `VentanaPrincipal.fxml` — boton sidebar ya existe (`btnPestaniaDiscapacidades`)
- `controladorVentanaPrincipal.java` — handler `abrirPestaniaDiscapacidades()` ya existe, RBAC ya oculta el boton para enfermeros
