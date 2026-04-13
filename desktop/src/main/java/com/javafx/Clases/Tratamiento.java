package com.javafx.Clases;

import com.javafx.dto.TratamientoResponse;
import javafx.beans.property.*;

/**
 * Modelo JavaFX para el catalogo de tratamientos.
 * Incluye el nivel de progresion clinica al que pertenece el tratamiento.
 */
public class Tratamiento {

    private final StringProperty codTrat;
    private final StringProperty nombreTrat;
    private final StringProperty definicionTrat;
    private final IntegerProperty idNivel;
    private final StringProperty nombreNivel;

    public Tratamiento(String codTrat, String nombreTrat, String definicionTrat,
                       Integer idNivel, String nombreNivel) {
        this.codTrat = new SimpleStringProperty(codTrat);
        this.nombreTrat = new SimpleStringProperty(nombreTrat);
        this.definicionTrat = new SimpleStringProperty(definicionTrat != null ? definicionTrat : "");
        this.idNivel = new SimpleIntegerProperty(idNivel != null ? idNivel : 0);
        this.nombreNivel = new SimpleStringProperty(nombreNivel != null ? nombreNivel : "");
    }

    /**
     * Crea un Tratamiento a partir de la respuesta de la API.
     */
    public static Tratamiento desdeTratamientoResponse(TratamientoResponse response) {
        return new Tratamiento(
            response.codTrat(),
            response.nombreTrat(),
            response.definicionTrat(),
            response.idNivel(),
            response.nombreNivel()
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

    public int getIdNivel() { return idNivel.get(); }
    public void setIdNivel(int idNivel) { this.idNivel.set(idNivel); }
    public IntegerProperty idNivelProperty() { return idNivel; }

    public String getNombreNivel() { return nombreNivel.get(); }
    public void setNombreNivel(String nombreNivel) { this.nombreNivel.set(nombreNivel); }
    public StringProperty nombreNivelProperty() { return nombreNivel; }

    @Override
    public String toString() {
        return getNombreTrat();
    }
}
