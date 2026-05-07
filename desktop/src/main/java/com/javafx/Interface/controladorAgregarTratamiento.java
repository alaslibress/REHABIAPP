package com.javafx.Interface;

import com.javafx.Clases.Discapacidad;
import com.javafx.Clases.NivelProgresion;
import com.javafx.Clases.PdfMetadato;
import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.Tratamiento;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.Clases.Videojuego;
import com.javafx.DAO.VideojuegoDAO;
import com.javafx.dto.TratamientoRequest;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.RehabiAppException;
import com.javafx.excepcion.ValidacionException;
import com.javafx.service.CatalogoService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controlador del formulario modal para crear/editar tratamientos.
 * Modo creacion: campos vacios, boton "Crear", codigo editable.
 * Modo edicion: campos rellenados, boton "Guardar", codigo deshabilitado (PK no editable).
 */
public class controladorAgregarTratamiento {

    @FXML private Label    lblTituloVentana;
    @FXML private TextField txtCodigo;
    @FXML private TextField txtNombre;
    @FXML private TextArea  txtAreaDefinicion;
    @FXML private ComboBox<Discapacidad>   cmbDiscapacidad;
    @FXML private ComboBox<NivelProgresion> cmbNivelProgresion;
    @FXML private Button   btnCancelar;
    @FXML private Button   btnGuardar;

    // B — PDF
    @FXML private Button btnImportarPdf;
    @FXML private Label  lblNombrePdf;
    @FXML private Button btnDescargarPdf;
    @FXML private Button btnEliminarPdf;

    // C — Videojuegos
    @FXML private TableView<JuegoSeleccionable>             tablaJuegos;
    @FXML private TableColumn<JuegoSeleccionable, Boolean>  colJuegoSel;
    @FXML private TableColumn<JuegoSeleccionable, String>   colJuegoCod;
    @FXML private TableColumn<JuegoSeleccionable, String>   colJuegoNom;
    @FXML private TableColumn<JuegoSeleccionable, String>   colJuegoCue;

    private CatalogoService catalogoService;
    private final VideojuegoDAO videojuegoDAO = new VideojuegoDAO();

    /** Modos de uso del formulario. */
    private enum Modo { CREAR, EDITAR, VER }
    private Modo modo = Modo.CREAR;

    // Estado modo edicion (se mantiene por compatibilidad con el codigo existente)
    private boolean modoEdicion = false;
    private String  codigoOriginal;

    // Estado PDF
    private byte[]  pdfBytesPendientes;
    private String  pdfNombrePendiente;
    private boolean pdfMarcadoParaBorrar;
    private boolean tienePdfRemoto;

    private static final long PDF_MAX_BYTES = 10L * 1024 * 1024;

    // Estado juegos
    private final ObservableList<JuegoSeleccionable> juegosFila = FXCollections.observableArrayList();
    private Set<Long> juegosVinculadosOriginal = new HashSet<>();

    /** Wrapper observable para checkbox + Videojuego. */
    public static class JuegoSeleccionable {
        private final Videojuego juego;
        private final SimpleBooleanProperty seleccionado = new SimpleBooleanProperty(false);

        public JuegoSeleccionable(Videojuego j, boolean sel) {
            this.juego = j;
            this.seleccionado.set(sel);
        }

        public Videojuego getJuego() { return juego; }
        public SimpleBooleanProperty seleccionadoProperty() { return seleccionado; }
    }

    @FXML
    public void initialize() {
        catalogoService = new CatalogoService();
        cargarComboBoxes();
        configurarTablaJuegos();

        // RBAC: nurse solo puede ver, no editar juegos ni PDF
        SesionUsuario sesion = SesionUsuario.getInstancia();
        if (sesion.esEnfermero()) {
            tablaJuegos.setEditable(false);
            tablaJuegos.setDisable(true);
            btnImportarPdf.setDisable(true);
            btnEliminarPdf.setDisable(true);
        }
    }

