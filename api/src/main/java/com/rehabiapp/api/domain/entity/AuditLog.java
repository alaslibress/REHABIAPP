package com.rehabiapp.api.domain.entity;

import com.rehabiapp.api.domain.enums.AccionAuditoria;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que representa una entrada en el registro de auditoría del sistema.
 *
 * <p>Tabla: audit_log</p>
 *
 * <p>Registro INMUTABLE de todas las operaciones sobre datos clínicos y de personal,
 * incluyendo accesos de LECTURA (que Hibernate Envers no registra).
 * NO debe anotarse con @Audited para evitar recursividad.</p>
 *
 * <p>Cumplimiento legal:</p>
 * <ul>
 *   <li>RGPD Art. 30 — Registro de actividades de tratamiento de datos.</li>
 *   <li>Ley 41/2002 — Registro de accesos a historiales clínicos.</li>
 *   <li>ENS Alto — Trazabilidad completa de operaciones sobre datos sensibles.</li>
 * </ul>
 *
 * <p>IMPORTANTE: Esta tabla sólo permite INSERT. Nunca se deben exponer
 * operaciones de actualización o borrado sobre audit_log.</p>
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    /**
     * Identificador UUID generado por Hibernate al persistir.
     * La BD acepta uuid generado en Java (compatible con el DEFAULT uuidv7() de PG).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_audit", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID idAudit;

    /**
     * Fecha y hora exacta en que se registró la operación.
     * Se inicializa automáticamente al momento de la creación del objeto.
     */
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    /**
     * DNI del sanitario que realizó la operación.
     */
    @Column(name = "dni_usuario", length = 20)
    private String dniUsuario;

    /**
     * Nombre completo del usuario para legibilidad del log.
     */
    @Column(name = "nombre_usuario", length = 200)
    private String nombreUsuario;

    /**
     * Tipo de acción realizada (CREAR, LEER, ACTUALIZAR, ELIMINAR).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "accion", length = 20, nullable = false)
    private AccionAuditoria accion;

    /**
     * Nombre de la entidad sobre la que se realizó la acción (p.ej. "Paciente").
     */
    @Column(name = "entidad", length = 100)
    private String entidad;

    /**
     * Identificador del registro afectado (DNI, código, etc.).
     */
    @Column(name = "id_entidad", length = 200)
    private String idEntidad;

    /**
     * Descripción adicional de la operación o datos modificados.
     */
    @Column(name = "detalle", columnDefinition = "TEXT")
    private String detalle;

    /**
     * IP de origen de la petición. Longitud 45 para soportar IPv6.
     */
    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    // --- Getters y setters ---

    public UUID getIdAudit() {
        return idAudit;
    }

    public void setIdAudit(UUID idAudit) {
        this.idAudit = idAudit;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getDniUsuario() {
        return dniUsuario;
    }

    public void setDniUsuario(String dniUsuario) {
        this.dniUsuario = dniUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public AccionAuditoria getAccion() {
        return accion;
    }

    public void setAccion(AccionAuditoria accion) {
        this.accion = accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public String getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(String idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public void setIpOrigen(String ipOrigen) {
        this.ipOrigen = ipOrigen;
    }
}
