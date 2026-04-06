package com.javafx.Clases;

import com.javafx.dto.PacienteTratamientoResponse;
import javafx.beans.property.*;

import java.time.LocalDateTime;

/**
 * Modelo JavaFX para la visibilidad de tratamientos asignados a un paciente.
 */
public class PacienteTratamiento {

    private final StringProperty dniPac;
    private final StringProperty codTrat;
    private final StringProperty nombreTrat;
    private final BooleanProperty visible;
    private final ObjectProperty<LocalDateTime> fechaAsignacion;

    public PacienteTratamiento(String dniPac, String codTrat, String nombreTrat,
                               boolean visible, LocalDateTime fechaAsignacion) {
        this.dniPac = new SimpleStringProperty(dniPac);
        this.codTrat = new SimpleStringProperty(codTrat);
        this.nombreTrat = new SimpleStringProperty(nombreTrat);
        this.visible = new SimpleBooleanProperty(visible);
        this.fechaAsignacion = new SimpleObjectProperty<>(fechaAsignacion);
    }

    /**
     * Crea un PacienteTratamiento a partir de la respuesta de la API.
     */
    public static PacienteTratamiento desdePacienteTratamientoResponse(PacienteTratamientoResponse response) {
        return new PacienteTratamiento(
            response.dniPac(),
            response.codTrat(),
            response.nombreTrat(),
            Boolean.TRUE.equals(response.visible()),
            response.fechaAsignacion()
        );
    }

    public String getDniPac() { return dniPac.get(); }
    public StringProperty dniPacProperty() { return dniPac; }

    public String getCodTrat() { return codTrat.get(); }
    public StringProperty codTratProperty() { return codTrat; }

    public String getNombreTrat() { return nombreTrat.get(); }
    public StringProperty nombreTratProperty() { return nombreTrat; }

    /**
     * Alias para compatibilidad con el controlador de ficha de paciente.
     */
    public String getNombreTratamiento() { return nombreTrat.get(); }

    /**
     * Definicion del tratamiento.
     * Por limitacion de la API actual, PacienteTratamientoResponse no incluye la definicion.
     * Se devuelve string vacio hasta que la API lo incluya.
     */
    public String getDefinicionTratamiento() { return ""; }

    public boolean isVisible() { return visible.get(); }
    public void setVisible(boolean visible) { this.visible.set(visible); }
    public BooleanProperty visibleProperty() { return visible; }

    public LocalDateTime getFechaAsignacion() { return fechaAsignacion.get(); }
    public ObjectProperty<LocalDateTime> fechaAsignacionProperty() { return fechaAsignacion; }
}
