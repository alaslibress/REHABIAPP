package com.rehabiapp.api.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de solicitud de autenticación — login del sanitario.
 *
 * <p>Contiene el DNI y la contraseña en texto plano que se verificará
 * contra el hash BCrypt almacenado en la base de datos.</p>
 *
 * <p>Record inmutable con validación Jakarta Bean Validation integrada.</p>
 */
public record LoginRequest(
        @NotBlank String dni,
        @NotBlank String contrasena
) {}
