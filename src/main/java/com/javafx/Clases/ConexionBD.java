package com.javafx.Clases;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Clase para gestionar la conexion con la base de datos PostgreSQL.
 * Usa HikariCP como pool de conexiones thread-safe.
 *
 * Cada llamada a getConexion() devuelve una conexion independiente del pool.
 * El llamante es responsable de cerrarla (preferiblemente con try-with-resources),
 * lo que la devuelve automaticamente al pool.
 */
public class ConexionBD {

    //Pool de conexiones HikariCP
    private static HikariDataSource dataSource = null;

    //Propiedades de configuracion
    private static Properties props = null;
    private static Properties ipProps = null;

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
     * Se invoca de forma perezosa en la primera llamada a getConexion().
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

        //Configurar HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(usuario);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        //Parametros del pool
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);   // 30 segundos
        config.setIdleTimeout(600000);         // 10 minutos
        config.setMaxLifetime(1800000);        // 30 minutos
        config.setAutoCommit(true);

        //Nombre del pool para identificacion en logs
        config.setPoolName("RehabiAPP-Pool");

        //Query de validacion
        config.setConnectionTestQuery("SELECT 1");

        dataSource = new HikariDataSource(config);
        System.out.println("Pool de conexiones HikariCP inicializado correctamente");
        System.out.println("URL de conexion: " + url);
    }

    /**
     * Obtiene una conexion del pool.
     * Cada llamada devuelve una conexion independiente que DEBE cerrarse
     * tras su uso (try-with-resources recomendado).
     * Al cerrarla se devuelve automaticamente al pool.
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
     * Cierra el pool de conexiones.
     * Debe invocarse al cerrar la aplicacion.
     */
    public static void cerrarConexion() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
            System.out.println("Pool de conexiones cerrado correctamente");
        }
    }

    /**
     * Prueba la conexion a la base de datos ejecutando una consulta real.
     *
     * @return true si la conexion es exitosa y la BD responde, false en caso contrario
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
