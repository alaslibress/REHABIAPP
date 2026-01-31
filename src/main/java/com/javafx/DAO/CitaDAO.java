package com.javafx.DAO;

import com.javafx.Clases.Cita;
import com.javafx.Clases.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase DAO para gestionar las operaciones de base de datos de Citas
 * La tabla CITA es una relacion N:M entre paciente y sanitario
 */
public class CitaDAO {

    // ==================== METODOS DE CONSULTA ====================

    /**
     * Lista todas las citas ordenadas por fecha y hora
     * @return Lista de todas las citas
     */
    public List<Cita> listarTodas() {
        List<Cita> citas = new ArrayList<>();

        String query = "SELECT c.dni_pac, c.dni_san, c.fecha_cita, c.hora_cita, " +
                "CONCAT(p.nombre_pac, ' ', p.apellido1_pac, ' ', COALESCE(p.apellido2_pac, '')) AS nombre_paciente, " +
                "CONCAT(s.nombre_san, ' ', s.apellido1_san, ' ', COALESCE(s.apellido2_san, '')) AS nombre_sanitario " +
                "FROM cita c " +
                "JOIN paciente p ON c.dni_pac = p.dni_pac " +
                "JOIN sanitario s ON c.dni_san = s.dni_san " +
                "ORDER BY c.fecha_cita DESC, c.hora_cita ASC";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Cita cita = mapearCitaDesdeResultSet(rs);
                    citas.add(cita);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al listar citas: " + e.getMessage());
            e.printStackTrace();
        }

