package com.javafx.Clases;

/** Videojuego terapeutico (espejo de VideojuegoResponse del API). */
public record Videojuego(
    Long idVideojuego,
    String codigo,
    String nombre,
    String descripcion,
    String codDis,
    String discapacidadNombre,
    String parteCuerpo,
    String urlUnity,
    boolean activo
) {}
