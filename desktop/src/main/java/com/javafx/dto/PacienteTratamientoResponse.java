package com.javafx.dto;

import java.time.LocalDateTime;

public record PacienteTratamientoResponse(
    String dniPac,
    String codTrat,
    String nombreTrat,
    Boolean visible,
    LocalDateTime fechaAsignacion
) {}
