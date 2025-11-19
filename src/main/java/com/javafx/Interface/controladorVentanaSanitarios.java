package com.javafx.Interface;

import com.javafx.Clases.Sanitario;
import com.javafx.DAO.SanitarioDAO;
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

public class controladorVentanaSanitarios {

    @FXML
    private Button btnAbrirSanitario;

    @FXML
    private Button btnAnadirSanitario;

    @FXML
    private Button btnBuscarSanitarios;

    @FXML
    private Button btnEliminarSanitario;

    @FXML
    private Button btnFiltrarSanitarios;

    @FXML
    private Button btnSeleccionarTodo;

    @FXML
    private TableColumn<Sanitario, String> colApellidos;

    @FXML
    private TableColumn<Sanitario, String> colCargo;

    @FXML
    private TableColumn<Sanitario, String> colDNI;

    @FXML
    private TableColumn<Sanitario, String> colNombre;

    @FXML
    private Label lblBuscarSanitarios;

    @FXML
    private Label lblTituloPestania;

    @FXML
    private TableView<Sanitario> tblSanitarios;

    @FXML
    private TextField txfBuscarSanitarios;

    @FXML
    private VBox vboxContenedorPrincipal;
    
    //Lista observable para la tabla
    private ObservableList<Sanitario> listaSanitarios;
    
    //DAO para operaciones de base de datos
    private SanitarioDAO sanitarioDAO;

    //Metodo initialize se ejecuta automaticamente al cargar el FXML
    @FXML
    public void initialize() {
        //Inicializar DAO
        sanitarioDAO = new SanitarioDAO();
        
        //Configurar columnas de la tabla
        configurarTabla();
        
        //Cargar datos de la base de datos
        cargarSanitarios();
    }
    
    //Metodo para configurar las columnas de la tabla
    private void configurarTabla() {
        //Vincular columnas con propiedades del objeto Sanitario
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        colDNI.setCellValueFactory(new PropertyValueFactory<>("dni"));
        colCargo.setCellValueFactory(new PropertyValueFactory<>("cargo"));
        
        System.out.println("Tabla de sanitarios configurada");
    }
    
    //Metodo para cargar todos los sanitarios de la base de datos en la tabla
    private void cargarSanitarios() {
        try {
            //Obtener lista de sanitarios desde la base de datos
            List<Sanitario> sanitarios = sanitarioDAO.listarTodos();
            
            //Convertir a ObservableList y asignar a la tabla
            listaSanitarios = FXCollections.observableArrayList(sanitarios);
            tblSanitarios.setItems(listaSanitarios);
            
            System.out.println("Tabla cargada con " + sanitarios.size() + " sanitarios");
            
        } catch (Exception e) {
            System.err.println("Error al cargar sanitarios: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void abrirFichaSanitario(ActionEvent event) {
        //Obtener sanitario seleccionado en la tabla
        Sanitario sanitarioSeleccionado = tblSanitarios.getSelectionModel().getSelectedItem();
        
        if (sanitarioSeleccionado != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaSanitarioListar.fxml"));
                Parent root = loader.load();
                
                //Obtener el controlador y pasarle el DNI del sanitario
                controladorVentanaSanitarioListar controlador = loader.getController();
                controlador.cargarDatosSanitario(sanitarioSeleccionado.getDni());
                
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No hay ningun sanitario seleccionado");
        }
    }

    @FXML
    void abrirFiltrosSanitarios(ActionEvent event) {

    }

    @FXML
    void abrirFormularioNuevoSanitario(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarSanitario.fxml"));
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
    void eliminarSanitarioSeleccionado(ActionEvent event) {

    }

    @FXML
    void seleccionarTodosSanitarios(ActionEvent event) {

    }

}
