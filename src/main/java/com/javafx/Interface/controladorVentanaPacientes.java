package com.javafx.Interface;

import com.javafx.Clases.Paciente;
import com.javafx.DAO.PacienteDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.List;

public class controladorVentanaPacientes {

    @FXML
    private Button btnAbrirPaciente;

    @FXML
    private Button btnAnadirPaciente;

    @FXML
    private Button btnBuscarPacientes;

    @FXML
    private Button btnEliminarPaciente;

    @FXML
    private Button btnFiltrarPacientes;

    @FXML
    private Button btnSeleccionarTodoPacientes;

    @FXML
    private TableColumn<Paciente, String> colApellidos;

    @FXML
    private TableColumn<Paciente, Integer> colCargo;

    @FXML
    private TableColumn<Paciente, String> colDNI;

    @FXML
    private TableColumn<Paciente, String> colNombre;

    @FXML
    private Label lblBuscarPacientes;

    @FXML
    private Label lblTituloPestaniaPacientes;

    @FXML
    private TableView<Paciente> tblPacientes;

    @FXML
    private TextField txfBuscarPacientes;

    @FXML
    private VBox vboxContenedorPrinPacientes;
    
    //Lista observable para la tabla
    private ObservableList<Paciente> listaPacientes;
    
    //DAO para operaciones de base de datos
    private PacienteDAO pacienteDAO;

    //Metodo initialize se ejecuta automaticamente al cargar el FXML
    @FXML
    public void initialize() {
        //Inicializar DAO
        pacienteDAO = new PacienteDAO();
        
        //Configurar columnas de la tabla
        configurarTabla();
        
        //Cargar datos de la base de datos
        cargarPacientes();
    }
    
    //Metodo para configurar las columnas de la tabla
    private void configurarTabla() {
        //Vincular columnas con propiedades del objeto Paciente
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        colDNI.setCellValueFactory(new PropertyValueFactory<>("dni"));
        colCargo.setCellValueFactory(new PropertyValueFactory<>("protesis"));
        
        System.out.println("Tabla de pacientes configurada");
    }
    
    //Metodo para cargar todos los pacientes de la base de datos en la tabla
    private void cargarPacientes() {
        try {
            //Obtener lista de pacientes desde la base de datos
            List<Paciente> pacientes = pacienteDAO.listarTodos();
            
            //Convertir a ObservableList y asignar a la tabla
            listaPacientes = FXCollections.observableArrayList(pacientes);
            tblPacientes.setItems(listaPacientes);
            
            System.out.println("Tabla cargada con " + pacientes.size() + " pacientes");
            
        } catch (Exception e) {
            System.err.println("Error al cargar pacientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void abrirFichaPaciente(ActionEvent event) {
        //Obtener paciente seleccionado en la tabla
        Paciente pacienteSeleccionado = tblPacientes.getSelectionModel().getSelectedItem();
        
        if (pacienteSeleccionado != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaListarPaciente.fxml"));
                Parent root = loader.load();
                
                //Obtener el controlador y pasarle el DNI del paciente
                controladorVentanaPacienteListar controlador = loader.getController();
                controlador.cargarDatosPaciente(pacienteSeleccionado.getDni());
                
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No hay ningun paciente seleccionado");
        }
    }

    @FXML
    void abrirFiltrosPaciente(ActionEvent event) {

    }

    @FXML
    void abrirFormularioNuevoPaciente(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarPaciente.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void eliminarPacientesSeleccionado(ActionEvent event) {

    }

    @FXML
    void seleccionarTodosPacientes(ActionEvent event) {

    }

}
