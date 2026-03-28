package com.rehabiapp.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta para la relación N:M entre discapacidad y tratamiento.
 *
 * <p>Embebida en DiscapacidadTratamiento. Implementa equals y hashCode
 * para que JPA pueda gestionar correctamente la identidad de los registros.</p>
 */
@Embeddable
public class DiscapacidadTratamientoId implements Serializable {

    /**
     * Código de la discapacidad.
     */
    @Column(name = "cod_dis", length = 20)
    private String codDis;

    /**
     * Código del tratamiento.
     */
    @Column(name = "cod_trat", length = 20)
    private String codTrat;

    public DiscapacidadTratamientoId() {}

    public DiscapacidadTratamientoId(String codDis, String codTrat) {
        this.codDis = codDis;
        this.codTrat = codTrat;
    }

    // --- Getters y setters ---

    public String getCodDis() {
        return codDis;
    }

    public void setCodDis(String codDis) {
        this.codDis = codDis;
    }

    public String getCodTrat() {
        return codTrat;
    }

    public void setCodTrat(String codTrat) {
        this.codTrat = codTrat;
    }

    // --- equals y hashCode obligatorios para @Embeddable ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiscapacidadTratamientoId that)) return false;
        return Objects.equals(codDis, that.codDis) && Objects.equals(codTrat, that.codTrat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codDis, codTrat);
    }
}
