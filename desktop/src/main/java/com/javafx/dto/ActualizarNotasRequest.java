package com.javafx.dto;

/**
 * DTO de entrada para actualizar las notas clinicas de una asignacion paciente-discapacidad.
 * Serializado como JSON en PATCH /api/pacientes/{dniPac}/discapacidades/{codDis}/notas.
 */
public record ActualizarNotasRequest(
        String notas
) {}
