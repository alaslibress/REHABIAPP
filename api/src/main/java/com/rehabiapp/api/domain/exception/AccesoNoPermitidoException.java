package com.rehabiapp.api.domain.exception;

/**
 * Excepción de dominio lanzada cuando el usuario autenticado no tiene permisos
 * para realizar la operación solicitada.
 *
 * <p>Mapeada a HTTP 403 Forbidden en el manejador global de excepciones
 * de la capa de presentación.</p>
 *
 * <p>No pertenece a ningún framework — la capa de dominio permanece
 * libre de dependencias externas (principio Clean Architecture).</p>
 */
public class AccesoNoPermitidoException extends RuntimeException {

    public AccesoNoPermitidoException(String mensaje) {
        super(mensaje);
    }
}
