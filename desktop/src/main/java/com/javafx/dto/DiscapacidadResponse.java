package com.javafx.dto;

public record DiscapacidadResponse(
    String codDis,
    String nombreDis,
    String descripcionDis,
    Boolean necesitaProtesis
) {}
