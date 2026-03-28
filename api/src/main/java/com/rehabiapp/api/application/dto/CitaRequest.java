package com.rehabiapp.api.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de solicitud de creación de una cita médica.
 *
 * <p>La cita se identifica unívocamente por (dniPac, dniSan, fechaCita, horaCita).
 * El sistema no permite duplicar una cita con la misma combinación de claves.</p>
 */
public record CitaRequest(
        @NotBlank String dniPac,
        @NotBlank String dniSan,
        @NotNull LocalDate fechaCita,
        @NotNull LocalTime horaCita
) {}
