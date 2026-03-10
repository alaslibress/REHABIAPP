package com.javafx.DAO;

import com.javafx.Clases.ConexionBD;
import com.javafx.excepcion.ConexionException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase abstracta con metodos helper compartidos para todos los DAOs.
 * Proporciona funcionalidad comun para operaciones de base de datos.
 */
public abstract class BaseDAO {

    /**
     * Inserta telefonos en la tabla especificada.
     * Usa la conexion proporcionada para participar en una transaccion externa.
     *
     * @param conn Conexion activa (gestionada externamente)
     * @param tablaTelefono Nombre de la tabla de telefonos (telefono_paciente o telefono_sanitario)
     * @param dniColumna Nombre de la columna DNI (dni_pac o dni_san)
     * @param dni DNI del paciente o sanitario
     * @param tel1 Primer telefono (puede ser null o vacio)
     * @param tel2 Segundo telefono (puede ser null o vacio)
     * @throws ConexionException si hay error de base de datos
     */
    protected void insertarTelefonos(Connection conn, String tablaTelefono, String dniColumna,
                                     String dni, String tel1, String tel2) {
        String query = "INSERT INTO " + tablaTelefono + " (" + dniColumna + ", telefono) VALUES (?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {

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

        } catch (SQLException e) {
            throw new ConexionException("Error al insertar telefonos en " + tablaTelefono, e);
        }
    }

    /**
     * Elimina y reinserta telefonos en la tabla especificada (actualizacion).
     * Usa la conexion proporcionada para participar en una transaccion externa.
     *
     * @param conn Conexion activa (gestionada externamente)
     * @param tablaTelefono Nombre de la tabla de telefonos
     * @param dniColumna Nombre de la columna DNI
     * @param dni DNI del paciente o sanitario
     * @param tel1 Primer telefono
     * @param tel2 Segundo telefono
     * @throws ConexionException si hay error de base de datos
     */
    protected void actualizarTelefonos(Connection conn, String tablaTelefono, String dniColumna,
                                       String dni, String tel1, String tel2) {
        //Eliminar telefonos existentes
        String queryEliminar = "DELETE FROM " + tablaTelefono + " WHERE " + dniColumna + " = ?";

        try (PreparedStatement stmtEliminar = conn.prepareStatement(queryEliminar)) {
            stmtEliminar.setString(1, dni);
            stmtEliminar.executeUpdate();
        } catch (SQLException e) {
            throw new ConexionException("Error al eliminar telefonos de " + tablaTelefono, e);
        }

        //Reinsertar los nuevos
        insertarTelefonos(conn, tablaTelefono, dniColumna, dni, tel1, tel2);
    }

    /**
     * Carga los telefonos desde la tabla especificada
     *
     * @param tablaTelefono Nombre de la tabla de telefonos
     * @param dniColumna Nombre de la columna DNI
     * @param dni DNI del paciente o sanitario
     * @return Lista de telefonos (maximo 2)
     */
    protected List<String> cargarTelefonos(String tablaTelefono, String dniColumna, String dni) {
        List<String> telefonos = new ArrayList<>();
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
            throw new ConexionException("Error al cargar telefonos de " + tablaTelefono, e);
        }

        return telefonos;
    }

    /**
     * Traduce una SQLException a la excepcion tipada correspondiente
     * segun el sqlState de PostgreSQL.
     *
     * @param e SQLException original
     * @param mensajeGenerico Mensaje para ConexionException si no se identifica el error
     * @return RuntimeException tipada (nunca null)
     */
    protected RuntimeException traducirSQLException(SQLException e, String mensajeGenerico) {
        String sqlState = e.getSQLState();

        if (sqlState != null) {
            if ("23505".equals(sqlState)) {
                //Unique violation - se maneja en la subclase con mas detalle
                return new ConexionException(mensajeGenerico + " (constraint unica)", e);
            }
            if ("23503".equals(sqlState)) {
                //FK violation
                return new ConexionException(mensajeGenerico + " (referencia invalida)", e);
            }
        }

        return new ConexionException(mensajeGenerico, e);
    }
}
