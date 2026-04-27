package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa un tratamiento del catálogo clínico.
 *
 * <p>Tabla: tratamiento</p>
 * <p>Catálogo maestro de tratamientos disponibles. Se relaciona con
 * discapacidades a través de la tabla DiscapacidadTratamiento (N:M).
 * Opcionalmente vinculado a un nivel de progresión clínica (nullable).</p>
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

    /**
     * Nivel de progresión clínica al que pertenece este tratamiento (opcional).
     * Nullable para compatibilidad con tratamientos existentes sin nivel asignado.
     * Agregado en V4__tratamiento_nivel_progresion.sql.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nivel")
    private NivelProgresion nivel;

    /**
     * Juego terapeutico Unity asociado a este tratamiento (opcional).
     * Nullable — tratamientos sin juego asociado conservan null.
     * Anadido en V13__juego_articulacion.sql.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cod_juego")
    private Juego juego;

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

    public NivelProgresion getNivel() {
        return nivel;
    }

    public void setNivel(NivelProgresion nivel) {
        this.nivel = nivel;
    }

    public Juego getJuego() {
        return juego;
    }

    public void setJuego(Juego juego) {
        this.juego = juego;
    }
}
