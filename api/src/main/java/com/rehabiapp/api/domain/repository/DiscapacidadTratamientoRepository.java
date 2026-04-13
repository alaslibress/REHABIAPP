package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.DiscapacidadTratamiento;
import com.rehabiapp.api.domain.entity.DiscapacidadTratamientoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio para la relacion N:M entre discapacidades y tratamientos.
 *
 * <p>Necesario para gestionar los vinculos del catalogo clinico y para
 * verificar dependencias antes de eliminar discapacidades o tratamientos.</p>
 */
public interface DiscapacidadTratamientoRepository extends JpaRepository<DiscapacidadTratamiento, DiscapacidadTratamientoId> {

    List<DiscapacidadTratamiento> findByIdCodTrat(String codTrat);
    List<DiscapacidadTratamiento> findByIdCodDis(String codDis);
    boolean existsByIdCodDis(String codDis);
    boolean existsByIdCodTrat(String codTrat);
}
