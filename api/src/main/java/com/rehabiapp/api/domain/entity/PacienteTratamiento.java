package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa la visibilidad de un tratamiento para un paciente.
 *
 * <p>Tabla: paciente_tratamiento</p>
 * <p>Permite al especialista ocultar temporalmente tratamientos sin eliminar
 * la asignación clínica. Auditada con Envers para trazabilidad de los cambios.</p>
 */
@Audited
@Entity
@Table(name = "paciente_tratamiento")
public class PacienteTratamiento {

    /**
     * Clave primaria compuesta: DNI del paciente + código de tratamiento.
     */
    @EmbeddedId
    private PacienteTratamientoId id;

    /**
     * Paciente al que pertenece este tratamiento.
     * @MapsId vincula la parte "dniPac" del EmbeddedId.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("dniPac")
    @JoinColumn(name = "dni_pac")
    private Paciente paciente;

    /**
     * Tratamiento asignado al paciente.
     * @MapsId vincula la parte "codTrat" del EmbeddedId.
     * NOT_AUDITED porque Tratamiento es un catálogo estático no auditado.
     */
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("codTrat")
    @JoinColumn(name = "cod_trat")
    private Tratamiento tratamiento;

    /**
     * Indica si el tratamiento es visible para el paciente en la app móvil.
     * El especialista puede ocultarlo temporalmente sin eliminar la asignación.
     */
    @Column(name = "visible", nullable = false)
    private boolean visible = true;

    /**
     * Fecha y hora en que se realizó la asignación del tratamiento.
     * Se inicializa automáticamente al momento de la creación del objeto.
     */
    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion = LocalDateTime.now();

    // --- Getters y setters ---

    public PacienteTratamientoId getId() {
        return id;
    }

    public void setId(PacienteTratamientoId id) {
        this.id = id;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public Tratamiento getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(Tratamiento tratamiento) {
        this.tratamiento = tratamiento;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(LocalDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }
}
