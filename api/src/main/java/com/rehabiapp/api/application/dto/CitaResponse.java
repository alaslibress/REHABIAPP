package com.rehabiapp.api.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de respuesta con los datos de una cita médica.
 *
 * <p>Expone los cuatro campos que forman la clave primaria compuesta
 * de la cita, suficientes para identificarla y mostrarla en la agenda.</p>
 */
public record CitaResponse(
        String dniPac,
        String dniSan,
        LocalDate fechaCita,
        LocalTime horaCita
) {}
