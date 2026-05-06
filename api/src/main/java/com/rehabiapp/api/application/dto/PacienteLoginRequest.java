package com.rehabiapp.api.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de peticion para autenticar un paciente desde la app movil.
 *
 * <p>El identificador puede ser el DNI o el email del paciente.
 * La contrasena se envía en texto plano sobre HTTPS; el backend verifica contra el hash BCrypt.</p>
 */
@Schema(description = "Credenciales de acceso del paciente para la app movil")
public record PacienteLoginRequest(

        @NotBlank(message = "El identificador (DNI o email) es obligatorio")
        @Schema(description = "DNI o email del paciente", example = "12345678Z")
        String identifier,

        @NotBlank(message = "La contrasena es obligatoria")
        @Schema(description = "Contrasena del paciente en texto plano (HTTPS)")
        String contrasena

) {}
