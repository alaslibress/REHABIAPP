package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.Localidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocalidadRepository extends JpaRepository<Localidad, String> {

    /**
     * Busca una localidad por su nombre exacto.
     * Util para el patron find-or-create al insertar direcciones.
     */
    Optional<Localidad> findByNombreLocalidad(String nombreLocalidad);
}
