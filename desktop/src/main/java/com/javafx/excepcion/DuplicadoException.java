package com.javafx.excepcion;

/**
 * Error cuando se intenta crear un registro que ya existe (DNI, email, etc.).
 */
public class DuplicadoException extends RehabiAppException {

    private final String campoDuplicado;

    public DuplicadoException(String mensaje, String campoDuplicado) {
        super(mensaje);
        this.campoDuplicado = campoDuplicado;
    }

    public DuplicadoException(String mensaje, String campoDuplicado, Throwable causa) {
        super(mensaje, causa);
        this.campoDuplicado = campoDuplicado;
    }

    public String getCampoDuplicado() {
        return campoDuplicado;
    }

    /** Alias de getCampoDuplicado() para compatibilidad con controladores. */
    public String getCampo() {
        return campoDuplicado;
    }
}
