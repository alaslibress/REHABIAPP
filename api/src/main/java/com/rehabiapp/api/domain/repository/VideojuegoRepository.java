package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.Videojuego;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para los videojuegos terapeuticos del catalogo.
 *
 * <p>Las consultas usan @EntityGraph para evitar N+1 al navegar la
 * relacion con Discapacidad en las respuestas REST.</p>
 */
public interface VideojuegoRepository extends JpaRepository<Videojuego, Long> {

    /**
     * Lista paginada de videojuegos activos ordenados por nombre.
     */
    @EntityGraph(attributePaths = "discapacidad")
    Page<Videojuego> findByActivoTrueOrderByNombreAsc(Pageable pageable);

    /**
     * Lista de videojuegos activos para una discapacidad concreta.
     */
    @EntityGraph(attributePaths = "discapacidad")
    List<Videojuego> findByDiscapacidadCodDisAndActivoTrueOrderByNombreAsc(String codDis);

    /**
     * Busqueda por codigo unico (validacion de unicidad en alta y actualizacion).
     */
    Optional<Videojuego> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdVideojuegoNot(String codigo, Long idVideojuego);

    @EntityGraph(attributePaths = "discapacidad")
    Optional<Videojuego> findWithDiscapacidadByIdVideojuego(Long idVideojuego);
}
