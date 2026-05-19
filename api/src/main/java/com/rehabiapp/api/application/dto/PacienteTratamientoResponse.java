package com.rehabiapp.api.application.dto;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con los datos de la visibilidad de un tratamiento para un paciente.
 *
 * <p>El campo visible controla si el tratamiento aparece en la app movil del paciente.
 * El especialista puede ocultarlo temporalmente sin eliminar la asignacion clinica.</p>
 *
 * <p>codDis: codigo de la discapacidad principal del tratamiento para este paciente
 * (resuelto via discapacidad_tratamiento filtrado por paciente_discapacidad del paciente).
 * Null si el tratamiento no esta vinculado a ninguna discapacidad del paciente.</p>
 *
 * <p>idNivel: id del nivel de progresion clinica del tratamiento. Null si no tiene nivel.</p>
 *
 * <p>tienePdf: true si el tratamiento tiene un PDF de protocolo subido.</p>
 */
public record PacienteTratamientoResponse(
        String dniPac,
        String codTrat,
        String nombreTrat,
        Boolean visible,
        LocalDateTime fechaAsignacion,
        String codDis,
        Integer idNivel,
        Boolean tienePdf
) {}
