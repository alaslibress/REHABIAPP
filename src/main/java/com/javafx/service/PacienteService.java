package com.javafx.service;

import com.javafx.Clases.ConexionBD;
import com.javafx.Clases.Paciente;
import com.javafx.DAO.PacienteDAO;
import com.javafx.excepcion.ConexionException;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Capa de servicio para operaciones de Paciente.
 * Gestiona transacciones compuestas (paciente + telefonos + foto en una sola transaccion)
 * y registra auditoria automatica.
 *
 * Los metodos de escritura propagan las excepciones del DAO
 * (DuplicadoException, ConexionException, ValidacionException)
 * para que los controladores muestren mensajes especificos.
 */
public class PacienteService {

    private final PacienteDAO pacienteDAO;

    public PacienteService() {
        this.pacienteDAO = new PacienteDAO();
    }

    /**
     * Lista todos los pacientes
     * @return Lista de todos los pacientes
     */
    public List<Paciente> listarTodos() {
        return pacienteDAO.listarTodos();
    }

    /**
     * Busca un paciente por su DNI
     * @param dni DNI del paciente
     * @return Optional con el paciente si existe, vacio si no
     */
    public Optional<Paciente> buscarPorDni(String dni) {
        Paciente paciente = pacienteDAO.buscarPorDni(dni);
        return Optional.ofNullable(paciente);
    }

    /**
     * Inserta un nuevo paciente con sus telefonos y foto en una sola transaccion atomica.
     * Si cualquier operacion falla, se hace rollback de todo.
     * Registra la operacion en audit_log tras el commit.
     *
     * @param paciente Paciente a insertar
     * @param tel1 Primer telefono
     * @param tel2 Segundo telefono
     * @param archivoFoto Archivo de foto (puede ser null)
     * @throws com.javafx.excepcion.DuplicadoException si DNI/email/numSS ya existe
     * @throws com.javafx.excepcion.ValidacionException si los datos son invalidos
     * @throws com.javafx.excepcion.ConexionException si hay error de BD
     */
    public void insertar(Paciente paciente, String tel1, String tel2, File archivoFoto) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            //Insertar paciente (incluye direccion y contador de sanitario)
            pacienteDAO.insertar(conn, paciente);

            //Insertar telefonos
            pacienteDAO.insertarTelefonos(conn, paciente.getDni(), tel1, tel2);

            //Insertar foto si existe
            if (archivoFoto != null) {
                pacienteDAO.insertarFoto(conn, paciente.getDni(), archivoFoto);
            }

            conn.commit();

            //Auditoria despues del commit (fire-and-forget)
            AuditService.insertPaciente(paciente.getDni());

        } catch (SQLException e) {
            hacerRollback(conn);
            throw new ConexionException("Error al insertar paciente", e);

        } catch (RuntimeException e) {
            //DuplicadoException, ValidacionException, etc.
            hacerRollback(conn);
            throw e;

        } finally {
            cerrarConexion(conn);
        }
    }

    /**
     * Sobrecarga sin foto para compatibilidad
     */
    public void insertar(Paciente paciente, String tel1, String tel2) {
        insertar(paciente, tel1, tel2, null);
    }

    /**
     * Actualiza un paciente existente con sus telefonos y foto en una sola transaccion atomica.
     * Si cualquier operacion falla, se hace rollback de todo.
     * Registra la operacion en audit_log tras el commit.
     *
     * @param paciente Paciente con los nuevos datos
     * @param dniOriginal DNI original del paciente
     * @param tel1 Primer telefono
     * @param tel2 Segundo telefono
     * @param archivoFoto Archivo de foto (puede ser null, no se actualiza si es null)
     * @throws com.javafx.excepcion.DuplicadoException si el nuevo DNI/email/numSS ya existe
     * @throws com.javafx.excepcion.ConexionException si hay error de BD
     */
    public void actualizar(Paciente paciente, String dniOriginal, String tel1, String tel2, File archivoFoto) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            //Actualizar paciente
            pacienteDAO.actualizar(conn, paciente, dniOriginal);

            //Actualizar telefonos
            pacienteDAO.actualizarTelefonos(conn, paciente.getDni(), tel1, tel2);

            //Actualizar foto si existe
            if (archivoFoto != null) {
                pacienteDAO.insertarFoto(conn, paciente.getDni(), archivoFoto);
            }

            conn.commit();

            //Auditoria despues del commit (fire-and-forget)
            AuditService.updatePaciente(paciente.getDni());

        } catch (SQLException e) {
            hacerRollback(conn);
            throw new ConexionException("Error al actualizar paciente", e);

        } catch (RuntimeException e) {
            hacerRollback(conn);
            throw e;

        } finally {
            cerrarConexion(conn);
        }
    }

    /**
     * Sobrecarga sin foto para compatibilidad
     */
    public void actualizar(Paciente paciente, String dniOriginal, String tel1, String tel2) {
        actualizar(paciente, dniOriginal, tel1, tel2, null);
    }

    /**
     * Elimina (soft delete) un paciente.
     * Registra la operacion en audit_log.
     *
     * @param dni DNI del paciente a eliminar
     * @throws com.javafx.excepcion.ConexionException si hay error de BD
     */
    public void eliminar(String dni) {
        pacienteDAO.eliminar(dni);
        AuditService.deletePaciente(dni);
    }

    // ==================== METODOS AUXILIARES ====================

    private void hacerRollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                System.err.println("Error al hacer rollback en PacienteService: " + e.getMessage());
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
