package com.rehabiapp.api.application.dto;

/**
 * DTO de respuesta con los datos de un tratamiento del catálogo clínico.
 *
 * <p>Catálogo de solo lectura desde la API. Se puede filtrar por discapacidad
 * para mostrar solo los tratamientos aplicables al perfil del paciente.</p>
 *
 * <p>idNivel y nombreNivel son nullable — tratamientos existentes sin nivel
 * asignado devuelven null en estos campos (compatibilidad hacia atrás).</p>
 */
public record TratamientoResponse(
        String codTrat,
        String nombreTrat,
        String definicionTrat,
        Integer idNivel,
        String nombreNivel
) {}
