package com.javafx.DAO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.javafx.Clases.ApiClient;
import com.javafx.Clases.Cita;
import com.javafx.dto.CitaRequest;
import com.javafx.dto.CitaResponse;
import com.javafx.dto.PageResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DAO de citas reescrito para consumir la API REST central.
 * Ya no accede directamente a PostgreSQL.
 */
public class CitaDAO {

    private final ApiClient api;

    public CitaDAO() {
        this.api = ApiClient.getInstancia();
    }

    /** Constructor para pruebas unitarias. No usar en produccion. */
    CitaDAO(ApiClient api) {
        this.api = api;
    }

    // ==================== CONSULTAS ====================

    /**
     * Lista todas las citas del sistema (todas las fechas).
     * Usa el endpoint por fecha de los proximos 365 dias como aproximacion.
     */
    public List<Cita> listarTodas() {
        // La API no tiene endpoint "todas las citas". Usamos el listado por sanitario para la
        // vista de agenda, que es el caso de uso real del controlador de citas.
        PageResponse<CitaResponse> pagina = api.get(
            "/api/citas?page=0&size=10000",
            new TypeReference<PageResponse<CitaResponse>>() {}
        );
        return pagina.contenido().stream()
            .map(this::desdeCitaResponse)
            .toList();
    }

    /**
     * Lista las citas de una fecha concreta.
     */
    public List<Cita> listarPorFecha(LocalDate fecha) {
        PageResponse<CitaResponse> pagina = api.get(
            "/api/citas?fecha=" + fecha + "&page=0&size=10000",
            new TypeReference<PageResponse<CitaResponse>>() {}
        );
        return pagina.contenido().stream()
            .map(this::desdeCitaResponse)
            .toList();
    }

    /**
     * Lista las citas de un sanitario concreto.
     */
    public List<Cita> listarPorSanitario(String dniSan) {
        PageResponse<CitaResponse> pagina = api.get(
            "/api/citas/sanitario/" + dniSan + "?page=0&size=10000",
            new TypeReference<PageResponse<CitaResponse>>() {}
        );
        return pagina.contenido().stream()
            .map(this::desdeCitaResponse)
            .toList();
    }

    /**
     * Lista las citas de un sanitario en una fecha concreta.
     */
    public List<Cita> obtenerCitasPorSanitarioYFecha(String dniSan, LocalDate fecha) {
        return listarPorSanitario(dniSan).stream()
            .filter(c -> fecha.equals(c.getFecha()))
            .toList();
    }

    /**
     * Lista las citas de un paciente concreto.
     * Filtra localmente por no haber endpoint especifico.
     */
    public List<Cita> listarPorPaciente(String dniPac) {
        return listarTodas().stream()
            .filter(c -> dniPac.equals(c.getDniPaciente()))
            .toList();
    }

    /**
     * Comprueba si ya existe una cita del sanitario en una hora concreta.
     */
    public boolean existeCitaEnHorario(String dniSan, LocalDate fecha, LocalTime hora) {
        return obtenerCitasPorSanitarioYFecha(dniSan, fecha).stream()
            .anyMatch(c -> hora.equals(c.getHora()));
    }

    // ==================== ESCRITURA ====================

    /**
     * Inserta una nueva cita.
     */
    public void insertar(Cita cita) {
        CitaRequest request = new CitaRequest(
            cita.getDniPaciente(), cita.getDniSanitario(),
            cita.getFecha(), cita.getHora()
        );
        api.post("/api/citas", request, CitaResponse.class);
    }

    /**
     * Actualiza una cita (la API no tiene PUT de citas: se elimina y se crea).
     * @param dniPac DNI del paciente
     * @param dniSan DNI del sanitario
     * @param fechaAntigua Fecha original de la cita
     * @param horaAntigua Hora original de la cita
     * @param nuevaCita Nueva cita con los datos actualizados
     */
    public void actualizar(String dniPac, String dniSan, LocalDate fechaAntigua, LocalTime horaAntigua,
                           Cita nuevaCita) {
        eliminar(dniPac, dniSan, fechaAntigua, horaAntigua);
        insertar(nuevaCita);
    }

    /**
     * Elimina una cita por sus cuatro campos clave.
     */
    public void eliminar(String dniPac, String dniSan, LocalDate fecha, LocalTime hora) {
        String ruta = "/api/citas?dniPac=" + dniPac
            + "&dniSan=" + dniSan
            + "&fecha=" + fecha
            + "&hora=" + hora;
        api.delete(ruta);
    }

    // ==================== UTILIDADES ====================

    private Cita desdeCitaResponse(CitaResponse response) {
        return new Cita(
            response.dniPac(),
            response.dniSan(),
            response.fechaCita(),
            response.horaCita(),
            response.nombrePaciente() != null ? response.nombrePaciente() : "",
            response.nombreSanitario() != null ? response.nombreSanitario() : ""
        );
    }
}
