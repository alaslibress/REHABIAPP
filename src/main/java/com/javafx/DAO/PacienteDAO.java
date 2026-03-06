package com.javafx.DAO;

import com.javafx.Clases.Paciente;
import com.javafx.Clases.ConexionBD;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.ValidacionException;
import com.javafx.service.CifradoService;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase DAO para gestionar operaciones de base de datos relacionadas con Paciente.
 * Trabaja con las tablas: paciente, telefono_paciente.
 *
 * Los metodos de escritura (insertar, actualizar, eliminar) lanzan excepciones
 * personalizadas en vez de devolver boolean. Esto permite a la capa de servicio
 * y a los controladores ofrecer mensajes de error especificos al usuario.
 */
public class PacienteDAO {

    // ==================== METODOS DE CONSULTA ====================

    /**
     * Lista todos los pacientes activos de la base de datos
     * @return Lista de pacientes ordenados por nombre
     * @throws ConexionException si hay error de conexion con la BD
     */
    public List<Paciente> listarTodos() {
        List<Paciente> pacientes = new ArrayList<>();

        String query = "SELECT dni_pac, nombre_pac, apellido1_pac, apellido2_pac, " +
                "edad_pac, email_pac, num_ss, discapacidad_pac, tratamiento_pac, " +
                "estado_tratamiento, protesis, dni_san, " +
                "sexo, fecha_nacimiento, alergias, antecedentes, medicacion_actual, " +
                "consentimiento_rgpd, fecha_consentimiento, activo " +
                "FROM paciente WHERE (activo IS NULL OR activo = TRUE) ORDER BY nombre_pac";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Paciente paciente = mapearPacienteDesdeResultSet(rs);
                pacientes.add(paciente);
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al listar pacientes", e);
        }

