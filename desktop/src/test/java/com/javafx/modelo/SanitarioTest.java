package com.javafx.modelo;

import com.javafx.Clases.Sanitario;
import com.javafx.dto.SanitarioRequest;
import com.javafx.dto.SanitarioResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para la clase Sanitario.
 * Verifica los factory methods y la conversion a/desde DTOs de la API.
 */
class SanitarioTest {

    // ==================== desdeSanitarioResponse ====================

    @Test
    void desdeSanitarioResponse_mapeoCompleto_todosLosCamposCorrectos() {
        SanitarioResponse response = new SanitarioResponse(
            "S01", "Maria", "Lopez", "Ruiz",
            "maria@clinic.com", 12,
            true, "SPECIALIST",
            List.of("666111222", "666333444")
        );

        Sanitario s = Sanitario.desdeSanitarioResponse(response);

        assertEquals("S01", s.getDni());
        assertEquals("Maria", s.getNombre());
        assertEquals("Lopez", s.getApellido1());
        assertEquals("Ruiz", s.getApellido2());
        assertEquals("maria@clinic.com", s.getEmail());
        assertEquals(12, s.getNumPacientes());
        assertTrue(s.isActivo());
        assertEquals("SPECIALIST", s.getCargo());
        assertEquals("666111222", s.getTelefono1());
        assertEquals("666333444", s.getTelefono2());
    }

    @Test
    void desdeSanitarioResponse_camposNullOpcionales_usaDefaults() {
        SanitarioResponse response = new SanitarioResponse(
            "S02", "Pedro", "Gomez", null,
            "pedro@clinic.com", null,
            null, null,
            null
        );

        Sanitario s = Sanitario.desdeSanitarioResponse(response);

        assertEquals("", s.getApellido2());
        assertEquals(0, s.getNumPacientes());
        assertFalse(s.isActivo());
        assertEquals("", s.getCargo());
        assertEquals("", s.getTelefono1());
        assertEquals("", s.getTelefono2());
    }

    @Test
    void desdeSanitarioResponse_unTelefono_telefono2Vacio() {
        SanitarioResponse response = new SanitarioResponse(
            "S03", "Ana", "Diaz", "Vega",
            "ana@clinic.com", 5,
            true, "NURSE",
            List.of("677999000")
        );

        Sanitario s = Sanitario.desdeSanitarioResponse(response);

        assertEquals("677999000", s.getTelefono1());
        assertEquals("", s.getTelefono2());
    }

    // ==================== toSanitarioRequest ====================

    @Test
    void toSanitarioRequest_conContrasena_incluyeContrasena() {
        Sanitario s = new Sanitario("S01", "Maria", "Lopez", "Ruiz", "m@c.com", "SPECIALIST");
        s.setTelefono1("666111222");

        SanitarioRequest req = s.toSanitarioRequest("secreto123");

        assertEquals("S01", req.dniSan());
        assertEquals("Maria", req.nombreSan());
        assertEquals("Lopez", req.apellido1San());
        assertEquals("Ruiz", req.apellido2San());
        assertEquals("m@c.com", req.emailSan());
        assertEquals("SPECIALIST", req.cargo());
        assertEquals("secreto123", req.contrasena());
        assertEquals(List.of("666111222"), req.telefonos());
    }

    @Test
    void toSanitarioRequest_sinContrasena_enviaNullContrasena() {
        Sanitario s = new Sanitario("S01", "Maria", "Lopez", "Ruiz", "m@c.com", "SPECIALIST");

        SanitarioRequest req = s.toSanitarioRequest(null);

        assertNull(req.contrasena());
    }

    @Test
    void toSanitarioRequest_sinTelefonos_listaVacia() {
        Sanitario s = new Sanitario("S01", "Maria", "Lopez", "Ruiz", "m@c.com", "SPECIALIST");

        SanitarioRequest req = s.toSanitarioRequest("clave");

        assertTrue(req.telefonos().isEmpty());
    }

    // ==================== esEspecialista ====================

    @Test
    void esEspecialista_cargoSPECIALIST_devuelveTrue() {
        Sanitario s = new Sanitario("S01", "Maria", "Lopez", "Ruiz", "m@c.com", "SPECIALIST");
        assertTrue(s.esEspecialista());
    }

    @Test
    void esEspecialista_cargoSPECIALISTMayusculas_devuelveTrue() {
        Sanitario s = new Sanitario("S01", "Maria", "Lopez", "Ruiz", "m@c.com", "specialist");
        assertTrue(s.esEspecialista());
    }

    @Test
    void esEspecialista_cargoMedicoEspecialista_devuelveTrue() {
        Sanitario s = new Sanitario("S01", "Maria", "Lopez", "Ruiz", "m@c.com", "medico especialista");
        assertTrue(s.esEspecialista());
    }

    @Test
    void esEspecialista_cargoNURSE_devuelveFalse() {
        Sanitario s = new Sanitario("S01", "Carlos", "Vega", "Ruiz", "c@c.com", "NURSE");
        assertFalse(s.esEspecialista());
    }

    @Test
    void esEspecialista_cargoNull_devuelveFalse() {
        Sanitario s = new Sanitario("S01", "Carlos", "Vega", "Ruiz", "c@c.com", (String) null);
        assertFalse(s.esEspecialista());
    }

    // ==================== Persona interface ====================

    @Test
    void getApellidos_ambosApellidos_concatenaConEspacio() {
        Sanitario s = new Sanitario("S01", "Ana", "Ruiz", "Perez", "a@c.com", "NURSE");
        assertEquals("Ruiz Perez", s.getApellidos());
    }

    @Test
    void getNombreCompleto_devuelveFormatoCompleto() {
        Sanitario s = new Sanitario("S01", "Ana", "Ruiz", "Perez", "a@c.com", "NURSE");
        assertEquals("Ana Ruiz Perez", s.getNombreCompleto());
    }
}
