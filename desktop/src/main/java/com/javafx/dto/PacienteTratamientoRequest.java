package com.javafx.dto;

/**
 * DTO de entrada para asignar un tratamiento a un paciente.
 * Serializado como JSON en POST /api/pacientes/{dniPac}/tratamientos.
 */
public record PacienteTratamientoRequest(
        String codTrat
) {}
