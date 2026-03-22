package com.javafx.service;

import com.javafx.Clases.ConexionBD;
import com.javafx.Clases.Sanitario;
import com.javafx.DAO.SanitarioDAO;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.ValidacionException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Capa de servicio para operaciones de Sanitario.
 * Gestiona transacciones atomicas (sanitario + cargo + telefonos) y auditoria.
 *
 * Con HikariCP, cada operacion obtiene su propia conexion del pool via
 * try-with-resources. Al cerrar la conexion se devuelve al pool automaticamente.
 *
 * Todos los metodos de escritura son void y lanzan excepciones tipadas:
 * - DuplicadoException: DNI o email duplicado
 * - ValidacionException: datos invalidos
 * - ConexionException: error de base de datos
 */
public class SanitarioService {

    private final SanitarioDAO sanitarioDAO;

    public SanitarioService() {
        this.sanitarioDAO = new SanitarioDAO();
    }

    /**
     * Lista todos los sanitarios activos
     */
    public List<Sanitario> listarTodos() {
        return sanitarioDAO.listarTodos();
    }

    /**
     * Busca un sanitario por su DNI
     */
    public Optional<Sanitario> buscarPorDni(String dni) {
        return Optional.ofNullable(sanitarioDAO.buscarPorDni(dni));
    }

    /**
     * Inserta un nuevo sanitario con sus telefonos de forma atomica.
     * La operacion completa (sanitario + cargo + telefonos) se ejecuta en una sola transaccion.
     *
     * @param sanitario Sanitario a insertar
     * @param tel1 Primer telefono
     * @param tel2 Segundo telefono (opcional)
     * @throws DuplicadoException si ya existe un sanitario con ese DNI o email
     * @throws ConexionException si hay error de base de datos
     */
    public void insertar(Sanitario sanitario, String tel1, String tel2) {
        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);

            try {
                //Insertar sanitario + cargo en la misma transaccion
                sanitarioDAO.insertar(conn, sanitario);

                //Insertar telefonos en la misma transaccion
                sanitarioDAO.insertarTelefonos(conn, sanitario.getDni(), tel1, tel2);

                conn.commit();

            } catch (RuntimeException e) {
                rollback(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al insertar sanitario", e);
        }

        //Auditoria (fire-and-forget, fuera de la transaccion)
        AuditService.insertSanitario(sanitario.getDni());
    }

    /**
     * Actualiza un sanitario existente con sus telefonos de forma atomica.
     *
     * @param sanitario Sanitario con los nuevos datos
     * @param dniOriginal DNI original del sanitario
     * @param tel1 Primer telefono
     * @param tel2 Segundo telefono (opcional)
     * @throws DuplicadoException si el nuevo DNI o email ya existe
     * @throws ValidacionException si el sanitario no existe
     * @throws ConexionException si hay error de base de datos
     */
    public void actualizar(Sanitario sanitario, String dniOriginal, String tel1, String tel2) {
        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);

            try {
                //Actualizar sanitario + cargo
                sanitarioDAO.actualizar(conn, sanitario, dniOriginal);

                //Actualizar telefonos en la misma transaccion
                sanitarioDAO.actualizarTelefonos(conn, sanitario.getDni(), tel1, tel2);

                conn.commit();

            } catch (RuntimeException e) {
                rollback(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al actualizar sanitario", e);
        }

        //Auditoria
        AuditService.updateSanitario(sanitario.getDni());
    }

    /**
     * Desactiva un sanitario (soft delete)
     *
     * @param dni DNI del sanitario a desactivar
     * @throws ValidacionException si el sanitario no existe
     * @throws ConexionException si hay error de base de datos
     */
    public void eliminar(String dni) {
        sanitarioDAO.eliminar(dni);
        AuditService.deleteSanitario(dni);
    }

    // ==================== UTILIDADES ====================

    private void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                //Rollback fallido
            }
        }
    }
}
