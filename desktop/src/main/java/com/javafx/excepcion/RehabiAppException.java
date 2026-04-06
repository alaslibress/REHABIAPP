package com.javafx.excepcion;

/**
 * Excepcion base de la aplicacion RehabiAPP.
 * Todas las excepciones del dominio heredan de esta.
 */
public class RehabiAppException extends RuntimeException {

    public RehabiAppException(String mensaje) {
        super(mensaje);
    }

    public RehabiAppException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
