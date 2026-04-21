package com.javafx.excepcion;

/**
 * Error de validacion de datos de entrada.
 */
public class ValidacionException extends RehabiAppException {

    private final String campo;

    public ValidacionException(String mensaje, String campo) {
        super(mensaje);
        this.campo = campo;
    }

    public ValidacionException(String mensaje, String campo, Throwable causa) {
        super(mensaje, causa);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
