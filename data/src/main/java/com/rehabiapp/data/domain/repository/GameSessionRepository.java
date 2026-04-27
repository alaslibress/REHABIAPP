package com.rehabiapp.data.domain.repository;

import com.rehabiapp.data.domain.model.GameSession;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repositorio de sesiones de juego. Indices se crean via anotaciones en la entidad.
 */
public interface GameSessionRepository extends MongoRepository<GameSession, String> {

    Optional<GameSession> findBySessionId(String sessionId);

    boolean existsBySessionId(String sessionId);
}
