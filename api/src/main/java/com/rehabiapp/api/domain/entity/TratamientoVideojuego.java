package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa la asociacion entre un tratamiento y un videojuego terapeutico.
 *
 * <p>Tabla: tratamiento_videojuego (N:M)</p>
 *
 * <p>Auditada con Envers (tratamiento_videojuego_audit). Tanto Tratamiento como
 * Videojuego se referencian con NOT_AUDITED porque Tratamiento es catalogo
 * estatico y Videojuego se audita por separado.</p>
 */
@Audited
@Entity
@Table(name = "tratamiento_videojuego")
public class TratamientoVideojuego {

    @EmbeddedId
    private TratamientoVideojuegoId id;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("codTrat")
    @JoinColumn(name = "cod_trat")
    private Tratamiento tratamiento;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idVideojuego")
    @JoinColumn(name = "id_videojuego")
    private Videojuego videojuego;

    @Column(name = "fecha_vinculo", nullable = false)
    private LocalDateTime fechaVinculo = LocalDateTime.now();

    public TratamientoVideojuegoId getId() {
        return id;
    }

    public void setId(TratamientoVideojuegoId id) {
        this.id = id;
    }

    public Tratamiento getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(Tratamiento tratamiento) {
        this.tratamiento = tratamiento;
    }

    public Videojuego getVideojuego() {
        return videojuego;
    }

    public void setVideojuego(Videojuego videojuego) {
        this.videojuego = videojuego;
    }

    public LocalDateTime getFechaVinculo() {
        return fechaVinculo;
    }

    public void setFechaVinculo(LocalDateTime fechaVinculo) {
        this.fechaVinculo = fechaVinculo;
    }
}
