package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa la asignación de una discapacidad a un paciente.
 *
 * <p>Tabla: paciente_discapacidad</p>
 * <p>Registra qué discapacidades tiene cada paciente, junto con su nivel
 * de progresión clínica actual y las notas del especialista.
 * Auditada con Envers para seguimiento histórico de la evolución clínica.</p>
 */
@Audited
@Entity
@Table(name = "paciente_discapacidad")
public class PacienteDiscapacidad {

    /**
     * Clave primaria compuesta: DNI del paciente + código de discapacidad.
     */
    @EmbeddedId
    private PacienteDiscapacidadId id;

    /**
     * Paciente al que se asigna la discapacidad.
     * @MapsId vincula la parte "dniPac" del EmbeddedId.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("dniPac")
    @JoinColumn(name = "dni_pac")
    private Paciente paciente;

    /**
     * Discapacidad asignada al paciente.
     * @MapsId vincula la parte "codDis" del EmbeddedId.
     * NOT_AUDITED porque Discapacidad es un catálogo estático no auditado.
     */
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("codDis")
    @JoinColumn(name = "cod_dis")
    private Discapacidad discapacidad;

    /**
     * Nivel de progresión clínica actual del paciente en esta discapacidad.
     * Puede ser null si el nivel no ha sido asignado aún.
     * NOT_AUDITED porque NivelProgresion es un catálogo estático no auditado.
     */
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nivel_actual")
    private NivelProgresion nivelProgresion;

    /**
     * Fecha y hora en que se realizó la asignación.
     * Se inicializa automáticamente al momento de la creación del objeto.
     */
    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion = LocalDateTime.now();

    /**
     * Notas clínicas del especialista sobre esta asignación.
     */
    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    // --- Getters y setters ---

    public PacienteDiscapacidadId getId() {
        return id;
    }

    public void setId(PacienteDiscapacidadId id) {
        this.id = id;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public Discapacidad getDiscapacidad() {
        return discapacidad;
    }

    public void setDiscapacidad(Discapacidad discapacidad) {
        this.discapacidad = discapacidad;
    }

    public NivelProgresion getNivelProgresion() {
        return nivelProgresion;
    }

    public void setNivelProgresion(NivelProgresion nivelProgresion) {
        this.nivelProgresion = nivelProgresion;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(LocalDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }
}