        return citas;
    }

    /**
     * Lista las citas de una fecha especifica
     * @param fecha Fecha a buscar
     * @return Lista de citas de esa fecha
     */
    public List<Cita> listarPorFecha(LocalDate fecha) {
        List<Cita> citas = new ArrayList<>();

        String query = "SELECT c.dni_pac, c.dni_san, c.fecha_cita, c.hora_cita, " +
                "CONCAT(p.nombre_pac, ' ', p.apellido1_pac, ' ', COALESCE(p.apellido2_pac, '')) AS nombre_paciente, " +
                "CONCAT(s.nombre_san, ' ', s.apellido1_san, ' ', COALESCE(s.apellido2_san, '')) AS nombre_sanitario " +
                "FROM cita c " +
                "JOIN paciente p ON c.dni_pac = p.dni_pac " +
                "JOIN sanitario s ON c.dni_san = s.dni_san " +
                "WHERE c.fecha_cita = ? " +
                "ORDER BY c.hora_cita ASC";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setDate(1, Date.valueOf(fecha));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Cita cita = mapearCitaDesdeResultSet(rs);
                    citas.add(cita);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al listar citas por fecha: " + e.getMessage());
            e.printStackTrace();
        }

        return citas;
    }

    /**
     * Lista las citas de un sanitario especifico
     * @param dniSanitario DNI del sanitario
     * @return Lista de citas del sanitario
     */
    public List<Cita> listarPorSanitario(String dniSanitario) {
        List<Cita> citas = new ArrayList<>();

        String query = "SELECT c.dni_pac, c.dni_san, c.fecha_cita, c.hora_cita, " +
                "CONCAT(p.nombre_pac, ' ', p.apellido1_pac, ' ', COALESCE(p.apellido2_pac, '')) AS nombre_paciente, " +
                "CONCAT(s.nombre_san, ' ', s.apellido1_san, ' ', COALESCE(s.apellido2_san, '')) AS nombre_sanitario " +
                "FROM cita c " +
                "JOIN paciente p ON c.dni_pac = p.dni_pac " +
                "JOIN sanitario s ON c.dni_san = s.dni_san " +
                "WHERE c.dni_san = ? " +
                "ORDER BY c.fecha_cita DESC, c.hora_cita ASC";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dniSanitario);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Cita cita = mapearCitaDesdeResultSet(rs);
                    citas.add(cita);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al listar citas por sanitario: " + e.getMessage());
            e.printStackTrace();
        }

        return citas;
    }

    /**
     * Lista las citas de un paciente especifico
     * @param dniPaciente DNI del paciente
     * @return Lista de citas del paciente
     */
    public List<Cita> listarPorPaciente(String dniPaciente) {
        List<Cita> citas = new ArrayList<>();

        String query = "SELECT c.dni_pac, c.dni_san, c.fecha_cita, c.hora_cita, " +
                "CONCAT(p.nombre_pac, ' ', p.apellido1_pac, ' ', COALESCE(p.apellido2_pac, '')) AS nombre_paciente, " +
                "CONCAT(s.nombre_san, ' ', s.apellido1_san, ' ', COALESCE(s.apellido2_san, '')) AS nombre_sanitario " +
                "FROM cita c " +
                "JOIN paciente p ON c.dni_pac = p.dni_pac " +
                "JOIN sanitario s ON c.dni_san = s.dni_san " +
                "WHERE c.dni_pac = ? " +
                "ORDER BY c.fecha_cita DESC, c.hora_cita ASC";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dniPaciente);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Cita cita = mapearCitaDesdeResultSet(rs);
                    citas.add(cita);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al listar citas por paciente: " + e.getMessage());
            e.printStackTrace();
        }

        return citas;
    }

    /**
     * Lista las citas de un sanitario en una fecha especifica
     * @param dniSanitario DNI del sanitario
     * @param fecha Fecha a buscar
     * @return Lista de citas del sanitario en esa fecha
     */
    public List<Cita> obtenerCitasPorSanitarioYFecha(String dniSanitario, LocalDate fecha) {
        List<Cita> citas = new ArrayList<>();

        String query = "SELECT c.dni_pac, c.dni_san, c.fecha_cita, c.hora_cita, " +
                "CONCAT(p.nombre_pac, ' ', p.apellido1_pac, ' ', COALESCE(p.apellido2_pac, '')) AS nombre_paciente, " +
                "CONCAT(s.nombre_san, ' ', s.apellido1_san, ' ', COALESCE(s.apellido2_san, '')) AS nombre_sanitario " +
                "FROM cita c " +
                "JOIN paciente p ON c.dni_pac = p.dni_pac " +
                "JOIN sanitario s ON c.dni_san = s.dni_san " +
                "WHERE c.dni_san = ? AND c.fecha_cita = ? " +
                "ORDER BY c.hora_cita ASC";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dniSanitario);
            stmt.setDate(2, Date.valueOf(fecha));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Cita cita = mapearCitaDesdeResultSet(rs);
                    citas.add(cita);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al listar citas por sanitario y fecha: " + e.getMessage());
            e.printStackTrace();
        }

        return citas;
    }

    /**
     * Verifica si existe una cita en una fecha y hora especifica para un sanitario
     * @param dniSanitario DNI del sanitario
     * @param fecha Fecha de la cita
     * @param hora Hora de la cita
     * @return true si ya existe una cita en ese horario
     */
    public boolean existeCitaEnHorario(String dniSanitario, LocalDate fecha, LocalTime hora) {
        String query = "SELECT COUNT(*) FROM cita WHERE dni_san = ? AND fecha_cita = ? AND hora_cita = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dniSanitario);
            stmt.setDate(2, Date.valueOf(fecha));
            stmt.setTime(3, Time.valueOf(hora));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al verificar cita en horario: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==================== METODOS DE INSERCION ====================

    /**
     * Inserta una nueva cita en la base de datos
     * @param cita Cita a insertar
     * @return true si la insercion fue exitosa
     */
    public boolean insertar(Cita cita) {
        String query = "INSERT INTO cita (dni_pac, dni_san, fecha_cita, hora_cita) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, cita.getDniPaciente());
            stmt.setString(2, cita.getDniSanitario());
            stmt.setDate(3, Date.valueOf(cita.getFecha()));
            stmt.setTime(4, Time.valueOf(cita.getHora()));

            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas > 0) {
                System.out.println("Cita insertada correctamente: " + cita);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error al insertar cita: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==================== METODOS DE ACTUALIZACION ====================

    /**
     * Actualiza la fecha y hora de una cita existente
     * @param dniPaciente DNI del paciente
     * @param dniSanitario DNI del sanitario
     * @param fechaOriginal Fecha original de la cita
     * @param horaOriginal Hora original de la cita
     * @param nuevaFecha Nueva fecha
     * @param nuevaHora Nueva hora
     * @return true si la actualizacion fue exitosa
     */
    public boolean actualizar(String dniPaciente, String dniSanitario,
                              LocalDate fechaOriginal, LocalTime horaOriginal,
                              LocalDate nuevaFecha, LocalTime nuevaHora) {

        String query = "UPDATE cita SET fecha_cita = ?, hora_cita = ? " +
                "WHERE dni_pac = ? AND dni_san = ? AND fecha_cita = ? AND hora_cita = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setDate(1, Date.valueOf(nuevaFecha));
            stmt.setTime(2, Time.valueOf(nuevaHora));
            stmt.setString(3, dniPaciente);
            stmt.setString(4, dniSanitario);
            stmt.setDate(5, Date.valueOf(fechaOriginal));
            stmt.setTime(6, Time.valueOf(horaOriginal));

            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas > 0) {
                System.out.println("Cita actualizada correctamente");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error al actualizar cita: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==================== METODOS DE ELIMINACION ====================

    /**
     * Elimina una cita de la base de datos
     * @param dniPaciente DNI del paciente
     * @param dniSanitario DNI del sanitario
     * @param fecha Fecha de la cita
     * @param hora Hora de la cita
     * @return true si la eliminacion fue exitosa
     */
    public boolean eliminar(String dniPaciente, String dniSanitario, LocalDate fecha, LocalTime hora) {
        String query = "DELETE FROM cita WHERE dni_pac = ? AND dni_san = ? AND fecha_cita = ? AND hora_cita = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dniPaciente);
            stmt.setString(2, dniSanitario);
            stmt.setDate(3, Date.valueOf(fecha));
            stmt.setTime(4, Time.valueOf(hora));

            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas > 0) {
                System.out.println("Cita eliminada correctamente");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error al eliminar cita: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Elimina una cita usando el objeto Cita
     * @param cita Cita a eliminar
     * @return true si la eliminacion fue exitosa
     */
    public boolean eliminar(Cita cita) {
        return eliminar(cita.getDniPaciente(), cita.getDniSanitario(), cita.getFecha(), cita.getHora());
    }

    // ==================== METODOS AUXILIARES ====================

    /**
     * Mapea un ResultSet a un objeto Cita
     */
    private Cita mapearCitaDesdeResultSet(ResultSet rs) throws SQLException {
        String dniPaciente = rs.getString("dni_pac");
        String dniSanitario = rs.getString("dni_san");

        //Obtener fecha
        Date fechaSql = rs.getDate("fecha_cita");
        LocalDate fecha = fechaSql != null ? fechaSql.toLocalDate() : null;

        //Obtener hora
        Time horaSql = rs.getTime("hora_cita");
        LocalTime hora = horaSql != null ? horaSql.toLocalTime() : null;

        //Nombres para mostrar en tabla
        String nombrePaciente = rs.getString("nombre_paciente");
        String nombreSanitario = rs.getString("nombre_sanitario");

        return new Cita(dniPaciente, dniSanitario, fecha, hora, nombrePaciente, nombreSanitario);
    }
}