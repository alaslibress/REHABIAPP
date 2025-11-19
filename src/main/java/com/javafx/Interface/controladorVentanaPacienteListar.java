package com.javafx.Interface;

import com.javafx.Clases.Paciente;
import com.javafx.DAO.PacienteDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class controladorVentanaPacienteListar {

    @FXML
    private Button btnAceptar;

    @FXML
    private Button btnDescargarInforme;

    @FXML
    private Button btnEditar;

    @FXML
    private ToggleGroup grupoProtesis;

    @FXML
    private ImageView imgFotoPaciente;

    @FXML
    private Label lblApellidosValor;

    @FXML
    private Label lblDNIValor;

    @FXML
    private Label lblDireccionValor;

    @FXML
    private Label lblDiscapacidadValor;

    @FXML
    private Label lblEdadValor;

    @FXML
    private Label lblEmailValor;

    @FXML
    private Label lblNSSValor;

    @FXML
    private Label lblNombreValor;

    @FXML
    private Label lblTelefono1Valor;

    @FXML
    private Label lblTelefono2Valor;

    @FXML
    private Label lblTituloVentana;

    @FXML
    private RadioButton radioProtesisNo;

    @FXML
    private RadioButton radioProtesisSi;

    @FXML
    private TextArea txtAreaEstado;

    @FXML
    private TextArea txtAreaTratamiento;
    
    //DAO para obtener datos
    private PacienteDAO pacienteDAO;
    
    //Paciente actual
    private Paciente pacienteActual;
    
    //Metodo initialize se ejecuta al cargar el FXML
    @FXML
    public void initialize() {
        pacienteDAO = new PacienteDAO();
    }
    
    //Metodo para cargar los datos de un paciente usando su DNI
    public void cargarDatosPaciente(String dni) {
        //Obtener paciente completo de la base de datos
        pacienteActual = pacienteDAO.obtenerPorDNI(dni);
        
        if (pacienteActual != null) {
            //Rellenar los labels con los datos del paciente
            lblNombreValor.setText(pacienteActual.getNombre());
            lblApellidosValor.setText(pacienteActual.getApellidos());
            lblEdadValor.setText(String.valueOf(pacienteActual.getEdad()));
            lblEmailValor.setText(pacienteActual.getEmail() != null ? pacienteActual.getEmail() : "-");
            lblTelefono1Valor.setText(pacienteActual.getTelefono1() != null ? pacienteActual.getTelefono1() : "-");
            lblTelefono2Valor.setText(pacienteActual.getTelefono2() != null ? pacienteActual.getTelefono2() : "-");
            lblNSSValor.setText(pacienteActual.getNumSS());
            lblDNIValor.setText(pacienteActual.getDni());
            lblDireccionValor.setText(pacienteActual.getDireccion() != null ? pacienteActual.getDireccion() : "-");
            txtAreaEstado.setText(pacienteActual.getEstadoPaciente() != null ? pacienteActual.getEstadoPaciente() : "-");
            lblDiscapacidadValor.setText(pacienteActual.getDiscapacidad() != null ? pacienteActual.getDiscapacidad() : "-");
            txtAreaTratamiento.setText(pacienteActual.getTratamiento() != null ? pacienteActual.getTratamiento() : "-");
            
            //Marcar radio button de protesis
            if (pacienteActual.getProtesis() > 0) {
                radioProtesisSi.setSelected(true);
            } else {
                radioProtesisNo.setSelected(true);
            }
            
            System.out.println("Datos del paciente cargados correctamente");
        } else {
            System.err.println("No se pudo obtener el paciente con DNI: " + dni);
        }
    }

    @FXML
    void abrirVentanaEditar(ActionEvent event) {
        //TODO: Implementar ventana de edicion
        System.out.println("Abrir ventana de edicion");
    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnAceptar.getScene().getWindow();
        stage.close();
    }

    @FXML
    void descargarInformePDF(ActionEvent event) {
        //TODO: Implementar descarga de PDF
        System.out.println("Descargar informe PDF");
    }

}
