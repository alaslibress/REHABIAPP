package com.rehabiapp.api.application.dto;

/**
 * DTO de respuesta con los datos de una discapacidad del catálogo clínico.
 *
 * <p>Catálogo de solo lectura. Las operaciones de escritura sobre el catálogo
 * se realizan actualmente desde el desktop ERP con acceso JDBC directo.</p>
 */
public record DiscapacidadResponse(
        String codDis,
        String nombreDis,
        String descripcionDis,
        Boolean necesitaProtesis
) {}
