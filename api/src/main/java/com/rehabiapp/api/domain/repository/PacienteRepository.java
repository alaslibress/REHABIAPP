package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de dominio para la entidad Paciente.
 *
 * <p>Los pacientes NUNCA se eliminan físicamente (soft delete).
 * Las consultas activas filtran siempre por activo=true.
 * La retención de datos post-baja (5 años, Ley 41/2002) se gestiona
 * a nivel de servicio, no en este repositorio.</p>
 */
public interface PacienteRepository extends JpaRepository<Paciente, String> {

    /**
     * Busca un paciente activo por su DNI.
     *
     * @param dni DNI del paciente a buscar
     * @return Optional con el paciente si existe y está activo
     */
    Optional<Paciente> findByDniPacAndActivoTrue(String dni);

    /**
     * Devuelve todos los pacientes asignados a un sanitario (activos e inactivos).
     * Útil para el listado histórico del especialista.
     *
     * <p>Spring Data JPA navega la relación: paciente.sanitario.dniSan</p>
     *
     * @param dniSan DNI del sanitario responsable
     * @param pageable configuración de la página
     * @return página de pacientes del sanitario
     */
    Page<Paciente> findAllBySanitarioDniSan(String dniSan, Pageable pageable);

    /**
     * Devuelve todos los pacientes activos con paginación.
     * Sólo accesible para SPECIALIST (restricción RBAC en la capa de servicio).
     *
     * @param pageable configuración de la página
     * @return página de pacientes activos
     */
    Page<Paciente> findAllByActivoTrue(Pageable pageable);
}
