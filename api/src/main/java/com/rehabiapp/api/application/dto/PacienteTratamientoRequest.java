package com.rehabiapp.api.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para asignar un tratamiento a un paciente.
 *
 * <p>POST /api/pacientes/{dniPac}/tratamientos</p>
 */
public record PacienteTratamientoRequest(
        @NotBlank String codTrat
) {}