        return pacientes;
    }

    /**
     * Busca pacientes por texto en cualquier campo
     * @param texto Texto a buscar
     * @return Lista de pacientes que coinciden
     * @throws ConexionException si hay error de conexion con la BD
     */
    public List<Paciente> buscarPorTexto(String texto) {
        List<Paciente> pacientes = new ArrayList<>();

        String query = "SELECT dni_pac, nombre_pac, apellido1_pac, apellido2_pac, " +
                "edad_pac, email_pac, num_ss, discapacidad_pac, tratamiento_pac, " +
                "estado_tratamiento, protesis, dni_san, " +
                "sexo, fecha_nacimiento, alergias, antecedentes, medicacion_actual, " +
                "consentimiento_rgpd, fecha_consentimiento, activo " +
                "FROM paciente WHERE (activo IS NULL OR activo = TRUE) AND (" +
                "LOWER(dni_pac) LIKE ? OR LOWER(nombre_pac) LIKE ? OR " +
                "LOWER(apellido1_pac) LIKE ? OR LOWER(apellido2_pac) LIKE ? OR " +
                "LOWER(email_pac) LIKE ? OR LOWER(num_ss) LIKE ?) " +
                "ORDER BY nombre_pac";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            String patron = "%" + texto.toLowerCase() + "%";
            for (int i = 1; i <= 6; i++) {
                stmt.setString(i, patron);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Paciente paciente = mapearPacienteDesdeResultSet(rs);
                    pacientes.add(paciente);
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al buscar pacientes", e);
        }

        return pacientes;
    }

    /**
     * Busca un paciente por su DNI
     * @param dni DNI del paciente
     * @return Paciente encontrado o null si no existe
     * @throws ConexionException si hay error de conexion con la BD
     */
    public Paciente buscarPorDni(String dni) {
        String query = "SELECT dni_pac, nombre_pac, apellido1_pac, apellido2_pac, " +
                "edad_pac, email_pac, num_ss, discapacidad_pac, tratamiento_pac, " +
                "estado_tratamiento, protesis, dni_san, " +
                "sexo, fecha_nacimiento, alergias, antecedentes, medicacion_actual, " +
                "consentimiento_rgpd, fecha_consentimiento, activo " +
                "FROM paciente WHERE dni_pac = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearPacienteDesdeResultSet(rs);
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al buscar paciente por DNI", e);
        }

        return null;
    }

    /**
     * Obtiene un paciente completo por DNI incluyendo telefonos y direccion
     * @param dni DNI del paciente
     * @return Paciente con todos sus datos o null si no existe
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
     * @throws ConexionException si hay error de conexion con la BD
     */
    public boolean existeDni(String dni) {
        String query = "SELECT COUNT(*) FROM paciente WHERE dni_pac = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al verificar DNI", e);
        }

        return false;
    }

    /**
     * Verifica si existe un paciente con el email especificado
     * @throws ConexionException si hay error de conexion con la BD
     */
    public boolean existeEmail(String email) {
        String query = "SELECT COUNT(*) FROM paciente WHERE email_pac = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al verificar email", e);
        }

        return false;
    }

    /**
     * Verifica si existe un email excluyendo un DNI especifico
     * @throws ConexionException si hay error de conexion con la BD
     */
    public boolean existeEmailExcluyendoDni(String email, String dniExcluir) {
        String query = "SELECT COUNT(*) FROM paciente WHERE email_pac = ? AND dni_pac != ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            stmt.setString(2, dniExcluir);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al verificar email", e);
        }

        return false;
    }

    /**
     * Verifica si existe un numero de seguridad social
     * @throws ConexionException si hay error de conexion con la BD
     */
    public boolean existeNumSS(String numSS) {
        String query = "SELECT COUNT(*) FROM paciente WHERE num_ss = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, numSS);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al verificar numero SS", e);
        }

        return false;
    }

    /**
     * Verifica si existe un numero SS excluyendo un DNI especifico
     * @throws ConexionException si hay error de conexion con la BD
     */
    public boolean existeNumSSExcluyendoDni(String numSS, String dniExcluir) {
        String query = "SELECT COUNT(*) FROM paciente WHERE num_ss = ? AND dni_pac != ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, numSS);
            stmt.setString(2, dniExcluir);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al verificar numero SS", e);
        }

        return false;
    }

    // ==================== METODOS DE INSERCION ====================

    /**
     * Inserta un nuevo paciente usando una conexion propia.
     * Gestiona su propia transaccion internamente.
     *
     * @param paciente Paciente a insertar
     * @throws DuplicadoException si el DNI, email o num SS ya existe
     * @throws ValidacionException si los datos de direccion son incompletos
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void insertar(Paciente paciente) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            insertar(conn, paciente);

            conn.commit();
            System.out.println("Paciente insertado correctamente: " + paciente.getDni());

        } catch (SQLException e) {
            hacerRollback(conn);
            throw traducirSQLException(e, "paciente");

        } finally {
            cerrarConexion(conn);
        }
    }

    /**
     * Inserta un nuevo paciente usando una conexion externa (para transacciones compuestas).
     * NO gestiona transaccion: el caller es responsable de commit/rollback.
     *
     * @param conn Conexion activa con autoCommit=false
     * @param paciente Paciente a insertar
     * @throws SQLException si hay error SQL
     * @throws ValidacionException si los datos de direccion son incompletos
     */
    public void insertar(Connection conn, Paciente paciente) throws SQLException {
        //Primero insertar la direccion y obtener el id_direccion
        Integer idDireccion = insertarDireccionCompleta(conn, paciente);

        //Insertar paciente con id_direccion y campos clinicos
        String queryPaciente = "INSERT INTO paciente (dni_pac, dni_san, nombre_pac, apellido1_pac, " +
                "apellido2_pac, email_pac, num_ss, id_direccion, discapacidad_pac, " +
                "tratamiento_pac, estado_tratamiento, protesis, edad_pac, " +
                "sexo, fecha_nacimiento, alergias, antecedentes, medicacion_actual, " +
                "consentimiento_rgpd, fecha_consentimiento, activo) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)";

        try (PreparedStatement stmt = conn.prepareStatement(queryPaciente)) {
            stmt.setString(1, paciente.getDni());
            stmt.setString(2, paciente.getDniSanitario());
            stmt.setString(3, paciente.getNombre());
            stmt.setString(4, paciente.getApellido1());
            stmt.setString(5, paciente.getApellido2() != null ? paciente.getApellido2() : "");
            stmt.setString(6, paciente.getEmail());
            stmt.setString(7, paciente.getNumSS());

            if (idDireccion != null) {
                stmt.setInt(8, idDireccion);
            } else {
                throw new ValidacionException("La direccion es obligatoria", "direccion");
            }

            stmt.setString(9, paciente.getDiscapacidad() != null ? paciente.getDiscapacidad() : "");
            stmt.setString(10, paciente.getTratamiento() != null ? paciente.getTratamiento() : "");
            stmt.setString(11, paciente.getEstadoTratamiento() != null ? paciente.getEstadoTratamiento() : "");
            stmt.setInt(12, paciente.getProtesis());
            stmt.setInt(13, paciente.getEdad());

            //Campos clinicos nuevos
            stmt.setString(14, paciente.getSexo() != null ? paciente.getSexo() : "");

            LocalDate fechaNac = paciente.getFechaNacimiento();
            if (fechaNac != null) {
                stmt.setDate(15, Date.valueOf(fechaNac));
            } else {
                stmt.setNull(15, java.sql.Types.DATE);
            }

            //Cifrar campos clinicos sensibles antes de almacenar (AES-256-GCM)
            stmt.setString(16, CifradoService.cifrar(paciente.getAlergias() != null ? paciente.getAlergias() : ""));
            stmt.setString(17, CifradoService.cifrar(paciente.getAntecedentes() != null ? paciente.getAntecedentes() : ""));
            stmt.setString(18, CifradoService.cifrar(paciente.getMedicacionActual() != null ? paciente.getMedicacionActual() : ""));
            stmt.setBoolean(19, paciente.isConsentimientoRgpd());

            if (paciente.isConsentimientoRgpd()) {
                LocalDateTime ahora = paciente.getFechaConsentimiento() != null
                        ? paciente.getFechaConsentimiento()
                        : LocalDateTime.now();
                stmt.setTimestamp(20, Timestamp.valueOf(ahora));
            } else {
                stmt.setNull(20, java.sql.Types.TIMESTAMP);
            }

            stmt.executeUpdate();
        }

        //Actualizar contador de pacientes del sanitario
        actualizarContadorPacientes(conn, paciente.getDniSanitario(), 1);
    }

    /**
     * Inserta los telefonos de un paciente
     * @param dniPaciente DNI del paciente
     * @param telefono1 Primer telefono
     * @param telefono2 Segundo telefono (opcional)
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void insertarTelefonos(String dniPaciente, String telefono1, String telefono2) {
        try (Connection conn = ConexionBD.getConexion()) {
            insertarTelefonos(conn, dniPaciente, telefono1, telefono2);
        } catch (SQLException e) {
            throw new ConexionException("Error al insertar telefonos", e);
        }
    }

    /**
     * Inserta telefonos usando conexion externa (para transacciones compuestas)
     */
    public void insertarTelefonos(Connection conn, String dniPaciente, String telefono1, String telefono2) throws SQLException {
        String query = "INSERT INTO telefono_paciente (dni_pac, telefono) VALUES (?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
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
        }
    }

    /**
     * Inserta la foto de un paciente como BYTEA en la base de datos
     * @param dniPaciente DNI del paciente
     * @param archivoFoto Archivo de la foto
     * @throws ConexionException si hay error de conexion con la BD
     * @throws ValidacionException si el archivo de foto no es valido
     */
    public void insertarFoto(String dniPaciente, File archivoFoto) {
        try (Connection conn = ConexionBD.getConexion()) {
            insertarFoto(conn, dniPaciente, archivoFoto);
        } catch (SQLException e) {
            throw new ConexionException("Error al insertar foto", e);
        }
    }

    /**
     * Inserta la foto usando conexion externa (para transacciones compuestas)
     */
    public void insertarFoto(Connection conn, String dniPaciente, File archivoFoto) throws SQLException {
        String query = "UPDATE paciente SET foto = ? WHERE dni_pac = ?";

        try {
            byte[] fotoBytes = Files.readAllBytes(archivoFoto.toPath());

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setBytes(1, fotoBytes);
                stmt.setString(2, dniPaciente);
                stmt.executeUpdate();
                System.out.println("Foto guardada correctamente (" + fotoBytes.length + " bytes)");
            }
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidacionException("Error al leer archivo de foto: " + e.getMessage(), "foto");
        }
    }

    // ==================== METODOS DE ACTUALIZACION ====================

    /**
     * Actualiza un paciente existente en la base de datos
     * @param paciente Paciente con los nuevos datos
     * @param dniOriginal DNI original del paciente
     * @throws DuplicadoException si el nuevo DNI, email o num SS ya existe
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void actualizar(Paciente paciente, String dniOriginal) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            actualizar(conn, paciente, dniOriginal);

            conn.commit();
            System.out.println("Paciente actualizado correctamente: " + paciente.getDni());

        } catch (SQLException e) {
            hacerRollback(conn);
            throw traducirSQLException(e, "paciente");

        } finally {
            cerrarConexion(conn);
        }
    }

    /**
     * Actualiza un paciente usando conexion externa (para transacciones compuestas)
     */
    public void actualizar(Connection conn, Paciente paciente, String dniOriginal) throws SQLException {
        String queryPaciente = "UPDATE paciente SET dni_pac = ?, nombre_pac = ?, apellido1_pac = ?, " +
                "apellido2_pac = ?, email_pac = ?, num_ss = ?, discapacidad_pac = ?, " +
                "tratamiento_pac = ?, estado_tratamiento = ?, protesis = ?, edad_pac = ?, " +
                "sexo = ?, fecha_nacimiento = ?, alergias = ?, antecedentes = ?, " +
                "medicacion_actual = ?, consentimiento_rgpd = ?, fecha_consentimiento = ? " +
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

            stmt.setString(12, paciente.getSexo() != null ? paciente.getSexo() : "");

            LocalDate fechaNac = paciente.getFechaNacimiento();
            if (fechaNac != null) {
                stmt.setDate(13, Date.valueOf(fechaNac));
            } else {
                stmt.setNull(13, java.sql.Types.DATE);
            }

            //Cifrar campos clinicos sensibles antes de almacenar (AES-256-GCM)
            stmt.setString(14, CifradoService.cifrar(paciente.getAlergias() != null ? paciente.getAlergias() : ""));
            stmt.setString(15, CifradoService.cifrar(paciente.getAntecedentes() != null ? paciente.getAntecedentes() : ""));
            stmt.setString(16, CifradoService.cifrar(paciente.getMedicacionActual() != null ? paciente.getMedicacionActual() : ""));
            stmt.setBoolean(17, paciente.isConsentimientoRgpd());

            if (paciente.isConsentimientoRgpd()) {
                LocalDateTime fechaConsent = paciente.getFechaConsentimiento() != null
                        ? paciente.getFechaConsentimiento()
                        : LocalDateTime.now();
                stmt.setTimestamp(18, Timestamp.valueOf(fechaConsent));
            } else {
                stmt.setNull(18, java.sql.Types.TIMESTAMP);
            }

            stmt.setString(19, dniOriginal);

            stmt.executeUpdate();
        }
    }

    /**
     * Actualiza los telefonos de un paciente
     * @param dniPaciente DNI del paciente
     * @param telefono1 Primer telefono
     * @param telefono2 Segundo telefono
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void actualizarTelefonos(String dniPaciente, String telefono1, String telefono2) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            actualizarTelefonos(conn, dniPaciente, telefono1, telefono2);

            conn.commit();

        } catch (SQLException e) {
            hacerRollback(conn);
            throw new ConexionException("Error al actualizar telefonos", e);

        } finally {
            cerrarConexion(conn);
        }
    }

    /**
     * Actualiza telefonos usando conexion externa (para transacciones compuestas)
     */
    public void actualizarTelefonos(Connection conn, String dniPaciente, String telefono1, String telefono2) throws SQLException {
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
    }

    /**
     * Actualiza la foto de un paciente
     * @param dniPaciente DNI del paciente
     * @param archivoFoto Archivo de la nueva foto
     */
    public void actualizarFoto(String dniPaciente, File archivoFoto) {
        insertarFoto(dniPaciente, archivoFoto);
    }

    // ==================== METODOS DE ELIMINACION ====================

    /**
     * Realiza una baja logica (soft delete) de un paciente.
     * Cumplimiento legal: Ley 41/2002 exige conservar historiales minimo 5 anios.
     * Los telefonos y citas se conservan para auditoria.
     *
     * @param dni DNI del paciente a dar de baja
     * @throws ConexionException si hay error de conexion con la BD
     * @throws ValidacionException si el paciente no existe
     */
    public void eliminar(String dni) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            //Obtener DNI del sanitario para actualizar contador
            String dniSanitario = obtenerDniSanitarioPorPaciente(conn, dni);

            //Soft delete: marcar como inactivo en vez de borrar fisicamente
            String softDelete = "UPDATE paciente SET activo = FALSE, fecha_baja = CURRENT_TIMESTAMP WHERE dni_pac = ?";
            try (PreparedStatement stmt = conn.prepareStatement(softDelete)) {
                stmt.setString(1, dni);
                int filasAfectadas = stmt.executeUpdate();

                if (filasAfectadas == 0) {
                    conn.rollback();
                    throw new ValidacionException("No se encontro el paciente con DNI: " + dni, "dni");
                }

                //Actualizar contador del sanitario
                if (dniSanitario != null) {
                    actualizarContadorPacientes(conn, dniSanitario, -1);
                }

                conn.commit();
                System.out.println("Paciente dado de baja (soft delete): " + dni);
            }

        } catch (SQLException e) {
            hacerRollback(conn);
            throw new ConexionException("Error al dar de baja paciente", e);

        } finally {
            cerrarConexion(conn);
        }
    }

    // ==================== METODOS DE FOTO ====================

    /**
     * Obtiene la foto de un paciente como Image de JavaFX
     * @param dniPaciente DNI del paciente
     * @return Image con la foto o null si no existe
     */
    public Image obtenerFoto(String dniPaciente) {
        String query = "SELECT foto FROM paciente WHERE dni_pac = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dniPaciente);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] fotoBytes = rs.getBytes("foto");
                    if (fotoBytes != null && fotoBytes.length > 0) {
                        try {
                            return new Image(new ByteArrayInputStream(fotoBytes));
                        } catch (IllegalArgumentException e) {
                            System.err.println("Error: formato de imagen invalido para paciente " + dniPaciente);
                            return null;
                        }
                    }
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al obtener foto", e);
        }

        return null;
    }

    // ==================== METODOS AUXILIARES PRIVADOS ====================

    /**
     * Traduce una SQLException a la excepcion personalizada correspondiente.
     * Mapeo de sqlState:
     * - 23505 (unique_violation) -> DuplicadoException
     * - 23503 (foreign_key_violation) -> ValidacionException
     * - 08xxx (connection) -> ConexionException
     * - Resto -> ConexionException
     */
    private RuntimeException traducirSQLException(SQLException e, String entidad) {
        String sqlState = e.getSQLState();
        String mensaje = e.getMessage();

        if (sqlState != null) {
            //Violacion de restriccion UNIQUE
            if ("23505".equals(sqlState)) {
                String campo = detectarCampoDuplicado(mensaje, entidad);
                return new DuplicadoException(
                        "Ya existe un registro con ese " + campo, campo, e);
            }

            //Violacion de clave foranea
            if ("23503".equals(sqlState)) {
                return new ValidacionException(
                        "Referencia invalida: " + mensaje, "clave_foranea", e);
            }
        }

        //Error generico de BD
        return new ConexionException("Error de base de datos: " + mensaje, e);
    }

    /**
     * Detecta que campo causo la violacion de unicidad analizando el mensaje de error.
     * PostgreSQL incluye el nombre de la restriccion en el mensaje.
     */
    private String detectarCampoDuplicado(String mensaje, String entidad) {
        if (mensaje == null) return "campo desconocido";

        String mensajeLower = mensaje.toLowerCase();

        if (mensajeLower.contains("dni_pac") || mensajeLower.contains("paciente_pkey")) {
            return "DNI";
        }
        if (mensajeLower.contains("email_pac") || mensajeLower.contains("email")) {
            return "email";
        }
        if (mensajeLower.contains("num_ss")) {
            return "numero de seguridad social";
        }
        if (mensajeLower.contains("dni_san") || mensajeLower.contains("sanitario_pkey")) {
            return "DNI";
        }
        if (mensajeLower.contains("email_san")) {
            return "email";
        }

        return "campo duplicado";
    }

    /**
     * Hace rollback de forma segura (sin propagar excepciones)
     */
    private void hacerRollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }
        }
    }

    /**
     * Cierra la conexion de forma segura restaurando autocommit
     */
    private void cerrarConexion(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error al restaurar autocommit: " + e.getMessage());
            }
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error al cerrar conexion: " + e.getMessage());
            }
        }
    }

    /**
     * Inserta la direccion completa del paciente
     * Orden: localidad -> cp -> direccion
     */
    private Integer insertarDireccionCompleta(Connection conn, Paciente paciente) throws SQLException {
        String localidad = paciente.getLocalidad();
        String provincia = paciente.getProvincia();
        String codigoPostal = paciente.getCodigoPostal();
        String calle = paciente.getCalle();
        String numero = paciente.getNumero();
        String piso = paciente.getPiso();

        if (localidad == null || localidad.trim().isEmpty() ||
                codigoPostal == null || codigoPostal.trim().isEmpty() ||
                calle == null || calle.trim().isEmpty()) {
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

            if (numero != null && !numero.trim().isEmpty()) {
                try {
                    stmt.setInt(2, Integer.parseInt(numero.trim()));
                } catch (NumberFormatException e) {
                    stmt.setInt(2, 0);
                }
            } else {
                stmt.setInt(2, 0);
            }

            stmt.setString(3, piso != null ? piso : "");
            stmt.setString(4, codigoPostal);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_direccion");
                }
            }
        }

        return null;
    }

    private void insertarLocalidadSiNoExiste(Connection conn, String localidad, String provincia) throws SQLException {
        String queryVerificar = "SELECT COUNT(*) FROM localidad WHERE nombre_localidad = ?";
        try (PreparedStatement stmtVerificar = conn.prepareStatement(queryVerificar)) {
            stmtVerificar.setString(1, localidad);

            try (ResultSet rs = stmtVerificar.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }
        }

        String queryInsertar = "INSERT INTO localidad (nombre_localidad, provincia) VALUES (?, ?)";
        try (PreparedStatement stmtInsertar = conn.prepareStatement(queryInsertar)) {
            stmtInsertar.setString(1, localidad);
            stmtInsertar.setString(2, provincia != null ? provincia : "");
            stmtInsertar.executeUpdate();
        }
    }

    private void insertarCPSiNoExiste(Connection conn, String codigoPostal, String localidad) throws SQLException {
        String queryVerificar = "SELECT COUNT(*) FROM cp WHERE cp = ?";
        try (PreparedStatement stmtVerificar = conn.prepareStatement(queryVerificar)) {
            stmtVerificar.setString(1, codigoPostal);

            try (ResultSet rs = stmtVerificar.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }
        }

        String queryInsertar = "INSERT INTO cp (cp, nombre_localidad) VALUES (?, ?)";
        try (PreparedStatement stmtInsertar = conn.prepareStatement(queryInsertar)) {
            stmtInsertar.setString(1, codigoPostal);
            stmtInsertar.setString(2, localidad);
            stmtInsertar.executeUpdate();
        }
    }

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

        //Leer campos clinicos con try-catch defensivo (por si la migracion SQL no se ejecuto)
        try {
            String sexo = rs.getString("sexo");
            paciente.setSexo(sexo != null ? sexo : "");

            Date fechaNac = rs.getDate("fecha_nacimiento");
            if (fechaNac != null) {
                paciente.setFechaNacimiento(fechaNac.toLocalDate());
            }

            //Descifrar campos clinicos sensibles (AES-256-GCM)
            String alergias = rs.getString("alergias");
            paciente.setAlergias(alergias != null ? CifradoService.descifrar(alergias) : "");

            String antecedentes = rs.getString("antecedentes");
            paciente.setAntecedentes(antecedentes != null ? CifradoService.descifrar(antecedentes) : "");

            String medicacion = rs.getString("medicacion_actual");
            paciente.setMedicacionActual(medicacion != null ? CifradoService.descifrar(medicacion) : "");

            boolean consentimiento = rs.getBoolean("consentimiento_rgpd");
            paciente.setConsentimientoRgpd(consentimiento);

            Timestamp fechaConsent = rs.getTimestamp("fecha_consentimiento");
            if (fechaConsent != null) {
                paciente.setFechaConsentimiento(fechaConsent.toLocalDateTime());
            }

            boolean activo = rs.getBoolean("activo");
            if (rs.wasNull()) {
                paciente.setActivo(true);
            } else {
                paciente.setActivo(activo);
            }
        } catch (SQLException e) {
            //Columnas no existen aun (migracion SQL pendiente): usar valores por defecto
            System.err.println("Campos clinicos no disponibles en BD (migracion pendiente): " + e.getMessage());
        }

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

            try (ResultSet rs = stmt.executeQuery()) {
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
            }

        } catch (SQLException e) {
            System.err.println("Error al cargar telefonos del paciente: " + e.getMessage());
        }
    }

    /**
     * Carga la direccion de un paciente desde la base de datos
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

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    paciente.setCalle(rs.getString("calle"));

                    int numero = rs.getInt("numero");
                    paciente.setNumero(numero > 0 ? String.valueOf(numero) : "");

                    paciente.setPiso(rs.getString("piso"));
                    paciente.setCodigoPostal(rs.getString("cp"));
                    paciente.setLocalidad(rs.getString("nombre_localidad"));
                    paciente.setProvincia(rs.getString("provincia"));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al cargar direccion del paciente: " + e.getMessage());
        }
    }

    private String obtenerDniSanitarioPorPaciente(Connection conn, String dniPaciente) throws SQLException {
        String query = "SELECT dni_san FROM paciente WHERE dni_pac = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, dniPaciente);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("dni_san");
                }
            }
        }
        return null;
    }

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
