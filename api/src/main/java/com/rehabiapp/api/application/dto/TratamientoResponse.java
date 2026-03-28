package com.rehabiapp.api.application.dto;

/**
 * DTO de respuesta con los datos de un tratamiento del catálogo clínico.
 *
 * <p>Catálogo de solo lectura desde la API. Se puede filtrar por discapacidad
 * para mostrar solo los tratamientos aplicables al perfil del paciente.</p>
 */
public record TratamientoResponse(
        String codTrat,
        String nombreTrat,
        String definicionTrat
) {}
