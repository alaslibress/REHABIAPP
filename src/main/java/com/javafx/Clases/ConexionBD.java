package com.javafx.Clases;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Clase para gestionar la conexion con la base de datos PostgreSQL
 * Usa HikariCP como pool de conexiones para garantizar thread-safety
 * y rendimiento en operaciones concurrentes.
 *
 * Cada llamada a getConexion() devuelve una Connection nueva del pool.
 * El llamante es responsable de cerrarla (con try-with-resources o finally).
 */
public class ConexionBD {

    //Propiedades de configuracion de base de datos
    private static Properties props = null;

    //Propiedades de configuracion de IP (para exportacion)
    private static Properties ipProps = null;

    //Pool de conexiones HikariCP
    private static HikariDataSource dataSource = null;

    //Constructor privado para evitar instanciacion externa
    private ConexionBD() {
    }

    //Metodo para cargar las propiedades desde el archivo
    private static void cargarPropiedades() {
        if (props == null) {
            props = new Properties();
            try {
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

    /**
     * Inicializa el pool de conexiones HikariCP.
     * Se ejecuta una sola vez (lazy initialization sincronizada).
     */
    private static synchronized void inicializarPool() {
        if (dataSource != null) {
            return;
        }

        cargarPropiedades();
        cargarPropiedadesIP();

        //Construir URL de conexion
        String url;
        if (ipProps != null && ipProps.getProperty("aws.ip") != null) {
            String ip = ipProps.getProperty("aws.ip");
            String port = ipProps.getProperty("aws.port");
            String database = ipProps.getProperty("aws.database");
            url = "jdbc:postgresql://" + ip + ":" + port + "/" + database;
        } else {
            url = props.getProperty("db.url");
        }

        String usuario = props.getProperty("db.usuario");
        String password = props.getProperty("db.password");

        //Configuracion SSL/TLS (ENS Nivel Alto)
        String ssl = props.getProperty("db.ssl", "false");
        String sslmode = props.getProperty("db.sslmode", "prefer");

        if ("true".equalsIgnoreCase(ssl)) {
            String separador = url.contains("?") ? "&" : "?";
            url = url + separador + "ssl=true&sslmode=" + sslmode;
            System.out.println("SSL habilitado para conexion a BD (modo: " + sslmode + ")");
        }

        //Configurar HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(usuario);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        //Parametros del pool
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000);         // 5 minutos
        config.setConnectionTimeout(10000);    // 10 segundos
        config.setMaxLifetime(1800000);        // 30 minutos
        config.setPoolName("RehabiAPP-Pool");

        //Validacion de conexiones
        config.setConnectionTestQuery("SELECT 1");

        dataSource = new HikariDataSource(config);
        System.out.println("Pool HikariCP inicializado correctamente");
        System.out.println("URL de conexion: " + url);
    }

    /**
     * Obtiene una conexion del pool HikariCP.
     * IMPORTANTE: El llamante DEBE cerrar la conexion (try-with-resources o finally).
     * Al cerrar la Connection, HikariCP la devuelve al pool automaticamente.
     *
     * @return Connection nueva del pool
     */
    public static Connection getConexion() {
        try {
            if (dataSource == null) {
                inicializarPool();
            }
            return dataSource.getConnection();
        } catch (SQLException e) {
            System.err.println("Error al obtener conexion del pool: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    //Alias del metodo getConexion para compatibilidad
    public static Connection getConnection() {
        return getConexion();
    }

    /**
     * Cierra el pool de conexiones HikariCP.
     * Se ejecuta al cerrar la aplicacion (shutdown hook).
     */
    public static void cerrarConexion() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
            System.out.println("Pool HikariCP cerrado correctamente");
        }
    }

    /**
     * Prueba la conexion a la base de datos.
     * Obtiene una conexion del pool, ejecuta SELECT 1 y la devuelve.
     *
     * @return true si la conexion es exitosa y la BD responde
     */
    public static boolean probarConexion() {
        try (Connection conn = getConexion()) {
            if (conn == null || conn.isClosed()) {
                return false;
            }

            try (java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT 1")) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("Error al verificar conexion: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Error inesperado al probar conexion: " + e.getMessage());
            return false;
        }
    }
}
