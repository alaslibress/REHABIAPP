package com.rehabiapp.api.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Resumen de la ultima sesion de juego registrada para un paciente.
 *
 * <p>Devuelto por `/data` y reutilizado en el dashboard.</p>
 */
@Schema(description = "Resumen de la ultima sesion de juego de un paciente")
public record UltimaSesionDto(
        String videojuegoCodigo,
        String videojuegoNombre,
        Instant fecha,
        Integer puntuacion,
        Long duracionMs
) {}
