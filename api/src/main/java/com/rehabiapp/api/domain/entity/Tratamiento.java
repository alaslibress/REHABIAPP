package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa un tratamiento del catálogo clínico.
 *
 * <p>Tabla: tratamiento</p>
 * <p>Catálogo maestro de tratamientos disponibles. Se relaciona con
 * discapacidades a través de la tabla DiscapacidadTratamiento (N:M).</p>
 */
@Entity
@Table(name = "tratamiento")
public class Tratamiento {

    /**
     * Código del tratamiento. Clave primaria natural VARCHAR(20).
     */
    @Id
    @Column(name = "cod_trat", length = 20)
    private String codTrat;

    /**
     * Nombre del tratamiento. Único en el catálogo.
     */
    @Column(name = "nombre_trat", length = 200, unique = true, nullable = false)
    private String nombreTrat;

    /**
     * Definición o descripción técnica del tratamiento.
     */
    @Column(name = "definicion_trat", columnDefinition = "TEXT")
    private String definicionTrat;

    // --- Getters y setters ---

    public String getCodTrat() {
        return codTrat;
    }

    public void setCodTrat(String codTrat) {
        this.codTrat = codTrat;
    }

    public String getNombreTrat() {
        return nombreTrat;
    }

    public void setNombreTrat(String nombreTrat) {
        this.nombreTrat = nombreTrat;
    }

    public String getDefinicionTrat() {
        return definicionTrat;
    }

    public void setDefinicionTrat(String definicionTrat) {
        this.definicionTrat = definicionTrat;
    }
}
