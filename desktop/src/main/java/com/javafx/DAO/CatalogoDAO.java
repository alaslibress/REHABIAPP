package com.javafx.DAO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.javafx.Clases.ApiClient;
import com.javafx.Clases.Discapacidad;
import com.javafx.Clases.NivelProgresion;
import com.javafx.Clases.PdfMetadato;
import com.javafx.Clases.Tratamiento;
import com.javafx.Clases.Videojuego;
import com.javafx.dto.DiscapacidadRequest;
import com.javafx.dto.DiscapacidadResponse;
import com.javafx.dto.NivelProgresionResponse;
import com.javafx.dto.TratamientoRequest;
import com.javafx.dto.TratamientoResponse;
import com.javafx.excepcion.ValidacionException;

import java.util.List;

/**
 * DAO de solo lectura para el catalogo clinico.
 * Consume los endpoints /api/catalogo de la API REST.
 */
public class CatalogoDAO {

    private final ApiClient api = ApiClient.getInstancia();

    /**
     * Lista todas las discapacidades del catalogo.
     */
    public List<Discapacidad> listarDiscapacidades() {
        List<DiscapacidadResponse> respuestas = api.get(
            "/api/catalogo/discapacidades",
            new TypeReference<List<DiscapacidadResponse>>() {}
        );
        return respuestas.stream()
            .map(Discapacidad::desdeDiscapacidadResponse)
            .toList();
    }

    /**
     * Obtiene una discapacidad concreta por su codigo.
     */
    public Discapacidad obtenerDiscapacidad(String codDis) {
        DiscapacidadResponse response = api.get(
            "/api/catalogo/discapacidades/" + codDis, DiscapacidadResponse.class);
        return Discapacidad.desdeDiscapacidadResponse(response);
    }

    /**
     * Lista todos los tratamientos del catalogo.
     */
    public List<Tratamiento> listarTratamientos() {
        List<TratamientoResponse> respuestas = api.get(
            "/api/catalogo/tratamientos",
            new TypeReference<List<TratamientoResponse>>() {}
        );
        return respuestas.stream()
            .map(Tratamiento::desdeTratamientoResponse)
            .toList();
    }

    /**
     * Obtiene un tratamiento concreto por su codigo.
     */
    public Tratamiento obtenerTratamiento(String codTrat) {
        TratamientoResponse response = api.get(
            "/api/catalogo/tratamientos/" + codTrat, TratamientoResponse.class);
        return Tratamiento.desdeTratamientoResponse(response);
    }

    /**
     * Lista los tratamientos vinculados a una discapacidad concreta.
     */
    public List<Tratamiento> listarTratamientosPorDiscapacidad(String codDis) {
        List<TratamientoResponse> respuestas = api.get(
            "/api/catalogo/tratamientos/discapacidad/" + codDis,
            new TypeReference<List<TratamientoResponse>>() {}
        );
        return respuestas.stream()
            .map(Tratamiento::desdeTratamientoResponse)
            .toList();
    }

    /**
     * Lista los niveles de progresion clinica.
     */
    public List<NivelProgresion> listarNivelesProgresion() {
        List<NivelProgresionResponse> respuestas = api.get(
            "/api/catalogo/niveles-progresion",
            new TypeReference<List<NivelProgresionResponse>>() {}
        );
        return respuestas.stream()
            .map(NivelProgresion::desdeNivelProgresionResponse)
            .toList();
    }

    // ==================== DISCAPACIDADES CRUD ====================

    /**
     * Crea una nueva discapacidad en el catalogo.
     */
    public Discapacidad crearDiscapacidad(DiscapacidadRequest request) {
        DiscapacidadResponse response = api.post(
            "/api/catalogo/discapacidades", request, DiscapacidadResponse.class);
        return Discapacidad.desdeDiscapacidadResponse(response);
    }

    /**
     * Actualiza una discapacidad existente.
     */
    public Discapacidad actualizarDiscapacidad(String codDis, DiscapacidadRequest request) {
        DiscapacidadResponse response = api.put(
            "/api/catalogo/discapacidades/" + codDis, request, DiscapacidadResponse.class);
        return Discapacidad.desdeDiscapacidadResponse(response);
    }

    /**
     * Elimina una discapacidad del catalogo.
     */
    public void eliminarDiscapacidad(String codDis) {
        api.delete("/api/catalogo/discapacidades/" + codDis);
    }

