package com.rehabiapp.api.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear/actualizar un tratamiento del catalogo.
 *
 * <p>idNivel es opcional: si es null el tratamiento queda sin nivel de progresion asignado.
 * codTrat actua como clave primaria y no puede modificarse tras la creacion.</p>
 */
public record TratamientoRequest(
    @NotBlank(message = "El codigo es obligatorio")
    @Size(max = 20, message = "El codigo no puede superar 20 caracteres")
    String codTrat,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    String nombreTrat,

    String definicionTrat,

    Integer idNivel
) {}
