package com.rehabiapp.api.application.dto;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con los datos de la visibilidad de un tratamiento para un paciente.
 *
 * <p>El campo visible controla si el tratamiento aparece en la app móvil del paciente.
 * El especialista puede ocultarlo temporalmente sin eliminar la asignación clínica.</p>
 */
public record PacienteTratamientoResponse(
        String dniPac,
        String codTrat,
        String nombreTrat,
        Boolean visible,
        LocalDateTime fechaAsignacion
) {}
