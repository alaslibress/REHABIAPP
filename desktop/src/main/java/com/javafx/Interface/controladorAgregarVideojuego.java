package com.javafx.Interface;

import com.javafx.Clases.Discapacidad;
import com.javafx.Clases.Videojuego;
import com.javafx.Clases.VideojuegoRequest;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.VideojuegoDAO;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.RehabiAppException;
import com.javafx.excepcion.ValidacionException;
import com.javafx.service.CatalogoService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controlador del formulario modal para crear y editar videojuegos terapeuticos.
 * Modo creacion: campos vacios, boton "Crear", codigo editable.
 * Modo edicion: campos rellenados, boton "Guardar", codigo deshabilitado (clave humana).
 * El ComboBox de discapacidad ES editable en ambos modos — es la pieza que
 * permite al SPECIALIST asociar un videojuego a la discapacidad correcta.
 */
public class controladorAgregarVideojuego {

    @FXML private Label lblTituloVentana;
    @FXML private TextField txtCodigo;
    @FXML private TextField txtNombre;
    @FXML private TextArea txtAreaDescripcion;
    @FXML private ComboBox<Discapacidad> cmbDiscapacidad;
    @FXML private TextField txtParteCuerpo;
    @FXML private TextField txtUrlUnity;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    private final VideojuegoDAO videojuegoDAO = new VideojuegoDAO();
    private final CatalogoService catalogoService = new CatalogoService();

    private boolean modoEdicion = false;
    private Long idVideojuegoEditar = null;

    @FXML
    public void initialize() {
        configurarComboDiscapacidad();
        cargarDiscapacidades();
    }

    /**
     * Carga los datos de un videojuego existente para edicion.
     * El campo codigo queda read-only (clave humana referenciada en tratamiento_videojuego).
     * La discapacidad ES editable — permite reasignarla a otra.
     */
    public void cargarDatosParaEdicion(Videojuego v) {
        modoEdicion = true;
        idVideojuegoEditar = v.idVideojuego();

        lblTituloVentana.setText("Editar Videojuego");
        btnGuardar.setText("Guardar");

        txtCodigo.setText(v.codigo());
        txtCodigo.setDisable(true);
        txtNombre.setText(v.nombre());
        txtAreaDescripcion.setText(v.descripcion() != null ? v.descripcion() : "");
        txtParteCuerpo.setText(v.parteCuerpo() != null ? v.parteCuerpo() : "");
        txtUrlUnity.setText(v.urlUnity() != null ? v.urlUnity() : "");

        // Seleccionar la discapacidad actual en el ComboBox por su codDis
        cmbDiscapacidad.getItems().stream()
                .filter(d -> v.codDis() != null && v.codDis().equals(d.getCodDis()))
                .findFirst()
                .ifPresent(cmbDiscapacidad.getSelectionModel()::select);
    }

    private void configurarComboDiscapacidad() {
        cmbDiscapacidad.setCellFactory(lv -> new ListCell<Discapacidad>() {
            @Override
            protected void updateItem(Discapacidad item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNombreDis() + " (" + item.getCodDis() + ")");
            }
        });
        cmbDiscapacidad.setButtonCell(new ListCell<Discapacidad>() {
            @Override
            protected void updateItem(Discapacidad item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNombreDis() + " (" + item.getCodDis() + ")");
            }
        });
    }

    private void cargarDiscapacidades() {
        try {
            cmbDiscapacidad.getItems().setAll(catalogoService.listarDiscapacidades());
        } catch (RehabiAppException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se pudieron cargar las discapacidades. El formulario no puede enviarse sin seleccion.",
                    TipoMensaje.ERROR);
            btnGuardar.setDisable(true);
        }
    }

    private boolean validarCampos() {
        if (!modoEdicion && txtCodigo.getText().trim().isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa("El codigo es obligatorio.", TipoMensaje.ADVERTENCIA);
            txtCodigo.requestFocus();
            return false;
        }
        if (!modoEdicion && txtCodigo.getText().trim().length() > 50) {
            VentanaUtil.mostrarVentanaInformativa("El codigo no puede superar 50 caracteres.", TipoMensaje.ADVERTENCIA);
            txtCodigo.requestFocus();
            return false;
        }
        if (txtNombre.getText().trim().isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa("El nombre es obligatorio.", TipoMensaje.ADVERTENCIA);
            txtNombre.requestFocus();
            return false;
        }
        if (txtNombre.getText().trim().length() > 200) {
            VentanaUtil.mostrarVentanaInformativa("El nombre no puede superar 200 caracteres.", TipoMensaje.ADVERTENCIA);
            txtNombre.requestFocus();
            return false;
        }
        if (cmbDiscapacidad.getSelectionModel().isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa("Debe seleccionar una discapacidad.", TipoMensaje.ADVERTENCIA);
            cmbDiscapacidad.requestFocus();
            return false;
        }
        if (txtParteCuerpo.getText().trim().isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa("La parte del cuerpo es obligatoria.", TipoMensaje.ADVERTENCIA);
            txtParteCuerpo.requestFocus();
            return false;
        }
        if (txtParteCuerpo.getText().trim().length() > 100) {
            VentanaUtil.mostrarVentanaInformativa("La parte del cuerpo no puede superar 100 caracteres.", TipoMensaje.ADVERTENCIA);
            txtParteCuerpo.requestFocus();
            return false;
        }
        return true;
    }

    @FXML
    void guardarVideojuego(ActionEvent event) {
        if (!validarCampos()) return;

        Discapacidad discSeleccionada = cmbDiscapacidad.getSelectionModel().getSelectedItem();
        VideojuegoRequest request = new VideojuegoRequest(
                modoEdicion ? null : txtCodigo.getText().trim(),
                txtNombre.getText().trim(),
                txtAreaDescripcion.getText().trim(),
                discSeleccionada.getCodDis(),
                txtParteCuerpo.getText().trim(),
                txtUrlUnity.getText().trim().isEmpty() ? null : txtUrlUnity.getText().trim()
        );

        try {
            if (modoEdicion) {
                videojuegoDAO.actualizar(idVideojuegoEditar, request);
                VentanaUtil.mostrarVentanaInformativa(
                        "Videojuego actualizado correctamente.", TipoMensaje.EXITO);
            } else {
                videojuegoDAO.crear(new VideojuegoRequest(
                        txtCodigo.getText().trim(),
                        txtNombre.getText().trim(),
                        txtAreaDescripcion.getText().trim(),
                        discSeleccionada.getCodDis(),
                        txtParteCuerpo.getText().trim(),
                        txtUrlUnity.getText().trim().isEmpty() ? null : txtUrlUnity.getText().trim()
                ));
                VentanaUtil.mostrarVentanaInformativa(
                        "Videojuego creado correctamente.", TipoMensaje.EXITO);
            }
            cerrarVentana(event);

        } catch (DuplicadoException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Codigo de videojuego ya existe: " + e.getMessage(), TipoMensaje.ERROR);
        } catch (ValidacionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de validacion: " + e.getMessage(), TipoMensaje.ERROR);
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se pudo comunicar con el servidor: " + e.getMessage(), TipoMensaje.ERROR);
        } catch (RehabiAppException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error: " + e.getMessage(), TipoMensaje.ERROR);
        }
    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
}
