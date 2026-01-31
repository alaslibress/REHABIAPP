package com.javafx.DAO;

import com.javafx.Clases.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase abstracta con metodos helper compartidos para todos los DAOs
 * Proporciona funcionalidad comun para operaciones de base de datos
 */
public abstract class BaseDAO {

    /**
     * Interface funcional para ejecutar transacciones
     * @param <T> Tipo de retorno de la transaccion
     */
    public interface TransaccionCallback<T> {
        T ejecutar(Connection conn) throws SQLException;
    }

    /**
     * Ejecuta una transaccion de forma segura con manejo automatico de commit/rollback
     * @param callback Callback con la logica de la transaccion
     * @param valorDefecto Valor a retornar en caso de error
     * @param <T> Tipo de retorno
     * @return Resultado de la transaccion o valorDefecto si hay error
     */
    protected <T> T ejecutarTransaccion(TransaccionCallback<T> callback, T valorDefecto) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            T resultado = callback.ejecutar(conn);

            conn.commit();
            return resultado;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error al hacer rollback: " + ex.getMessage());
                }
            }
            System.err.println("Error en transaccion: " + e.getMessage());
            e.printStackTrace();
            return valorDefecto;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error al restaurar autocommit: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Inserta telefonos en la tabla especificada
     * @param tablaTelefono Nombre de la tabla de telefonos (telefono_paciente o telefono_sanitario)
     * @param dniColumna Nombre de la columna DNI (dni_pac o dni_san)
     * @param dni DNI del paciente o sanitario
     * @param tel1 Primer telefono (puede ser null o vacio)
     * @param tel2 Segundo telefono (puede ser null o vacio)
     * @return true si la insercion fue exitosa
     */
    protected boolean insertarTelefonos(String tablaTelefono, String dniColumna, String dni, String tel1, String tel2) {
        String query = "INSERT INTO " + tablaTelefono + " (" + dniColumna + ", telefono) VALUES (?, ?)";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            if (tel1 != null && !tel1.trim().isEmpty()) {
                stmt.setString(1, dni);
                stmt.setString(2, tel1.trim());
                stmt.executeUpdate();
            }

            if (tel2 != null && !tel2.trim().isEmpty()) {
                stmt.setString(1, dni);
                stmt.setString(2, tel2.trim());
                stmt.executeUpdate();
            }

            return true;

        } catch (SQLException e) {
            System.err.println("Error al insertar telefonos: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Carga los telefonos desde la tabla especificada
     * @param tablaTelefono Nombre de la tabla de telefonos (telefono_paciente o telefono_sanitario)
     * @param dniColumna Nombre de la columna DNI (dni_pac o dni_san)
     * @param dni DNI del paciente o sanitario
     * @return Lista de telefonos (maximo 2)
     */
    protected List<String> cargarTelefonos(String tablaTelefono, String dniColumna, String dni) {
        List<String> telefonos = new ArrayList<String>();
        String query = "SELECT telefono FROM " + tablaTelefono + " WHERE " + dniColumna + " = ? ORDER BY id_telefono LIMIT 2";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next() && telefonos.size() < 2) {
                    telefonos.add(rs.getString("telefono"));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al cargar telefonos: " + e.getMessage());
        }

        return telefonos;
    }
}
