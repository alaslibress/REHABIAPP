package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa un videojuego terapeutico del catalogo.
 *
 * <p>Tabla: videojuego</p>
 *
 * <p>Los minijuegos se hospedan externamente (AWS S3 + CloudFront) y se
 * referencian por URL. Cada videojuego esta asociado a una discapacidad
 * y a una parte del cuerpo. Se vincula con tratamientos mediante la
 * tabla N:M tratamiento_videojuego.</p>
 *
 * <p>Auditado con Envers (videojuego_audit). El borrado es logico
 * mediante el campo activo=false.</p>
 */
@Audited
@Entity
@Table(name = "videojuego")
public class Videojuego {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_videojuego")
    private Long idVideojuego;

    @Column(name = "codigo", length = 50, nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", length = 200, nullable = false)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Discapacidad terapeutica a la que aplica este videojuego.
     * NOT_AUDITED porque Discapacidad es un catalogo estatico.
     */
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cod_dis", nullable = false)
    private Discapacidad discapacidad;

    @Column(name = "parte_cuerpo", length = 100, nullable = false)
    private String parteCuerpo;

    @Column(name = "url_unity", length = 500)
    private String urlUnity;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // --- Getters y setters ---

    public Long getIdVideojuego() {
        return idVideojuego;
    }

    public void setIdVideojuego(Long idVideojuego) {
        this.idVideojuego = idVideojuego;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Discapacidad getDiscapacidad() {
        return discapacidad;
    }

    public void setDiscapacidad(Discapacidad discapacidad) {
        this.discapacidad = discapacidad;
    }

    public String getParteCuerpo() {
        return parteCuerpo;
    }

    public void setParteCuerpo(String parteCuerpo) {
        this.parteCuerpo = parteCuerpo;
    }

    public String getUrlUnity() {
        return urlUnity;
    }

    public void setUrlUnity(String urlUnity) {
        this.urlUnity = urlUnity;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
