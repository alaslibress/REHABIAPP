package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.Cita;
import com.rehabiapp.api.domain.entity.CitaId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Repositorio de dominio para la entidad Cita.
 *
 * <p>Las citas se consultan habitualmente por fecha (agenda diaria del centro)
 * o por sanitario (agenda del profesional). Ambas consultas usan paginación.</p>
 */
public interface CitaRepository extends JpaRepository<Cita, CitaId> {

    /**
     * Devuelve todas las citas de una fecha concreta, con paginación.
     */
    Page<Cita> findByIdFechaCita(LocalDate fecha, Pageable pageable);

    /**
     * Devuelve todas las citas de un sanitario concreto, con paginación.
     */
    Page<Cita> findByIdDniSan(String dniSan, Pageable pageable);

    /**
     * Devuelve todas las citas de un paciente (pasadas y futuras), con paginacion.
     * Usado por la app movil para mostrar el historial completo del paciente.
     */
    Page<Cita> findByIdDniPac(String dniPac, Pageable pageable);

    /**
     * Devuelve las citas futuras de un paciente ordenadas por fecha y hora ascendente.
     * El primer elemento de la lista corresponde a la proxima cita del paciente.
     */
    @Query("""
            SELECT c FROM Cita c
            WHERE c.id.dniPac = :dni
              AND (c.id.fechaCita > :hoy
                   OR (c.id.fechaCita = :hoy AND c.id.horaCita >= :ahora))
            ORDER BY c.id.fechaCita ASC, c.id.horaCita ASC
            """)
    List<Cita> findProximasByPaciente(@Param("dni") String dni,
                                      @Param("hoy") LocalDate hoy,
                                      @Param("ahora") LocalTime ahora,
                                      Pageable pageable);
}
