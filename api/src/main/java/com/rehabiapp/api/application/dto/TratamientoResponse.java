package com.rehabiapp.api.application.dto;

/**
 * DTO de respuesta con los datos de un tratamiento del catalogo clinico.
 *
 * <p>idNivel y nombreNivel son nullable — tratamientos existentes sin nivel
 * asignado devuelven null en estos campos (compatibilidad hacia atras).</p>
 *
 * <p>codJuego es nullable — tratamientos sin juego terapeutico asociado
 * devuelven null. Anadido en V13__juego_articulacion.sql.</p>
 */
public record TratamientoResponse(
        String codTrat,
        String nombreTrat,
        String definicionTrat,
        Integer idNivel,
        String nombreNivel,
        String codJuego,
        String nombreJuego
) {}
