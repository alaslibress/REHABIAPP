package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa la relación N:M entre discapacidad y tratamiento.
 *
 * <p>Tabla: discapacidad_tratamiento</p>
 * <p>Asocia qué tratamientos son aplicables a cada discapacidad.
 * Las referencias a Discapacidad y Tratamiento usan insertable/updatable=false
 * para que JPA no intente gestionar los campos de la PK dos veces.</p>
 */
@Entity
@Table(name = "discapacidad_tratamiento")
public class DiscapacidadTratamiento {

    /**
     * Clave primaria compuesta (cod_dis, cod_trat).
     */
    @EmbeddedId
    private DiscapacidadTratamientoId id;

    /**
     * Discapacidad asociada. insertable/updatable=false porque los campos
     * de la FK ya están gestionados por el @EmbeddedId.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cod_dis", insertable = false, updatable = false)
    private Discapacidad discapacidad;

    /**
     * Tratamiento asociado. insertable/updatable=false por la misma razón.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cod_trat", insertable = false, updatable = false)
    private Tratamiento tratamiento;

    // --- Getters y setters ---

    public DiscapacidadTratamientoId getId() {
        return id;
    }

    public void setId(DiscapacidadTratamientoId id) {
        this.id = id;
    }

    public Discapacidad getDiscapacidad() {
        return discapacidad;
    }

    public void setDiscapacidad(Discapacidad discapacidad) {
        this.discapacidad = discapacidad;
    }

    public Tratamiento getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(Tratamiento tratamiento) {
        this.tratamiento = tratamiento;
    }
}
