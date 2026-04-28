package com.rehabiapp.data.domain.repository;

import com.rehabiapp.data.domain.document.GameSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface GameSessionRepository extends MongoRepository<GameSession, String> {

    boolean existsByPatientDniAndGameIdAndSessionStart(
            String patientDni, String gameId, Instant sessionStart
    );
}
