package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa un número de teléfono de un sanitario.
 *
 * <p>Tabla: telefono_sanitario</p>
 * <p>Relación N:1 con Sanitario. Un sanitario puede tener múltiples teléfonos.</p>
 */
@Entity
@Table(name = "telefono_sanitario")
public class TelefonoSanitario {

    /**
     * Identificador técnico generado por la BD.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_telefono")
    private Long idTelefono;

    /**
     * Sanitario al que pertenece este teléfono. Carga diferida para evitar N+1.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dni_san", nullable = false)
    private Sanitario sanitario;

    /**
     * Número de teléfono.
     */
    @Column(name = "telefono", length = 20, nullable = false)
    private String telefono;

    // --- Getters y setters ---

    public Long getIdTelefono() {
        return idTelefono;
    }

    public void setIdTelefono(Long idTelefono) {
        this.idTelefono = idTelefono;
    }

    public Sanitario getSanitario() {
        return sanitario;
    }

    public void setSanitario(Sanitario sanitario) {
        this.sanitario = sanitario;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
}
