package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.Sanitario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
