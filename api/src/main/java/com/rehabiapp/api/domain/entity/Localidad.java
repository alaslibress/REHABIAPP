package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa una localidad del catálogo geográfico de España.
 *
 * <p>Tabla: localidad</p>
 * <p>Se utiliza como referencia para la dirección de pacientes y sanitarios.</p>
 */
@Entity
@Table(name = "localidad")
public class Localidad {

    /**
     * Nombre de la localidad. Clave primaria natural de tipo VARCHAR.
     */
    @Id
    @Column(name = "nombre_localidad", length = 100)
    private String nombreLocalidad;

    /**
     * Provincia a la que pertenece la localidad.
     */
    @Column(name = "provincia", length = 100, nullable = false)
    private String provincia;

    // --- Getters y setters ---

    public String getNombreLocalidad() {
        return nombreLocalidad;
    }

    public void setNombreLocalidad(String nombreLocalidad) {
        this.nombreLocalidad = nombreLocalidad;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }
}
