package com.javafx.service;

import com.javafx.Clases.ConexionBD;
import com.javafx.Clases.Paciente;
import com.javafx.DAO.PacienteDAO;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.ValidacionException;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Capa de servicio para operaciones de Paciente.
 * Gestiona transacciones atomicas (paciente + telefonos + foto) y auditoria.
 *
 * Con HikariCP, cada operacion obtiene su propia conexion del pool via
 * try-with-resources. Al cerrar la conexion se devuelve al pool automaticamente
 * con autoCommit reseteado.
 *
 * Todos los metodos de escritura son void y lanzan excepciones tipadas:
 * - DuplicadoException: DNI, email o NSS duplicado
 * - ValidacionException: datos invalidos (direccion incompleta, etc.)
 * - ConexionException: error de base de datos
 */
public class PacienteService {

    private final PacienteDAO pacienteDAO;

    public PacienteService() {
        this.pacienteDAO = new PacienteDAO();
    }

    /**
     * Lista todos los pacientes activos
     */
    public List<Paciente> listarTodos() {
        return pacienteDAO.listarTodos();
    }

    /**
     * Busca un paciente por su DNI
     */
    public Optional<Paciente> buscarPorDni(String dni) {
        return Optional.ofNullable(pacienteDAO.buscarPorDni(dni));
    }

    /**
     * Inserta un nuevo paciente con sus telefonos de forma atomica.
     * La operacion completa (paciente + telefonos) se ejecuta en una sola transaccion.
     *
     * @param paciente Paciente a insertar
     * @param tel1 Primer telefono
     * @param tel2 Segundo telefono (opcional)
     * @throws DuplicadoException si ya existe un paciente con ese DNI, email o NSS
     * @throws ValidacionException si los datos son invalidos
     * @throws ConexionException si hay error de base de datos
     */
    public void insertar(Paciente paciente, String tel1, String tel2) {
        insertar(paciente, tel1, tel2, null);
    }

    /**
     * Inserta un nuevo paciente con sus telefonos y foto de forma atomica.
     *
     * @param paciente Paciente a insertar
     * @param tel1 Primer telefono
     * @param tel2 Segundo telefono (opcional)
     * @param archivoFoto Archivo de foto (puede ser null)
     */
    public void insertar(Paciente paciente, String tel1, String tel2, File archivoFoto) {
        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);

            try {
                //Insertar paciente (incluye direccion y contador sanitario)
                pacienteDAO.insertar(conn, paciente);

                //Insertar telefonos en la misma transaccion
                pacienteDAO.insertarTelefonos(conn, paciente.getDni(), tel1, tel2);

                //Insertar foto si existe, en la misma transaccion
                if (archivoFoto != null) {
                    pacienteDAO.insertarFoto(conn, paciente.getDni(), archivoFoto);
                }

                conn.commit();

            } catch (RuntimeException e) {
                rollback(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al insertar paciente", e);
        }

        //Auditoria (fire-and-forget, fuera de la transaccion)
        AuditService.insertPaciente(paciente.getDni());
    }

    /**
     * Actualiza un paciente existente con sus telefonos de forma atomica.
     *
     * @param paciente Paciente con los nuevos datos
     * @param dniOriginal DNI original del paciente
     * @param tel1 Primer telefono
     * @param tel2 Segundo telefono (opcional)
     * @throws DuplicadoException si el nuevo DNI/email/NSS ya existe
     * @throws ValidacionException si el paciente no existe
     * @throws ConexionException si hay error de base de datos
     */
    public void actualizar(Paciente paciente, String dniOriginal, String tel1, String tel2) {
        actualizar(paciente, dniOriginal, tel1, tel2, null);
    }

    /**
     * Actualiza un paciente existente con sus telefonos y foto de forma atomica.
     */
    public void actualizar(Paciente paciente, String dniOriginal, String tel1, String tel2, File archivoFoto) {
        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);

            try {
                //Actualizar paciente
                pacienteDAO.actualizar(conn, paciente, dniOriginal);

                //Actualizar telefonos en la misma transaccion
                pacienteDAO.actualizarTelefonos(conn, paciente.getDni(), tel1, tel2);

                //Actualizar foto si existe, en la misma transaccion
                if (archivoFoto != null) {
                    pacienteDAO.insertarFoto(conn, paciente.getDni(), archivoFoto);
                }

                conn.commit();

            } catch (RuntimeException e) {
                rollback(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al actualizar paciente", e);
        }

        //Auditoria
        AuditService.updatePaciente(paciente.getDni());
    }

    /**
     * Desactiva un paciente (soft delete)
     *
     * @param dni DNI del paciente a desactivar
     * @throws ValidacionException si el paciente no existe
     * @throws ConexionException si hay error de base de datos
     */
    public void eliminar(String dni) {
        pacienteDAO.eliminar(dni);
        AuditService.deletePaciente(dni);
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
