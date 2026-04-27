package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa una discapacidad del catálogo clínico.
 *
 * <p>Tabla: discapacidad</p>
 * <p>Catálogo maestro gestionado por el especialista.
 * Referenciado por pacientes a través de PacienteDiscapacidad.</p>
 */
@Entity
@Table(name = "discapacidad")
public class Discapacidad {

    /**
     * Código de la discapacidad. Clave primaria natural VARCHAR(20).
     */
    @Id
    @Column(name = "cod_dis", length = 20)
    private String codDis;

    /**
     * Nombre descriptivo de la discapacidad. Único en el catálogo.
     */
    @Column(name = "nombre_dis", length = 200, unique = true, nullable = false)
    private String nombreDis;

    /**
     * Descripción clínica detallada de la discapacidad.
     */
    @Column(name = "descripcion_dis", columnDefinition = "TEXT")
    private String descripcionDis;

    /**
     * Indica si el tratamiento de esta discapacidad requiere prótesis.
     */
    @Column(name = "necesita_protesis")
    private boolean necesitaProtesis = false;

    /**
     * Articulacion afectada por esta discapacidad. Nullable — discapacidades
     * existentes sin articulacion asignada conservan null.
     * Anadida en V13__juego_articulacion.sql.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_articulacion")
    private Articulacion articulacion;

    // --- Getters y setters ---

    public String getCodDis() {
        return codDis;
    }

    public void setCodDis(String codDis) {
        this.codDis = codDis;
    }

    public String getNombreDis() {
        return nombreDis;
    }

    public void setNombreDis(String nombreDis) {
        this.nombreDis = nombreDis;
    }

    public String getDescripcionDis() {
        return descripcionDis;
    }

    public void setDescripcionDis(String descripcionDis) {
        this.descripcionDis = descripcionDis;
    }

    public boolean isNecesitaProtesis() {
        return necesitaProtesis;
    }

    public void setNecesitaProtesis(boolean necesitaProtesis) {
        this.necesitaProtesis = necesitaProtesis;
    }

    public Articulacion getArticulacion() {
        return articulacion;
    }

    public void setArticulacion(Articulacion articulacion) {
        this.articulacion = articulacion;
    }
}
