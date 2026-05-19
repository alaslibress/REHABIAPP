package com.rehabiapp.api.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehabiapp.api.application.dto.VideojuegoRequest;
import com.rehabiapp.api.domain.entity.Discapacidad;
import com.rehabiapp.api.domain.repository.DiscapacidadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integracion del controlador de videojuegos terapeuticos.
 *
 * <p>Cubre alta, listado, baja logica y errores de validacion.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class VideojuegoControllerIT {

    @Autowired private WebApplicationContext wac;
    @Autowired private DiscapacidadRepository discapacidadRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();

        if (!discapacidadRepository.existsById("DIS-T01")) {
            Discapacidad d = new Discapacidad();
            d.setCodDis("DIS-T01");
            d.setNombreDis("Discapacidad de prueba");
            d.setDescripcionDis("Para test");
            d.setNecesitaProtesis(false);
            discapacidadRepository.save(d);
        }
    }

    @Test
    @WithMockUser(roles = "SPECIALIST")
    void crear_comoSpecialist_retorna201() throws Exception {
        var req = new VideojuegoRequest("VJ-T01", "Juego de prueba",
                "desc", "DIS-T01", "Mano", "https://example.com");
        mockMvc.perform(post("/api/videojuegos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("VJ-T01"));
    }

    @Test
    @WithMockUser(roles = "NURSE")
    void crear_comoNurse_retorna403() throws Exception {
        var req = new VideojuegoRequest("VJ-T02", "Otro", null,
                "DIS-T01", "Mano", null);
        mockMvc.perform(post("/api/videojuegos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "NURSE")
    void listar_autenticado_retorna200() throws Exception {
        mockMvc.perform(get("/api/videojuegos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SPECIALIST")
    void desactivar_inexistente_retorna404() throws Exception {
        mockMvc.perform(delete("/api/videojuegos/{id}", 999_999L))
                .andExpect(status().isNotFound());
    }
}
