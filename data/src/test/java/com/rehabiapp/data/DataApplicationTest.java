package com.rehabiapp.data;

import com.rehabiapp.data.domain.repository.GameSessionRepository;
import com.rehabiapp.data.domain.repository.PatientProgressRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Test de arranque del contexto de Spring Boot.
 *
 * MongoDB se excluye via perfil "test" y se sustituye por mocks de Mockito
 * para satisfacer las dependencias de beans que requieren MongoTemplate
 * y los repositorios Spring Data MongoDB.
 */
@SpringBootTest
@ActiveProfiles("test")
class DataApplicationTest {

    @MockitoBean
    MongoTemplate mongoTemplate;

    @MockitoBean
    GameSessionRepository gameSessionRepository;

    @MockitoBean
    PatientProgressRepository patientProgressRepository;

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranca sin errores
    }
}
