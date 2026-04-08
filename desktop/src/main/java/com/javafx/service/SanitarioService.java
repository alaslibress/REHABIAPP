package com.javafx.service;

import com.javafx.Clases.Sanitario;
import com.javafx.DAO.SanitarioDAO;
import com.javafx.dto.LoginResponse;

import java.util.List;
import java.util.Optional;

/**
 * Capa de servicio para operaciones de Sanitario.
 * Delega al SanitarioDAO que consume la API REST.
 * La auditoria, el BCrypt y el cifrado son responsabilidad de la API.
 *
 * <p><b>Atomicidad:</b> La creacion y actualizacion de un sanitario son operaciones
 * atomicas server-side. Una sola llamada HTTP a POST/PUT /api/sanitarios dispara una
 * unica transaccion @Transactional en la API que persiste sanitario, cargo, telefonos
 * y contrasena hasheada via cascade. Si cualquier parte falla, se hace rollback completo
 * — no queda ningun registro parcial en la base de datos.</p>
 */
public class SanitarioService {

    private final SanitarioDAO sanitarioDAO;

    public SanitarioService() {
        this(new SanitarioDAO());
    }

    /** Constructor para tests con DAO inyectado. */
    SanitarioService(SanitarioDAO sanitarioDAO) {
        this.sanitarioDAO = sanitarioDAO;
    }

    /**
     * Lista todos los sanitarios activos.
     */
    public List<Sanitario> listarTodos() {
        return sanitarioDAO.listarTodos();
    }

    /**
     * Busca sanitarios por texto libre.
     */
    public List<Sanitario> buscarPorTexto(String texto) {
        return sanitarioDAO.buscarPorTexto(texto);
    }

    /**
     * Busca un sanitario por su DNI.
     */
    public Optional<Sanitario> buscarPorDni(String dni) {
        return Optional.ofNullable(sanitarioDAO.buscarPorDni(dni));
    }

    /**
     * Autentica al sanitario contra la API. Almacena los tokens JWT en ApiClient.
     */
    public LoginResponse autenticar(String dni, String contrasena) {
        return sanitarioDAO.autenticar(dni, contrasena);
    }

    /**
     * Inserta un nuevo sanitario.
     */
    public void insertar(Sanitario sanitario, String contrasena) {
        sanitarioDAO.insertar(sanitario, contrasena);
    }

    /**
     * Inserta un nuevo sanitario con telefonos explicitamente.
     */
    public void insertar(Sanitario sanitario, String contrasena, String tel1, String tel2) {
        sanitario.setTelefono1(tel1 != null ? tel1 : "");
        sanitario.setTelefono2(tel2 != null ? tel2 : "");
        sanitarioDAO.insertar(sanitario, contrasena);
    }

    /**
     * Actualiza un sanitario existente.
     */
    public void actualizar(Sanitario sanitario, String dniOriginal) {
        sanitarioDAO.actualizar(sanitario, dniOriginal);
    }

    /**
     * Actualiza un sanitario con telefonos explicitamente.
     */
    public void actualizar(Sanitario sanitario, String dniOriginal, String tel1, String tel2) {
        sanitario.setTelefono1(tel1 != null ? tel1 : "");
        sanitario.setTelefono2(tel2 != null ? tel2 : "");
        sanitarioDAO.actualizar(sanitario, dniOriginal);
    }

    /**
     * Cambia la contrasena de un sanitario.
     */
    public void cambiarContrasena(String dni, String nuevaContrasena) {
        sanitarioDAO.cambiarContrasena(dni, nuevaContrasena);
    }

    /**
     * Desactiva un sanitario (soft delete).
     */
    public void eliminar(String dni) {
        sanitarioDAO.eliminar(dni);
    }

    /**
     * Comprueba si existe un sanitario con el DNI dado.
     */
    public boolean existeDni(String dni) {
        return sanitarioDAO.existeDni(dni);
    }

    /**
     * Verifica la contrasena de un sanitario.
     */
    public boolean verificarContrasena(String dni, String contrasena) {
        return sanitarioDAO.verificarContrasena(dni, contrasena);
    }
}
