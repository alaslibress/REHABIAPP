package com.rehabiapp.api.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear/actualizar un juego terapeutico del catalogo.
 *
 * <p>urlJuego debe ser HTTPS — los juegos estan hospedados en AWS CloudFront.
 * idArticulacion es obligatorio: cada juego esta disenado para una articulacion concreta.</p>
 */
public record JuegoRequest(
        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 32, message = "El codigo no puede superar 32 caracteres")
        String codJuego,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        String descripcion,

        @NotBlank(message = "La URL del juego es obligatoria")
        @Size(max = 400, message = "La URL no puede superar 400 caracteres")
        @Pattern(regexp = "https://.*", message = "La URL del juego debe comenzar con https://")
        String urlJuego,

        @NotNull(message = "La articulacion es obligatoria")
        Integer idArticulacion,

        Boolean activo
) {}
