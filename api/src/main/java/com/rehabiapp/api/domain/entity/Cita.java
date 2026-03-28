package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;

/**
 * Entidad JPA que representa una cita médica entre paciente y sanitario.
 *
 * <p>Tabla: cita</p>
 * <p>Clave primaria compuesta (dni_pac, dni_san, fecha_cita, hora_cita).
 * Auditada con Hibernate Envers. Los cambios se registran en cita_audit.</p>
 */
@Audited
@Entity
@Table(name = "cita")
public class Cita {

    /**
     * Clave primaria compuesta: paciente, sanitario, fecha y hora.
     */
    @EmbeddedId
    private CitaId id;

    /**
     * Paciente de la cita. @MapsId delega la parte "dniPac" del EmbeddedId.
     * FetchType.LAZY para evitar carga innecesaria del historial del paciente.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("dniPac")
    @JoinColumn(name = "dni_pac")
    private Paciente paciente;

    /**
     * Sanitario que atiende la cita. @MapsId delega la parte "dniSan".
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("dniSan")
    @JoinColumn(name = "dni_san")
    private Sanitario sanitario;

    // --- Getters y setters ---

    public CitaId getId() {
        return id;
    }

    public void setId(CitaId id) {
        this.id = id;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public Sanitario getSanitario() {
        return sanitario;
    }

    public void setSanitario(Sanitario sanitario) {
        this.sanitario = sanitario;
    }
}
