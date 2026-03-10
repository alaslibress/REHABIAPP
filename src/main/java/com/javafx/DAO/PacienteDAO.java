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
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestionar operaciones de base de datos relacionadas con Paciente.
 * Trabaja con las tablas: paciente, telefono_paciente, direccion, localidad, cp.
 *
 * Los campos clinicos sensibles (alergias, antecedentes, medicacion_actual) se
 * cifran con AES-256-GCM via CifradoService antes de persistirse.
 *
 * Los metodos de escritura lanzan excepciones tipadas (DuplicadoException,
 * ValidacionException, ConexionException) en vez de devolver boolean.
 * Los metodos de escritura tienen sobrecargas con Connection para participar
 * en transacciones externas gestionadas desde la capa de servicio.
 */
public class PacienteDAO extends BaseDAO {

    // ==================== METODOS DE CONSULTA ====================

    /**
     * Lista todos los pacientes activos de la base de datos.
     * Solo devuelve campos basicos (sin campos cifrados) para rendimiento.
     * @throws ConexionException si hay error de conexion con la BD
     */
    public List<Paciente> listarTodos() {
        List<Paciente> pacientes = new ArrayList<>();

        String query = "SELECT p.dni_pac, p.nombre_pac, p.apellido1_pac, p.apellido2_pac, " +
                "p.edad_pac, p.email_pac, p.num_ss, p.discapacidad_pac, p.tratamiento_pac, " +
                "p.estado_tratamiento, p.protesis, p.dni_san, p.sexo, p.fecha_nacimiento, p.activo, " +
                "(SELECT string_agg(d.nombre_dis, ', ' ORDER BY d.nombre_dis) " +
                "FROM paciente_discapacidad pd JOIN discapacidad d ON pd.cod_dis = d.cod_dis " +
                "WHERE pd.dni_pac = p.dni_pac) AS discapacidades_asignadas " +
                "FROM paciente p WHERE p.activo = TRUE ORDER BY p.nombre_pac";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                pacientes.add(mapearPacienteBasicoDesdeResultSet(rs));
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al listar pacientes", e);
        }

