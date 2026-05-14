package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Informe Markdown de una sesion de juego terapeutico.
 *
 * Derivado del documento GameSession en MongoDB — no es el registro principal.
 * La columna contenido_md es TEXT para permitir busqueda futura via tsvector.
 * La columna mongo_id tiene restriccion UNIQUE para garantizar idempotencia:
 * si el pipeline reintenta la escritura, el controlador devuelve 200 con la
 * fila existente en lugar de insertar un duplicado.
 */
@Entity
@Table(name = "session_reports")
public class SessionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mongo_id", nullable = false, unique = true, length = 48)
    private String mongoId;

    @Column(name = "paciente_dni", nullable = false, length = 20)
    private String pacienteDni;

    @Column(name = "cod_juego", nullable = false, length = 64)
    private String codJuego;

    @Column(name = "cod_trat", length = 32)
    private String codTrat;

    @Column(name = "fecha_sesion", nullable = false)
    private Instant fechaSesion;

    @Column(name = "duracion_seg", nullable = false)
    private Integer duracionSeg;

    // Sin @Lob: PostgreSQL mapea String a TEXT directamente. @Lob fuerza el uso
    // de Large Objects (OID) que requiere modo no-autocommit y rompe la lectura.
    @Column(name = "contenido_md", nullable = false, columnDefinition = "TEXT")
    private String contenidoMd;

    @Column(name = "md_hash", nullable = false, length = 64)
    private String mdHash;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    protected SessionReport() {}

    public SessionReport(String mongoId, String pacienteDni, String codJuego,
                         String codTrat, Instant fechaSesion, Integer duracionSeg,
                         String contenidoMd, String mdHash) {
        this.mongoId = mongoId;
        this.pacienteDni = pacienteDni;
        this.codJuego = codJuego;
        this.codTrat = codTrat;
        this.fechaSesion = fechaSesion;
        this.duracionSeg = duracionSeg;
        this.contenidoMd = contenidoMd;
        this.mdHash = mdHash;
    }

    public Long getId() { return id; }
    public String getMongoId() { return mongoId; }
    public String getPacienteDni() { return pacienteDni; }
    public String getCodJuego() { return codJuego; }
    public String getCodTrat() { return codTrat; }
    public Instant getFechaSesion() { return fechaSesion; }
    public Integer getDuracionSeg() { return duracionSeg; }
    public String getContenidoMd() { return contenidoMd; }
    public String getMdHash() { return mdHash; }
    public Instant getFechaCreacion() { return fechaCreacion; }
}
