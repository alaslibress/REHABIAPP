package com.rehabiapp.api.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear o actualizar un videojuego terapeutico.
 *
 * <p>codigo identifica de forma humana al videojuego (clave unica).
 * codDis enlaza con la discapacidad terapeutica del catalogo.</p>
 */
@Schema(description = "Datos de entrada para alta o actualizacion de un videojuego terapeutico")
public record VideojuegoRequest(
        @Schema(description = "Codigo unico legible del videojuego", example = "VJ-MANO-001")
        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 50, message = "El codigo no puede superar 50 caracteres")
        String codigo,

        @Schema(description = "Nombre comercial del videojuego", example = "Atrapa la pelota")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
        String nombre,

        @Schema(description = "Descripcion clinica del videojuego")
        String descripcion,

        @Schema(description = "Codigo de la discapacidad asociada", example = "DIS-001")
        @NotBlank(message = "El codigo de discapacidad es obligatorio")
        @Size(max = 20, message = "El codigo de discapacidad no puede superar 20 caracteres")
        String codDis,

        @Schema(description = "Parte del cuerpo que ejercita el videojuego", example = "Mano derecha")
        @NotBlank(message = "La parte del cuerpo es obligatoria")
        @Size(max = 100, message = "La parte del cuerpo no puede superar 100 caracteres")
        String parteCuerpo,

        @Schema(description = "URL externa al build Unity WebGL del videojuego")
        @Size(max = 500, message = "La URL no puede superar 500 caracteres")
        String urlUnity
) {}
