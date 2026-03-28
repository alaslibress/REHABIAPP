package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.NivelProgresion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de dominio para el catálogo de niveles de progresión clínica.
 *
 * <p>El catálogo se devuelve siempre ordenado por el campo "orden"
 * para garantizar la presentación correcta de la secuencia terapéutica.</p>
 */
public interface NivelProgresionRepository extends JpaRepository<NivelProgresion, Integer> {

    /**
     * Devuelve todos los niveles de progresión ordenados de menor a mayor.
     *
     * @return lista de niveles ordenados por su posición terapéutica
     */
    List<NivelProgresion> findAllByOrderByOrdenAsc();
}
