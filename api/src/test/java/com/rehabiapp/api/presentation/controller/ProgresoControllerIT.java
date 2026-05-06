package com.rehabiapp.api.presentation.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests RBAC del controlador de progreso del paciente.
 *
 * <p>Solo se verifica el control de acceso por rol — el flujo HTTP completo
 * con `/data` se prueba en TestSprite con un mock de pipeline.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ProgresoControllerIT {

    @Autowired private WebApplicationContext wac;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
    }

    @Test
    @WithMockUser(roles = "NURSE")
    void regenerar_comoNurse_retorna403() throws Exception {
        mockMvc.perform(post("/api/pacientes/12345678Z/progreso/markdown/regenerar"))
                .andExpect(status().isForbidden());
    }
}
