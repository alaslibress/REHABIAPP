package com.rehabiapp.data.domain.repository;

import com.rehabiapp.data.domain.document.GameSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameSessionRepository extends MongoRepository<GameSession, String> {

    boolean existsByPatientDniAndGameIdAndSessionStart(
            String patientDni, String gameId, Instant sessionStart
    );

    // Cuenta sesiones recibidas despues de un instante dado (endpoint check-new-data).
    long countByPatientDniAndReceivedAtAfter(String patientDni, Instant since);

    // Ultima sesion recibida del paciente. Usado por endpoint last-session.
    Optional<GameSession> findTopByPatientDniOrderByReceivedAtDesc(String patientDni);

    // Ultimas N sesiones del paciente para construir el bloque de "Ultimas sesiones" del MD.
    List<GameSession> findTop20ByPatientDniOrderBySessionStartDesc(String patientDni);
}