    // ==================== TRATAMIENTOS CRUD ====================

    /**
     * Crea un nuevo tratamiento en el catalogo.
     */
    public Tratamiento crearTratamiento(TratamientoRequest request) {
        TratamientoResponse response = api.post(
            "/api/catalogo/tratamientos", request, TratamientoResponse.class);
        return Tratamiento.desdeTratamientoResponse(response);
    }

    /**
     * Actualiza un tratamiento existente.
     */
    public Tratamiento actualizarTratamiento(String codTrat, TratamientoRequest request) {
        TratamientoResponse response = api.put(
            "/api/catalogo/tratamientos/" + codTrat, request, TratamientoResponse.class);
        return Tratamiento.desdeTratamientoResponse(response);
    }

    /**
     * Elimina un tratamiento del catalogo.
     */
    public void eliminarTratamiento(String codTrat) {
        api.delete("/api/catalogo/tratamientos/" + codTrat);
    }

    // ==================== VINCULOS TRATAMIENTO-DISCAPACIDAD ====================

    /**
     * Vincula un tratamiento a una discapacidad.
     */
    public void vincularTratamientoDiscapacidad(String codTrat, String codDis) {
        api.post("/api/catalogo/tratamientos/" + codTrat + "/discapacidades/" + codDis,
            null, Void.class);
    }

    /**
     * Desvincula un tratamiento de una discapacidad.
     */
    public void desvincularTratamientoDiscapacidad(String codTrat, String codDis) {
        api.delete("/api/catalogo/tratamientos/" + codTrat + "/discapacidades/" + codDis);
    }

    /**
     * Lista las discapacidades vinculadas a un tratamiento.
     */
    public List<Discapacidad> listarDiscapacidadesDeTratamiento(String codTrat) {
        List<DiscapacidadResponse> respuestas = api.get(
            "/api/catalogo/tratamientos/" + codTrat + "/discapacidades",
            new TypeReference<List<DiscapacidadResponse>>() {}
        );
        return respuestas.stream()
            .map(Discapacidad::desdeDiscapacidadResponse)
            .toList();
    }

    // ==================== PDF DE TRATAMIENTO ====================

    /**
     * Sube el PDF de un tratamiento (multipart, parte "file").
     */
    public void subirPdfTratamiento(String codTrat, byte[] bytes, String filename) {
        api.uploadFile("/api/tratamientos/" + codTrat + "/pdf",
                "file", bytes, filename, "application/pdf", Void.class);
    }

    /**
     * Obtiene los metadatos del PDF del tratamiento. Devuelve null si no hay PDF (404).
     */
    public PdfMetadato consultarMetadatosPdf(String codTrat) {
        try {
            return api.get("/api/tratamientos/" + codTrat + "/pdf/metadatos", PdfMetadato.class);
        } catch (ValidacionException e) {
            return null;
        }
    }

    /**
     * Descarga los bytes del PDF del tratamiento.
     */
    public byte[] descargarPdfTratamiento(String codTrat) {
        return api.getBytes("/api/tratamientos/" + codTrat + "/pdf");
    }

    /**
     * Elimina el PDF del tratamiento.
     */
    public void eliminarPdfTratamiento(String codTrat) {
        api.delete("/api/tratamientos/" + codTrat + "/pdf");
    }

    // ==================== ASOCIACION TRATAMIENTO-VIDEOJUEGO ====================

    /**
     * Lista los videojuegos vinculados a un tratamiento.
     */
    public List<Videojuego> listarJuegosDeTratamiento(String codTrat) {
        return api.get(
            "/api/catalogo/tratamientos/" + codTrat + "/videojuegos",
            new TypeReference<List<Videojuego>>() {}
        );
    }

    /**
     * Vincula un videojuego a un tratamiento.
     */
    public void vincularJuego(String codTrat, long idVideojuego) {
        api.post("/api/catalogo/tratamientos/" + codTrat + "/videojuegos/" + idVideojuego,
                null, Void.class);
    }

    /**
     * Desvincula un videojuego de un tratamiento.
     */
    public void desvincularJuego(String codTrat, long idVideojuego) {
        api.delete("/api/catalogo/tratamientos/" + codTrat + "/videojuegos/" + idVideojuego);
    }
}
