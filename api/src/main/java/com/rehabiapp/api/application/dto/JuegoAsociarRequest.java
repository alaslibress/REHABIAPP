package com.rehabiapp.api.application.dto;

/**
 * DTO de entrada para el endpoint PUT /api/catalogo/tratamientos/{cod}/juego.
 *
 * <p>codJuego nullable: si es null se desasocia el juego del tratamiento.</p>
 */
public record JuegoAsociarRequest(String codJuego) {}
