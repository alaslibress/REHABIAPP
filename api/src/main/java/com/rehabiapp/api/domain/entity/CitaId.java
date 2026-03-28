package com.rehabiapp.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Clave primaria compuesta para la entidad Cita.
 *
 * <p>Una cita queda unívocamente identificada por la combinación de:
 * DNI del paciente, DNI del sanitario, fecha y hora de la cita.</p>
 *
 * <p>Implementa equals y hashCode para que JPA gestione
 * correctamente la identidad de los registros.</p>
 */
@Embeddable
public class CitaId implements Serializable {

    /**
     * DNI del paciente citado.
     */
    @Column(name = "dni_pac", length = 20)
    private String dniPac;

    /**
     * DNI del sanitario que atiende la cita.
     */
    @Column(name = "dni_san", length = 20)
    private String dniSan;

    /**
     * Fecha de la cita.
     */
    @Column(name = "fecha_cita")
    private LocalDate fechaCita;

    /**
     * Hora de inicio de la cita.
     */
    @Column(name = "hora_cita")
    private LocalTime horaCita;

    public CitaId() {}

    public CitaId(String dniPac, String dniSan, LocalDate fechaCita, LocalTime horaCita) {
        this.dniPac = dniPac;
        this.dniSan = dniSan;
        this.fechaCita = fechaCita;
        this.horaCita = horaCita;
    }

    // --- Getters y setters ---

    public String getDniPac() {
        return dniPac;
    }

    public void setDniPac(String dniPac) {
        this.dniPac = dniPac;
    }

    public String getDniSan() {
        return dniSan;
    }

    public void setDniSan(String dniSan) {
        this.dniSan = dniSan;
    }

    public LocalDate getFechaCita() {
        return fechaCita;
    }

    public void setFechaCita(LocalDate fechaCita) {
        this.fechaCita = fechaCita;
    }

    public LocalTime getHoraCita() {
        return horaCita;
    }

    public void setHoraCita(LocalTime horaCita) {
        this.horaCita = horaCita;
    }

    // --- equals y hashCode obligatorios para @Embeddable ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CitaId that)) return false;
        return Objects.equals(dniPac, that.dniPac)
                && Objects.equals(dniSan, that.dniSan)
                && Objects.equals(fechaCita, that.fechaCita)
                && Objects.equals(horaCita, that.horaCita);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dniPac, dniSan, fechaCita, horaCita);
    }
}
