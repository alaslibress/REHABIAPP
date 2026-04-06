package com.javafx.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record CitaResponse(
    String dniPac,
    String dniSan,
    LocalDate fechaCita,
    LocalTime horaCita,
    String nombrePaciente,
    String nombreSanitario
) {}
