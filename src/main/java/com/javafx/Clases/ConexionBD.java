package com.javafx.Clases;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

//Clase para gestionar la conexion con la base de datos PostgreSQL
//Lee la configuracion desde un archivo properties
public class ConexionBD {

    //Propiedades de configuracion
    private static Properties props = null;

    //Instancia unica de la conexion (Singleton)
    private static Connection conexion = null;

    //Constructor privado para evitar instanciacion externa
    private ConexionBD() {
    }

    //Metodo para cargar las propiedades desde el archivo
    private static void cargarPropiedades() {
        if (props == null) {
            props = new Properties();
            try {
                //Cargar el archivo properties desde resources
                InputStream input = ConexionBD.class.getClassLoader()
                        .getResourceAsStream("config/database.properties");

                if (input == null) {
                    System.err.println("No se pudo encontrar database.properties");
                    return;
                }

                props.load(input);
                input.close();

            } catch (IOException e) {
                System.err.println("Error al cargar el archivo de configuracion");
                e.printStackTrace();
            }
        }
    }

    //Metodo que obtiene la conexion a la base de datos
    //Retorna Connection objeto de conexion a la BD
    public static Connection getConexion() {
        try {
            //Si la conexion no existe o esta cerrada, crear nueva conexion
            if (conexion == null || conexion.isClosed()) {

                //Cargar propiedades si no estan cargadas
                cargarPropiedades();

                //Obtener datos de conexion del properties
                String url = props.getProperty("db.url");
                String usuario = props.getProperty("db.usuario");
                String password = props.getProperty("db.password");
                String driver = props.getProperty("db.driver");

                //Cargar el driver de PostgreSQL
                Class.forName(driver);

                //Establecer la conexion
                conexion = DriverManager.getConnection(url, usuario, password);
                System.out.println("Conexion establecida correctamente con PostgreSQL");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontro el driver de PostgreSQL");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Error al conectar con la base de datos");
            e.printStackTrace();
        }

        return conexion;
    }

    //Alias del metodo getConexion para compatibilidad
    public static Connection getConnection() {
        return getConexion();
    }

    //Metodo para cerrar la conexion a la base de datos
    public static void cerrarConexion() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
                System.out.println("Conexion cerrada correctamente");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar la conexion");
            e.printStackTrace();
        }
    }

    //Metodo para probar la conexion
    //Retorna true si la conexion es exitosa, false en caso contrario
    public static boolean probarConexion() {
        try {
            Connection conn = getConexion();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}