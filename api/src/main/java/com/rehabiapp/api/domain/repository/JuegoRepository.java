package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.Juego;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Juego.
 */
public interface JuegoRepository extends JpaRepository<Juego, String> {

    /** Lista juegos activos filtrados por articulacion. */
    List<Juego> findByArticulacionIdArticulacionAndActivoTrue(Integer idArticulacion);

    /** Lista todos los juegos activos del catalogo. */
    List<Juego> findByActivoTrue();

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndCodJuegoNot(String nombre, String codJuego);
}
