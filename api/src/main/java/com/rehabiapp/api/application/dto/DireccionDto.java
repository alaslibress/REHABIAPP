package com.rehabiapp.api.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de direccion postal usado tanto en request como en response del paciente.
 * Todos los campos son opcionales excepto calle, cp y nombreLocalidad.
 * El cp se valida con regex de 5 digitos.
 */
public record DireccionDto(
        @NotBlank @Size(max = 200) String calle,
        @Size(max = 20) String numero,
        @Size(max = 20) String piso,
        @NotBlank @Pattern(regexp = "^[0-9]{5}$") String cp,
        @NotBlank @Size(max = 100) String nombreLocalidad,
        @Size(max = 100) String provincia
) {}
