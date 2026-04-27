package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa una articulacion / parte del cuerpo del catalogo clinico.
 *
 * <p>Tabla: articulacion</p>
 * <p>Taxonomia de partes del cuerpo alineada con el diagrama SVG del frontend movil
 * (BodyPartId en mobile/frontend/src/types/progress.ts).
 * Creada en V13__juego_articulacion.sql.</p>
 */
@Entity
@Table(name = "articulacion")
public class Articulacion {

    /** Identificador autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_articulacion")
    private Integer idArticulacion;

    /** Codigo unico alineado con BodyPartId del frontend movil (ej. LEFT_HAND). */
    @Column(name = "codigo", length = 32, unique = true, nullable = false)
    private String codigo;

    /** Nombre legible en español para mostrar en formularios. */
    @Column(name = "nombre", length = 80, nullable = false)
    private String nombre;

    // --- Getters y setters ---

    public Integer getIdArticulacion() {
        return idArticulacion;
    }

    public void setIdArticulacion(Integer idArticulacion) {
        this.idArticulacion = idArticulacion;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
