package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.Discapacidad;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de dominio para el catálogo de discapacidades.
 *
 * <p>Las operaciones CRUD estándar de JpaRepository son suficientes
 * para este catálogo de sólo lectura frecuente.</p>
 */
public interface DiscapacidadRepository extends JpaRepository<Discapacidad, String> {

    boolean existsByNombreDis(String nombreDis);
    boolean existsByNombreDisAndCodDisNot(String nombreDis, String codDis);
}
