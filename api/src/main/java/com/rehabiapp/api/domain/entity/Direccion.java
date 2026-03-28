package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa una dirección postal.
 *
 * <p>Tabla: direccion</p>
 * <p>Referenciada por pacientes y sanitarios. Se asocia a un código postal
 * que a su vez apunta a su localidad (N:1 con CodigoPostal).</p>
 */
@Entity
@Table(name = "direccion")
public class Direccion {

    /**
     * Identificador técnico generado por la secuencia de la BD.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_direccion")
    private Long idDireccion;

    /**
     * Nombre de la calle o vía.
     */
    @Column(name = "calle", length = 200, nullable = false)
    private String calle;

    /**
     * Número del inmueble en la vía pública.
     */
    @Column(name = "numero", length = 20)
    private String numero;

    /**
     * Piso o planta del inmueble.
     */
    @Column(name = "piso", length = 20)
    private String piso;

    /**
     * Código postal de la dirección. Carga diferida para evitar N+1.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cp", nullable = false)
    private CodigoPostal codigoPostal;

    // --- Getters y setters ---

    public Long getIdDireccion() {
        return idDireccion;
    }

    public void setIdDireccion(Long idDireccion) {
        this.idDireccion = idDireccion;
    }

    public String getCalle() {
        return calle;
    }

    public void setCalle(String calle) {
        this.calle = calle;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getPiso() {
        return piso;
    }

    public void setPiso(String piso) {
        this.piso = piso;
    }

    public CodigoPostal getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(CodigoPostal codigoPostal) {
        this.codigoPostal = codigoPostal;
    }
}
