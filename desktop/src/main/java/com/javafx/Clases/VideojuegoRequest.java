package com.javafx.Clases;

/**
 * DTO de entrada para alta o actualizacion de un videojuego terapeutico.
 * Espejo de com.rehabiapp.api.application.dto.VideojuegoRequest del API.
 */
public record VideojuegoRequest(
    String codigo,
    String nombre,
    String descripcion,
    String codDis,
    String parteCuerpo,
    String urlUnity
) {}
