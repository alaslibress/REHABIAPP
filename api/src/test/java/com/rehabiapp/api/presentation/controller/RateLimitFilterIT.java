package com.rehabiapp.api.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehabiapp.api.application.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test del filtro de rate limiting sobre {@code /api/auth/login}.
 *
 * <p>Limite configurado: 10 peticiones/minuto/IP. La undecima solicitud
 * desde el mismo origen debe devolver 429 con cabecera Retry-After.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class RateLimitFilterIT {

    @Autowired private WebApplicationContext wac;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
    }

    @Test
    void login_undecimaPeticion_retorna429() throws Exception {
        LoginRequest req = new LoginRequest("00000000A", "wrongpassword");
        String body = objectMapper.writeValueAsString(req);

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));
        }
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        // La undecima peticion debe ser bloqueada por el rate limit
        assertEquals(429, result.getResponse().getStatus());
    }
}
