package com.rehabiapp.api.domain.audit;

import jakarta.persistence.*;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * Entidad de revisión personalizada para Hibernate Envers.
 *
 * <p>Extiende la revisión estándar con el DNI del usuario autenticado
 * que realizó el cambio y la IP de origen de la petición.
 * Necesario para cumplir con ENS Alto (trazabilidad completa) y RGPD Art. 30.</p>
 *
 * <p>La tabla "revinfo" es creada explícitamente en la migración V3 de Flyway.
 * Envers NO debe auto-generar esta tabla en producción.</p>
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(RehabiRevisionListener.class)
public class RehabiRevisionEntity {

    /**
     * Número secuencial de la revisión. Generado por la secuencia revinfo_rev_seq.
     * La secuencia usa allocationSize=1 para garantizar unicidad sin huecos en producción.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revinfo_seq")
    @SequenceGenerator(name = "revinfo_seq", sequenceName = "revinfo_rev_seq", allocationSize = 1)
    @RevisionNumber
    private int rev;

    /**
     * Marca de tiempo de la revisión en milisegundos epoch.
     * Gestionado automáticamente por Envers.
     */
    @RevisionTimestamp
    private long revtstmp;

    /**
     * DNI del sanitario que realizó el cambio.
     * Extraído del SecurityContext en el momento de crear la revisión.
     */
    @Column(name = "usuario", length = 20)
    private String usuario;

    /**
     * Dirección IP de origen de la petición HTTP.
     * Longitud 45 para soportar tanto IPv4 (15 chars) como IPv6 (39 chars).
     * En entornos Kubernetes detrás de un ALB de AWS, se lee del header X-Forwarded-For.
     */
    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    // --- Getters y setters ---

    public int getRev() {
        return rev;
    }

    public void setRev(int rev) {
        this.rev = rev;
    }

    public long getRevtstmp() {
        return revtstmp;
    }

    public void setRevtstmp(long revtstmp) {
        this.revtstmp = revtstmp;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public void setIpOrigen(String ipOrigen) {
        this.ipOrigen = ipOrigen;
    }
}
