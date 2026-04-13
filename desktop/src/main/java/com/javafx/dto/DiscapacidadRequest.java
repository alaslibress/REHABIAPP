package com.javafx.dto;

/**
 * DTO de peticion para crear/actualizar discapacidades via API.
 */
public record DiscapacidadRequest(
    String codDis,
    String nombreDis,
    String descripcionDis,
    Boolean necesitaProtesis
) {}
