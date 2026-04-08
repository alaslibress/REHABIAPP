package com.javafx.dto;

/**
 * DTO de direccion postal usado en PacienteRequest/PacienteResponse.
 * Mismo shape que el record DireccionDto de la API — Jackson lo mapea por nombres de campo.
 */
public record DireccionDto(
        String calle,
        String numero,
        String piso,
        String cp,
        String nombreLocalidad,
        String provincia
) {}
