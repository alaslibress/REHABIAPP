package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.application.dto.PacienteTratamientoResponse;
import com.rehabiapp.api.domain.entity.PacienteTratamiento;
import com.rehabiapp.api.domain.entity.PacienteTratamientoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de dominio para la visibilidad de tratamientos por paciente.
 *
 * <p>Permite al especialista controlar que tratamientos ve el paciente
 * en la aplicacion movil sin eliminar las asignaciones clinicas.</p>
 */
public interface PacienteTratamientoRepository extends JpaRepository<PacienteTratamiento, PacienteTratamientoId> {

    /**
     * Devuelve todos los tratamientos asignados a un paciente (visibles y ocultos).
     *
     * @param dniPac DNI del paciente
     * @return lista de asignaciones tratamiento-paciente
     */
    List<PacienteTratamiento> findByIdDniPac(String dniPac);

    /**
     * Devuelve los tratamientos del paciente enriquecidos con codDis, idNivel y tienePdf.
     *
     * <p>Resuelve la discapacidad principal del tratamiento uniendo paciente_tratamiento
     * con discapacidad_tratamiento filtrado por las discapacidades del propio paciente.
     * Si un tratamiento esta vinculado a varias discapacidades del paciente, devuelve
     * la primera segun orden alfabetico del codigo (resultado determinista).</p>
     */
    @Query("""
            SELECT new com.rehabiapp.api.application.dto.PacienteTratamientoResponse(
                pt.id.dniPac,
                pt.id.codTrat,
                pt.tratamiento.nombreTrat,
                pt.visible,
                pt.fechaAsignacion,
                (SELECT MIN(pd.id.codDis)
                 FROM PacienteDiscapacidad pd
                 JOIN DiscapacidadTratamiento dt ON dt.id.codDis = pd.id.codDis
                                               AND dt.id.codTrat = pt.id.codTrat
                 WHERE pd.id.dniPac = pt.id.dniPac),
                pt.tratamiento.nivel.idNivel,
                CASE WHEN pt.tratamiento.archivoPdf IS NOT NULL THEN TRUE ELSE FALSE END
            )
            FROM PacienteTratamiento pt
            WHERE pt.id.dniPac = :dniPac
            ORDER BY pt.id.codTrat
            """)
    List<PacienteTratamientoResponse> findEnriquecidoByDniPac(@Param("dniPac") String dniPac);

    /**
     * Comprueba si algun paciente tiene asignado este tratamiento.
     * Necesario para impedir la eliminacion de tratamientos en uso.
     */
    boolean existsByIdCodTrat(String codTrat);
}
