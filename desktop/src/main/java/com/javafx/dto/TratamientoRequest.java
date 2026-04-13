package com.javafx.dto;

/**
 * DTO de peticion para crear/actualizar tratamientos via API.
 */
public record TratamientoRequest(
    String codTrat,
    String nombreTrat,
    String definicionTrat,
    Integer idNivel
) {}
