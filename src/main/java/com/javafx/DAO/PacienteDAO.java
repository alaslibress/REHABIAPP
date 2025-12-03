package com.javafx.DAO;

import com.javafx.Clases.Paciente;
import com.javafx.Clases.ConexionBD;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase DAO para gestionar operaciones de base de datos relacionadas con Paciente
 * Trabaja con las tablas: paciente, telefono_paciente
 */
public class PacienteDAO {

    // ==================== METODOS DE CONSULTA ====================

    /**
     * Lista todos los pacientes de la base de datos
     * @return Lista de pacientes ordenados por nombre
     */
    public List<Paciente> listarTodos() {
        List<Paciente> pacientes = new ArrayList<>();

        String query = "SELECT dni_pac, nombre_pac, apellido1_pac, apellido2_pac, " +
                "edad_pac, email_pac, num_ss, discapacidad_pac, tratamiento_pac, " +
                "estado_tratamiento, protesis, dni_san " +
                "FROM paciente ORDER BY nombre_pac";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Paciente paciente = mapearPacienteDesdeResultSet(rs);
                pacientes.add(paciente);
            }

        } catch (SQLException e) {
            System.err.println("Error al listar pacientes: " + e.getMessage());
            e.printStackTrace();
        }

        return pacientes;
    }

    /**
     * Busca pacientes por texto en cualquier campo
     * @param texto Texto a buscar
     * @return Lista de pacientes que coinciden
     */
    public List<Paciente> buscarPorTexto(String texto) {
        List<Paciente> pacientes = new ArrayList<>();

        String query = "SELECT dni_pac, nombre_pac, apellido1_pac, apellido2_pac, " +
                "edad_pac, email_pac, num_ss, discapacidad_pac, tratamiento_pac, " +
                "estado_tratamiento, protesis, dni_san " +
                "FROM paciente WHERE " +
                "LOWER(dni_pac) LIKE ? OR LOWER(nombre_pac) LIKE ? OR " +
                "LOWER(apellido1_pac) LIKE ? OR LOWER(apellido2_pac) LIKE ? OR " +
                "LOWER(email_pac) LIKE ? OR LOWER(num_ss) LIKE ? " +
                "ORDER BY nombre_pac";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            String patron = "%" + texto.toLowerCase() + "%";
            for (int i = 1; i <= 6; i++) {
                stmt.setString(i, patron);
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Paciente paciente = mapearPacienteDesdeResultSet(rs);
                pacientes.add(paciente);
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar pacientes: " + e.getMessage());
            e.printStackTrace();
        }

        return pacientes;
    }

    /**
     * Busca un paciente por su DNI
     * @param dni DNI del paciente
     * @return Paciente encontrado o null
     */
    public Paciente buscarPorDni(String dni) {
        String query = "SELECT dni_pac, nombre_pac, apellido1_pac, apellido2_pac, " +
                "edad_pac, email_pac, num_ss, discapacidad_pac, tratamiento_pac, " +
                "estado_tratamiento, protesis, dni_san " +
                "FROM paciente WHERE dni_pac = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapearPacienteDesdeResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar paciente por DNI: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Obtiene un paciente completo por DNI incluyendo telefonos y direccion
     * @param dni DNI del paciente
     * @return Paciente con todos sus datos
     */
    public Paciente obtenerPorDNI(String dni) {
        Paciente paciente = buscarPorDni(dni);

        if (paciente != null) {
            cargarTelefonosPaciente(paciente);
            cargarDireccionPaciente(paciente);
        }

        return paciente;
    }

    /**
     * Verifica si existe un paciente con el DNI especificado
     */
    public boolean existeDni(String dni) {
        String query = "SELECT COUNT(*) FROM paciente WHERE dni_pac = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("Error al verificar DNI: " + e.getMessage());
        }

        return false;
    }

    /**
     * Verifica si existe un paciente con el email especificado
     */
    public boolean existeEmail(String email) {
        String query = "SELECT COUNT(*) FROM paciente WHERE email_pac = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("Error al verificar email: " + e.getMessage());
        }

        return false;
    }

    /**
     * Verifica si existe un email excluyendo un DNI especifico
     */
    public boolean existeEmailExcluyendoDni(String email, String dniExcluir) {
        String query = "SELECT COUNT(*) FROM paciente WHERE email_pac = ? AND dni_pac != ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            stmt.setString(2, dniExcluir);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("Error al verificar email excluyendo DNI: " + e.getMessage());
        }

        return false;
    }

    /**
     * Verifica si existe un numero de seguridad social
     */
    public boolean existeNumSS(String numSS) {
        String query = "SELECT COUNT(*) FROM paciente WHERE num_ss = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, numSS);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("Error al verificar numero SS: " + e.getMessage());
        }

        return false;
    }

    /**
     * Verifica si existe un numero SS excluyendo un DNI especifico
     */
    public boolean existeNumSSExcluyendoDni(String numSS, String dniExcluir) {
        String query = "SELECT COUNT(*) FROM paciente WHERE num_ss = ? AND dni_pac != ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, numSS);
            stmt.setString(2, dniExcluir);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("Error al verificar numero SS excluyendo DNI: " + e.getMessage());
        }

        return false;
    }

    // ==================== METODOS DE INSERCION ====================

    /**
     * Inserta un nuevo paciente en la base de datos
     * Primero inserta la direccion en las tablas correspondientes
     * @param paciente Paciente a insertar
     * @return true si la insercion fue exitosa
     */
    public boolean insertar(Paciente paciente) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            //Primero insertar la direccion y obtener el id_direccion
            Integer idDireccion = insertarDireccionCompleta(conn, paciente);

            //Insertar paciente con id_direccion
            String queryPaciente = "INSERT INTO paciente (dni_pac, dni_san, nombre_pac, apellido1_pac, " +
                    "apellido2_pac, email_pac, num_ss, id_direccion, discapacidad_pac, " +
                    "tratamiento_pac, estado_tratamiento, protesis, edad_pac) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(queryPaciente)) {
                stmt.setString(1, paciente.getDni());
                stmt.setString(2, paciente.getDniSanitario());
                stmt.setString(3, paciente.getNombre());
                stmt.setString(4, paciente.getApellido1());
                stmt.setString(5, paciente.getApellido2() != null ? paciente.getApellido2() : "");
                stmt.setString(6, paciente.getEmail());
                stmt.setString(7, paciente.getNumSS());

                //id_direccion (obligatorio)
                if (idDireccion != null) {
                    stmt.setInt(8, idDireccion);
                } else {
                    //Si no hay direccion, lanzar error
                    conn.rollback();
                    System.err.println("Error: La direccion es obligatoria");
                    return false;
                }

                stmt.setString(9, paciente.getDiscapacidad() != null ? paciente.getDiscapacidad() : "");
                stmt.setString(10, paciente.getTratamiento() != null ? paciente.getTratamiento() : "");
                stmt.setString(11, paciente.getEstadoTratamiento() != null ? paciente.getEstadoTratamiento() : "");
                stmt.setInt(12, paciente.getProtesis());
                stmt.setInt(13, paciente.getEdad());

                int filasAfectadas = stmt.executeUpdate();

                if (filasAfectadas == 0) {
                    conn.rollback();
                    return false;
                }
            }

            //Actualizar contador de pacientes del sanitario
            actualizarContadorPacientes(conn, paciente.getDniSanitario(), 1);

            conn.commit();
            System.out.println("Paciente insertado correctamente: " + paciente.getDni());
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error al hacer rollback: " + ex.getMessage());
                }
            }
            System.err.println("Error al insertar paciente: " + e.getMessage());
            e.printStackTrace();
            return false;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error al restaurar autocommit: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Inserta la direccion completa del paciente
     * Orden: localidad -> cp -> direccion
     * @param conn Conexion activa
     * @param paciente Paciente con datos de direccion
     * @return ID de la direccion insertada o null si falla
     */
    private Integer insertarDireccionCompleta(Connection conn, Paciente paciente) throws SQLException {
        String localidad = paciente.getLocalidad();
        String provincia = paciente.getProvincia();
        String codigoPostal = paciente.getCodigoPostal();
        String calle = paciente.getCalle();
        String numero = paciente.getNumero();
        String piso = paciente.getPiso();

        //Verificar que al menos tengamos los campos minimos
        if (localidad == null || localidad.trim().isEmpty() ||
                codigoPostal == null || codigoPostal.trim().isEmpty() ||
                calle == null || calle.trim().isEmpty()) {
            System.err.println("Datos de direccion incompletos");
            return null;
        }

        //1. Insertar localidad si no existe
        insertarLocalidadSiNoExiste(conn, localidad, provincia);

        //2. Insertar codigo postal si no existe
        insertarCPSiNoExiste(conn, codigoPostal, localidad);

        //3. Insertar direccion y obtener el ID
        String queryDireccion = "INSERT INTO direccion (calle, numero, piso, cp) VALUES (?, ?, ?, ?) RETURNING id_direccion";

        try (PreparedStatement stmt = conn.prepareStatement(queryDireccion)) {
            stmt.setString(1, calle);

            //Numero es INTEGER en la BD - convertir de String a int
            if (numero != null && !numero.trim().isEmpty()) {
                try {
                    stmt.setInt(2, Integer.parseInt(numero.trim()));
                } catch (NumberFormatException e) {
                    //Si no es un numero valido, poner 0
                    stmt.setInt(2, 0);
                }
            } else {
                stmt.setInt(2, 0);
            }

            stmt.setString(3, piso != null ? piso : "");
            stmt.setString(4, codigoPostal);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id_direccion");
            }
        }

        return null;
    }

    /**
     * Inserta una localidad si no existe en la base de datos
     */
    private void insertarLocalidadSiNoExiste(Connection conn, String localidad, String provincia) throws SQLException {
        //Verificar si existe
        String queryVerificar = "SELECT COUNT(*) FROM localidad WHERE nombre_localidad = ?";
        try (PreparedStatement stmtVerificar = conn.prepareStatement(queryVerificar)) {
            stmtVerificar.setString(1, localidad);
            ResultSet rs = stmtVerificar.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return; //Ya existe
            }
        }

        //Insertar nueva localidad
        String queryInsertar = "INSERT INTO localidad (nombre_localidad, provincia) VALUES (?, ?)";
        try (PreparedStatement stmtInsertar = conn.prepareStatement(queryInsertar)) {
            stmtInsertar.setString(1, localidad);
            stmtInsertar.setString(2, provincia != null ? provincia : "");
            stmtInsertar.executeUpdate();
        }
    }

    /**
     * Inserta un codigo postal si no existe en la base de datos
     */
    private void insertarCPSiNoExiste(Connection conn, String codigoPostal, String localidad) throws SQLException {
        //Verificar si existe
        String queryVerificar = "SELECT COUNT(*) FROM cp WHERE cp = ?";
        try (PreparedStatement stmtVerificar = conn.prepareStatement(queryVerificar)) {
            stmtVerificar.setString(1, codigoPostal);
            ResultSet rs = stmtVerificar.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return; //Ya existe
            }
        }

        //Insertar nuevo CP
        String queryInsertar = "INSERT INTO cp (cp, nombre_localidad) VALUES (?, ?)";
        try (PreparedStatement stmtInsertar = conn.prepareStatement(queryInsertar)) {
            stmtInsertar.setString(1, codigoPostal);
            stmtInsertar.setString(2, localidad);
            stmtInsertar.executeUpdate();
        }
    }

    /**
     * Inserta los telefonos de un paciente
     * @param dniPaciente DNI del paciente
     * @param telefono1 Primer telefono
     * @param telefono2 Segundo telefono (opcional)
     * @return true si la insercion fue exitosa
     */
    public boolean insertarTelefonos(String dniPaciente, String telefono1, String telefono2) {
        String query = "INSERT INTO telefono_paciente (dni_pac, telefono) VALUES (?, ?)";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            if (telefono1 != null && !telefono1.trim().isEmpty()) {
                stmt.setString(1, dniPaciente);
                stmt.setString(2, telefono1.trim());
                stmt.executeUpdate();
            }

            if (telefono2 != null && !telefono2.trim().isEmpty()) {
                stmt.setString(1, dniPaciente);
                stmt.setString(2, telefono2.trim());
                stmt.executeUpdate();
            }

            return true;

        } catch (SQLException e) {
            System.err.println("Error al insertar telefonos: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Inserta la foto de un paciente como BYTEA en la base de datos
     * @param dniPaciente DNI del paciente
     * @param archivoFoto Archivo de la foto
     * @return true si la insercion fue exitosa
     */
    public boolean insertarFoto(String dniPaciente, File archivoFoto) {
        String query = "UPDATE paciente SET foto = ? WHERE dni_pac = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            //Leer el archivo como bytes
            byte[] fotoBytes = Files.readAllBytes(archivoFoto.toPath());
            stmt.setBytes(1, fotoBytes);
            stmt.setString(2, dniPaciente);

            int filasAfectadas = stmt.executeUpdate();
            System.out.println("Foto guardada correctamente (" + fotoBytes.length + " bytes)");
            return filasAfectadas > 0;

        } catch (Exception e) {
            System.err.println("Error al insertar foto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==================== METODOS DE ACTUALIZACION ====================

    /**
     * Actualiza un paciente existente en la base de datos
     * @param paciente Paciente con los nuevos datos
     * @param dniOriginal DNI original del paciente
     * @return true si la actualizacion fue exitosa
     */
    public boolean actualizar(Paciente paciente, String dniOriginal) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            //Actualizar paciente
            String queryPaciente = "UPDATE paciente SET dni_pac = ?, nombre_pac = ?, apellido1_pac = ?, " +
                    "apellido2_pac = ?, email_pac = ?, num_ss = ?, discapacidad_pac = ?, " +
                    "tratamiento_pac = ?, estado_tratamiento = ?, protesis = ?, edad_pac = ? " +
                    "WHERE dni_pac = ?";

            try (PreparedStatement stmt = conn.prepareStatement(queryPaciente)) {
                stmt.setString(1, paciente.getDni());
                stmt.setString(2, paciente.getNombre());
                stmt.setString(3, paciente.getApellido1());
                stmt.setString(4, paciente.getApellido2() != null ? paciente.getApellido2() : "");
                stmt.setString(5, paciente.getEmail());
                stmt.setString(6, paciente.getNumSS());
                stmt.setString(7, paciente.getDiscapacidad() != null ? paciente.getDiscapacidad() : "");
                stmt.setString(8, paciente.getTratamiento() != null ? paciente.getTratamiento() : "");
                stmt.setString(9, paciente.getEstadoTratamiento() != null ? paciente.getEstadoTratamiento() : "");
                stmt.setInt(10, paciente.getProtesis());
                stmt.setInt(11, paciente.getEdad());
                stmt.setString(12, dniOriginal);

                stmt.executeUpdate();
            }

            conn.commit();
            System.out.println("Paciente actualizado correctamente: " + paciente.getDni());
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error al hacer rollback: " + ex.getMessage());
                }
            }
            System.err.println("Error al actualizar paciente: " + e.getMessage());
            e.printStackTrace();
            return false;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error al restaurar autocommit: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Actualiza los telefonos de un paciente
     * @param dniPaciente DNI del paciente
     * @param telefono1 Primer telefono
     * @param telefono2 Segundo telefono
     * @return true si la actualizacion fue exitosa
     */
    public boolean actualizarTelefonos(String dniPaciente, String telefono1, String telefono2) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            //Eliminar telefonos existentes
            String queryEliminar = "DELETE FROM telefono_paciente WHERE dni_pac = ?";
            try (PreparedStatement stmtEliminar = conn.prepareStatement(queryEliminar)) {
                stmtEliminar.setString(1, dniPaciente);
                stmtEliminar.executeUpdate();
            }

            //Insertar nuevos telefonos
            String queryInsertar = "INSERT INTO telefono_paciente (dni_pac, telefono) VALUES (?, ?)";
            try (PreparedStatement stmtInsertar = conn.prepareStatement(queryInsertar)) {

                if (telefono1 != null && !telefono1.trim().isEmpty()) {
                    stmtInsertar.setString(1, dniPaciente);
                    stmtInsertar.setString(2, telefono1.trim());
                    stmtInsertar.executeUpdate();
                }

                if (telefono2 != null && !telefono2.trim().isEmpty()) {
                    stmtInsertar.setString(1, dniPaciente);
                    stmtInsertar.setString(2, telefono2.trim());
                    stmtInsertar.executeUpdate();
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error al hacer rollback: " + ex.getMessage());
                }
            }
            System.err.println("Error al actualizar telefonos: " + e.getMessage());
            e.printStackTrace();
            return false;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error al restaurar autocommit: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Actualiza la foto de un paciente
     * @param dniPaciente DNI del paciente
     * @param archivoFoto Archivo de la nueva foto
     * @return true si la actualizacion fue exitosa
     */
    public boolean actualizarFoto(String dniPaciente, File archivoFoto) {
        return insertarFoto(dniPaciente, archivoFoto);
    }

    // ==================== METODOS DE ELIMINACION ====================

    /**
     * Elimina un paciente de la base de datos
     * @param dni DNI del paciente a eliminar
     * @return true si la eliminacion fue exitosa
     */
    public boolean eliminar(String dni) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            //Obtener DNI del sanitario antes de eliminar para actualizar contador
            String dniSanitario = obtenerDniSanitarioPorPaciente(conn, dni);

            //Eliminar telefonos del paciente primero
            String deleteTelefonos = "DELETE FROM telefono_paciente WHERE dni_pac = ?";
            try (PreparedStatement stmtTel = conn.prepareStatement(deleteTelefonos)) {
                stmtTel.setString(1, dni);
                stmtTel.executeUpdate();
            }

            //Eliminar citas del paciente
            String deleteCitas = "DELETE FROM cita WHERE dni_pac = ?";
            try (PreparedStatement stmtCitas = conn.prepareStatement(deleteCitas)) {
                stmtCitas.setString(1, dni);
                stmtCitas.executeUpdate();
            }

            //Eliminar paciente
            String deletePaciente = "DELETE FROM paciente WHERE dni_pac = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deletePaciente)) {
                stmt.setString(1, dni);
                int filasAfectadas = stmt.executeUpdate();

                if (filasAfectadas > 0) {
                    //Actualizar contador del sanitario
                    if (dniSanitario != null) {
                        actualizarContadorPacientes(conn, dniSanitario, -1);
                    }

                    conn.commit();
                    System.out.println("Paciente eliminado correctamente: " + dni);
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error al hacer rollback: " + ex.getMessage());
                }
            }
            System.err.println("Error al eliminar paciente: " + e.getMessage());
            e.printStackTrace();
            return false;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error al restaurar autocommit: " + e.getMessage());
                }
            }
        }
    }

    // ==================== METODOS DE FOTO ====================

    /**
     * Obtiene la foto de un paciente como Image de JavaFX
     * Lee los bytes desde la columna BYTEA de la base de datos
     * @param dniPaciente DNI del paciente
     * @return Image con la foto o null si no existe
     */
    public Image obtenerFoto(String dniPaciente) {
        String query = "SELECT foto FROM paciente WHERE dni_pac = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dniPaciente);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                byte[] fotoBytes = rs.getBytes("foto");
                if (fotoBytes != null && fotoBytes.length > 0) {
                    return new Image(new ByteArrayInputStream(fotoBytes));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener foto: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // ==================== METODOS AUXILIARES PRIVADOS ====================

    /**
     * Mapea un ResultSet a un objeto Paciente
     */
    private Paciente mapearPacienteDesdeResultSet(ResultSet rs) throws SQLException {
        String dni = rs.getString("dni_pac");
        String nombre = rs.getString("nombre_pac");
        String apellido1 = rs.getString("apellido1_pac");
        String apellido2 = rs.getString("apellido2_pac");
        int edad = rs.getInt("edad_pac");
        String email = rs.getString("email_pac");
        String numSS = rs.getString("num_ss");
        String discapacidad = rs.getString("discapacidad_pac");
        String tratamiento = rs.getString("tratamiento_pac");
        String estadoTratamiento = rs.getString("estado_tratamiento");
        int protesis = rs.getInt("protesis");
        String dniSanitario = rs.getString("dni_san");

        Paciente paciente = new Paciente(
                dni,
                nombre,
                apellido1 != null ? apellido1 : "",
                apellido2 != null ? apellido2 : "",
                edad,
                email,
                numSS,
                discapacidad != null ? discapacidad : "",
                tratamiento != null ? tratamiento : "",
                estadoTratamiento != null ? estadoTratamiento : "",
                protesis,
                dniSanitario
        );

        return paciente;
    }

    /**
     * Carga los telefonos de un paciente desde la base de datos
     */
    private void cargarTelefonosPaciente(Paciente paciente) {
        String queryTel = "SELECT telefono FROM telefono_paciente WHERE dni_pac = ? ORDER BY id_telefono LIMIT 2";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(queryTel)) {

            stmt.setString(1, paciente.getDni());
            ResultSet rs = stmt.executeQuery();

            int indice = 0;
            while (rs.next() && indice < 2) {
                String telefono = rs.getString("telefono");

                if (indice == 0) {
                    paciente.setTelefono1(telefono);
                } else {
                    paciente.setTelefono2(telefono);
                }
                indice++;
            }

        } catch (SQLException e) {
            System.err.println("Error al cargar telefonos del paciente: " + e.getMessage());
        }
    }

    /**
     * Carga la direccion de un paciente desde la base de datos
     * Hace JOIN con las tablas direccion, cp y localidad
     */
    private void cargarDireccionPaciente(Paciente paciente) {
        String query = "SELECT d.calle, d.numero, d.piso, d.cp, " +
                "c.nombre_localidad, l.provincia " +
                "FROM paciente p " +
                "JOIN direccion d ON p.id_direccion = d.id_direccion " +
                "LEFT JOIN cp c ON d.cp = c.cp " +
                "LEFT JOIN localidad l ON c.nombre_localidad = l.nombre_localidad " +
                "WHERE p.dni_pac = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, paciente.getDni());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                paciente.setCalle(rs.getString("calle"));

                //Numero es INTEGER en la BD
                int numero = rs.getInt("numero");
                paciente.setNumero(numero > 0 ? String.valueOf(numero) : "");

                paciente.setPiso(rs.getString("piso"));
                paciente.setCodigoPostal(rs.getString("cp"));
                paciente.setLocalidad(rs.getString("nombre_localidad"));
                paciente.setProvincia(rs.getString("provincia"));
            }

        } catch (SQLException e) {
            System.err.println("Error al cargar direccion del paciente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtiene el DNI del sanitario asignado a un paciente
     */
    private String obtenerDniSanitarioPorPaciente(Connection conn, String dniPaciente) throws SQLException {
        String query = "SELECT dni_san FROM paciente WHERE dni_pac = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, dniPaciente);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("dni_san");
            }
        }
        return null;
    }

    /**
     * Actualiza el contador de pacientes de un sanitario
     */
    private void actualizarContadorPacientes(Connection conn, String dniSanitario, int incremento) throws SQLException {
        if (dniSanitario == null || dniSanitario.isEmpty()) {
            return;
        }

        String query = "UPDATE sanitario SET num_de_pacientes = num_de_pacientes + ? " +
                "WHERE dni_san = ? AND num_de_pacientes + ? >= 0";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, incremento);
            stmt.setString(2, dniSanitario);
            stmt.setInt(3, incremento);
            stmt.executeUpdate();
        }
    }
}