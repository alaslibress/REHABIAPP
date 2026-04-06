package com.javafx.Clases;

import com.javafx.dto.PacienteDiscapacidadResponse;
import javafx.beans.property.*;

import java.time.LocalDateTime;

/**
 * Modelo JavaFX para la asignacion paciente-discapacidad con su nivel de progresion.
 */
public class PacienteDiscapacidad {

    private final StringProperty dniPac;
    private final StringProperty codDis;
    private final StringProperty nombreDis;
    private final IntegerProperty idNivel;
    private final StringProperty nombreNivel;
    private final ObjectProperty<LocalDateTime> fechaAsignacion;
    private final StringProperty notas;

    public PacienteDiscapacidad(String dniPac, String codDis, String nombreDis,
                                Integer idNivel, String nombreNivel,
                                LocalDateTime fechaAsignacion, String notas) {
        this.dniPac = new SimpleStringProperty(dniPac);
        this.codDis = new SimpleStringProperty(codDis);
        this.nombreDis = new SimpleStringProperty(nombreDis);
        this.idNivel = new SimpleIntegerProperty(idNivel != null ? idNivel : 0);
        this.nombreNivel = new SimpleStringProperty(nombreNivel != null ? nombreNivel : "");
        this.fechaAsignacion = new SimpleObjectProperty<>(fechaAsignacion);
        this.notas = new SimpleStringProperty(notas != null ? notas : "");
    }

    /**
     * Crea un PacienteDiscapacidad a partir de la respuesta de la API.
     */
    public static PacienteDiscapacidad desdePacienteDiscapacidadResponse(PacienteDiscapacidadResponse response) {
        return new PacienteDiscapacidad(
            response.dniPac(),
            response.codDis(),
            response.nombreDis(),
            response.idNivel(),
            response.nombreNivel(),
            response.fechaAsignacion(),
            response.notas()
        );
    }

    public String getDniPac() { return dniPac.get(); }
    public StringProperty dniPacProperty() { return dniPac; }

    public String getCodDis() { return codDis.get(); }
    public StringProperty codDisProperty() { return codDis; }

    public String getNombreDis() { return nombreDis.get(); }
    public StringProperty nombreDisProperty() { return nombreDis; }

    /**
     * Alias para compatibilidad con el controlador de ficha de paciente.
     */
    public String getNombreDiscapacidad() { return nombreDis.get(); }

    public int getIdNivel() { return idNivel.get(); }
    public void setIdNivel(int idNivel) { this.idNivel.set(idNivel); }
    public IntegerProperty idNivelProperty() { return idNivel; }

    /**
     * Alias para compatibilidad con el controlador de ficha de paciente.
     */
    public int getIdNivelActual() { return idNivel.get(); }

    /**
     * Indica si la discapacidad requiere protesis.
     * Por limitacion de la API actual, este campo no viene en PacienteDiscapacidadResponse.
     * Se devuelve false por defecto hasta que la API lo incluya.
     */
    public boolean isNecesitaProtesis() { return false; }

    public String getNombreNivel() { return nombreNivel.get(); }
    public void setNombreNivel(String nombreNivel) { this.nombreNivel.set(nombreNivel); }
    public StringProperty nombreNivelProperty() { return nombreNivel; }

    public LocalDateTime getFechaAsignacion() { return fechaAsignacion.get(); }
    public ObjectProperty<LocalDateTime> fechaAsignacionProperty() { return fechaAsignacion; }

    public String getNotas() { return notas.get(); }
    public void setNotas(String notas) { this.notas.set(notas); }
    public StringProperty notasProperty() { return notas; }
}
