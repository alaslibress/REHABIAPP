package com.javafx.dto;

import java.util.List;

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
