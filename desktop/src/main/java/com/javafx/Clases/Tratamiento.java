package com.javafx.Clases;

import com.javafx.dto.TratamientoResponse;
import javafx.beans.property.*;

/**
 * Modelo JavaFX para el catalogo de tratamientos.
 */
public class Tratamiento {

    private final StringProperty codTrat;
    private final StringProperty nombreTrat;
    private final StringProperty definicionTrat;

    public Tratamiento(String codTrat, String nombreTrat, String definicionTrat) {
        this.codTrat = new SimpleStringProperty(codTrat);
        this.nombreTrat = new SimpleStringProperty(nombreTrat);
        this.definicionTrat = new SimpleStringProperty(definicionTrat);
    }

    /**
     * Crea un Tratamiento a partir de la respuesta de la API.
     */
    public static Tratamiento desdeTratamientoResponse(TratamientoResponse response) {
        return new Tratamiento(
            response.codTrat(),
            response.nombreTrat(),
            response.definicionTrat() != null ? response.definicionTrat() : ""
        );
    }

    public String getCodTrat() { return codTrat.get(); }
    public void setCodTrat(String codTrat) { this.codTrat.set(codTrat); }
    public StringProperty codTratProperty() { return codTrat; }

    public String getNombreTrat() { return nombreTrat.get(); }
    public void setNombreTrat(String nombreTrat) { this.nombreTrat.set(nombreTrat); }
    public StringProperty nombreTratProperty() { return nombreTrat; }

    public String getDefinicionTrat() { return definicionTrat.get(); }
    public void setDefinicionTrat(String definicionTrat) { this.definicionTrat.set(definicionTrat); }
    public StringProperty definicionTratProperty() { return definicionTrat; }

    @Override
    public String toString() {
        return getNombreTrat();
    }
}
