package com.rehabiapp.api.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehabiapp.api.application.dto.LoginRequest;
import com.rehabiapp.api.application.dto.PacienteLoginRequest;
import com.rehabiapp.api.application.dto.RefreshRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para AuthController.
 *
 * <p>Usa el perfil "test" con H2 en memoria y Flyway desactivado para
 * no depender de una instancia PostgreSQL real en el entorno de CI.</p>
 *
 * <p>Se verifican escenarios de autenticación fallida y protección de endpoints.</p>
 *
 * <p>Nota de compatibilidad: Spring Boot 4.0.5 eliminó @AutoConfigureMockMvc de
 * spring-boot-test-autoconfigure. Se construye MockMvc manualmente con
 * MockMvcBuilders.webAppContextSetup() para mantener la integración con Spring Security.
 * ObjectMapper se instancia directamente porque no está registrado como bean en el
 * contexto de test sin la auto-configuración web completa.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    // ObjectMapper instanciado directamente — no disponible como bean en el contexto de test
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void configurarMockMvc() {
        // Construir MockMvc manualmente aplicando el filtro de Spring Security
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    @Test
    void login_conCredencialesInvalidas_retorna403() throws Exception {
        // Credenciales inexistentes — el servicio no encontrará el sanitario
        // y devolverá 403 Forbidden (credenciales incorrectas)
        LoginRequest request = new LoginRequest("99999999Z", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_conBodyVacio_retorna400() throws Exception {
        // Un body vacío sin los campos obligatorios (@NotBlank) debe devolver 400 Bad Request
        // por validación de Jakarta Bean Validation
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginPaciente_conCredencialesInvalidas_retorna403() throws Exception {
        // Paciente inexistente — PacienteAuthApplicationService lanza AccesoNoPermitidoException → 403
        PacienteLoginRequest request = new PacienteLoginRequest("99999999X", "wrongpassword");

        mockMvc.perform(post("/api/auth/login-paciente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginPaciente_conBodyVacio_retorna400() throws Exception {
        // Body vacio sin campos obligatorios (@NotBlank) debe devolver 400
        mockMvc.perform(post("/api/auth/login-paciente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshPaciente_conRefreshTokenInvalido_retorna401() throws Exception {
        // Un token malformado dispara JwtException → GlobalExceptionHandler → 401 Unauthorized
        RefreshRequest request = new RefreshRequest("esto.no.es.un.jwt.valido");

        mockMvc.perform(post("/api/auth/refresh-paciente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshPaciente_conBodyVacio_retorna400() throws Exception {
        // Body vacio sin el campo refreshToken (@NotBlank) debe devolver 400
        mockMvc.perform(post("/api/auth/refresh-paciente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void endpointProtegido_sinToken_retorna401O403() throws Exception {
        // Los endpoints que requieren autenticación deben rechazar peticiones sin JWT
        // Spring Security puede devolver 401 Unauthorized o 403 Forbidden según configuración
        mockMvc.perform(get("/api/sanitarios"))
                .andExpect(result ->
                        assertTrue(
                                result.getResponse().getStatus() == 401 ||
                                result.getResponse().getStatus() == 403,
                                "Debe devolver 401 o 403 para peticiones sin JWT"
                        )
                );
    }
}
