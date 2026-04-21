package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.Sanitario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repositorio de dominio para la entidad Sanitario.
 *
 * <p>Las consultas filtran siempre por activo=true para evitar
 * exponer registros dados de baja lógica.</p>
 *
 * <p>Todos los métodos de listado devuelven Page para evitar
 * conjuntos de resultados ilimitados (requisito del skill springboot4-postgresql).</p>
 */
public interface SanitarioRepository extends JpaRepository<Sanitario, String> {

    /**
     * Busca un sanitario activo por su DNI.
     *
     * @param dni DNI del sanitario a buscar
     * @return Optional con el sanitario si existe y está activo
     */
    Optional<Sanitario> findByDniSanAndActivoTrue(String dni);

    /**
     * Devuelve todos los sanitarios activos con paginación.
     *
     * @param pageable configuración de la página
     * @return página de sanitarios activos
     */
    Page<Sanitario> findAllByActivoTrue(Pageable pageable);

    /**
     * Busca sanitarios activos por texto libre (case-insensitive) en los campos
     * dni_san, nombre_san, apellido1_san, apellido2_san y email_san.
     *
     * <p>Equivalente al LIKE del desktop ERP para el buscador de sanitarios.
     * LOWER + LIKE se traduce a ILIKE en PostgreSQL por el dialecto de Hibernate.</p>
     *
     * @param texto    Término de búsqueda libre
     * @param pageable Configuración de paginación y ordenación
     * @return Página de sanitarios activos cuyo texto coincide
     */
    @Query("""
            SELECT s FROM Sanitario s
            WHERE s.activo = true
              AND (LOWER(s.dniSan)       LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(s.nombreSan)    LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(s.apellido1San) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(s.apellido2San) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(s.emailSan)     LIKE LOWER(CONCAT('%', :texto, '%')))
            """)
    Page<Sanitario> buscarPorTexto(@Param("texto") String texto, Pageable pageable);
}