        return pacientes;
    }

    /**
     * Busca pacientes activos por texto en cualquier campo
     * @throws ConexionException si hay error de conexion con la BD
     */
    public List<Paciente> buscarPorTexto(String texto) {
        List<Paciente> pacientes = new ArrayList<>();

        String query = "SELECT p.dni_pac, p.nombre_pac, p.apellido1_pac, p.apellido2_pac, " +
                "p.edad_pac, p.email_pac, p.num_ss, p.discapacidad_pac, p.tratamiento_pac, " +
                "p.estado_tratamiento, p.protesis, p.dni_san, p.sexo, p.fecha_nacimiento, p.activo, " +
                "(SELECT string_agg(d.nombre_dis, ', ' ORDER BY d.nombre_dis) " +
                "FROM paciente_discapacidad pd JOIN discapacidad d ON pd.cod_dis = d.cod_dis " +
                "WHERE pd.dni_pac = p.dni_pac) AS discapacidades_asignadas " +
                "FROM paciente p WHERE p.activo = TRUE AND (" +
                "LOWER(p.dni_pac) LIKE ? OR LOWER(p.nombre_pac) LIKE ? OR " +
                "LOWER(p.apellido1_pac) LIKE ? OR LOWER(p.apellido2_pac) LIKE ? OR " +
                "LOWER(p.email_pac) LIKE ? OR LOWER(p.num_ss) LIKE ?) " +
                "ORDER BY p.nombre_pac";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            String patron = "%" + texto.toLowerCase() + "%";
            for (int i = 1; i <= 6; i++) {
                stmt.setString(i, patron);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    pacientes.add(mapearPacienteBasicoDesdeResultSet(rs));
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al buscar pacientes", e);
        }

        return pacientes;
    }

    /**
     * Busca un paciente por su DNI con todos los campos clinicos (incluidos cifrados).
     * Usado para la ficha detalle del paciente.
     * @return Paciente encontrado o null
     * @throws ConexionException si hay error de conexion con la BD
     */
    public Paciente buscarPorDni(String dni) {
        String query = "SELECT dni_pac, nombre_pac, apellido1_pac, apellido2_pac, " +
                "edad_pac, email_pac, num_ss, discapacidad_pac, tratamiento_pac, " +
                "estado_tratamiento, protesis, dni_san, sexo, fecha_nacimiento, " +
                "alergias, antecedentes, medicacion_actual, " +
                "consentimiento_rgpd, fecha_consentimiento, activo " +
                "FROM paciente WHERE dni_pac = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearPacienteCompletoDesdeResultSet(rs);
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al buscar paciente por DNI", e);
        }

        return null;
    }

    /**
     * Obtiene un paciente completo por DNI incluyendo telefonos y direccion
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
            throw new ConexionException("Error al verificar email excluyendo DNI", e);
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
            throw new ConexionException("Error al verificar numero SS excluyendo DNI", e);
        }

        return false;
    }

    // ==================== METODOS DE INSERCION ====================

    /**
     * Inserta un nuevo paciente usando una conexion proporcionada (para transacciones externas).
     * Los campos clinicos sensibles se cifran con AES-256-GCM antes de persistirse.
     *
     * @param conn Conexion gestionada externamente (con autoCommit=false)
     * @param paciente Paciente a insertar
     * @throws DuplicadoException si ya existe un paciente con ese DNI, email o NSS
     * @throws ValidacionException si los datos de direccion son incompletos
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void insertar(Connection conn, Paciente paciente) {
        //Insertar direccion y obtener id_direccion
        Integer idDireccion = insertarDireccionCompleta(conn, paciente);

        if (idDireccion == null) {
            throw new ValidacionException("Los datos de direccion son obligatorios e incompletos", "direccion");
        }

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
            stmt.setInt(8, idDireccion);
            stmt.setString(9, paciente.getDiscapacidad() != null ? paciente.getDiscapacidad() : "");
            stmt.setString(10, paciente.getTratamiento() != null ? paciente.getTratamiento() : "");
            stmt.setString(11, paciente.getEstadoTratamiento() != null ? paciente.getEstadoTratamiento() : "");
            stmt.setInt(12, paciente.getProtesis());
            stmt.setInt(13, paciente.getEdad());

            //Campos clinicos v2
            setStringONull(stmt, 14, paciente.getSexo());
            setDateONull(stmt, 15, paciente.getFechaNacimiento());

            //Campos sensibles cifrados con AES-256-GCM
            stmt.setString(16, CifradoService.cifrar(paciente.getAlergias()));
            stmt.setString(17, CifradoService.cifrar(paciente.getAntecedentes()));
            stmt.setString(18, CifradoService.cifrar(paciente.getMedicacionActual()));

            //Consentimiento RGPD
            stmt.setBoolean(19, paciente.isConsentimientoRgpd());

            //Fecha de consentimiento: se establece automaticamente si acepta el consentimiento
            if (paciente.isConsentimientoRgpd()) {
                stmt.setTimestamp(20, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                stmt.setNull(20, Types.TIMESTAMP);
            }

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw traducirPacienteSQLException(e);
        }

        //Actualizar contador de pacientes del sanitario
        actualizarContadorPacientes(conn, paciente.getDniSanitario(), 1);
    }

    /**
     * Inserta un nuevo paciente gestionando su propia transaccion.
     * Metodo de compatibilidad para llamadas sin transaccion externa.
     *
     * @param paciente Paciente a insertar
     * @throws DuplicadoException si ya existe un paciente con ese DNI, email o NSS
     * @throws ValidacionException si los datos son invalidos
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void insertar(Paciente paciente) {
        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);

            try {
                insertar(conn, paciente);
                conn.commit();
            } catch (RuntimeException e) {
                rollback(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al insertar paciente", e);
        }
    }

    /**
     * Inserta los telefonos de un paciente usando una conexion proporcionada.
     *
     * @param conn Conexion gestionada externamente
     * @param dniPaciente DNI del paciente
     * @param telefono1 Primer telefono
     * @param telefono2 Segundo telefono (opcional)
     * @throws ConexionException si hay error de base de datos
     */
    public void insertarTelefonos(Connection conn, String dniPaciente, String telefono1, String telefono2) {
        insertarTelefonos(conn, "telefono_paciente", "dni_pac", dniPaciente, telefono1, telefono2);
    }

    /**
     * Inserta los telefonos gestionando su propia conexion.
     * Metodo de compatibilidad.
     */
    public void insertarTelefonos(String dniPaciente, String telefono1, String telefono2) {
        try (Connection conn = ConexionBD.getConexion()) {
            insertarTelefonos(conn, dniPaciente, telefono1, telefono2);
        } catch (SQLException e) {
            throw new ConexionException("Error al insertar telefonos del paciente", e);
        }
    }

    /**
     * Inserta la foto de un paciente como BYTEA.
     *
     * @param conn Conexion gestionada externamente
     * @param dniPaciente DNI del paciente
     * @param archivoFoto Archivo de la foto
     * @throws ConexionException si hay error al guardar la foto
     */
    public void insertarFoto(Connection conn, String dniPaciente, File archivoFoto) {
        String query = "UPDATE paciente SET foto = ? WHERE dni_pac = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            byte[] fotoBytes = Files.readAllBytes(archivoFoto.toPath());
            stmt.setBytes(1, fotoBytes);
            stmt.setString(2, dniPaciente);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new ConexionException("Error al insertar foto del paciente", e);
        }
    }

    /**
     * Inserta la foto gestionando su propia conexion.
     */
    public void insertarFoto(String dniPaciente, File archivoFoto) {
        try (Connection conn = ConexionBD.getConexion()) {
            insertarFoto(conn, dniPaciente, archivoFoto);
        } catch (SQLException e) {
            throw new ConexionException("Error al insertar foto del paciente", e);
        }
    }

    // ==================== METODOS DE ACTUALIZACION ====================

    /**
     * Actualiza un paciente existente usando conexion externa.
     * Los campos clinicos sensibles se cifran con AES-256-GCM.
     *
     * @param conn Conexion gestionada externamente
     * @param paciente Paciente con los nuevos datos
     * @param dniOriginal DNI original del paciente
     * @throws DuplicadoException si el nuevo DNI/email/NSS ya existe
     * @throws ConexionException si hay error de conexion
     */
    public void actualizar(Connection conn, Paciente paciente, String dniOriginal) {
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

            //Campos clinicos v2
            setStringONull(stmt, 12, paciente.getSexo());
            setDateONull(stmt, 13, paciente.getFechaNacimiento());

            //Campos sensibles cifrados con AES-256-GCM
            stmt.setString(14, CifradoService.cifrar(paciente.getAlergias()));
            stmt.setString(15, CifradoService.cifrar(paciente.getAntecedentes()));
            stmt.setString(16, CifradoService.cifrar(paciente.getMedicacionActual()));

            //Consentimiento RGPD
            stmt.setBoolean(17, paciente.isConsentimientoRgpd());
            if (paciente.isConsentimientoRgpd() && paciente.getFechaConsentimiento() == null) {
                stmt.setTimestamp(18, Timestamp.valueOf(LocalDateTime.now()));
            } else if (paciente.getFechaConsentimiento() != null) {
                stmt.setTimestamp(18, Timestamp.valueOf(paciente.getFechaConsentimiento()));
            } else {
                stmt.setNull(18, Types.TIMESTAMP);
            }

            stmt.setString(19, dniOriginal);

            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas == 0) {
                throw new ValidacionException("No se encontro el paciente con DNI: " + dniOriginal, "dni");
            }
        } catch (SQLException e) {
            throw traducirPacienteSQLException(e);
        }
    }

    /**
     * Actualiza un paciente gestionando su propia transaccion.
     */
    public void actualizar(Paciente paciente, String dniOriginal) {
        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);

            try {
                actualizar(conn, paciente, dniOriginal);
                conn.commit();
            } catch (RuntimeException e) {
                rollback(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al actualizar paciente", e);
        }
    }

    /**
     * Actualiza los telefonos de un paciente usando conexion externa.
     */
    public void actualizarTelefonos(Connection conn, String dniPaciente, String telefono1, String telefono2) {
        actualizarTelefonos(conn, "telefono_paciente", "dni_pac", dniPaciente, telefono1, telefono2);
    }

    /**
     * Actualiza los telefonos gestionando su propia transaccion.
     */
    public void actualizarTelefonos(String dniPaciente, String telefono1, String telefono2) {
        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);

            try {
                actualizarTelefonos(conn, dniPaciente, telefono1, telefono2);
                conn.commit();
            } catch (RuntimeException e) {
                rollback(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al actualizar telefonos del paciente", e);
        }
    }

    /**
     * Actualiza la foto de un paciente
     */
    public void actualizarFoto(String dniPaciente, File archivoFoto) {
        insertarFoto(dniPaciente, archivoFoto);
    }

    // ==================== METODOS DE ELIMINACION ====================

    /**
     * Soft delete: marca un paciente como inactivo en vez de eliminarlo fisicamente.
     * Cumple con Ley 41/2002 (conservacion de historias clinicas 5 anios).
     *
     * @param dni DNI del paciente a desactivar
     * @throws ValidacionException si el paciente no existe
     * @throws ConexionException si hay error de conexion
     */
    public void eliminar(String dni) {
        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);

            try {
                //Obtener DNI del sanitario antes de desactivar para actualizar contador
                String dniSanitario = obtenerDniSanitarioPorPaciente(conn, dni);

                //Soft delete: marcar como inactivo con fecha de baja
                String softDelete = "UPDATE paciente SET activo = FALSE, fecha_baja = CURRENT_TIMESTAMP " +
                        "WHERE dni_pac = ? AND activo = TRUE";
                try (PreparedStatement stmt = conn.prepareStatement(softDelete)) {
                    stmt.setString(1, dni);
                    int filasAfectadas = stmt.executeUpdate();

                    if (filasAfectadas == 0) {
                        throw new ValidacionException("No se encontro el paciente activo con DNI: " + dni, "dni");
                    }
                }

                //Actualizar contador del sanitario
                if (dniSanitario != null) {
                    actualizarContadorPacientes(conn, dniSanitario, -1);
                }

                conn.commit();

            } catch (RuntimeException e) {
                rollback(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al desactivar paciente", e);
        }
    }

    // ==================== METODOS DE FOTO ====================

    /**
     * Obtiene la foto de un paciente como Image de JavaFX
     *
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
                            //Formato de imagen invalido, devolver null
                            return null;
                        }
                    }
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al obtener foto del paciente", e);
        }

        return null;
    }

    // ==================== METODOS AUXILIARES PRIVADOS ====================

    /**
     * Mapea campos basicos del ResultSet a un Paciente.
     * Usado en listados (listarTodos, buscarPorTexto) donde no se necesitan campos cifrados.
     */
    private Paciente mapearPacienteBasicoDesdeResultSet(ResultSet rs) throws SQLException {
        //Usar discapacidades asignadas via N:M si la columna existe, sino campo legacy
        String discapacidades;
        try {
            String asignadas = rs.getString("discapacidades_asignadas");
            discapacidades = (asignadas != null && !asignadas.isEmpty()) ? asignadas :
                    (rs.getString("discapacidad_pac") != null ? rs.getString("discapacidad_pac") : "");
        } catch (SQLException e) {
            //Si la columna no existe en la query, usar campo legacy
            discapacidades = rs.getString("discapacidad_pac") != null ? rs.getString("discapacidad_pac") : "";
        }

        Paciente paciente = new Paciente(
                rs.getString("dni_pac"),
                rs.getString("nombre_pac"),
                rs.getString("apellido1_pac") != null ? rs.getString("apellido1_pac") : "",
                rs.getString("apellido2_pac") != null ? rs.getString("apellido2_pac") : "",
                rs.getInt("edad_pac"),
                rs.getString("email_pac"),
                rs.getString("num_ss"),
                discapacidades,
                rs.getString("tratamiento_pac") != null ? rs.getString("tratamiento_pac") : "",
                rs.getString("estado_tratamiento") != null ? rs.getString("estado_tratamiento") : "",
                rs.getInt("protesis"),
                rs.getString("dni_san")
        );

        //Campos clinicos basicos (no cifrados)
        paciente.setSexo(rs.getString("sexo") != null ? rs.getString("sexo") : "");

        Date fechaNac = rs.getDate("fecha_nacimiento");
        if (fechaNac != null) {
            paciente.setFechaNacimiento(fechaNac.toLocalDate());
        }

        paciente.setActivo(rs.getBoolean("activo"));

        return paciente;
    }

    /**
     * Mapea todos los campos del ResultSet a un Paciente, incluyendo campos cifrados.
     * Usado en buscarPorDni() para la ficha detalle del paciente.
     * Los campos sensibles se descifran con AES-256-GCM.
     */
    private Paciente mapearPacienteCompletoDesdeResultSet(ResultSet rs) throws SQLException {
        Paciente paciente = new Paciente(
                rs.getString("dni_pac"),
                rs.getString("nombre_pac"),
                rs.getString("apellido1_pac") != null ? rs.getString("apellido1_pac") : "",
                rs.getString("apellido2_pac") != null ? rs.getString("apellido2_pac") : "",
                rs.getInt("edad_pac"),
                rs.getString("email_pac"),
                rs.getString("num_ss"),
                rs.getString("discapacidad_pac") != null ? rs.getString("discapacidad_pac") : "",
                rs.getString("tratamiento_pac") != null ? rs.getString("tratamiento_pac") : "",
                rs.getString("estado_tratamiento") != null ? rs.getString("estado_tratamiento") : "",
                rs.getInt("protesis"),
                rs.getString("dni_san")
        );

        //Campos clinicos basicos
        paciente.setSexo(rs.getString("sexo") != null ? rs.getString("sexo") : "");

        Date fechaNac = rs.getDate("fecha_nacimiento");
        if (fechaNac != null) {
            paciente.setFechaNacimiento(fechaNac.toLocalDate());
        }

        //Campos sensibles descifrados con AES-256-GCM
        paciente.setAlergias(CifradoService.descifrar(rs.getString("alergias")));
        paciente.setAntecedentes(CifradoService.descifrar(rs.getString("antecedentes")));
        paciente.setMedicacionActual(CifradoService.descifrar(rs.getString("medicacion_actual")));

        //Consentimiento RGPD
        paciente.setConsentimientoRgpd(rs.getBoolean("consentimiento_rgpd"));

        Timestamp fechaConsentimiento = rs.getTimestamp("fecha_consentimiento");
        if (fechaConsentimiento != null) {
            paciente.setFechaConsentimiento(fechaConsentimiento.toLocalDateTime());
        }

        paciente.setActivo(rs.getBoolean("activo"));

        return paciente;
    }

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
            throw new ConexionException("Error al cargar telefonos del paciente", e);
        }
    }

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
            throw new ConexionException("Error al cargar direccion del paciente", e);
        }
    }

    /**
     * Inserta la direccion completa del paciente (localidad -> cp -> direccion)
     */
    private Integer insertarDireccionCompleta(Connection conn, Paciente paciente) {
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

        try {
            insertarLocalidadSiNoExiste(conn, localidad, provincia);
            insertarCPSiNoExiste(conn, codigoPostal, localidad);

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

        } catch (SQLException e) {
            throw new ConexionException("Error al insertar direccion", e);
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

    private void actualizarContadorPacientes(Connection conn, String dniSanitario, int incremento) {
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
        } catch (SQLException e) {
            throw new ConexionException("Error al actualizar contador de pacientes", e);
        }
    }

    /**
     * Traduce SQLExceptions especificas de paciente a excepciones tipadas
     */
    private RuntimeException traducirPacienteSQLException(SQLException e) {
        String sqlState = e.getSQLState();
        String mensaje = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (sqlState != null && "23505".equals(sqlState)) {
            //Unique violation - determinar que campo
            if (mensaje.contains("dni_pac") || mensaje.contains("paciente_pkey")) {
                return new DuplicadoException("Ya existe un paciente con ese DNI", "DNI", e);
            }
            if (mensaje.contains("email_pac")) {
                return new DuplicadoException("Ya existe un paciente con ese email", "email", e);
            }
            if (mensaje.contains("num_ss")) {
                return new DuplicadoException("Ya existe un paciente con ese numero de seguridad social", "NSS", e);
            }
            return new DuplicadoException("Ya existe un paciente con esos datos", "datos", e);
        }

        if (sqlState != null && "23503".equals(sqlState)) {
            return new ValidacionException("Referencia invalida: sanitario o direccion no encontrada", "referencia", e);
        }

        return new ConexionException("Error de base de datos en operacion de paciente", e);
    }

    // ==================== UTILIDADES ====================

    /**
     * Establece un String en el PreparedStatement, o NULL si esta vacio
     */
    private void setStringONull(PreparedStatement stmt, int indice, String valor) throws SQLException {
        if (valor != null && !valor.trim().isEmpty()) {
            stmt.setString(indice, valor);
        } else {
            stmt.setNull(indice, Types.VARCHAR);
        }
    }

    /**
     * Establece un Date en el PreparedStatement, o NULL si es null
     */
    private void setDateONull(PreparedStatement stmt, int indice, LocalDate fecha) throws SQLException {
        if (fecha != null) {
            stmt.setDate(indice, Date.valueOf(fecha));
        } else {
            stmt.setNull(indice, Types.DATE);
        }
    }

    private void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                //Rollback fallido, no se puede hacer nada mas
            }
        }
    }
}
