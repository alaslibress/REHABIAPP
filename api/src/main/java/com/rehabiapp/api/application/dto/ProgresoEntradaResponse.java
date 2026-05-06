package com.rehabiapp.api.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Punto en la serie temporal de progreso de un paciente para una metrica concreta.
 */
@Schema(description = "Entrada (punto) en la serie temporal de progreso")
public record ProgresoEntradaResponse(
        Instant fecha,
        Double valor
) {}
