package com.rehabiapp.api.application.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de respuesta con los datos de un paciente.
 *
 * <p>Los campos clínicos (alergias, antecedentes, medicacionActual) se
 * devuelven descifrados — el converter los descifra automáticamente
 * al leer de la BD. Solo accesibles para rol SPECIALIST.</p>
 *
 * <p>El paciente NUNCA se elimina físicamente. El campo activo indica
 * si está dado de alta o de baja lógica (RGPD Art. 17 + Ley 41/2002).</p>
 */
public record PacienteResponse(
        String dniPac,
        String dniSan,
        String nombrePac,
        String apellido1Pac,
        String apellido2Pac,
        Integer edadPac,
        String emailPac,
        String numSs,
        String sexo,
        LocalDate fechaNacimiento,
        Boolean protesis,
        Boolean activo,
        // Campos clínicos — solo para SPECIALIST (control en la capa de presentación)
        String alergias,
        String antecedentes,
        String medicacionActual,
        Boolean consentimientoRgpd,
        List<String> telefonos
) {}
