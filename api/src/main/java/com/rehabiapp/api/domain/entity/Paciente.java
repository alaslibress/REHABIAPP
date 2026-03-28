package com.rehabiapp.api.domain.entity;

import com.rehabiapp.api.domain.enums.Sexo;
import com.rehabiapp.api.infrastructure.encryption.CampoClinicoConverter;
import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa a un paciente del sistema de rehabilitación.
 *
 * <p>Tabla: paciente</p>
 *
 * <p>CRITICO - Seguridad y privacidad:</p>
 * <ul>
 *   <li>Los campos clínicos sensibles (alergias, antecedentes, medicacion_actual)
 *       se cifran automáticamente con AES-256-GCM mediante CampoClinicoConverter
 *       antes de ser persistidos en PostgreSQL. RGPD Art. 9.</li>
 *   <li>El paciente NUNCA se elimina físicamente. Soft delete: activo=false + fechaBaja.
 *       Retención mínima 5 años tras la baja (Ley 41/2002).</li>
 *   <li>Todos los cambios quedan auditados en paciente_audit vía Hibernate Envers.</li>
 *   <li>El consentimiento RGPD es obligatorio para el tratamiento de datos clínicos.</li>
 * </ul>
 */
@Audited
@Entity
@Table(name = "paciente")
public class Paciente {

    /**
     * DNI del paciente. Clave primaria natural VARCHAR(20).
     */
    @Id
    @Column(name = "dni_pac", length = 20)
    private String dniPac;

    /**
     * Sanitario responsable del paciente. Carga diferida para evitar N+1.
     * ON DELETE RESTRICT — no se puede borrar un sanitario con pacientes asignados.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dni_san", nullable = false)
    private Sanitario sanitario;

    /**
     * Dirección postal del paciente. Opcional (puede ser null).
     * NOT_AUDITED porque Direccion no es una entidad auditada.
     */
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_direccion")
    private Direccion direccion;

    /**
     * Nombre de pila del paciente.
     */
    @Column(name = "nombre_pac", length = 100, nullable = false)
    private String nombrePac;

    /**
     * Primer apellido del paciente.
     */
    @Column(name = "apellido1_pac", length = 100, nullable = false)
    private String apellido1Pac;

    /**
     * Segundo apellido del paciente (puede ser nulo).
     */
    @Column(name = "apellido2_pac", length = 100)
    private String apellido2Pac;

    /**
     * Edad actual del paciente en años.
     */
    @Column(name = "edad_pac")
    private Integer edadPac;

    /**
     * Correo electrónico único del paciente.
     */
    @Column(name = "email_pac", length = 200, unique = true)
    private String emailPac;

    /**
     * Número de la Seguridad Social. Único en el sistema.
     */
    @Column(name = "num_ss", length = 20, unique = true)
    private String numSs;

    /**
     * Descripción de la discapacidad principal (campo de texto libre heredado).
     */
    @Column(name = "discapacidad_pac", length = 200)
    private String discapacidadPac;

    /**
     * Descripción del tratamiento principal (campo de texto libre heredado).
     */
    @Column(name = "tratamiento_pac", length = 200)
    private String tratamientoPac;

    /**
     * Estado actual del tratamiento (texto libre).
     */
    @Column(name = "estado_tratamiento", length = 100)
    private String estadoTratamiento;

    /**
     * Indica si el paciente usa prótesis.
     */
    @Column(name = "protesis")
    private boolean protesis = false;

    /**
     * Fotografía del paciente almacenada como bytes (BYTEA en PostgreSQL).
     */
    @Column(name = "foto")
    private byte[] foto;

    /**
     * Fecha de nacimiento del paciente.
     */
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    /**
     * Sexo biológico del paciente. Almacenado como texto para legibilidad.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", length = 20)
    private Sexo sexo;

    /**
     * CAMPO CLINICO CIFRADO — Alergias conocidas del paciente.
     * Se cifra con AES-256-GCM antes de persistir (RGPD Art. 9).
     * El converter gestiona el cifrado/descifrado de forma transparente.
     */
    @Convert(converter = CampoClinicoConverter.class)
    @Column(name = "alergias", columnDefinition = "TEXT")
    private String alergias;

    /**
     * CAMPO CLINICO CIFRADO — Antecedentes médicos del paciente.
     * Se cifra con AES-256-GCM antes de persistir (RGPD Art. 9).
     */
    @Convert(converter = CampoClinicoConverter.class)
    @Column(name = "antecedentes", columnDefinition = "TEXT")
    private String antecedentes;

    /**
     * CAMPO CLINICO CIFRADO — Medicación actual del paciente.
     * Se cifra con AES-256-GCM antes de persistir (RGPD Art. 9).
     */
    @Convert(converter = CampoClinicoConverter.class)
    @Column(name = "medicacion_actual", columnDefinition = "TEXT")
    private String medicacionActual;

