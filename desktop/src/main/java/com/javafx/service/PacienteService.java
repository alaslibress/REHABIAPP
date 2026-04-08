package com.javafx.service;

import com.javafx.Clases.Paciente;
import com.javafx.DAO.PacienteDAO;
import com.javafx.excepcion.ValidacionException;
import javafx.scene.image.Image;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Capa de servicio para operaciones de Paciente.
 * Delega al PacienteDAO que consume la API REST.
 * La auditoria y el cifrado son responsabilidad de la API.
 *
 * <p><b>Atomicidad:</b> La creacion y actualizacion de un paciente son operaciones
 * atomicas server-side. Una sola llamada HTTP multipart a POST/PUT /api/pacientes
 * dispara una unica transaccion @Transactional en la API que persiste paciente,
 * telefonos, direccion y foto via cascade. Si cualquier parte falla, se hace
 * rollback completo — no queda ningun registro parcial en la base de datos.</p>
 */
public class PacienteService {

    private final PacienteDAO pacienteDAO;

    public PacienteService() {
        this(new PacienteDAO());
    }

    /** Constructor para tests con DAO inyectado. */
    PacienteService(PacienteDAO pacienteDAO) {
        this.pacienteDAO = pacienteDAO;
    }

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
        pacienteDAO.insertar(paciente, null);
    }

    /**
     * Inserta un nuevo paciente con telefonos explicitamente.
     */
    public void insertar(Paciente paciente, String tel1, String tel2) {
        paciente.setTelefono1(tel1 != null ? tel1 : "");
        paciente.setTelefono2(tel2 != null ? tel2 : "");
        pacienteDAO.insertar(paciente, null);
    }

    /**
     * Inserta un nuevo paciente con telefonos y foto en una unica transaccion atomica.
     * Si la foto falla, el paciente NO se crea (rollback total en el server).
     */
    public void insertar(Paciente paciente, String tel1, String tel2, File archivoFoto) {
        paciente.setTelefono1(tel1 != null ? tel1 : "");
        paciente.setTelefono2(tel2 != null ? tel2 : "");
        byte[] fotoBytes = leerBytesArchivo(archivoFoto);
        pacienteDAO.insertar(paciente, fotoBytes);
    }

    /**
     * Actualiza un paciente existente.
     */
    public void actualizar(Paciente paciente, String dniOriginal) {
        pacienteDAO.actualizar(paciente, dniOriginal, null);
    }

    /**
     * Actualiza un paciente con telefonos explicitamente.
     */
    public void actualizar(Paciente paciente, String dniOriginal, String tel1, String tel2) {
        paciente.setTelefono1(tel1 != null ? tel1 : "");
        paciente.setTelefono2(tel2 != null ? tel2 : "");
        pacienteDAO.actualizar(paciente, dniOriginal, null);
    }

    /**
     * Actualiza un paciente con telefonos y foto en una unica transaccion atomica.
     * Si fotoBytes es null (archivoFoto null), la foto existente se mantiene intacta.
     */
    public void actualizar(Paciente paciente, String dniOriginal, String tel1, String tel2, File archivoFoto) {
        paciente.setTelefono1(tel1 != null ? tel1 : "");
        paciente.setTelefono2(tel2 != null ? tel2 : "");
        byte[] fotoBytes = leerBytesArchivo(archivoFoto);
        pacienteDAO.actualizar(paciente, dniOriginal, fotoBytes);
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

    /**
     * Lee los bytes de un archivo, devolviendo null si el archivo es null.
     * @throws ValidacionException si el archivo no se puede leer.
     */
    private byte[] leerBytesArchivo(File archivo) {
        if (archivo == null) return null;
        try {
            return java.nio.file.Files.readAllBytes(archivo.toPath());
        } catch (java.io.IOException e) {
            throw new ValidacionException(
                    "No se pudo leer el archivo: " + archivo.getName(), "foto");
        }
    }
}
