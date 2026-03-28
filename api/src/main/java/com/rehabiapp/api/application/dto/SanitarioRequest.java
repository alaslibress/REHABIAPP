package com.rehabiapp.api.application.dto;

import com.rehabiapp.api.domain.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de solicitud de creación o actualización de un sanitario.
 *
 * <p>La contraseña se recibe en texto plano y se hashea con BCrypt
 * (factor 12) en la capa de servicio antes de persistir.</p>
 *
 * <p>El cargo determina el rol RBAC del sanitario en el sistema.</p>
 */
public record SanitarioRequest(
        @NotBlank @Size(max = 20) String dniSan,
        @NotBlank @Size(max = 100) String nombreSan,
        @NotBlank @Size(max = 100) String apellido1San,
        @Size(max = 100) String apellido2San,
        @Email @NotBlank String emailSan,
        @NotBlank String contrasena,
        @NotNull Rol cargo,
        List<String> telefonos
) {}
