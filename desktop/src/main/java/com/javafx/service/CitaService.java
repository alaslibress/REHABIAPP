package com.javafx.service;

import com.javafx.Clases.Cita;
import com.javafx.DAO.CitaDAO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Capa de servicio para operaciones de Cita.
 * Delega directamente al CitaDAO que consume la API REST.
 */
public class CitaService {

    private final CitaDAO citaDAO = new CitaDAO();

    /**
     * Lista todas las citas del sistema.
     */
    public List<Cita> listarTodas() {
        return citaDAO.listarTodas();
    }

    /**
     * Lista las citas de una fecha concreta.
     */
    public List<Cita> listarPorFecha(LocalDate fecha) {
        return citaDAO.listarPorFecha(fecha);
    }

    /**
     * Lista las citas de un sanitario concreto.
     */
    public List<Cita> listarPorSanitario(String dniSan) {
        return citaDAO.listarPorSanitario(dniSan);
    }

    /**
     * Lista las citas de un sanitario en una fecha concreta.
     */
    public List<Cita> obtenerCitasPorSanitarioYFecha(String dniSan, LocalDate fecha) {
        return citaDAO.obtenerCitasPorSanitarioYFecha(dniSan, fecha);
    }

    /**
     * Lista las citas de un paciente concreto.
     */
    public List<Cita> listarPorPaciente(String dniPac) {
        return citaDAO.listarPorPaciente(dniPac);
    }

    /**
     * Comprueba si ya existe una cita en ese horario para el sanitario.
     */
    public boolean existeCitaEnHorario(String dniSan, LocalDate fecha, LocalTime hora) {
        return citaDAO.existeCitaEnHorario(dniSan, fecha, hora);
    }

    /**
     * Inserta una nueva cita.
     */
    public void insertar(Cita cita) {
        citaDAO.insertar(cita);
    }

    /**
     * Actualiza una cita (delete + insert en la API).
     */
    public void actualizar(String dniPac, String dniSan, LocalDate fechaAntigua, LocalTime horaAntigua,
                           Cita nuevaCita) {
        citaDAO.actualizar(dniPac, dniSan, fechaAntigua, horaAntigua, nuevaCita);
    }

    /**
     * Elimina una cita.
     */
    public void eliminar(String dniPac, String dniSan, LocalDate fecha, LocalTime hora) {
        citaDAO.eliminar(dniPac, dniSan, fecha, hora);
    }
}
