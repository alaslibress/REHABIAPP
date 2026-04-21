package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, Long> {

    /**
     * Busca una direccion existente que coincida exactamente con calle/numero/piso/cp.
     * Util para reutilizar registros en lugar de duplicar.
     */
    Optional<Direccion> findByCalleAndNumeroAndPisoAndCodigoPostalCp(
            String calle, String numero, String piso, String cp);
}
