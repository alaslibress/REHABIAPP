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

    //Propiedades de configuracion de base de datos
    private static Properties props = null;

    //Propiedades de configuracion de IP (para exportacion)
    private static Properties ipProps = null;

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

    //Metodo para cargar las propiedades de IP desde el archivo
    private static void cargarPropiedadesIP() {
        if (ipProps == null) {
            ipProps = new Properties();
            try {
                //Cargar el archivo ip.properties desde resources
                InputStream input = ConexionBD.class.getClassLoader()
                        .getResourceAsStream("config/ip.properties");

                if (input == null) {
                    System.err.println("No se pudo encontrar ip.properties, usando database.properties");
                    return;
                }

                ipProps.load(input);
                input.close();

            } catch (IOException e) {
                System.err.println("Error al cargar el archivo ip.properties");
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
                cargarPropiedadesIP();

                //Obtener datos de conexion del properties
                String url;

                // Si existe ip.properties, construir URL desde ahí (para exportación)
                if (ipProps != null && ipProps.getProperty("aws.ip") != null) {
                    String ip = ipProps.getProperty("aws.ip");
                    String port = ipProps.getProperty("aws.port");
                    String database = ipProps.getProperty("aws.database");
                    url = "jdbc:postgresql://" + ip + ":" + port + "/" + database;
                } else {
                    // Sino, usar la URL del database.properties
                    url = props.getProperty("db.url");
                }

                String usuario = props.getProperty("db.usuario");
                String password = props.getProperty("db.password");
                String driver = props.getProperty("db.driver");

                //Cargar el driver de PostgreSQL
                Class.forName(driver);

                //Establecer la conexion
                conexion = DriverManager.getConnection(url, usuario, password);
                System.out.println("Conexion establecida correctamente con PostgreSQL");
                System.out.println("URL de conexion: " + url);
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

    /**
     * Metodo para probar la conexion a la base de datos
     * Ejecuta una consulta real para verificar que la base de datos responde
     *
     * @return true si la conexion es exitosa y la BD responde, false en caso contrario
     */
    public static boolean probarConexion() {
        Connection conn = null;
        java.sql.Statement stmt = null;
        java.sql.ResultSet rs = null;

        try {
            // Intentar obtener o crear una nueva conexión
            conn = getConexion();

            // Verificar que la conexión no es null y no está cerrada
            if (conn == null || conn.isClosed()) {
                return false;
            }

            // Ejecutar una consulta simple para verificar que la BD responde
            // SELECT 1 es una consulta universal que funciona en PostgreSQL
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT 1");

            // Si la consulta retorna algún resultado, la conexión es válida
            if (rs.next()) {
                return true;
            }

            // Si no retorna nada, hay un problema con la BD
            return false;

        } catch (SQLException e) {
            System.err.println("Error al verificar conexión con consulta: " + e.getMessage());

            // Resetear la conexión en caso de error
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException ex) {
                // Ignorar
            }
            conexion = null;

            return false;
        } catch (Exception e) {
            // Capturar cualquier otra excepción (NullPointerException, etc.)
            System.err.println("Error inesperado al probar conexión: " + e.getMessage());
            conexion = null;
            return false;
        } finally {
            // Cerrar recursos en orden inverso
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                // NO cerramos conn porque es la conexión singleton que se reutiliza
            } catch (SQLException e) {
                System.err.println("Error al cerrar recursos de prueba: " + e.getMessage());
            }
        }
    }
}