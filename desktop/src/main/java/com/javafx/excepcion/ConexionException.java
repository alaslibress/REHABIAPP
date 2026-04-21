package com.javafx.excepcion;

/**
 * Error de conexion con la base de datos o la API REST.
 */
public class ConexionException extends RehabiAppException {

    public ConexionException(String mensaje) {
        super(mensaje);
    }

    public ConexionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
