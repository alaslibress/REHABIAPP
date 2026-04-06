package com.javafx.dto;

import java.util.List;

public record SanitarioRequest(
    String dniSan,
    String nombreSan,
    String apellido1San,
    String apellido2San,
    String emailSan,
    String contrasena,
    String cargo,
    List<String> telefonos
) {}
