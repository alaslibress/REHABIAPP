package com.rehabiapp.api.application.dto;

/**
 * DTO de respuesta con los datos de un nivel de progresión clínica.
 *
 * <p>Los niveles se devuelven ordenados por el campo "orden" para garantizar
 * la presentación correcta de la secuencia terapéutica al cliente.</p>
 */
public record NivelProgresionResponse(
        Integer idNivel,
        String nombre,
        Integer orden,
        String descripcion
) {}