    /**
     * Indica si el paciente ha dado su consentimiento explícito (RGPD Art. 6 y 9).
     */
    @Column(name = "consentimiento_rgpd")
    private boolean consentimientoRgpd = false;

    /**
     * Fecha y hora en que se registró el consentimiento RGPD.
     */
    @Column(name = "fecha_consentimiento")
    private LocalDateTime fechaConsentimiento;

    /**
     * Indica si el paciente está activo en el sistema.
     * false = dado de baja lógica. NUNCA se elimina físicamente.
     */
    @Column(name = "activo")
    private boolean activo = true;

    /**
     * Fecha y hora de la baja lógica del paciente.
     * Null si el paciente sigue activo.
     */
    @Column(name = "fecha_baja")
    private LocalDateTime fechaBaja;

    /**
     * Teléfonos del paciente. Relación 1:N con TelefonoPaciente.
     * Cascade ALL para gestionar los teléfonos junto con el paciente.
     * @NotAudited: colecciones hacia entidades no auditadas usan @NotAudited.
     */
    @NotAudited
    @OneToMany(mappedBy = "paciente", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TelefonoPaciente> telefonos = new ArrayList<>();

    // --- Getters y setters ---

    public String getDniPac() {
        return dniPac;
    }

    public void setDniPac(String dniPac) {
        this.dniPac = dniPac;
    }

    public Sanitario getSanitario() {
        return sanitario;
    }

    public void setSanitario(Sanitario sanitario) {
        this.sanitario = sanitario;
    }

    public Direccion getDireccion() {
        return direccion;
    }

    public void setDireccion(Direccion direccion) {
        this.direccion = direccion;
    }

    public String getNombrePac() {
        return nombrePac;
    }

    public void setNombrePac(String nombrePac) {
        this.nombrePac = nombrePac;
    }

    public String getApellido1Pac() {
        return apellido1Pac;
    }

    public void setApellido1Pac(String apellido1Pac) {
        this.apellido1Pac = apellido1Pac;
    }

    public String getApellido2Pac() {
        return apellido2Pac;
    }

    public void setApellido2Pac(String apellido2Pac) {
        this.apellido2Pac = apellido2Pac;
    }

    public Integer getEdadPac() {
        return edadPac;
    }

    public void setEdadPac(Integer edadPac) {
        this.edadPac = edadPac;
    }

    public String getEmailPac() {
        return emailPac;
    }

    public void setEmailPac(String emailPac) {
        this.emailPac = emailPac;
    }

    public String getNumSs() {
        return numSs;
    }

    public void setNumSs(String numSs) {
        this.numSs = numSs;
    }

    public String getDiscapacidadPac() {
        return discapacidadPac;
    }

    public void setDiscapacidadPac(String discapacidadPac) {
        this.discapacidadPac = discapacidadPac;
    }

    public String getTratamientoPac() {
        return tratamientoPac;
    }

    public void setTratamientoPac(String tratamientoPac) {
        this.tratamientoPac = tratamientoPac;
    }

    public String getEstadoTratamiento() {
        return estadoTratamiento;
    }

    public void setEstadoTratamiento(String estadoTratamiento) {
        this.estadoTratamiento = estadoTratamiento;
    }

    public boolean isProtesis() {
        return protesis;
    }

    public void setProtesis(boolean protesis) {
        this.protesis = protesis;
    }

    public byte[] getFoto() {
        return foto;
    }

    public void setFoto(byte[] foto) {
        this.foto = foto;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public Sexo getSexo() {
        return sexo;
    }

    public void setSexo(Sexo sexo) {
        this.sexo = sexo;
    }

    public String getAlergias() {
        return alergias;
    }

    public void setAlergias(String alergias) {
        this.alergias = alergias;
    }

    public String getAntecedentes() {
        return antecedentes;
    }

    public void setAntecedentes(String antecedentes) {
        this.antecedentes = antecedentes;
    }

    public String getMedicacionActual() {
        return medicacionActual;
    }

    public void setMedicacionActual(String medicacionActual) {
        this.medicacionActual = medicacionActual;
    }

    public boolean isConsentimientoRgpd() {
        return consentimientoRgpd;
    }

    public void setConsentimientoRgpd(boolean consentimientoRgpd) {
        this.consentimientoRgpd = consentimientoRgpd;
    }

    public LocalDateTime getFechaConsentimiento() {
        return fechaConsentimiento;
    }

    public void setFechaConsentimiento(LocalDateTime fechaConsentimiento) {
        this.fechaConsentimiento = fechaConsentimiento;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaBaja() {
        return fechaBaja;
    }

    public void setFechaBaja(LocalDateTime fechaBaja) {
        this.fechaBaja = fechaBaja;
    }

    public List<TelefonoPaciente> getTelefonos() {
        return telefonos;
    }

    public void setTelefonos(List<TelefonoPaciente> telefonos) {
        this.telefonos = telefonos;
    }
}
