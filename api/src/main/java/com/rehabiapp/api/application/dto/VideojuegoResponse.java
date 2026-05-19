package com.rehabiapp.api.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta con los datos del videojuego enriquecido con la discapacidad.
 */
@Schema(description = "Datos publicos de un videojuego terapeutico")
public record VideojuegoResponse(
        Long idVideojuego,
        String codigo,
        String nombre,
        String descripcion,
        String codDis,
        String discapacidadNombre,
        String parteCuerpo,
        String urlUnity,
        boolean activo
) {}
