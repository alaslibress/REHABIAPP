package com.javafx.excepcion;

/**
 * Error cuando el usuario no tiene permisos para la operacion (RBAC).
 */
public class PermisoException extends RehabiAppException {

    public PermisoException(String mensaje) {
        super(mensaje);
    }

    public PermisoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
