package com.javafx.excepcion;

/**
 * Error de autenticacion (credenciales invalidas, token expirado).
 */
public class AutenticacionException extends RehabiAppException {

    public AutenticacionException(String mensaje) {
        super(mensaje);
    }

    public AutenticacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
