package com.javafx.DAO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.javafx.Clases.ApiClient;
import com.javafx.Clases.Discapacidad;
import com.javafx.Clases.NivelProgresion;
import com.javafx.Clases.Tratamiento;
import com.javafx.dto.DiscapacidadResponse;
import com.javafx.dto.NivelProgresionResponse;
import com.javafx.dto.TratamientoResponse;

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
}
