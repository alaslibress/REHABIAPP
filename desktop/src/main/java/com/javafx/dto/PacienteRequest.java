package com.javafx.dto;

import java.time.LocalDate;
import java.util.List;

public record PacienteRequest(
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
    String alergias,
    String antecedentes,
    String medicacionActual,
    Boolean consentimientoRgpd,
    DireccionDto direccion,
    List<String> telefonos
) {}
