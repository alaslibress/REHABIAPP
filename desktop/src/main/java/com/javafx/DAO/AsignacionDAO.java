package com.javafx.DAO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.javafx.Clases.ApiClient;
import com.javafx.Clases.PacienteDiscapacidad;
import com.javafx.Clases.PacienteTratamiento;
import com.javafx.dto.PacienteDiscapacidadRequest;
import com.javafx.dto.PacienteDiscapacidadResponse;
import com.javafx.dto.PacienteTratamientoResponse;

import java.util.List;

/**
 * DAO para gestionar la asignacion de discapacidades y tratamientos a pacientes.
 * Consume los endpoints /api/pacientes/{dniPac}/discapacidades y
 * /api/pacientes/{dniPac}/tratamientos de la API REST.
 */
public class AsignacionDAO {

    private final ApiClient api = ApiClient.getInstancia();

    // ==================== DISCAPACIDADES ====================

    /**
     * Lista las discapacidades asignadas a un paciente con su nivel de progresion.
     */
    public List<PacienteDiscapacidad> listarDiscapacidades(String dniPac) {
        List<PacienteDiscapacidadResponse> respuestas = api.get(
            "/api/pacientes/" + dniPac + "/discapacidades",
            new TypeReference<List<PacienteDiscapacidadResponse>>() {}
        );
        return respuestas.stream()
            .map(PacienteDiscapacidad::desdePacienteDiscapacidadResponse)
            .toList();
    }

    /**
     * Asigna una discapacidad a un paciente con nivel de progresion y notas opcionales.
     */
    public PacienteDiscapacidad asignarDiscapacidad(String dniPac,
                                                     PacienteDiscapacidadRequest request) {
        PacienteDiscapacidadResponse response = api.post(
            "/api/pacientes/" + dniPac + "/discapacidades",
            request,
            PacienteDiscapacidadResponse.class
        );
        return PacienteDiscapacidad.desdePacienteDiscapacidadResponse(response);
    }

    /**
     * Actualiza el nivel de progresion de una discapacidad asignada.
     */
    public PacienteDiscapacidad actualizarNivel(String dniPac, String codDis, Integer idNivel) {
        PacienteDiscapacidadResponse response = api.put(
            "/api/pacientes/" + dniPac + "/discapacidades/" + codDis + "/nivel?idNivel=" + idNivel,
            null,
            PacienteDiscapacidadResponse.class
        );
        return PacienteDiscapacidad.desdePacienteDiscapacidadResponse(response);
    }

    // ==================== TRATAMIENTOS ====================

    /**
     * Lista los tratamientos asignados a un paciente con su flag de visibilidad.
     */
    public List<PacienteTratamiento> listarTratamientos(String dniPac) {
        List<PacienteTratamientoResponse> respuestas = api.get(
            "/api/pacientes/" + dniPac + "/tratamientos",
            new TypeReference<List<PacienteTratamientoResponse>>() {}
        );
        return respuestas.stream()
            .map(PacienteTratamiento::desdePacienteTratamientoResponse)
            .toList();
    }

    /**
     * Alterna la visibilidad de un tratamiento asignado a un paciente.
     */
    public PacienteTratamiento toggleVisibilidad(String dniPac, String codTrat) {
        PacienteTratamientoResponse response = api.put(
            "/api/pacientes/" + dniPac + "/tratamientos/" + codTrat + "/visibilidad",
            null,
            PacienteTratamientoResponse.class
        );
        return PacienteTratamiento.desdePacienteTratamientoResponse(response);
    }
}
