package com.rehabiapp.api.domain.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa un número de teléfono de un paciente.
 *
 * <p>Tabla: telefono_paciente</p>
 * <p>Relación N:1 con Paciente. Un paciente puede tener múltiples teléfonos.</p>
 */
@Entity
@Table(name = "telefono_paciente")
public class TelefonoPaciente {

    /**
     * Identificador técnico generado por la BD.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_telefono")
    private Long idTelefono;

    /**
     * Paciente al que pertenece este teléfono. Carga diferida para evitar N+1.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dni_pac", nullable = false)
    private Paciente paciente;

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

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
}
