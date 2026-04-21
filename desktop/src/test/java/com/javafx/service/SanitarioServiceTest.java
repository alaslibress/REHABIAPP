package com.javafx.service;

import com.javafx.Clases.Sanitario;
import com.javafx.DAO.SanitarioDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para SanitarioService.
 * Verifica que el servicio delega correctamente al DAO y que
 * gestiona los telefonos antes de llamar a insertar/actualizar.
 */
@ExtendWith(MockitoExtension.class)
class SanitarioServiceTest {

    @Mock
    private SanitarioDAO sanitarioDAOMock;

    private SanitarioService servicio;

    @BeforeEach
    void setUp() {
        servicio = new SanitarioService(sanitarioDAOMock);
    }

    private Sanitario sanitarioMinimo() {
        return new Sanitario("12345678A", "Pedro", "Sanchez", "Lopez",
            "pedro@test.com", "SPECIALIST");
    }

    // ==================== listarTodos ====================

    @Test
    void listarTodos_delegaAlDAO() {
        List<Sanitario> lista = List.of(sanitarioMinimo());
        when(sanitarioDAOMock.listarTodos()).thenReturn(lista);

        List<Sanitario> resultado = servicio.listarTodos();

        assertEquals(1, resultado.size());
        verify(sanitarioDAOMock).listarTodos();
    }

    // ==================== buscarPorDni ====================

    @Test
    void buscarPorDni_encontrado_retornaOptionalConSanitario() {
        Sanitario s = sanitarioMinimo();
        when(sanitarioDAOMock.buscarPorDni("12345678A")).thenReturn(s);

        Optional<Sanitario> resultado = servicio.buscarPorDni("12345678A");

        assertTrue(resultado.isPresent());
        assertEquals("12345678A", resultado.get().getDni());
    }

    @Test
    void buscarPorDni_noEncontrado_retornaOptionalVacio() {
        when(sanitarioDAOMock.buscarPorDni("NOEXISTE")).thenReturn(null);

        Optional<Sanitario> resultado = servicio.buscarPorDni("NOEXISTE");

        assertFalse(resultado.isPresent());
    }

    // ==================== insertar ====================

    @Test
    void insertar_conTelefonos_setearTelefonosYDelegaAlDAO() {
        Sanitario s = sanitarioMinimo();
        servicio.insertar(s, "secreta", "600111222", "600333444");

        assertEquals("600111222", s.getTelefono1());
        assertEquals("600333444", s.getTelefono2());
        verify(sanitarioDAOMock).insertar(s, "secreta");
    }

    @Test
    void insertar_telefonosNull_losConvierteAVacio() {
        Sanitario s = sanitarioMinimo();
        servicio.insertar(s, "secreta", null, null);

        assertEquals("", s.getTelefono1());
        assertEquals("", s.getTelefono2());
        verify(sanitarioDAOMock).insertar(s, "secreta");
    }

    @Test
    void insertar_sinTelefonos_delegaDirectamente() {
        Sanitario s = sanitarioMinimo();
        servicio.insertar(s, "secreta");

        verify(sanitarioDAOMock).insertar(s, "secreta");
    }

    // ==================== actualizar ====================

    @Test
    void actualizar_conTelefonos_setearTelefonosYDelegaAlDAO() {
        Sanitario s = sanitarioMinimo();
        servicio.actualizar(s, "12345678A", "611000111", "");

        assertEquals("611000111", s.getTelefono1());
        assertEquals("", s.getTelefono2());
        verify(sanitarioDAOMock).actualizar(s, "12345678A");
    }

    @Test
    void actualizar_sinTelefonos_delegaDirectamente() {
        Sanitario s = sanitarioMinimo();
        servicio.actualizar(s, "12345678A");

        verify(sanitarioDAOMock).actualizar(s, "12345678A");
    }

    // ==================== eliminar ====================

    @Test
    void eliminar_delegaAlDAO() {
        servicio.eliminar("12345678A");
        verify(sanitarioDAOMock).eliminar("12345678A");
    }

    // ==================== existeDni ====================

    @Test
    void existeDni_delegaAlDAO() {
        when(sanitarioDAOMock.existeDni("12345678A")).thenReturn(true);
        assertTrue(servicio.existeDni("12345678A"));
    }

    // ==================== cambiarContrasena ====================

    @Test
    void cambiarContrasena_delegaAlDAO() {
        servicio.cambiarContrasena("12345678A", "nuevaPass");
        verify(sanitarioDAOMock).cambiarContrasena("12345678A", "nuevaPass");
    }
}
