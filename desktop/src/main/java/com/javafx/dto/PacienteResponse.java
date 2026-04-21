package com.javafx.dto;

import java.time.LocalDate;
import java.util.List;

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
    String alergias,
    String antecedentes,
    String medicacionActual,
    Boolean consentimientoRgpd,
    List<String> telefonos,
    DireccionDto direccion
) {}
