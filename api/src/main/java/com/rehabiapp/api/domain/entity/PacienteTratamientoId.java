package com.rehabiapp.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta para la visibilidad de tratamientos por paciente.
 *
 * <p>Identifica unívocamente la relación entre un paciente y un tratamiento.</p>
 */
@Embeddable
public class PacienteTratamientoId implements Serializable {

    /**
     * DNI del paciente.
     */
    @Column(name = "dni_pac", length = 20)
    private String dniPac;

    /**
     * Código del tratamiento.
     */
    @Column(name = "cod_trat", length = 20)
    private String codTrat;

    public PacienteTratamientoId() {}

    public PacienteTratamientoId(String dniPac, String codTrat) {
        this.dniPac = dniPac;
        this.codTrat = codTrat;
    }

    // --- Getters y setters ---

    public String getDniPac() {
        return dniPac;
    }

    public void setDniPac(String dniPac) {
        this.dniPac = dniPac;
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
        if (!(o instanceof PacienteTratamientoId that)) return false;
        return Objects.equals(dniPac, that.dniPac) && Objects.equals(codTrat, that.codTrat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dniPac, codTrat);
    }
}
