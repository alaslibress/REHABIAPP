package com.rehabiapp.api.application.dto;

/**
 * DTO de respuesta con los datos de un juego terapeutico del catalogo.
 *
 * <p>idArticulacion y nombreArticulacion permiten al cliente desktop/movil
 * mostrar el filtro de articulacion sin una segunda llamada.</p>
 */
public record JuegoResponse(
        String codJuego,
        String nombre,
        String descripcion,
        String urlJuego,
        Integer idArticulacion,
        String nombreArticulacion,
        boolean activo
) {}
