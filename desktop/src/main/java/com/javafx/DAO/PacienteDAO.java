package com.javafx.DAO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.javafx.Clases.ApiClient;
import com.javafx.Clases.Paciente;
import com.javafx.dto.PageResponse;
import com.javafx.dto.PacienteResponse;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * DAO de pacientes reescrito para consumir la API REST central.
 * Ya no accede directamente a PostgreSQL. El cifrado clinico y la
 * auditoria son responsabilidad de la API.
 */
public class PacienteDAO {

    private final ApiClient api;

    public PacienteDAO() {
        this.api = ApiClient.getInstancia();
    }

    /** Constructor para pruebas unitarias. No usar en produccion. */
    PacienteDAO(ApiClient api) {
        this.api = api;
    }

    // ==================== CONSULTAS ====================

    /**
     * Lista todos los pacientes activos.
     */
    public List<Paciente> listarTodos() {
        PageResponse<PacienteResponse> pagina = api.get(
            "/api/pacientes?page=0&size=10000&sort=nombrePac,asc",
            new TypeReference<PageResponse<PacienteResponse>>() {}
        );
        return pagina.contenido().stream()
            .map(Paciente::desdePacienteResponse)
            .toList();
    }

    /**
     * Busca pacientes por texto libre (DNI, nombre, apellidos, email, num_ss).
     */
    public List<Paciente> buscarPorTexto(String texto) {
        String ruta = "/api/pacientes/buscar?texto="
            + URLEncoder.encode(texto, StandardCharsets.UTF_8)
            + "&page=0&size=10000&sort=nombrePac,asc";
        PageResponse<PacienteResponse> pagina = api.get(
            ruta,
            new TypeReference<PageResponse<PacienteResponse>>() {}
        );
        return pagina.contenido().stream()
            .map(Paciente::desdePacienteResponse)
            .toList();
    }

    /**
     * Obtiene un paciente por DNI incluyendo datos clinicos descifrados.
     */
    public Paciente buscarPorDni(String dni) {
        PacienteResponse response = api.get("/api/pacientes/" + dni, PacienteResponse.class);
        return Paciente.desdePacienteResponse(response);
    }

    /**
     * Alias de buscarPorDni para compatibilidad con controladores existentes.
     */
    public Paciente obtenerPorDNI(String dni) {
        return buscarPorDni(dni);
    }

    /**
     * Comprueba si existe un paciente con el DNI dado.
     */
    public boolean existeDni(String dni) {
        try {
            api.get("/api/pacientes/" + dni, PacienteResponse.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Comprueba si existe un paciente con el email dado.
     * Busca en la lista completa por no haber endpoint especifico.
     */
    public boolean existeEmail(String email) {
        return listarTodos().stream()
            .anyMatch(p -> email.equalsIgnoreCase(p.getEmail()));
    }

    /**
     * Comprueba si existe un paciente con el numero de SS dado.
     */
    public boolean existeNumSS(String numSS) {
        return listarTodos().stream()
            .anyMatch(p -> numSS.equalsIgnoreCase(p.getNumSS()));
    }

    // ==================== ESCRITURA ====================

    /**
     * Inserta un nuevo paciente. Los telefonos se incluyen en el DTO.
     */
    public void insertar(Paciente paciente) {
        api.post("/api/pacientes", paciente.toPacienteRequest(), PacienteResponse.class);
    }

    /**
     * Actualiza un paciente existente.
     * @param paciente Datos nuevos del paciente
     * @param dniOriginal DNI antes de cualquier cambio (clave de la ruta)
     */
    public void actualizar(Paciente paciente, String dniOriginal) {
        api.put("/api/pacientes/" + dniOriginal, paciente.toPacienteRequest(), PacienteResponse.class);
    }

    /**
     * Elimina (soft delete) un paciente. Solo permitido para SPECIALIST.
     */
    public void eliminar(String dni) {
        api.delete("/api/pacientes/" + dni);
    }

    // ==================== FOTOS ====================

    /**
     * Sube la foto de un paciente como multipart/form-data.
     * @param dni DNI del paciente
     * @param archivo Archivo de imagen
     */
    public void insertarFoto(String dni, File archivo) {
        try {
            byte[] bytes = Files.readAllBytes(archivo.toPath());
            String boundary = "----RehabiAppBoundary" + System.currentTimeMillis();

            String inicio = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"foto\"; filename=\"" + archivo.getName() + "\"\r\n"
                + "Content-Type: " + detectarTipoContenido(archivo.getName()) + "\r\n\r\n";
            String fin = "\r\n--" + boundary + "--\r\n";

            byte[] inicioBytes = inicio.getBytes(StandardCharsets.UTF_8);
            byte[] finBytes = fin.getBytes(StandardCharsets.UTF_8);
            byte[] cuerpo = new byte[inicioBytes.length + bytes.length + finBytes.length];
            System.arraycopy(inicioBytes, 0, cuerpo, 0, inicioBytes.length);
            System.arraycopy(bytes, 0, cuerpo, inicioBytes.length, bytes.length);
            System.arraycopy(finBytes, 0, cuerpo, inicioBytes.length + bytes.length, finBytes.length);

            // HttpClient propio necesario para multipart/form-data (ApiClient no soporta este tipo de cuerpo).
            // La URL se obtiene de ApiClient para evitar hardcodear localhost:8080.
            HttpClient cliente = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(api.getBaseUrl() + "/api/pacientes/" + dni + "/foto"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Authorization", "Bearer " + api.getAccessToken())
                .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo))
                .build();

            cliente.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new com.javafx.excepcion.ConexionException("Error al subir la foto: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene la foto de un paciente como objeto Image de JavaFX.
     */
    public Image obtenerFoto(String dni) {
        try {
            // HttpClient propio necesario para recibir bytes en bruto; ApiClient devuelve String.
            // La URL se obtiene de ApiClient para evitar hardcodear localhost:8080.
            HttpClient cliente = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(api.getBaseUrl() + "/api/pacientes/" + dni + "/foto"))
                .header("Authorization", "Bearer " + api.getAccessToken())
                .GET()
                .build();

            HttpResponse<byte[]> response = cliente.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200 && response.body() != null && response.body().length > 0) {
                return new Image(new ByteArrayInputStream(response.body()));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== UTILIDADES ====================

    private String detectarTipoContenido(String nombreArchivo) {
        String nombre = nombreArchivo.toLowerCase();
        if (nombre.endsWith(".png")) return "image/png";
        if (nombre.endsWith(".jpg") || nombre.endsWith(".jpeg")) return "image/jpeg";
        if (nombre.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }
}
