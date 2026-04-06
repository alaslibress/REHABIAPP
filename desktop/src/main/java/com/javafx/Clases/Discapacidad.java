package com.javafx.Clases;

import com.javafx.dto.DiscapacidadResponse;
import javafx.beans.property.*;

/**
 * Modelo JavaFX para el catalogo de discapacidades.
 */
public class Discapacidad {

    private final StringProperty codDis;
    private final StringProperty nombreDis;
    private final StringProperty descripcionDis;
    private final BooleanProperty necesitaProtesis;

    public Discapacidad(String codDis, String nombreDis, String descripcionDis, boolean necesitaProtesis) {
        this.codDis = new SimpleStringProperty(codDis);
        this.nombreDis = new SimpleStringProperty(nombreDis);
        this.descripcionDis = new SimpleStringProperty(descripcionDis);
        this.necesitaProtesis = new SimpleBooleanProperty(necesitaProtesis);
    }

    /**
     * Crea una Discapacidad a partir de la respuesta de la API.
     */
    public static Discapacidad desdeDiscapacidadResponse(DiscapacidadResponse response) {
        return new Discapacidad(
            response.codDis(),
            response.nombreDis(),
            response.descripcionDis() != null ? response.descripcionDis() : "",
            Boolean.TRUE.equals(response.necesitaProtesis())
        );
    }

    public String getCodDis() { return codDis.get(); }
    public void setCodDis(String codDis) { this.codDis.set(codDis); }
    public StringProperty codDisProperty() { return codDis; }

    public String getNombreDis() { return nombreDis.get(); }
    public void setNombreDis(String nombreDis) { this.nombreDis.set(nombreDis); }
    public StringProperty nombreDisProperty() { return nombreDis; }

    public String getDescripcionDis() { return descripcionDis.get(); }
    public void setDescripcionDis(String descripcionDis) { this.descripcionDis.set(descripcionDis); }
    public StringProperty descripcionDisProperty() { return descripcionDis; }

    public boolean isNecesitaProtesis() { return necesitaProtesis.get(); }
    public void setNecesitaProtesis(boolean necesitaProtesis) { this.necesitaProtesis.set(necesitaProtesis); }
    public BooleanProperty necesitaProtesisProperty() { return necesitaProtesis; }

    @Override
    public String toString() {
        return getNombreDis();
    }
}
