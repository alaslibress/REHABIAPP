package com.rehabiapp.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta para la asociacion tratamiento-videojuego.
 *
 * <p>Identifica univocamente el vinculo entre un tratamiento y un videojuego
 * terapeutico (tabla N:M tratamiento_videojuego).</p>
 */
@Embeddable
public class TratamientoVideojuegoId implements Serializable {

    @Column(name = "cod_trat", length = 20)
    private String codTrat;

    @Column(name = "id_videojuego")
    private Long idVideojuego;

    public TratamientoVideojuegoId() {}

    public TratamientoVideojuegoId(String codTrat, Long idVideojuego) {
        this.codTrat = codTrat;
        this.idVideojuego = idVideojuego;
    }

    public String getCodTrat() {
        return codTrat;
    }

    public void setCodTrat(String codTrat) {
        this.codTrat = codTrat;
    }

    public Long getIdVideojuego() {
        return idVideojuego;
    }

    public void setIdVideojuego(Long idVideojuego) {
        this.idVideojuego = idVideojuego;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TratamientoVideojuegoId that)) return false;
        return Objects.equals(codTrat, that.codTrat)
                && Objects.equals(idVideojuego, that.idVideojuego);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codTrat, idVideojuego);
    }
}
