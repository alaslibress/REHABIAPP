package com.javafx.service;

import com.javafx.Clases.Paciente;
import com.javafx.DAO.PacienteDAO;
import javafx.scene.image.Image;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Capa de servicio para operaciones de Paciente.
 * Delega directamente al PacienteDAO que consume la API REST.
 * La auditoria y el cifrado son responsabilidad de la API.
 */
public class PacienteService {

    private final PacienteDAO pacienteDAO = new PacienteDAO();

    /**
     * Lista todos los pacientes activos.
     */
    public List<Paciente> listarTodos() {
        return pacienteDAO.listarTodos();
    }

    /**
     * Busca pacientes por texto libre.
     */
    public List<Paciente> buscarPorTexto(String texto) {
        return pacienteDAO.buscarPorTexto(texto);
    }

    /**
     * Busca un paciente por su DNI.
     */
    public Optional<Paciente> buscarPorDni(String dni) {
        return Optional.ofNullable(pacienteDAO.buscarPorDni(dni));
    }

    /**
     * Inserta un nuevo paciente. Los telefonos deben estar en el objeto Paciente.
     */
    public void insertar(Paciente paciente) {
        pacienteDAO.insertar(paciente);
    }

    /**
     * Inserta un nuevo paciente con telefonos explicitamente.
     * Los telefonos se asignan al objeto antes de enviarlo a la API.
     */
    public void insertar(Paciente paciente, String tel1, String tel2) {
        paciente.setTelefono1(tel1 != null ? tel1 : "");
        paciente.setTelefono2(tel2 != null ? tel2 : "");
        pacienteDAO.insertar(paciente);
    }

    /**
     * Inserta un nuevo paciente con telefonos y foto.
     */
    public void insertar(Paciente paciente, String tel1, String tel2, File archivoFoto) {
        insertar(paciente, tel1, tel2);
        if (archivoFoto != null) {
            pacienteDAO.insertarFoto(paciente.getDni(), archivoFoto);
        }
    }

    /**
     * Actualiza un paciente existente.
     */
    public void actualizar(Paciente paciente, String dniOriginal) {
        pacienteDAO.actualizar(paciente, dniOriginal);
    }

    /**
     * Actualiza un paciente con telefonos explicitamente.
     */
    public void actualizar(Paciente paciente, String dniOriginal, String tel1, String tel2) {
        paciente.setTelefono1(tel1 != null ? tel1 : "");
        paciente.setTelefono2(tel2 != null ? tel2 : "");
        pacienteDAO.actualizar(paciente, dniOriginal);
    }

    /**
     * Actualiza un paciente con telefonos y foto.
     */
    public void actualizar(Paciente paciente, String dniOriginal, String tel1, String tel2, File archivoFoto) {
        actualizar(paciente, dniOriginal, tel1, tel2);
        if (archivoFoto != null) {
            pacienteDAO.insertarFoto(paciente.getDni(), archivoFoto);
        }
    }

    /**
     * Desactiva un paciente (soft delete).
     */
    public void eliminar(String dni) {
        pacienteDAO.eliminar(dni);
    }

    /**
     * Obtiene la foto de un paciente.
     */
    public Image obtenerFoto(String dni) {
        return pacienteDAO.obtenerFoto(dni);
    }

    /**
     * Comprueba si existe un paciente con el DNI dado.
     */
    public boolean existeDni(String dni) {
        return pacienteDAO.existeDni(dni);
    }
}
