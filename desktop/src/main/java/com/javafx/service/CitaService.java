package com.javafx.service;

import com.javafx.Clases.Cita;
import com.javafx.DAO.CitaDAO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Capa de servicio para operaciones de Cita
 * Actua como wrapper del CitaDAO proporcionando una interfaz simplificada
 */
public class CitaService {

    private final CitaDAO citaDAO;

    /**
     * Query base reutilizable para consultas de citas
     * Incluye JOIN con paciente y sanitario para obtener nombres completos
     */
    public static final String QUERY_BASE_CITA =
        "SELECT c.dni_pac, c.dni_san, c.fecha_cita, c.hora_cita, " +
        "CONCAT(p.nombre_pac, ' ', p.apellido1_pac, ' ', COALESCE(p.apellido2_pac, '')) AS nombre_paciente, " +
        "CONCAT(s.nombre_san, ' ', s.apellido1_san, ' ', COALESCE(s.apellido2_san, '')) AS nombre_sanitario " +
        "FROM cita c " +
        "JOIN paciente p ON c.dni_pac = p.dni_pac " +
        "JOIN sanitario s ON c.dni_san = s.dni_san";

    /**
     * Constructor que inicializa el DAO
     */
    public CitaService() {
        this.citaDAO = new CitaDAO();
    }

    /**
     * Lista todas las citas
     * @return Lista de todas las citas ordenadas por fecha y hora
     */
    public List<Cita> listarTodas() {
        return citaDAO.listarTodas();
    }

    /**
     * Lista las citas de una fecha especifica
     * @param fecha Fecha a buscar
     * @return Lista de citas de esa fecha
     */
    public List<Cita> listarPorFecha(LocalDate fecha) {
        return citaDAO.listarPorFecha(fecha);
    }

    /**
     * Inserta una nueva cita
     * @param cita Cita a insertar
     * @throws com.javafx.excepcion.DuplicadoException si ya existe una cita en ese horario
     * @throws com.javafx.excepcion.ConexionException si hay error de BD
     */
    public void insertar(Cita cita) {
        citaDAO.insertar(cita);
    }

    /**
     * Elimina una cita
     * @param dniPac DNI del paciente
     * @param dniSan DNI del sanitario
     * @param fecha Fecha de la cita
     * @param hora Hora de la cita
     * @throws com.javafx.excepcion.ConexionException si hay error de BD
     */
    public void eliminar(String dniPac, String dniSan, LocalDate fecha, LocalTime hora) {
        citaDAO.eliminar(dniPac, dniSan, fecha, hora);
    }
}
