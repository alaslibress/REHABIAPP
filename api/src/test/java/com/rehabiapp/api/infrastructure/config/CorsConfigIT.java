package com.rehabiapp.api.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integracion para la configuracion CORS de la API.
 *
 * <p>Verifica que el preflight {@code OPTIONS} desde el origen del bucket S3 del juego
 * Unity WebGL es aceptado y devuelve los headers {@code Access-Control-Allow-*} esperados.
 * Sin estos tests, una regresion en CORS romperia silenciosamente la integracion con el juego.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class CorsConfigIT {

    private static final String ORIGEN_JUEGO = "http://s3-bucket-rehabiapp-piano-640681720314.s3-website-us-east-1.amazonaws.com";

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    @Test
    void preflight_desdeBucketS3_aceptaPostSesionJuego() throws Exception {
        mockMvc.perform(options("/api/telemetria/sesion-juego")
                        .header("Origin", ORIGEN_JUEGO)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEN_JUEGO))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void preflight_desdeOrigenNoPermitido_retorna403() throws Exception {
        mockMvc.perform(options("/api/telemetria/sesion-juego")
                        .header("Origin", "http://atacante.invalido.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
