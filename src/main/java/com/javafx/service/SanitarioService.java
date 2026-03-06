package com.javafx.service;

import com.javafx.Clases.ConexionBD;
import com.javafx.Clases.Sanitario;
import com.javafx.DAO.SanitarioDAO;
import com.javafx.excepcion.ConexionException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Capa de servicio para operaciones de Sanitario.
 * Gestiona transacciones compuestas (sanitario + cargo + telefonos en una sola transaccion)
 * y registra auditoria automatica.
 *
 * Los metodos de escritura propagan las excepciones del DAO
 * (DuplicadoException, ConexionException, ValidacionException)
 * para que los controladores muestren mensajes especificos.
 */
public class SanitarioService {

    private final SanitarioDAO sanitarioDAO;

    public SanitarioService() {
        this.sanitarioDAO = new SanitarioDAO();
    }

    /**
     * Lista todos los sanitarios
     * @return Lista de todos los sanitarios
     */
    public List<Sanitario> listarTodos() {
        return sanitarioDAO.listarTodos();
    }

    /**
     * Busca un sanitario por su DNI
     * @param dni DNI del sanitario
     * @return Optional con el sanitario si existe, vacio si no
     */
    public Optional<Sanitario> buscarPorDni(String dni) {
        Sanitario sanitario = sanitarioDAO.buscarPorDni(dni);
        return Optional.ofNullable(sanitario);
    }

    /**
     * Inserta un nuevo sanitario con sus telefonos en una sola transaccion atomica.
     * Si cualquier operacion falla, se hace rollback de todo.
     * Registra la operacion en audit_log tras el commit.
     *
     * @param sanitario Sanitario a insertar
     * @param tel1 Primer telefono
     * @param tel2 Segundo telefono
     * @throws com.javafx.excepcion.DuplicadoException si DNI/email ya existe
     * @throws com.javafx.excepcion.ConexionException si hay error de BD
     */
    public void insertar(Sanitario sanitario, String tel1, String tel2) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            //Insertar sanitario (incluye cargo en sanitario_agrega_sanitario)
            sanitarioDAO.insertar(conn, sanitario);

            //Insertar telefonos
            sanitarioDAO.insertarTelefonos(conn, sanitario.getDni(), tel1, tel2);

            conn.commit();

            //Auditoria despues del commit (fire-and-forget)
            AuditService.insertSanitario(sanitario.getDni());

        } catch (SQLException e) {
            hacerRollback(conn);
            throw new ConexionException("Error al insertar sanitario", e);

        } catch (RuntimeException e) {
            hacerRollback(conn);
            throw e;

        } finally {
            cerrarConexion(conn);
        }
    }

    /**
     * Actualiza un sanitario existente con sus telefonos.
     * Registra la operacion en audit_log.
     *
     * @param sanitario Sanitario con los nuevos datos
     * @param dniOriginal DNI original del sanitario
     * @param tel1 Primer telefono
     * @param tel2 Segundo telefono
     * @throws com.javafx.excepcion.DuplicadoException si el nuevo DNI/email ya existe
     * @throws com.javafx.excepcion.ConexionException si hay error de BD
     */
    public void actualizar(Sanitario sanitario, String dniOriginal, String tel1, String tel2) {
        sanitarioDAO.actualizar(sanitario, dniOriginal);
        sanitarioDAO.actualizarTelefonos(sanitario.getDni(), tel1, tel2);
        AuditService.updateSanitario(sanitario.getDni());
    }

    /**
     * Elimina (soft delete) un sanitario.
     * Registra la operacion en audit_log.
     *
     * @param dni DNI del sanitario a eliminar
     * @throws com.javafx.excepcion.ConexionException si hay error de BD
     */
    public void eliminar(String dni) {
        sanitarioDAO.eliminar(dni);
        AuditService.deleteSanitario(dni);
    }

    // ==================== METODOS AUXILIARES ====================

    private void hacerRollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                System.err.println("Error al hacer rollback en SanitarioService: " + e.getMessage());
            }
        }
    }

    private void cerrarConexion(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error al restaurar autocommit: " + e.getMessage());
            }
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error al cerrar conexion: " + e.getMessage());
            }
        }
    }
}
