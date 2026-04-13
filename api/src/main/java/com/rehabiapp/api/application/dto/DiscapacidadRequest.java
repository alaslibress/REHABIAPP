package com.rehabiapp.api.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear/actualizar una discapacidad del catalogo.
 *
 * <p>El campo codDis actua como clave primaria y no puede modificarse tras la creacion.
 * necesitaProtesis es opcional: null se interpreta como false.</p>
 */
public record DiscapacidadRequest(
    @NotBlank(message = "El codigo es obligatorio")
    @Size(max = 20, message = "El codigo no puede superar 20 caracteres")
    String codDis,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    String nombreDis,

    String descripcionDis,

    Boolean necesitaProtesis
) {}
