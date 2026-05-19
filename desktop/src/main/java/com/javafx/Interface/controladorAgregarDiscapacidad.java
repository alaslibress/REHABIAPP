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
     * Cambia el titulo, el texto del boton, y deshabilita el campo codigo (PK).
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
     *
     * @return true si todos los campos obligatorios estan rellenos y son validos
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

        if (txtCodigo.getText().trim().length() > 20) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El codigo no puede superar 20 caracteres.",
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
            // 409: codigo o nombre ya existen
            VentanaUtil.mostrarVentanaInformativa(
                    "Dato duplicado: " + e.getMessage(),
                    TipoMensaje.ERROR
            );
        } catch (ValidacionException e) {
            // 400: campos invalidos segun la API
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de validacion: " + e.getMessage(),
                    TipoMensaje.ERROR
            );
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se pudo comunicar con el servidor: " + e.getMessage(),
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
