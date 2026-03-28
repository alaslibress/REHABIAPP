package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.Tratamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de dominio para el catálogo de tratamientos.
 *
 * <p>Incluye consulta JPQL para filtrar tratamientos por discapacidad,
 * necesaria para la selección terapéutica por perfil del paciente.</p>
 */
public interface TratamientoRepository extends JpaRepository<Tratamiento, String> {

    /**
     * Devuelve todos los tratamientos asociados a una discapacidad concreta.
     *
     * <p>La consulta JPQL usa parámetro nombrado para prevenir inyección SQL.
     * JOIN implícito sobre DiscapacidadTratamiento sin N+1 al ser una sola query.</p>
     *
     * @param codDis código de la discapacidad
     * @return lista de tratamientos aplicables a esa discapacidad
     */
    @Query("SELECT t FROM Tratamiento t JOIN DiscapacidadTratamiento dt "
            + "ON dt.id.codTrat = t.codTrat WHERE dt.id.codDis = :codDis")
    List<Tratamiento> findByCodDis(@Param("codDis") String codDis);
}
