package com.rehabiapp.api.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de solicitud de creación o actualización de un paciente.
 *
 * <p>Los campos clínicos sensibles (alergias, antecedentes, medicacionActual)
 * se cifran automáticamente con AES-256-GCM en la capa de entidad
 * mediante CampoClinicoConverter antes de persistir (RGPD Art. 9).</p>
 *
 * <p>El dniSan vincula el paciente con su sanitario responsable.</p>
 */
public record PacienteRequest(
        @NotBlank @Size(max = 20) String dniPac,
        @NotBlank String dniSan,
        @NotBlank @Size(max = 100) String nombrePac,
        @NotBlank @Size(max = 100) String apellido1Pac,
        @Size(max = 100) String apellido2Pac,
        Integer edadPac,
        @Email String emailPac,
        @Size(max = 20) String numSs,
        String sexo,
        LocalDate fechaNacimiento,
        Boolean protesis,
        // Campos clínicos cifrados antes de persistir
        String alergias,
        String antecedentes,
        String medicacionActual,
        Boolean consentimientoRgpd,
        List<String> telefonos
) {}
