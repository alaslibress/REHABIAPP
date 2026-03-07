package com.javafx.service;

import com.javafx.Clases.Paciente;
import com.javafx.DAO.PacienteDAO;

import java.util.List;
import java.util.Optional;

/**
 * Capa de servicio para operaciones de Paciente
 * Actua como wrapper del PacienteDAO proporcionando una interfaz simplificada
 */
public class PacienteService {

    private final PacienteDAO pacienteDAO;

    /**
     * Constructor que inicializa el DAO
     */
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
     * Inserta un nuevo paciente con sus telefonos
     * @param paciente Paciente a insertar
     * @param tel1 Primer telefono
     * @param tel2 Segundo telefono
     * @return true si la insercion fue exitosa
     */
    public boolean insertar(Paciente paciente, String tel1, String tel2) {
        boolean pacienteInsertado = pacienteDAO.insertar(paciente);

        if (pacienteInsertado) {
            pacienteDAO.insertarTelefonos(paciente.getDni(), tel1, tel2);
            return true;
        }

        return false;
    }

    /**
     * Actualiza un paciente existente con sus telefonos
     * @param paciente Paciente con los nuevos datos
     * @param dniOriginal DNI original del paciente
     * @param tel1 Primer telefono
     * @param tel2 Segundo telefono
     * @return true si la actualizacion fue exitosa
     */
    public boolean actualizar(Paciente paciente, String dniOriginal, String tel1, String tel2) {
        boolean pacienteActualizado = pacienteDAO.actualizar(paciente, dniOriginal);

        if (pacienteActualizado) {
            pacienteDAO.actualizarTelefonos(paciente.getDni(), tel1, tel2);
            return true;
        }

        return false;
    }

    /**
     * Elimina un paciente
     * @param dni DNI del paciente a eliminar
     * @return true si la eliminacion fue exitosa
     */
    public boolean eliminar(String dni) {
        return pacienteDAO.eliminar(dni);
    }
}
