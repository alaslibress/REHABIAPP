package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.PacienteDiscapacidad;
import com.rehabiapp.api.domain.entity.PacienteDiscapacidadId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de dominio para las asignaciones de discapacidades a pacientes.
 *
 * <p>La consulta por DNI del paciente es el caso de uso principal,
 * para construir el perfil clínico completo del paciente.</p>
 */
public interface PacienteDiscapacidadRepository extends JpaRepository<PacienteDiscapacidad, PacienteDiscapacidadId> {

    /**
     * Devuelve todas las discapacidades asignadas a un paciente.
     *
     * @param dniPac DNI del paciente
     * @return lista de asignaciones discapacidad-paciente
     */
    List<PacienteDiscapacidad> findByIdDniPac(String dniPac);

    /**
     * Comprueba si algun paciente tiene asignada esta discapacidad.
     * Necesario para impedir la eliminacion de discapacidades en uso.
     */
    boolean existsByIdCodDis(String codDis);
}
