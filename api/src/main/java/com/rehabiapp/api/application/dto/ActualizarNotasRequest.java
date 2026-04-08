package com.rehabiapp.api.application.dto;

/**
 * DTO de entrada para actualizar las notas clinicas de una asignacion paciente-discapacidad.
 *
 * <p>PATCH /api/pacientes/{dniPac}/discapacidades/{codDis}/notas</p>
 */
public record ActualizarNotasRequest(
        String notas
) {}
