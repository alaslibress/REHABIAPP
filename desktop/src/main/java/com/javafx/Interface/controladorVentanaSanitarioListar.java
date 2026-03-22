package com.javafx.Interface;

import com.javafx.Clases.Sanitario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.SanitarioDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controlador para la ventana de informacion detallada de un sanitario
 * Muestra todos los datos del sanitario seleccionado y permite editarlos
 */
public class controladorVentanaSanitarioListar {

    @FXML
    private Button btnAceptar;

    @FXML
    private Button btnEditar;

    @FXML
    private Label lblApellidosValor;

    @FXML
    private Label lblCargoValor;

    @FXML
    private Label lblDNIValor;

    @FXML
    private Label lblEmailValor;

    @FXML
    private Label lblNombreValor;

    @FXML
    private Label lblPacientesAsignValor;

    @FXML
    private Label lblTelefonoDosValor;

    @FXML
    private Label lblTelefonoUnoValor;

    @FXML
    private Label lblTituloVentana;

    //DAO para obtener datos de la base de datos
    private SanitarioDAO sanitarioDAO;

    //Sanitario que se esta mostrando actualmente
    private Sanitario sanitarioActual;

    //Indica si se han realizado cambios para refrescar la ventana padre
    private boolean cambiosRealizados = false;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        sanitarioDAO = new SanitarioDAO();
    }

    /**
     * Carga los datos de un sanitario usando su DNI
     * @param dni DNI del sanitario a cargar
     */
    public void cargarDatosSanitario(String dni) {
        //Obtener sanitario completo de la base de datos (incluye telefonos)
        sanitarioActual = sanitarioDAO.obtenerPorDNI(dni);

        if (sanitarioActual != null) {
            //Mostrar los datos en los labels de la interfaz
            mostrarDatosEnLabels();
        } else {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se pudo obtener la informacion del sanitario.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Muestra los datos del sanitario actual en los labels de la interfaz
     */
    private void mostrarDatosEnLabels() {
        //Datos basicos del sanitario
        lblNombreValor.setText(sanitarioActual.getNombre());
        lblApellidosValor.setText(sanitarioActual.getApellidos());
        lblDNIValor.setText(sanitarioActual.getDni());

        //Mostrar cargo o guion si no tiene
        String cargo = sanitarioActual.getCargo();
        lblCargoValor.setText(cargo != null && !cargo.isEmpty() ? cargo : "-");

        //Mostrar email o guion si no tiene
        String email = sanitarioActual.getEmail();
        lblEmailValor.setText(email != null && !email.isEmpty() ? email : "-");

        //Mostrar telefonos o guion si no tiene
        String tel1 = sanitarioActual.getTelefono1();
        String tel2 = sanitarioActual.getTelefono2();
        lblTelefonoUnoValor.setText(tel1 != null && !tel1.isEmpty() ? tel1 : "-");
        lblTelefonoDosValor.setText(tel2 != null && !tel2.isEmpty() ? tel2 : "-");

        //Mostrar numero de pacientes asignados
        lblPacientesAsignValor.setText(String.valueOf(sanitarioActual.getNumPacientes()));
    }

    /**
     * Abre la ventana de edicion del sanitario actual
     * @param event Evento del boton
     */
    @FXML
    void abrirEdicion(ActionEvent event) {
        //Verificar que hay un sanitario cargado
        if (sanitarioActual == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "No hay datos del sanitario para editar.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        try {
            //Cargar la ventana de agregar/editar sanitario
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarSanitario.fxml"));
            Parent root = loader.load();

            //Obtener el controlador y pasarle los datos del sanitario para editar
            controladorAgregarSanitario controlador = loader.getController();
            controlador.cargarDatosParaEdicion(sanitarioActual);

            //Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            //Crear y mostrar la ventana modal
            Stage stage = new Stage();
            stage.setTitle("Modificar Sanitario");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            VentanaUtil.establecerIconoVentana(stage);
            stage.showAndWait();

            //Marcar que se realizaron cambios
            cambiosRealizados = true;

            //Recargar los datos del sanitario por si se modificaron
            //El DNI podria haber cambiado, asi que usamos el DNI del label actualizado
            String dniActualizado = lblDNIValor.getText();

            //Intentar recargar con el DNI actual o buscar el nuevo si cambio
            Sanitario sanitarioRecargado = sanitarioDAO.obtenerPorDNI(dniActualizado);

            if (sanitarioRecargado != null) {
                sanitarioActual = sanitarioRecargado;
                mostrarDatosEnLabels();
            } else {
                //Si no se encuentra, puede que se haya cambiado el DNI
                //Cerramos la ventana para que el usuario vuelva a la lista
                VentanaUtil.mostrarVentanaInformativa(
                        "El sanitario ha sido modificado. La ventana se cerrara.",
                        TipoMensaje.INFORMACION
                );
                cerrarVentana(event);
            }

        } catch (Exception e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se pudo abrir la ventana de edicion.",
                    TipoMensaje.ERROR
            );
            System.err.println("Error al abrir ventana de edicion: " + e.getMessage());
        }
    }

    /**
     * Indica si se han realizado cambios en el sanitario
     * @return true si se han realizado cambios
     */
    public boolean hayCambiosRealizados() {
        return cambiosRealizados;
    }

    /**
     * Cierra la ventana de informacion
     * @param event Evento del boton
     */
    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnAceptar.getScene().getWindow();
        stage.close();
    }
}
