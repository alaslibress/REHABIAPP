package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.SanitarioResponse;
import com.rehabiapp.api.application.mapper.SanitarioMapper;
import com.rehabiapp.api.domain.entity.Sanitario;
import com.rehabiapp.api.domain.enums.AccionAuditoria;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import com.rehabiapp.api.domain.repository.SanitarioRepository;
import com.rehabiapp.api.infrastructure.audit.AuditService;
import com.rehabiapp.api.infrastructure.security.PasswordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para SanitarioService.
 *
 * <p>Verifica la lógica de negocio del servicio usando Mockito para aislar
 * las dependencias (repositorio, mapper, auditoría) sin necesidad de BD.</p>
 *
 * <p>Cubre: consulta por DNI, soft delete y manejo de errores cuando el
 * sanitario no existe.</p>
 */
@ExtendWith(MockitoExtension.class)
class SanitarioServiceTest {

    @Mock
    private SanitarioRepository sanitarioRepository;

    @Mock
    private SanitarioMapper sanitarioMapper;

    @Mock
    private PasswordService passwordService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private SanitarioService sanitarioService;

    @Test
    void obtenerPorDni_cuandoExiste_retornaResponse() {
        // Preparación: repositorio devuelve un sanitario existente
        Sanitario sanitario = crearSanitarioMock();
        SanitarioResponse expectedResponse = crearResponseMock();

        when(sanitarioRepository.findByDniSanAndActivoTrue("12345678A"))
                .thenReturn(Optional.of(sanitario));
        when(sanitarioMapper.toResponse(sanitario)).thenReturn(expectedResponse);

        // Ejecución
        SanitarioResponse result = sanitarioService.obtenerPorDni("12345678A");

        // Verificaciones
        assertEquals(expectedResponse, result);
        // Debe registrar la lectura en el log de auditoría (Ley 41/2002)
        verify(auditService).registrar(
                eq(AccionAuditoria.LEER),
                eq("sanitario"),
                eq("12345678A"),
                anyString()
        );
    }

    @Test
    void obtenerPorDni_cuandoNoExiste_lanzaExcepcion() {
        // El repositorio no encuentra el sanitario — debe lanzar excepción de dominio
        when(sanitarioRepository.findByDniSanAndActivoTrue("99999999Z"))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> sanitarioService.obtenerPorDni("99999999Z")
        );
    }

    @Test
    void eliminar_realizaSoftDelete() {
        // Preparación: sanitario activo en la BD
        Sanitario sanitario = crearSanitarioMock();
        when(sanitarioRepository.findByDniSanAndActivoTrue("12345678A"))
                .thenReturn(Optional.of(sanitario));
        when(sanitarioRepository.save(any())).thenReturn(sanitario);

        // Ejecución del soft delete
        sanitarioService.eliminar("12345678A");

        // El sanitario debe quedar con activo=false y fechaBaja establecida
        assertFalse(sanitario.isActivo(), "El sanitario debe quedar inactivo tras la baja lógica");
        assertNotNull(sanitario.getFechaBaja(), "Debe registrarse la fecha de baja");

        // Debe registrar la operación en el log de auditoría
        verify(auditService).registrar(
                eq(AccionAuditoria.ELIMINAR),
                eq("sanitario"),
                eq("12345678A"),
                anyString()
        );
    }

    // --- Métodos auxiliares para crear objetos de prueba ---

    /**
     * Crea un sanitario mock con datos de prueba mínimos.
     */
    private Sanitario crearSanitarioMock() {
        Sanitario s = new Sanitario();
        s.setDniSan("12345678A");
        s.setNombreSan("Juan");
        s.setApellido1San("García");
        s.setEmailSan("juan@test.com");
        s.setActivo(true);
        s.setContrasenaSan("$2a$12$hashedPassword");
        return s;
    }

    /**
     * Crea un SanitarioResponse mock con los datos de prueba correspondientes.
     */
    private SanitarioResponse crearResponseMock() {
        return new SanitarioResponse(
                "12345678A",
                "Juan",
                "García",
                null,
                "juan@test.com",
                0,
                true,
                "SPECIALIST",
                List.of()
        );
    }
}
