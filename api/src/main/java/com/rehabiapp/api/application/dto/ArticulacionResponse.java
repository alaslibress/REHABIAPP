package com.rehabiapp.api.application.dto;

/**
 * DTO de respuesta con los datos de una articulacion del catalogo clinico.
 *
 * <p>codigo esta alineado con BodyPartId del frontend movil (ej. LEFT_HAND).</p>
 */
public record ArticulacionResponse(
        Integer idArticulacion,
        String codigo,
        String nombre
) {}
