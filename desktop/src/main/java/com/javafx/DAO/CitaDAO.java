package com.javafx.DAO;

import com.javafx.Clases.Cita;
import com.javafx.Clases.ConexionBD;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.ValidacionException;

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
 * Clase DAO para gestionar las operaciones de base de datos de Citas.
 * La tabla CITA es una relacion N:M entre paciente y sanitario.
 *
 * Los metodos de escritura lanzan excepciones personalizadas
 * en vez de devolver boolean.
 */
public class CitaDAO {

    // ==================== METODOS DE CONSULTA ====================

    /**
     * Lista todas las citas ordenadas por fecha y hora
     * @return Lista de todas las citas
     * @throws ConexionException si hay error de conexion con la BD
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
            throw new ConexionException("Error al listar citas", e);
        }

        return citas;
    }

    /**
     * Lista las citas de una fecha especifica
     * @param fecha Fecha a buscar
     * @return Lista de citas de esa fecha
     * @throws ConexionException si hay error de conexion con la BD
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
            throw new ConexionException("Error al listar citas por fecha", e);
        }

        return citas;
    }

    /**
     * Lista las citas de un sanitario especifico
     * @param dniSanitario DNI del sanitario
     * @return Lista de citas del sanitario
     * @throws ConexionException si hay error de conexion con la BD
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
            throw new ConexionException("Error al listar citas por sanitario", e);
        }

        return citas;
    }

    /**
     * Lista las citas de un paciente especifico
     * @param dniPaciente DNI del paciente
     * @return Lista de citas del paciente
     * @throws ConexionException si hay error de conexion con la BD
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
            throw new ConexionException("Error al listar citas por paciente", e);
        }

        return citas;
    }

    /**
     * Lista las citas de un sanitario en una fecha especifica
     * @throws ConexionException si hay error de conexion con la BD
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
            throw new ConexionException("Error al listar citas por sanitario y fecha", e);
        }

        return citas;
    }

    /**
     * Verifica si existe una cita en una fecha y hora especifica para un sanitario
     * @throws ConexionException si hay error de conexion con la BD
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
            throw new ConexionException("Error al verificar cita en horario", e);
        }

        return false;
    }

    // ==================== METODOS DE INSERCION ====================

    /**
     * Inserta una nueva cita en la base de datos
     * @param cita Cita a insertar
     * @throws DuplicadoException si ya existe una cita en ese horario
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void insertar(Cita cita) {
        String query = "INSERT INTO cita (dni_pac, dni_san, fecha_cita, hora_cita) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, cita.getDniPaciente());
            stmt.setString(2, cita.getDniSanitario());
            stmt.setDate(3, Date.valueOf(cita.getFecha()));
            stmt.setTime(4, Time.valueOf(cita.getHora()));

            stmt.executeUpdate();
            System.out.println("Cita insertada correctamente: " + cita);

        } catch (SQLException e) {
            String sqlState = e.getSQLState();

            if (sqlState != null && "23505".equals(sqlState)) {
                throw new DuplicadoException("Ya existe una cita en ese horario", "horario", e);
            }
            if (sqlState != null && "23503".equals(sqlState)) {
                throw new ValidacionException("Paciente o sanitario no encontrado", "referencia", e);
            }

            throw new ConexionException("Error al insertar cita", e);
        }
    }

    // ==================== METODOS DE ACTUALIZACION ====================

    /**
     * Actualiza la fecha y hora de una cita existente
     * @throws DuplicadoException si ya existe una cita en el nuevo horario
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void actualizar(String dniPaciente, String dniSanitario,
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

            if (filasAfectadas == 0) {
                throw new ValidacionException("No se encontro la cita para actualizar", "cita");
            }

            System.out.println("Cita actualizada correctamente");

        } catch (SQLException e) {
            String sqlState = e.getSQLState();

            if (sqlState != null && "23505".equals(sqlState)) {
                throw new DuplicadoException("Ya existe una cita en el nuevo horario", "horario", e);
            }

            throw new ConexionException("Error al actualizar cita", e);
        }
    }

    // ==================== METODOS DE ELIMINACION ====================

    /**
     * Elimina una cita de la base de datos
     * @throws ConexionException si hay error de conexion con la BD
     * @throws ValidacionException si la cita no existe
     */
    public void eliminar(String dniPaciente, String dniSanitario, LocalDate fecha, LocalTime hora) {
        String query = "DELETE FROM cita WHERE dni_pac = ? AND dni_san = ? AND fecha_cita = ? AND hora_cita = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dniPaciente);
            stmt.setString(2, dniSanitario);
            stmt.setDate(3, Date.valueOf(fecha));
            stmt.setTime(4, Time.valueOf(hora));

            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas == 0) {
                throw new ValidacionException("No se encontro la cita para eliminar", "cita");
            }

            System.out.println("Cita eliminada correctamente");

        } catch (SQLException e) {
            throw new ConexionException("Error al eliminar cita", e);
        }
    }

    /**
     * Elimina una cita usando el objeto Cita
     */
    public void eliminar(Cita cita) {
        eliminar(cita.getDniPaciente(), cita.getDniSanitario(), cita.getFecha(), cita.getHora());
    }

    // ==================== METODOS AUXILIARES ====================

    private Cita mapearCitaDesdeResultSet(ResultSet rs) throws SQLException {
        String dniPaciente = rs.getString("dni_pac");
        String dniSanitario = rs.getString("dni_san");

        Date fechaSql = rs.getDate("fecha_cita");
        LocalDate fecha = fechaSql != null ? fechaSql.toLocalDate() : null;

        Time horaSql = rs.getTime("hora_cita");
        LocalTime hora = horaSql != null ? horaSql.toLocalTime() : null;

        String nombrePaciente = rs.getString("nombre_paciente");
        String nombreSanitario = rs.getString("nombre_sanitario");

        return new Cita(dniPaciente, dniSanitario, fecha, hora, nombrePaciente, nombreSanitario);
    }
}
