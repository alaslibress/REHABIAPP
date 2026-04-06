package com.javafx.modelo;

import com.javafx.Clases.Paciente;
import com.javafx.dto.PacienteRequest;
import com.javafx.dto.PacienteResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para la clase Paciente.
 * Verifica los factory methods y la conversion a/desde DTOs de la API.
 */
class PacienteTest {

    // ==================== desdePacienteResponse ====================

    @Test
    void desdePacienteResponse_mapeoCompleto_todosLosCamposCorrectos() {
        PacienteResponse response = new PacienteResponse(
            "12345678A", "S01", "Juan", "Garcia", "Lopez",
            35, "juan@test.com", "SS123456",
            "M", LocalDate.of(1989, 5, 15),
            true, true,
            "Penicilina", "Diabetes tipo 2", "Metformina",
            true,
            List.of("600111222", "600333444")
        );

        Paciente p = Paciente.desdePacienteResponse(response);

        assertEquals("12345678A", p.getDni());
        assertEquals("S01", p.getDniSanitario());
        assertEquals("Juan", p.getNombre());
        assertEquals("Garcia", p.getApellido1());
        assertEquals("Lopez", p.getApellido2());
        assertEquals(35, p.getEdad());
        assertEquals("juan@test.com", p.getEmail());
        assertEquals("SS123456", p.getNumSS());
        assertEquals("M", p.getSexo());
        assertEquals(LocalDate.of(1989, 5, 15), p.getFechaNacimiento());
        assertTrue(p.isProtesis());
        assertTrue(p.isActivo());
        assertEquals("Penicilina", p.getAlergias());
        assertEquals("Diabetes tipo 2", p.getAntecedentes());
        assertEquals("Metformina", p.getMedicacionActual());
        assertTrue(p.isConsentimientoRgpd());
        assertEquals("600111222", p.getTelefono1());
        assertEquals("600333444", p.getTelefono2());
    }

    @Test
    void desdePacienteResponse_camposNullOpcionales_usaDefaults() {
        PacienteResponse response = new PacienteResponse(
            "12345678B", "S01", "Ana", "Martinez", null,
            null, "ana@test.com", "SS999",
            null, null,
            null, null,
            null, null, null,
            null,
            null
        );

        Paciente p = Paciente.desdePacienteResponse(response);

        assertEquals("", p.getApellido2());
        assertEquals(0, p.getEdad());
        assertEquals("", p.getSexo());
        assertNull(p.getFechaNacimiento());
        assertFalse(p.isProtesis());
        assertFalse(p.isActivo());
        assertEquals("", p.getAlergias());
        assertEquals("", p.getAntecedentes());
        assertEquals("", p.getMedicacionActual());
        assertFalse(p.isConsentimientoRgpd());
        assertEquals("", p.getTelefono1());
        assertEquals("", p.getTelefono2());
    }

    @Test
    void desdePacienteResponse_unTelefono_telefono2Vacio() {
        PacienteResponse response = new PacienteResponse(
            "12345678C", "S01", "Luis", "Perez", "Ruiz",
            40, "luis@test.com", "SS777",
            "M", null, false, true,
            null, null, null, false,
            List.of("600555666")
        );

        Paciente p = Paciente.desdePacienteResponse(response);

        assertEquals("600555666", p.getTelefono1());
        assertEquals("", p.getTelefono2());
    }

    // ==================== toPacienteRequest ====================

    @Test
    void toPacienteRequest_dosTelefonos_incluyeAmbos() {
        Paciente p = new Paciente("12345678A", "Juan", "Garcia", "Lopez",
            35, "juan@test.com", "SS123", true, "S01");
        p.setSexo("M");
        p.setFechaNacimiento(LocalDate.of(1989, 5, 15));
        p.setAlergias("Penicilina");
        p.setConsentimientoRgpd(true);
        p.setTelefono1("600111222");
        p.setTelefono2("600333444");

        PacienteRequest req = p.toPacienteRequest();

        assertEquals("12345678A", req.dniPac());
        assertEquals("S01", req.dniSan());
        assertEquals("Juan", req.nombrePac());
        assertEquals("Garcia", req.apellido1Pac());
        assertEquals("Lopez", req.apellido2Pac());
        assertEquals(35, req.edadPac());
        assertEquals("juan@test.com", req.emailPac());
        assertEquals("SS123", req.numSs());
        assertEquals("M", req.sexo());
        assertEquals(LocalDate.of(1989, 5, 15), req.fechaNacimiento());
        assertTrue(req.protesis());
        assertEquals("Penicilina", req.alergias());
        assertTrue(req.consentimientoRgpd());
        assertEquals(List.of("600111222", "600333444"), req.telefonos());
    }

    @Test
    void toPacienteRequest_sinTelefonos_listaVacia() {
        Paciente p = new Paciente("12345678A", "Juan", "Garcia", "Lopez",
            30, "j@t.com", "SS1", false, "S01");

        PacienteRequest req = p.toPacienteRequest();

        assertTrue(req.telefonos().isEmpty());
    }

    @Test
    void toPacienteRequest_telefono1VacioTelefono2Relleno_soloTelefono2() {
        Paciente p = new Paciente("12345678A", "Juan", "Garcia", "Lopez",
            30, "j@t.com", "SS1", false, "S01");
        p.setTelefono1("");
        p.setTelefono2("699000111");

        PacienteRequest req = p.toPacienteRequest();

        assertEquals(List.of("699000111"), req.telefonos());
    }

    // ==================== Persona interface ====================

    @Test
    void getApellidos_ambosApellidos_concatenaConEspacio() {
        Paciente p = new Paciente("1A", "Juan", "Garcia", "Lopez", 30, "j@t.com", "SS1", "S01");
        assertEquals("Garcia Lopez", p.getApellidos());
    }

    @Test
    void getApellidos_sinSegundoApellido_devuelveSoloPrimero() {
        Paciente p = new Paciente("1A", "Juan", "Garcia", "", 30, "j@t.com", "SS1", "S01");
        assertEquals("Garcia", p.getApellidos());
    }

    @Test
    void getNombreCompleto_devuelveNombreYApellidos() {
        Paciente p = new Paciente("1A", "Juan", "Garcia", "Lopez", 30, "j@t.com", "SS1", "S01");
        assertEquals("Juan Garcia Lopez", p.getNombreCompleto());
    }
}
