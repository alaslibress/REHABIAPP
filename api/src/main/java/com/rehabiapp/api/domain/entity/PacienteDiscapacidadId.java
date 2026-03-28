package com.rehabiapp.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta para la asignación de discapacidades a pacientes.
 *
 * <p>Identifica unívocamente la relación entre un paciente y una discapacidad.</p>
 */
@Embeddable
public class PacienteDiscapacidadId implements Serializable {

    /**
     * DNI del paciente.
     */
    @Column(name = "dni_pac", length = 20)
    private String dniPac;

    /**
     * Código de la discapacidad asignada.
     */
    @Column(name = "cod_dis", length = 20)
    private String codDis;

    public PacienteDiscapacidadId() {}

    public PacienteDiscapacidadId(String dniPac, String codDis) {
        this.dniPac = dniPac;
        this.codDis = codDis;
    }

    // --- Getters y setters ---

    public String getDniPac() {
        return dniPac;
    }

    public void setDniPac(String dniPac) {
        this.dniPac = dniPac;
    }

    public String getCodDis() {
        return codDis;
    }

    public void setCodDis(String codDis) {
        this.codDis = codDis;
    }

    // --- equals y hashCode obligatorios para @Embeddable ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PacienteDiscapacidadId that)) return false;
        return Objects.equals(dniPac, that.dniPac) && Objects.equals(codDis, that.codDis);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dniPac, codDis);
    }
}
