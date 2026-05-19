package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.TratamientoVideojuego;
import com.rehabiapp.api.domain.entity.TratamientoVideojuegoId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de dominio para la asociacion N:M tratamiento-videojuego.
 */
public interface TratamientoVideojuegoRepository extends JpaRepository<TratamientoVideojuego, TratamientoVideojuegoId> {

    /**
     * Lista todos los videojuegos vinculados a un tratamiento.
     * EntityGraph para evitar N+1 al navegar Videojuego.discapacidad en la respuesta.
     */
    @EntityGraph(attributePaths = {"videojuego", "videojuego.discapacidad"})
    List<TratamientoVideojuego> findByIdCodTrat(String codTrat);

    /**
     * Lista todos los tratamientos vinculados a un videojuego.
     */
    List<TratamientoVideojuego> findByIdIdVideojuego(Long idVideojuego);
}
