package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.SessionReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionReportRepository extends JpaRepository<SessionReport, Long> {

    // Busqueda por mongo_id — clave de idempotencia para reintentos del pipeline
    Optional<SessionReport> findByMongoId(String mongoId);

    // Historial de sesiones del paciente ordenado por fecha descendente
    List<SessionReport> findByPacienteDniOrderByFechaSesionDesc(String pacienteDni, Pageable pageable);
}
