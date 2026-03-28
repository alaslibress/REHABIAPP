package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa un código postal vinculado a su localidad.
 *
 * <p>Tabla: cp</p>
 * <p>Relación N:1 con Localidad. FetchType.LAZY para evitar consultas N+1.</p>
 */
@Entity
@Table(name = "cp")
public class CodigoPostal {

    /**
     * Código postal. Clave primaria natural VARCHAR(10).
     */
    @Id
    @Column(name = "cp", length = 10)
    private String cp;

    /**
     * Localidad a la que pertenece este código postal.
     * Carga diferida para evitar consultas innecesarias.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nombre_localidad", nullable = false)
    private Localidad localidad;

    // --- Getters y setters ---

    public String getCp() {
        return cp;
    }

    public void setCp(String cp) {
        this.cp = cp;
    }

    public Localidad getLocalidad() {
        return localidad;
    }

    public void setLocalidad(Localidad localidad) {
        this.localidad = localidad;
    }
}
