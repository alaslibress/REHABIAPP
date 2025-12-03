package com.javafx.Clases;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Clase modelo que representa una Cita medica en el sistema
 * Una cita es una relacion entre un Paciente y un Sanitario con fecha y hora
 */
public class Cita {

    //Propiedades de la cita
    private final StringProperty dniPaciente;
    private final StringProperty dniSanitario;
    private final ObjectProperty<LocalDate> fecha;
    private final ObjectProperty<LocalTime> hora;

    //Propiedades adicionales para mostrar en la tabla
    private final StringProperty nombrePaciente;
    private final StringProperty nombreSanitario;

    //Formateadores de fecha y hora
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Constructor completo
     */
    public Cita(String dniPaciente, String dniSanitario, LocalDate fecha, LocalTime hora) {
        this.dniPaciente = new SimpleStringProperty(dniPaciente);
        this.dniSanitario = new SimpleStringProperty(dniSanitario);
        this.fecha = new SimpleObjectProperty<>(fecha);
        this.hora = new SimpleObjectProperty<>(hora);
        this.nombrePaciente = new SimpleStringProperty("");
        this.nombreSanitario = new SimpleStringProperty("");
    }

    /**
     * Constructor con nombres para mostrar en tabla
     */
    public Cita(String dniPaciente, String dniSanitario, LocalDate fecha, LocalTime hora,
                String nombrePaciente, String nombreSanitario) {
        this.dniPaciente = new SimpleStringProperty(dniPaciente);
        this.dniSanitario = new SimpleStringProperty(dniSanitario);
        this.fecha = new SimpleObjectProperty<>(fecha);
        this.hora = new SimpleObjectProperty<>(hora);
        this.nombrePaciente = new SimpleStringProperty(nombrePaciente);
        this.nombreSanitario = new SimpleStringProperty(nombreSanitario);
    }

    // ==================== GETTERS Y SETTERS ====================

    //DNI Paciente
    public String getDniPaciente() {
        return dniPaciente.get();
    }

    public void setDniPaciente(String dniPaciente) {
        this.dniPaciente.set(dniPaciente);
    }

    public StringProperty dniPacienteProperty() {
        return dniPaciente;
    }

    //DNI Sanitario
    public String getDniSanitario() {
        return dniSanitario.get();
    }

    public void setDniSanitario(String dniSanitario) {
        this.dniSanitario.set(dniSanitario);
    }

    public StringProperty dniSanitarioProperty() {
        return dniSanitario;
    }

    //Fecha
    public LocalDate getFecha() {
        return fecha.get();
    }

    public void setFecha(LocalDate fecha) {
        this.fecha.set(fecha);
    }

    public ObjectProperty<LocalDate> fechaProperty() {
        return fecha;
    }

    //Hora
    public LocalTime getHora() {
        return hora.get();
    }

    public void setHora(LocalTime hora) {
        this.hora.set(hora);
    }

    public ObjectProperty<LocalTime> horaProperty() {
        return hora;
    }

    //Nombre Paciente
    public String getNombrePaciente() {
        return nombrePaciente.get();
    }

    public void setNombrePaciente(String nombrePaciente) {
        this.nombrePaciente.set(nombrePaciente);
    }

    public StringProperty nombrePacienteProperty() {
        return nombrePaciente;
    }

    //Nombre Sanitario
    public String getNombreSanitario() {
        return nombreSanitario.get();
    }

    public void setNombreSanitario(String nombreSanitario) {
        this.nombreSanitario.set(nombreSanitario);
    }

    public StringProperty nombreSanitarioProperty() {
        return nombreSanitario;
    }

    // ==================== METODOS DE FORMATO ====================

    /**
     * Obtiene la fecha formateada como dd/MM/yyyy
     */
    public String getFechaFormateada() {
        if (fecha.get() != null) {
            return fecha.get().format(FORMATO_FECHA);
        }
        return "";
    }

    /**
     * Property para la fecha formateada (para TableView)
     */
    public StringProperty fechaFormateadaProperty() {
        return new SimpleStringProperty(getFechaFormateada());
    }

    /**
     * Obtiene la hora formateada como HH:mm
     */
    public String getHoraFormateada() {
        if (hora.get() != null) {
            return hora.get().format(FORMATO_HORA);
        }
        return "";
    }

    /**
     * Property para la hora formateada (para TableView)
     */
    public StringProperty horaFormateadaProperty() {
        return new SimpleStringProperty(getHoraFormateada());
    }

    @Override
    public String toString() {
        return "Cita{" +
                "dniPaciente='" + getDniPaciente() + '\'' +
                ", dniSanitario='" + getDniSanitario() + '\'' +
                ", fecha=" + getFechaFormateada() +
                ", hora=" + getHoraFormateada() +
                '}';
    }
}
