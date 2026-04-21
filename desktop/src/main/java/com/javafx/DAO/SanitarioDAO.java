package com.javafx.DAO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.javafx.Clases.ApiClient;
import com.javafx.Clases.Sanitario;
import com.javafx.dto.LoginResponse;
import com.javafx.dto.PageResponse;
import com.javafx.dto.SanitarioResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * DAO de sanitarios reescrito para consumir la API REST central.
 * Ya no accede directamente a PostgreSQL. BCrypt y auditoria son
 * responsabilidad de la API.
 */
public class SanitarioDAO {

    private final ApiClient api;

    public SanitarioDAO() {
        this.api = ApiClient.getInstancia();
    }

    /** Constructor para pruebas unitarias. No usar en produccion. */
    SanitarioDAO(ApiClient api) {
        this.api = api;
    }

    // ==================== CONSULTAS ====================

    /**
     * Lista todos los sanitarios activos.
     */
    public List<Sanitario> listarTodos() {
        PageResponse<SanitarioResponse> pagina = api.get(
            "/api/sanitarios?page=0&size=10000&sort=nombreSan,asc",
            new TypeReference<PageResponse<SanitarioResponse>>() {}
        );
        return pagina.contenido().stream()
            .map(Sanitario::desdeSanitarioResponse)
            .toList();
    }

    /**
     * Busca sanitarios por texto libre.
     */
    public List<Sanitario> buscarPorTexto(String texto) {
        String ruta = "/api/sanitarios/buscar?texto="
            + URLEncoder.encode(texto, StandardCharsets.UTF_8)
            + "&page=0&size=10000&sort=nombreSan,asc";
        PageResponse<SanitarioResponse> pagina = api.get(
            ruta,
            new TypeReference<PageResponse<SanitarioResponse>>() {}
        );
        return pagina.contenido().stream()
            .map(Sanitario::desdeSanitarioResponse)
            .toList();
    }

    /**
     * Obtiene un sanitario por DNI.
     */
    public Sanitario buscarPorDni(String dni) {
        SanitarioResponse response = api.get("/api/sanitarios/" + dni, SanitarioResponse.class);
        return Sanitario.desdeSanitarioResponse(response);
    }

    /**
     * Alias de buscarPorDni para compatibilidad con controladores existentes.
     */
    public Sanitario obtenerPorDNI(String dni) {
        return buscarPorDni(dni);
    }

    /**
     * Comprueba si existe un sanitario con el DNI dado.
     */
    public boolean existeDni(String dni) {
        try {
            api.get("/api/sanitarios/" + dni, SanitarioResponse.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Comprueba si existe un sanitario con el email dado.
     */
    public boolean existeEmail(String email) {
        return listarTodos().stream()
            .anyMatch(s -> email.equalsIgnoreCase(s.getEmail()));
    }

    // ==================== AUTENTICACION ====================

    /**
     * Autentica al sanitario contra la API. Almacena los tokens JWT en ApiClient.
     * @return LoginResponse con tokens y rol, o null si las credenciales son invalidas
     */
    public LoginResponse autenticar(String dni, String contrasena) {
        return api.login(dni, contrasena);
    }

    /**
     * Verifica la contrasena de un sanitario intentando login sin guardar sesion.
     * Se usa para re-confirmar identidad antes de operaciones criticas.
     */
    public boolean verificarContrasena(String dni, String contrasena) {
        try {
            api.login(dni, contrasena);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== ESCRITURA ====================

    /**
     * Inserta un nuevo sanitario.
     */
    public void insertar(Sanitario sanitario, String contrasena) {
        api.post("/api/sanitarios", sanitario.toSanitarioRequest(contrasena), SanitarioResponse.class);
    }

    /**
     * Actualiza un sanitario existente.
     */
    public void actualizar(Sanitario sanitario, String dniOriginal) {
        api.put("/api/sanitarios/" + dniOriginal,
            sanitario.toSanitarioRequest(null), SanitarioResponse.class);
    }

    /**
     * Cambia la contrasena de un sanitario.
     * @param dni DNI del sanitario
     * @param nuevaContrasena Nueva contrasena en texto plano
     */
    public void cambiarContrasena(String dni, String nuevaContrasena) {
        Sanitario sanitario = buscarPorDni(dni);
        api.put("/api/sanitarios/" + dni,
            sanitario.toSanitarioRequest(nuevaContrasena), SanitarioResponse.class);
    }

    /**
     * Elimina (soft delete) un sanitario. Solo permitido para SPECIALIST.
     */
    public void eliminar(String dni) {
        api.delete("/api/sanitarios/" + dni);
    }

    /**
     * Comprueba si existe el sanitario admin (ADMIN0000).
     * La API/Flyway gestiona el seed data; este metodo es solo de consulta.
     */
    public boolean existeAdmin() {
        return existeDni("ADMIN0000");
    }
}
