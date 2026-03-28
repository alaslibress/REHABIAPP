package com.rehabiapp.api.application.dto;

import java.util.List;

/**
 * DTO de respuesta con los datos de un sanitario.
 *
 * <p>NUNCA incluye la contraseña (ni en hash). Solo se devuelven
 * los datos necesarios para la presentación en el cliente.</p>
 *
 * <p>La contraseña hasheada se mantiene exclusivamente en la capa
 * de infraestructura y nunca cruza la frontera hacia la presentación.</p>
 */
public record SanitarioResponse(
        String dniSan,
        String nombreSan,
        String apellido1San,
        String apellido2San,
        String emailSan,
        Integer numDePacientes,
        Boolean activo,
        String cargo,
        List<String> telefonos
) {}
