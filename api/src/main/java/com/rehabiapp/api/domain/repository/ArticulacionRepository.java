package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.Articulacion;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para la entidad Articulacion.
 * Proporciona acceso de solo lectura al catalogo de articulaciones.
 */
public interface ArticulacionRepository extends JpaRepository<Articulacion, Integer> {

    boolean existsByCodigo(String codigo);
}
