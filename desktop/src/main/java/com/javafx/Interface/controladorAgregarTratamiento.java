package com.javafx.Interface;

import com.javafx.Clases.Discapacidad;
import com.javafx.Clases.NivelProgresion;
import com.javafx.Clases.Tratamiento;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.dto.TratamientoRequest;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.RehabiAppException;
import com.javafx.excepcion.ValidacionException;
import com.javafx.service.CatalogoService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.util.List;

/**
 * Controlador del formulario modal para crear/editar tratamientos.
 * Modo creacion: campos vacios, boton "Crear", codigo editable.
 * Modo edicion: campos rellenados, boton "Guardar", codigo deshabilitado (PK no editable).
 */
public class controladorAgregarTratamiento {

    @FXML
    private Label lblTituloVentana;

    @FXML
    private TextField txtCodigo;

    @FXML
    private TextField txtNombre;

    @FXML
    private TextArea txtAreaDefinicion;

    @FXML
    private ComboBox<Discapacidad> cmbDiscapacidad;

    @FXML
    private ComboBox<NivelProgresion> cmbNivelProgresion;

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
     * Metodo initialize se ejecuta automaticamente al cargar el FXML.
     */
    @FXML
    public void initialize() {
        catalogoService = new CatalogoService();
        cargarComboBoxes();
    }

    /**
     * Carga las discapacidades y niveles de progresion en los ComboBoxes.
     */
    private void cargarComboBoxes() {
        try {
            // Cargar discapacidades
            List<Discapacidad> discapacidades = catalogoService.listarDiscapacidades();
            cmbDiscapacidad.getItems().setAll(discapacidades);
            cmbDiscapacidad.setConverter(new StringConverter<Discapacidad>() {
                @Override
                public String toString(Discapacidad discapacidad) {
                    if (discapacidad == null) return "";
                    return discapacidad.getNombreDis();
                }

                @Override
                public Discapacidad fromString(String string) {
                    return null;
                }
            });
            cmbDiscapacidad.setPromptText("Seleccione discapacidad");

            // Cargar niveles de progresion
            List<NivelProgresion> niveles = catalogoService.listarNivelesProgresion();
            cmbNivelProgresion.getItems().setAll(niveles);
            cmbNivelProgresion.setConverter(new StringConverter<NivelProgresion>() {
                @Override
                public String toString(NivelProgresion nivel) {
                    if (nivel == null) return "";
                    return nivel.getNombre();
                }

                @Override
                public NivelProgresion fromString(String string) {
                    return null;
                }
            });
            cmbNivelProgresion.setPromptText("Seleccione nivel");

        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de conexion al cargar los datos del formulario.",
                    TipoMensaje.ERROR
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Carga los datos de un tratamiento existente para edicion.
     * Cambia el titulo, el texto del boton, y deshabilita el campo codigo (PK).
     */
    public void cargarDatosParaEdicion(Tratamiento tratamiento) {
        modoEdicion = true;
        codigoOriginal = tratamiento.getCodTrat();

        lblTituloVentana.setText("Editar Tratamiento");
        btnGuardar.setText("Guardar");

        // Codigo no editable en modo edicion (es la PK)
        txtCodigo.setText(tratamiento.getCodTrat());
        txtCodigo.setDisable(true);

        txtNombre.setText(tratamiento.getNombreTrat());
        txtAreaDefinicion.setText(tratamiento.getDefinicionTrat());

        // Seleccionar la discapacidad vinculada (buscar por la primera vinculada).
        // Los tratamientos pueden estar vinculados a multiples discapacidades.
        // En modo edicion, intentamos cargar la primera discapacidad vinculada.
        try {
            List<Discapacidad> vinculadas = catalogoService.listarDiscapacidadesDeTratamiento(tratamiento.getCodTrat());
            if (!vinculadas.isEmpty()) {
                // Buscar en los items del ComboBox la discapacidad que coincida
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

        // Seleccionar el nivel de progresion
        try {
            if (cmbNivelProgresion.getItems() != null) {
                for (NivelProgresion n : cmbNivelProgresion.getItems()) {
                    if (n != null && n.getIdNivel() == tratamiento.getIdNivel()) {
                        cmbNivelProgresion.setValue(n);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        if (cmbDiscapacidad.getValue() == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar una discapacidad.",
                    TipoMensaje.ADVERTENCIA
            );
            cmbDiscapacidad.requestFocus();
            return false;
        }

        if (cmbNivelProgresion.getValue() == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar un nivel de progresion.",
                    TipoMensaje.ADVERTENCIA
            );
            cmbNivelProgresion.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Guarda el tratamiento (creacion o actualizacion segun modo).
     */
    @FXML
    void guardarTratamiento(ActionEvent event) {
        if (!validarCampos()) {
            return;
        }

        TratamientoRequest request = new TratamientoRequest(
                txtCodigo.getText().trim(),
                txtNombre.getText().trim(),
                txtAreaDefinicion.getText().trim(),
                cmbNivelProgresion.getValue().getIdNivel()
        );

        Discapacidad discapacidadSeleccionada = cmbDiscapacidad.getValue();

        try {
            if (modoEdicion) {
                catalogoService.actualizarTratamiento(codigoOriginal, request);
                // Vincular a la discapacidad seleccionada (desvincular previas primero)
                try {
                    List<Discapacidad> vinculadasPrevias = catalogoService.listarDiscapacidadesDeTratamiento(codigoOriginal);
                    for (Discapacidad prev : vinculadasPrevias) {
                        catalogoService.desvincularTratamientoDiscapacidad(codigoOriginal, prev.getCodDis());
                    }
                } catch (Exception e) {
                    // Si falla desvincular, continuar
                    System.err.println("Aviso: error al desvincular discapacidades previas: " + e.getMessage());
                }
                catalogoService.vincularTratamientoDiscapacidad(codigoOriginal, discapacidadSeleccionada.getCodDis());

                VentanaUtil.mostrarVentanaInformativa(
                        "El tratamiento ha sido actualizado correctamente.",
                        TipoMensaje.EXITO
                );
            } else {
                catalogoService.crearTratamiento(request);
                // Vincular a la discapacidad seleccionada
                catalogoService.vincularTratamientoDiscapacidad(
                        txtCodigo.getText().trim(),
                        discapacidadSeleccionada.getCodDis()
                );
                VentanaUtil.mostrarVentanaInformativa(
                        "El tratamiento ha sido creado correctamente.",
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
     * Cierra la ventana modal.
     */
    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
}
