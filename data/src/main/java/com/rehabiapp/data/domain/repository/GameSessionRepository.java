package com.rehabiapp.data.domain.repository;

import com.rehabiapp.data.domain.document.GameSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameSessionRepository extends MongoRepository<GameSession, String> {

    boolean existsByPatientDniAndGameIdAndSessionStart(
            String patientDni, String gameId, Instant sessionStart
    );

    // Idempotencia con metricsHash — evita duplicados en reintentos Unity
    Optional<GameSession> findByPatientDniAndGameIdAndSessionStartAndMetricsHash(
            String patientDni, String gameId, Instant sessionStart, String metricsHash
    );

    // Cuenta sesiones recibidas despues de un instante dado (endpoint check-new-data)
    long countByPatientDniAndReceivedAtAfter(String patientDni, Instant since);

    // Ultima sesion recibida del paciente — endpoint last-session
    Optional<GameSession> findTopByPatientDniOrderByReceivedAtDesc(String patientDni);

    // Ultimas N sesiones para bloque "Ultimas sesiones" del MD del paciente
    List<GameSession> findTop20ByPatientDniOrderBySessionStartDesc(String patientDni);

    // Sesiones con reporte pendiente de escritura en Postgres — ordena por antiguedad
    List<GameSession> findTop50ByReportStatusOrderByReceivedAtAsc(String reportStatus);

    // Actualiza solo el campo reportStatus + reportLastError (sin reemplazar el documento)
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'reportStatus': ?1, 'reportLastError': ?2, 'reportAttempts': 0 } }")
    void actualizarReportStatus(String id, String status, String lastError);

    // Incrementa el contador de intentos y registra el ultimo error
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'reportAttempts': 1 }, '$set': { 'reportLastError': ?1 } }")
    void incrementarIntentos(String id, String lastError);
}
