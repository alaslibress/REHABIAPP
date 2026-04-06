package com.javafx.dto;

public record NivelProgresionResponse(
    Integer idNivel,
    String nombre,
    Integer orden,
    String descripcion
) {}
