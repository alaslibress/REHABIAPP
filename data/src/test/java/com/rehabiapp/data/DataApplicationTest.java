package com.rehabiapp.data;

import com.rehabiapp.data.domain.repository.GameSessionRepository;
import com.rehabiapp.data.domain.repository.LevelStatisticsRepository;
import com.rehabiapp.data.domain.repository.PatientProgressRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Test de arranque del contexto de Spring Boot.
 * MongoDB excluido via perfil "test"; repositorios mockeados para que
 * GameSessionIngestService pueda instanciarse sin infraestructura real.
 */
@SpringBootTest
@ActiveProfiles("test")
class DataApplicationTest {

    @MockitoBean
    GameSessionRepository gameSessionRepository;

    @MockitoBean
    PatientProgressRepository patientProgressRepository;

    @MockitoBean
    LevelStatisticsRepository levelStatisticsRepository;

    @MockitoBean
    MongoTemplate mongoTemplate;


    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranca sin errores
    }
}
