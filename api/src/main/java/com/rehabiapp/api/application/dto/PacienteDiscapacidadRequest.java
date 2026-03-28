package com.rehabiapp.api.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de solicitud de asignación de una discapacidad a un paciente.
 *
 * <p>El idNivel y las notas son opcionales en el momento de la asignación inicial.
 * Se pueden actualizar posteriormente con el endpoint de actualización de nivel.</p>
 */
public record PacienteDiscapacidadRequest(
        @NotBlank String codDis,
        Integer idNivel,
        String notas
) {}
