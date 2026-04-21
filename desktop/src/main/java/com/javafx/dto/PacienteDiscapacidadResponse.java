package com.javafx.dto;

import java.time.LocalDateTime;

public record PacienteDiscapacidadResponse(
    String dniPac,
    String codDis,
    String nombreDis,
    Integer idNivel,
    String nombreNivel,
    LocalDateTime fechaAsignacion,
    String notas
) {}
