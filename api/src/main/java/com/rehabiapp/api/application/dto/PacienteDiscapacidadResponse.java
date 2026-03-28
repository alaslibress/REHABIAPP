package com.rehabiapp.api.application.dto;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con los datos de la asignación de una discapacidad a un paciente.
 *
 * <p>Incluye información desnormalizada del nombre de la discapacidad
 * y del nivel de progresión para evitar consultas adicionales en el cliente.</p>
 */
public record PacienteDiscapacidadResponse(
        String dniPac,
        String codDis,
        String nombreDis,
        Integer idNivel,
        String nombreNivel,
        LocalDateTime fechaAsignacion,
        String notas
) {}
