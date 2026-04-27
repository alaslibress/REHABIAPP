package com.rehabiapp.api.application.dto;

/**
 * DTO de respuesta con los datos de una discapacidad del catalogo clinico.
 *
 * <p>idArticulacion es nullable — discapacidades existentes sin articulacion asignada
 * devuelven null en este campo (compatibilidad hacia atras).
 * Anadido en V13__juego_articulacion.sql.</p>
 */
public record DiscapacidadResponse(
        String codDis,
        String nombreDis,
        String descripcionDis,
        Boolean necesitaProtesis,
        Integer idArticulacion,
        String nombreArticulacion
) {}
