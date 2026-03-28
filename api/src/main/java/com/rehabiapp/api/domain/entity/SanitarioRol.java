package com.rehabiapp.api.domain.entity;

import com.rehabiapp.api.domain.enums.Rol;
import jakarta.persistence.*;

/**
 * Entidad JPA que representa el rol asignado a un sanitario.
 *
 * <p>Tabla: sanitario_agrega_sanitario</p>
 * <p>Comparte clave primaria con Sanitario (dniSan) e implementa
 * el control de acceso basado en roles (RBAC) del sistema.</p>
 *
 * <p>SPECIALIST: acceso completo. NURSE: lectura restringida.</p>
 */
@Entity
@Table(name = "sanitario_agrega_sanitario")
public class SanitarioRol {

    /**
     * DNI del sanitario. Coincide con la PK de la tabla sanitario.
     */
    @Id
    @Column(name = "dni_san", length = 20)
    private String dniSan;

    /**
     * Rol del sanitario en el sistema (SPECIALIST o NURSE).
     * Almacenado como cadena de texto para legibilidad en la BD.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "cargo", length = 50, nullable = false)
    private Rol cargo;

    /**
     * Sanitario al que corresponde este rol. Relación 1:1.
     * @MapsId comparte la clave primaria con la entidad Sanitario.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "dni_san")
    private Sanitario sanitario;

    // --- Getters y setters ---

    public String getDniSan() {
        return dniSan;
    }

    public void setDniSan(String dniSan) {
        this.dniSan = dniSan;
    }

    public Rol getCargo() {
        return cargo;
    }

    public void setCargo(Rol cargo) {
        this.cargo = cargo;
    }

    public Sanitario getSanitario() {
        return sanitario;
    }

    public void setSanitario(Sanitario sanitario) {
        this.sanitario = sanitario;
    }
}
