package com.javafx.Interface;

import com.javafx.Clases.Sanitario;
import com.javafx.DAO.SanitarioDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

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
    
    //DAO para obtener datos
    private SanitarioDAO sanitarioDAO;
    
    //Sanitario actual
    private Sanitario sanitarioActual;
    
    //Metodo initialize se ejecuta al cargar el FXML
    @FXML
    public void initialize() {
        sanitarioDAO = new SanitarioDAO();
    }
    
    //Metodo para cargar los datos de un sanitario usando su DNI
    public void cargarDatosSanitario(String dni) {
        //Obtener sanitario completo de la base de datos
        sanitarioActual = sanitarioDAO.obtenerPorDNI(dni);
        
        if (sanitarioActual != null) {
            //Rellenar los labels con los datos del sanitario
            lblNombreValor.setText(sanitarioActual.getNombre());
            lblApellidosValor.setText(sanitarioActual.getApellidos());
            lblDNIValor.setText(sanitarioActual.getDni());
            lblCargoValor.setText(sanitarioActual.getCargo() != null ? sanitarioActual.getCargo() : "-");
            lblEmailValor.setText(sanitarioActual.getEmail() != null ? sanitarioActual.getEmail() : "-");
            lblTelefonoUnoValor.setText(sanitarioActual.getTelefono1() != null ? sanitarioActual.getTelefono1() : "-");
            lblTelefonoDosValor.setText(sanitarioActual.getTelefono2() != null ? sanitarioActual.getTelefono2() : "-");
            lblPacientesAsignValor.setText(String.valueOf(sanitarioActual.getNumPacientes()));
            
            System.out.println("Datos del sanitario cargados correctamente");
        } else {
            System.err.println("No se pudo obtener el sanitario con DNI: " + dni);
        }
    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnAceptar.getScene().getWindow();
        stage.close();
    }

}
