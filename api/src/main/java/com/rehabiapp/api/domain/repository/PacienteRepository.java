package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Busca pacientes activos por texto libre (case-insensitive) en los campos
     * dni_pac, nombre_pac, apellido1_pac, apellido2_pac, email_pac y num_ss.
     *
     * <p>Equivalente al LIKE del desktop ERP para el buscador de pacientes.
     * LOWER + LIKE se traduce a ILIKE en PostgreSQL por el dialecto de Hibernate.</p>
     *
     * @param texto    Término de búsqueda libre
     * @param pageable Configuración de paginación y ordenación
     * @return Página de pacientes activos cuyo texto coincide
     */
    @Query("""
            SELECT p FROM Paciente p
            WHERE p.activo = true
              AND (LOWER(p.dniPac)       LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(p.nombrePac)    LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(p.apellido1Pac) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(p.apellido2Pac) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(p.emailPac)     LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(p.numSs)        LIKE LOWER(CONCAT('%', :texto, '%')))
            """)
    Page<Paciente> buscarPorTexto(@Param("texto") String texto, Pageable pageable);
}
