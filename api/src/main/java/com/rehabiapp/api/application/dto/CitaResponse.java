package com.rehabiapp.api.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de respuesta con los datos de una cita médica.
 *
 * <p>Expone los campos de la clave primaria compuesta más los nombres
 * completos del paciente y del sanitario para mostrarlos en la agenda
 * sin requerir una segunda consulta al cliente (desktop o mobile).</p>
 */
public record CitaResponse(
        String dniPac,
        String dniSan,
        LocalDate fechaCita,
        LocalTime horaCita,
        String nombrePaciente,
        String nombreSanitario
) {}
