package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.PacienteTratamiento;
import com.rehabiapp.api.domain.entity.PacienteTratamientoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de dominio para la visibilidad de tratamientos por paciente.
 *
 * <p>Permite al especialista controlar qué tratamientos ve el paciente
 * en la aplicación móvil sin eliminar las asignaciones clínicas.</p>
 */
public interface PacienteTratamientoRepository extends JpaRepository<PacienteTratamiento, PacienteTratamientoId> {

    /**
     * Devuelve todos los tratamientos asignados a un paciente (visibles y ocultos).
     *
     * @param dniPac DNI del paciente
     * @return lista de asignaciones tratamiento-paciente
     */
    List<PacienteTratamiento> findByIdDniPac(String dniPac);
}
