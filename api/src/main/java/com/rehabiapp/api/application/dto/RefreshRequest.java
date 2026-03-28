package com.rehabiapp.api.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de solicitud de renovación de token JWT.
 *
 * <p>El cliente envía el refreshToken obtenido en el login para
 * obtener un nuevo par de tokens sin volver a introducir credenciales.</p>
 */
public record RefreshRequest(
        @NotBlank String refreshToken
) {}
