package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa un nivel de progresión clínica en los tratamientos.
 *
 * <p>Tabla: nivel_progresion</p>
 * <p>Catálogo ordenado de niveles terapéuticos. El campo "orden" define
 * la secuencia ascendente de la progresión clínica del paciente.</p>
 */
@Entity
@Table(name = "nivel_progresion")
public class NivelProgresion {

    /**
     * Identificador técnico generado por la BD.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nivel")
    private Integer idNivel;

    /**
     * Nombre del nivel de progresión. Único en el catálogo.
     */
    @Column(name = "nombre", length = 100, nullable = false, unique = true)
    private String nombre;

    /**
     * Posición en la secuencia terapéutica. Único para garantizar
     * un orden determinista en los listados de progresión.
     */
    @Column(name = "orden", nullable = false, unique = true)
    private Integer orden;

    /**
     * Descripción clínica del nivel de progresión.
     */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // --- Getters y setters ---

    public Integer getIdNivel() {
        return idNivel;
    }

    public void setIdNivel(Integer idNivel) {
        this.idNivel = idNivel;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
