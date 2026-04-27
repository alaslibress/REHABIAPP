package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa un juego terapeutico Unity del catalogo clinico.
 *
 * <p>Tabla: juego</p>
 * <p>Los juegos estan hospedados en AWS S3/CloudFront y se identifican por su URL publica.
 * Cada juego esta asociado a una articulacion concreta.
 * Creada en V13__juego_articulacion.sql.</p>
 */
@Entity
@Table(name = "juego")
public class Juego {

    /** Codigo del juego. Clave primaria natural VARCHAR(32). */
    @Id
    @Column(name = "cod_juego", length = 32)
    private String codJuego;

    /** Nombre del juego mostrado en la interfaz de seleccion. */
    @Column(name = "nombre", length = 120, nullable = false)
    private String nombre;

    /** Descripcion clinica o instrucciones resumidas del juego. */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /** URL publica HTTPS del juego Unity en AWS CloudFront. */
    @Column(name = "url_juego", length = 400, nullable = false)
    private String urlJuego;

    /** Articulacion para la que esta disenado el juego (FK obligatoria). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_articulacion", nullable = false)
    private Articulacion articulacion;

    /** Permite desactivar un juego sin eliminarlo del catalogo. */
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    // --- Getters y setters ---

    public String getCodJuego() {
        return codJuego;
    }

    public void setCodJuego(String codJuego) {
        this.codJuego = codJuego;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getUrlJuego() {
        return urlJuego;
    }

    public void setUrlJuego(String urlJuego) {
        this.urlJuego = urlJuego;
    }

    public Articulacion getArticulacion() {
        return articulacion;
    }

    public void setArticulacion(Articulacion articulacion) {
        this.articulacion = articulacion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
