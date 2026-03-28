package com.rehabiapp.api.domain.exception;

/**
 * Excepción de dominio lanzada cuando no se encuentra el recurso solicitado.
 *
 * <p>Mapeada a HTTP 404 Not Found en el manejador global de excepciones
 * de la capa de presentación.</p>
 *
 * <p>No pertenece a ningún framework — la capa de dominio permanece
 * libre de dependencias externas (principio Clean Architecture).</p>
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