    private void configurarTablaJuegos() {
        tablaJuegos.setEditable(true);
        colJuegoSel.setCellValueFactory(c -> c.getValue().seleccionadoProperty());
        colJuegoSel.setCellFactory(CheckBoxTableCell.forTableColumn(colJuegoSel));
        colJuegoCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getJuego().codigo()));
        colJuegoNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getJuego().nombre()));
        colJuegoCue.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getJuego().parteCuerpo()));
        tablaJuegos.setItems(juegosFila);
    }

    private void cargarComboBoxes() {
        try {
            List<Discapacidad> discapacidades = catalogoService.listarDiscapacidades();
            cmbDiscapacidad.getItems().setAll(discapacidades);
            cmbDiscapacidad.setConverter(new StringConverter<Discapacidad>() {
                @Override public String toString(Discapacidad d) { return d == null ? "" : d.getNombreDis(); }
                @Override public Discapacidad fromString(String s) { return null; }
            });
            cmbDiscapacidad.setPromptText("Seleccione discapacidad");

            List<NivelProgresion> niveles = catalogoService.listarNivelesProgresion();
            cmbNivelProgresion.getItems().setAll(niveles);
            cmbNivelProgresion.setConverter(new StringConverter<NivelProgresion>() {
                @Override public String toString(NivelProgresion n) { return n == null ? "" : n.getNombre(); }
                @Override public NivelProgresion fromString(String s) { return null; }
            });
            cmbNivelProgresion.setPromptText("Seleccione nivel");

            // Recargar juegos cuando cambia la discapacidad seleccionada
            cmbDiscapacidad.valueProperty().addListener((obs, oldD, newD) -> {
                if (newD == null) { juegosFila.clear(); return; }
                cargarJuegosPorDiscapacidad(newD.getCodDis());
            });

        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                "Error de conexion al cargar los datos del formulario.", TipoMensaje.ERROR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarJuegosPorDiscapacidad(String codDis) {
        Task<List<Videojuego>> task = new Task<>() {
            @Override protected List<Videojuego> call() {
                return videojuegoDAO.listarPorDiscapacidad(codDis);
            }
        };
        task.setOnSucceeded(e -> {
            juegosFila.clear();
            for (Videojuego v : task.getValue()) {
                juegosFila.add(new JuegoSeleccionable(v,
                    juegosVinculadosOriginal.contains(v.idVideojuego())));
            }
        });
        task.setOnFailed(e -> {
            juegosFila.clear();
            System.err.println("Error al cargar juegos: " + task.getException().getMessage());
        });
        Thread t = new Thread(task, "carga-juegos");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Carga los datos de un tratamiento existente para edicion.
     */
    public void cargarDatosParaEdicion(Tratamiento tratamiento) {
        this.modo = Modo.EDITAR;
        modoEdicion = true;
        codigoOriginal = tratamiento.getCodTrat();

        lblTituloVentana.setText("Editar Tratamiento");
        btnGuardar.setText("Guardar");

        txtCodigo.setText(tratamiento.getCodTrat());
        txtCodigo.setDisable(true);
        txtNombre.setText(tratamiento.getNombreTrat());
        txtAreaDefinicion.setText(tratamiento.getDefinicionTrat());

        // Discapacidad vinculada
        try {
            List<Discapacidad> vinculadas = catalogoService.listarDiscapacidadesDeTratamiento(tratamiento.getCodTrat());
            if (!vinculadas.isEmpty()) {
                for (Discapacidad d : cmbDiscapacidad.getItems()) {
                    if (d != null && d.getCodDis().equals(vinculadas.get(0).getCodDis())) {
                        cmbDiscapacidad.setValue(d);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("No se pudieron cargar las discapacidades vinculadas: " + e.getMessage());
        }

        // Nivel de progresion
        try {
            for (NivelProgresion n : cmbNivelProgresion.getItems()) {
                if (n != null && n.getIdNivel() == tratamiento.getIdNivel()) {
                    cmbNivelProgresion.setValue(n);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Juegos vinculados
        try {
            List<Videojuego> vinculados = catalogoService.listarJuegosDeTratamiento(tratamiento.getCodTrat());
            juegosVinculadosOriginal = vinculados.stream()
                .map(Videojuego::idVideojuego).collect(Collectors.toSet());
        } catch (Exception e) {
            System.err.println("Error al cargar juegos vinculados: " + e.getMessage());
        }

        // Metadatos PDF
        cargarMetadatosPdfDeTratamiento(tratamiento.getCodTrat());
    }

    private void cargarMetadatosPdfDeTratamiento(String codTrat) {
        try {
            PdfMetadato meta = catalogoService.consultarMetadatosPdf(codTrat);
            if (meta != null) {
                tienePdfRemoto = true;
                lblNombrePdf.setText(meta.nombre() + " (" + (meta.tamano() / 1024) + " KB)");
                btnEliminarPdf.setVisible(true);
                btnDescargarPdf.setVisible(true);
            }
        } catch (Exception ignored) {
            // Sin PDF o API no disponible — estado por defecto
        }
    }

    private boolean validarCampos() {
        if (txtCodigo.getText().trim().isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa("El codigo es obligatorio.", TipoMensaje.ADVERTENCIA);
            txtCodigo.requestFocus();
            return false;
        }
        if (txtCodigo.getText().trim().length() > 20) {
            VentanaUtil.mostrarVentanaInformativa("El codigo no puede superar 20 caracteres.", TipoMensaje.ADVERTENCIA);
            txtCodigo.requestFocus();
            return false;
        }
        if (txtNombre.getText().trim().isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa("El nombre es obligatorio.", TipoMensaje.ADVERTENCIA);
            txtNombre.requestFocus();
            return false;
        }
        if (cmbDiscapacidad.getValue() == null) {
            VentanaUtil.mostrarVentanaInformativa("Debe seleccionar una discapacidad.", TipoMensaje.ADVERTENCIA);
            cmbDiscapacidad.requestFocus();
            return false;
        }
        if (cmbNivelProgresion.getValue() == null) {
            VentanaUtil.mostrarVentanaInformativa("Debe seleccionar un nivel de progresion.", TipoMensaje.ADVERTENCIA);
            cmbNivelProgresion.requestFocus();
            return false;
        }
        return true;
    }

    @FXML
    void guardarTratamiento(ActionEvent event) {
        // Defensa: en modo VER el boton Guardar esta oculto, pero se bloquea por codigo por si acaso
        if (modo == Modo.VER) return;
        if (!validarCampos()) return;

        TratamientoRequest request = new TratamientoRequest(
            txtCodigo.getText().trim(),
            txtNombre.getText().trim(),
            txtAreaDefinicion.getText().trim(),
            cmbNivelProgresion.getValue().getIdNivel()
        );
        Discapacidad discapacidadSeleccionada = cmbDiscapacidad.getValue();

        try {
            String codTrat;
            if (modoEdicion) {
                catalogoService.actualizarTratamiento(codigoOriginal, request);
                codTrat = codigoOriginal;
                // Desvincular discapacidades previas y vincular la nueva
                try {
                    List<Discapacidad> previas = catalogoService.listarDiscapacidadesDeTratamiento(codigoOriginal);
                    for (Discapacidad prev : previas) {
                        catalogoService.desvincularTratamientoDiscapacidad(codigoOriginal, prev.getCodDis());
                    }
                } catch (Exception e) {
                    System.err.println("Aviso: error al desvincular discapacidades previas: " + e.getMessage());
                }
                catalogoService.vincularTratamientoDiscapacidad(codigoOriginal, discapacidadSeleccionada.getCodDis());
                VentanaUtil.mostrarVentanaInformativa(
                    "El tratamiento ha sido actualizado correctamente.", TipoMensaje.EXITO);
            } else {
                catalogoService.crearTratamiento(request);
                codTrat = txtCodigo.getText().trim();
                catalogoService.vincularTratamientoDiscapacidad(codTrat, discapacidadSeleccionada.getCodDis());
                VentanaUtil.mostrarVentanaInformativa(
                    "El tratamiento ha sido creado correctamente.", TipoMensaje.EXITO);
            }

            // PDF: borrar si marcado, subir si hay bytes pendientes
            if (pdfMarcadoParaBorrar) {
                try { catalogoService.eliminarPdfTratamiento(codTrat); } catch (Exception ignored) {}
            }
            if (pdfBytesPendientes != null) {
                catalogoService.subirPdfTratamiento(codTrat, pdfBytesPendientes, pdfNombrePendiente);
            }

            // Juegos: diff vincular/desvincular
            sincronizarJuegos(codTrat);

            cerrarVentana(event);

        } catch (DuplicadoException e) {
            VentanaUtil.mostrarVentanaInformativa("Dato duplicado: " + e.getMessage(), TipoMensaje.ERROR);
        } catch (ValidacionException e) {
            VentanaUtil.mostrarVentanaInformativa("Error de validacion: " + e.getMessage(), TipoMensaje.ERROR);
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                "No se pudo comunicar con el servidor: " + e.getMessage(), TipoMensaje.ERROR);
        } catch (RehabiAppException e) {
            VentanaUtil.mostrarVentanaInformativa("Error: " + e.getMessage(), TipoMensaje.ERROR);
        }
    }

    private void sincronizarJuegos(String codTrat) {
        Set<Long> seleccionadosAhora = juegosFila.stream()
            .filter(j -> j.seleccionadoProperty().get())
            .map(j -> j.getJuego().idVideojuego())
            .collect(Collectors.toSet());

        for (Long id : seleccionadosAhora) {
            if (!juegosVinculadosOriginal.contains(id)) {
                try { catalogoService.vincularJuego(codTrat, id); }
                catch (Exception e) {
                    System.err.println("Error al vincular juego " + id + ": " + e.getMessage());
                }
            }
        }
        for (Long id : juegosVinculadosOriginal) {
            if (!seleccionadosAhora.contains(id)) {
                try { catalogoService.desvincularJuego(codTrat, id); }
                catch (Exception e) {
                    System.err.println("Error al desvincular juego " + id + ": " + e.getMessage());
                }
            }
        }
    }

    // ==================== HANDLERS PDF ====================

    @FXML
    private void importarPdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar PDF del tratamiento");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File f = fc.showOpenDialog(btnImportarPdf.getScene().getWindow());
        if (f == null) return;
        try {
            byte[] bytes = Files.readAllBytes(f.toPath());
            if (bytes.length == 0) {
                VentanaUtil.mostrarVentanaInformativa("El PDF esta vacio.", TipoMensaje.ADVERTENCIA);
                return;
            }
            if (bytes.length > PDF_MAX_BYTES) {
                VentanaUtil.mostrarVentanaInformativa("El PDF supera 10 MB.", TipoMensaje.ADVERTENCIA);
                return;
            }
            // Verificar magic bytes "%PDF-"
            if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P'
                    || bytes[2] != 'D' || bytes[3] != 'F' || bytes[4] != '-') {
                VentanaUtil.mostrarVentanaInformativa("El archivo no es un PDF valido.", TipoMensaje.ERROR);
                return;
            }
            this.pdfBytesPendientes = bytes;
            this.pdfNombrePendiente = f.getName();
            this.pdfMarcadoParaBorrar = false;
            lblNombrePdf.setText(pdfNombrePendiente + " (" + (bytes.length / 1024) + " KB)");
            btnEliminarPdf.setVisible(true);
            btnDescargarPdf.setVisible(false);
        } catch (IOException e) {
            VentanaUtil.mostrarVentanaInformativa(
                "No se pudo leer el archivo: " + e.getMessage(), TipoMensaje.ERROR);
        }
    }

    @FXML
    private void descargarPdf() {
        String codTrat = txtCodigo.getText();
        if (codTrat == null || codTrat.isBlank()) return;
        try {
            byte[] bytes = catalogoService.descargarPdfTratamiento(codTrat);
            FileChooser fc = new FileChooser();
            fc.setTitle("Guardar PDF como");
            String nombreSugerido = lblNombrePdf.getText().split(" \\(")[0];
            fc.setInitialFileName(nombreSugerido.isEmpty() ? "tratamiento.pdf" : nombreSugerido);
            File destino = fc.showSaveDialog(btnDescargarPdf.getScene().getWindow());
            if (destino != null) {
                Files.write(destino.toPath(), bytes);
                VentanaUtil.mostrarVentanaInformativa(
                    "PDF descargado en " + destino.getAbsolutePath(), TipoMensaje.EXITO);
            }
        } catch (Exception e) {
            VentanaUtil.mostrarVentanaInformativa(
                "Error al descargar PDF: " + e.getMessage(), TipoMensaje.ERROR);
        }
    }

    @FXML
    private void eliminarPdf() {
        pdfBytesPendientes = null;
        pdfNombrePendiente = null;
        if (tienePdfRemoto) pdfMarcadoParaBorrar = true;
        lblNombrePdf.setText("(Sin archivo)");
        btnEliminarPdf.setVisible(false);
        btnDescargarPdf.setVisible(false);
    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    /**
     * Carga los datos del tratamiento en modo solo lectura.
     * Llama a cargarDatosParaEdicion para rellenar los campos y luego
     * aplica la mascara de solo lectura (deshabilita inputs, oculta botones de mutacion).
     */
    public void cargarDatosParaVer(Tratamiento tratamiento) {
        cargarDatosParaEdicion(tratamiento);
        // Sobreescribir el modo a VER despues de cargar datos
        this.modo = Modo.VER;
        this.modoEdicion = false;
        aplicarModoSoloLectura();
    }

    /**
     * Aplica la mascara de solo lectura sobre el formulario.
     * Deshabilita inputs, oculta botones de mutacion y cambia etiqueta "Cancelar" a "Cerrar".
     */
    private void aplicarModoSoloLectura() {
        lblTituloVentana.setText("Ver Tratamiento");

        // Inputs de texto: no editables (mantienen legibilidad sin color gris de disabled)
        txtCodigo.setEditable(false);
        txtNombre.setEditable(false);
        txtAreaDefinicion.setEditable(false);

        // ComboBoxes: deshabilitados (no hay setEditable equivalente)
        cmbDiscapacidad.setDisable(true);
        cmbNivelProgresion.setDisable(true);

        // Tabla de juegos: visible para ver los vinculos pero no se puede cambiar
        tablaJuegos.setEditable(false);
        colJuegoSel.setEditable(false);

        // Botones de PDF: ocultar los de mutacion, dejar visible solo Descargar
        btnImportarPdf.setVisible(false);
        btnImportarPdf.setManaged(false);
        btnEliminarPdf.setVisible(false);
        btnEliminarPdf.setManaged(false);
        // btnDescargarPdf se muestra o no segun cargarMetadatosPdfDeTratamiento (sin cambio)

        // Pie: ocultar Guardar, cambiar Cancelar a Cerrar
        btnGuardar.setVisible(false);
        btnGuardar.setManaged(false);
        btnCancelar.setText("Cerrar");
        btnCancelar.getStyleClass().removeAll("button-peligro");
        if (!btnCancelar.getStyleClass().contains("button-secundario")) {
            btnCancelar.getStyleClass().add("button-secundario");
        }
    }
}
