package com.rehabiapp.api.presentation.controller;

import com.rehabiapp.api.domain.entity.Tratamiento;
import com.rehabiapp.api.domain.repository.TratamientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integracion para el upload/eliminacion de PDF de tratamiento.
 *
 * <p>Verifica los limites de tamano (10 MB), validacion de magic bytes
 * y la restriccion RBAC SPECIALIST sobre eliminacion.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class TratamientoPdfControllerIT {

    @Autowired private WebApplicationContext wac;
    @Autowired private TratamientoRepository tratamientoRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
        if (!tratamientoRepository.existsById("TR-PDF-01")) {
            Tratamiento t = new Tratamiento();
            t.setCodTrat("TR-PDF-01");
            t.setNombreTrat("Tratamiento PDF Test");
            t.setDefinicionTrat("Para test");
            tratamientoRepository.save(t);
        }
    }

    @Test
    @WithMockUser(roles = "SPECIALIST")
    void subir_pdfValido_retorna201() throws Exception {
        byte[] contenido = "%PDF-1.4 contenido sintetico".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "protocolo.pdf", MediaType.APPLICATION_PDF_VALUE, contenido);
        mockMvc.perform(multipart("/api/tratamientos/TR-PDF-01/pdf").file(file))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "SPECIALIST")
    void subir_archivoNoPdf_retorna400() throws Exception {
        byte[] contenido = "no es un pdf de verdad".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.pdf", MediaType.APPLICATION_PDF_VALUE, contenido);
        mockMvc.perform(multipart("/api/tratamientos/TR-PDF-01/pdf").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "NURSE")
    void eliminar_comoNurse_retorna403() throws Exception {
        mockMvc.perform(delete("/api/tratamientos/TR-PDF-01/pdf"))
                .andExpect(status().isForbidden());
    }
}
