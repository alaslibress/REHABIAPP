package com.javafx.dto;

public record TratamientoResponse(
    String codTrat,
    String nombreTrat,
    String definicionTrat,
    Integer idNivel,
    String nombreNivel
) {}
