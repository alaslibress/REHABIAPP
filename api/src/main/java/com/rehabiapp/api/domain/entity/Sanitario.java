package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa a un profesional sanitario del sistema.
 *
 * <p>Tabla: sanitario</p>
 * <p>Auditada con Hibernate Envers. Los cambios se registran en sanitario_audit.
 * La contraseña NO se audita (@NotAudited) por seguridad para no dejar rastro
 * de hashes en tablas de historial.</p>
 *
 * <p>Soft delete: activo=false + fechaBaja. El registro nunca se elimina físicamente.</p>
 * <p>Contraseña almacenada como hash BCrypt con cost factor 12 (RGPD Art. 32).</p>
 */
@Audited
@Entity
@Table(name = "sanitario")
public class Sanitario {

    /**
     * DNI del sanitario. Clave primaria natural VARCHAR(20).
     */
    @Id
    @Column(name = "dni_san", length = 20)
    private String dniSan;

    /**
     * Nombre de pila del sanitario.
     */
    @Column(name = "nombre_san", length = 100, nullable = false)
    private String nombreSan;

    /**
     * Primer apellido del sanitario.
     */
    @Column(name = "apellido1_san", length = 100, nullable = false)
    private String apellido1San;

    /**
     * Segundo apellido del sanitario (puede ser nulo).
     */
    @Column(name = "apellido2_san", length = 100)
    private String apellido2San;

    /**
     * Correo electrónico único del sanitario.
     */
    @Column(name = "email_san", length = 200, unique = true, nullable = false)
    private String emailSan;

    /**
     * Número de pacientes asignados actualmente al sanitario.
     */
    @Column(name = "num_de_pacientes")
    private int numDePacientes;

    /**
     * Hash BCrypt (cost factor 12) de la contraseña del sanitario.
     * No se audita para evitar exponer hashes en las tablas Envers.
     */
    @NotAudited
    @Column(name = "contrasena_san", nullable = false)
    private String contrasenaSan;

    /**
     * Indica si el sanitario está activo en el sistema.
     * false = dado de baja lógica.
     */
    @Column(name = "activo")
    private boolean activo = true;

    /**
     * Fecha y hora en que se realizó la baja lógica del sanitario.
     * Null si el sanitario sigue activo.
     */
    @Column(name = "fecha_baja")
    private LocalDateTime fechaBaja;

    /**
     * Teléfonos del sanitario. Relación 1:N con TelefonoSanitario.
     * Cascade ALL para gestionar los teléfonos junto con el sanitario.
     * @NotAudited: colecciones hacia entidades no auditadas usan @NotAudited, no targetAuditMode.
     */
    @NotAudited
    @OneToMany(mappedBy = "sanitario", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TelefonoSanitario> telefonos = new ArrayList<>();

    /**
     * Rol del sanitario en el sistema (SPECIALIST o NURSE).
     * Relación 1:1 inversa. Se gestiona en cascada junto con el sanitario.
     * NOT_AUDITED porque SanitarioRol no es una entidad auditada directamente.
     */
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @OneToOne(mappedBy = "sanitario", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private SanitarioRol rol;

    // --- Getters y setters ---

    public String getDniSan() {
        return dniSan;
    }

    public void setDniSan(String dniSan) {
        this.dniSan = dniSan;
    }

    public String getNombreSan() {
        return nombreSan;
    }

    public void setNombreSan(String nombreSan) {
        this.nombreSan = nombreSan;
    }

    public String getApellido1San() {
        return apellido1San;
    }

    public void setApellido1San(String apellido1San) {
        this.apellido1San = apellido1San;
    }

    public String getApellido2San() {
        return apellido2San;
    }

    public void setApellido2San(String apellido2San) {
        this.apellido2San = apellido2San;
    }

    public String getEmailSan() {
        return emailSan;
    }

    public void setEmailSan(String emailSan) {
        this.emailSan = emailSan;
    }

    public int getNumDePacientes() {
        return numDePacientes;
    }

    public void setNumDePacientes(int numDePacientes) {
        this.numDePacientes = numDePacientes;
    }

    public String getContrasenaSan() {
        return contrasenaSan;
    }

    public void setContrasenaSan(String contrasenaSan) {
        this.contrasenaSan = contrasenaSan;
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

    public List<TelefonoSanitario> getTelefonos() {
        return telefonos;
    }

    public void setTelefonos(List<TelefonoSanitario> telefonos) {
        this.telefonos = telefonos;
    }

    public SanitarioRol getRol() {
        return rol;
    }

    public void setRol(SanitarioRol rol) {
        this.rol = rol;
    }
}
