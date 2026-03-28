package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.Cita;
import com.rehabiapp.api.domain.entity.CitaId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

/**
 * Repositorio de dominio para la entidad Cita.
 *
 * <p>Las citas se consultan habitualmente por fecha (agenda diaria del centro)
 * o por sanitario (agenda del profesional). Ambas consultas usan paginación.</p>
 */
public interface CitaRepository extends JpaRepository<Cita, CitaId> {

    /**
     * Devuelve todas las citas de una fecha concreta, con paginación.
     * Usado para la vista de agenda diaria del centro.
     *
     * @param fecha    fecha de las citas a consultar
     * @param pageable configuración de la página
     * @return página de citas del día indicado
     */
    Page<Cita> findByIdFechaCita(LocalDate fecha, Pageable pageable);

    /**
     * Devuelve todas las citas de un sanitario concreto, con paginación.
     * Usado para la agenda personal del profesional.
     *
     * @param dniSan   DNI del sanitario
     * @param pageable configuración de la página
     * @return página de citas del sanitario indicado
     */
    Page<Cita> findByIdDniSan(String dniSan, Pageable pageable);
}
