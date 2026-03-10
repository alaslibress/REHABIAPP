package com.javafx.DAO;

import com.javafx.Clases.Sanitario;
import com.javafx.Clases.ConexionBD;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.ValidacionException;
import com.javafx.util.CifradoUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestionar operaciones de base de datos relacionadas con Sanitario.
 * Trabaja con las tablas: sanitario, sanitario_agrega_sanitario, telefono_sanitario.
 *
 * Contrasenas hasheadas con BCrypt (factor 12) via CifradoUtil.
 * Soporta migracion perezosa de contrasenas en texto plano a BCrypt.
 *
 * Los metodos de escritura lanzan excepciones tipadas en vez de devolver boolean.
 * Los metodos de escritura tienen sobrecargas con Connection para transacciones externas.
 */
public class SanitarioDAO extends BaseDAO {

    // ==================== METODOS DE CONSULTA ====================

    /**
     * Lista todos los sanitarios activos de la base de datos con su cargo.
     * Filtra por activo = TRUE para respetar el soft delete.
     * @throws ConexionException si hay error de conexion con la BD
     */
    public List<Sanitario> listarTodos() {
        List<Sanitario> sanitarios = new ArrayList<>();

        String query = "SELECT s.dni_san, s.nombre_san, s.apellido1_san, s.apellido2_san, " +
                "s.email_san, s.num_de_pacientes, s.contrasena_san, sas.cargo " +
                "FROM sanitario s " +
                "LEFT JOIN sanitario_agrega_sanitario sas ON s.dni_san = sas.dni_san " +
                "WHERE s.activo = TRUE " +
                "ORDER BY s.nombre_san";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                sanitarios.add(mapearSanitarioDesdeResultSet(rs));
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al listar sanitarios", e);
        }

        return sanitarios;
    }

    /**
     * Busca sanitarios activos que coincidan con el texto en cualquier campo
     * @throws ConexionException si hay error de conexion con la BD
     */
    public List<Sanitario> buscarPorTexto(String texto) {
        List<Sanitario> sanitarios = new ArrayList<>();

        String query = "SELECT s.dni_san, s.nombre_san, s.apellido1_san, s.apellido2_san, " +
                "s.email_san, s.num_de_pacientes, s.contrasena_san, sas.cargo " +
                "FROM sanitario s " +
                "LEFT JOIN sanitario_agrega_sanitario sas ON s.dni_san = sas.dni_san " +
                "WHERE s.activo = TRUE AND (" +
                "LOWER(s.dni_san) LIKE ? OR LOWER(s.nombre_san) LIKE ? OR " +
                "LOWER(s.apellido1_san) LIKE ? OR LOWER(s.apellido2_san) LIKE ? OR " +
                "LOWER(s.email_san) LIKE ?) ORDER BY s.nombre_san";

        String patron = "%" + texto.toLowerCase() + "%";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            for (int i = 1; i <= 5; i++) {
                stmt.setString(i, patron);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sanitarios.add(mapearSanitarioDesdeResultSet(rs));
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al buscar sanitarios", e);
        }

        return sanitarios;
    }

    /**
     * Busca un sanitario por su DNI (independientemente de si esta activo)
     * @return Sanitario encontrado o null
     * @throws ConexionException si hay error de conexion con la BD
     */
    public Sanitario buscarPorDni(String dni) {
        String query = "SELECT s.dni_san, s.nombre_san, s.apellido1_san, s.apellido2_san, " +
                "s.email_san, s.num_de_pacientes, s.contrasena_san, sas.cargo " +
                "FROM sanitario s " +
                "LEFT JOIN sanitario_agrega_sanitario sas ON s.dni_san = sas.dni_san " +
                "WHERE s.dni_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearSanitarioDesdeResultSet(rs);
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al buscar sanitario por DNI", e);
        }

        return null;
    }

    /**
     * Obtiene un sanitario completo por DNI incluyendo sus telefonos
     */
    public Sanitario obtenerPorDNI(String dni) {
        Sanitario sanitario = buscarPorDni(dni);

        if (sanitario != null) {
            cargarTelefonosSanitario(sanitario);
        }

        return sanitario;
    }

    /**
     * Verifica si existe un sanitario con el DNI especificado
     */
    public boolean existeDni(String dni) {
        String query = "SELECT COUNT(*) FROM sanitario WHERE dni_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al verificar DNI del sanitario", e);
        }

        return false;
    }

    /**
     * Verifica si existe un sanitario con el email especificado
     */
    public boolean existeEmail(String email) {
        String query = "SELECT COUNT(*) FROM sanitario WHERE email_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al verificar email del sanitario", e);
        }

        return false;
    }

    /**
     * Verifica si existe un email excluyendo un DNI especifico
     */
    public boolean existeEmailExcluyendoDni(String email, String dniExcluir) {
        String query = "SELECT COUNT(*) FROM sanitario WHERE email_san = ? AND dni_san != ?";

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

    // ==================== METODOS DE INSERCION ====================

    /**
     * Inserta un nuevo sanitario usando conexion proporcionada (para transacciones externas).
     * La contrasena se hashea con BCrypt antes de persistirse.
     *
     * @param conn Conexion gestionada externamente (con autoCommit=false)
     * @param sanitario Sanitario a insertar
     * @throws DuplicadoException si ya existe un sanitario con ese DNI o email
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void insertar(Connection conn, Sanitario sanitario) {
        //Insertar en tabla sanitario con contrasena hasheada con BCrypt
        String querySanitario = "INSERT INTO sanitario (dni_san, nombre_san, apellido1_san, " +
                "apellido2_san, email_san, num_de_pacientes, contrasena_san, activo) " +
                "VALUES (?, ?, ?, ?, ?, 0, ?, TRUE)";

        try (PreparedStatement stmt = conn.prepareStatement(querySanitario)) {
            stmt.setString(1, sanitario.getDni());
            stmt.setString(2, sanitario.getNombre());
            stmt.setString(3, sanitario.getApellido1());
            stmt.setString(4, sanitario.getApellido2());
            stmt.setString(5, sanitario.getEmail());
            //Hashear la contrasena con BCrypt factor 12
            stmt.setString(6, CifradoUtil.hashContrasena(sanitario.getContrasena()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw traducirSanitarioSQLException(e);
        }

        //Insertar cargo
        String queryCargo = "INSERT INTO sanitario_agrega_sanitario (dni_san, cargo) VALUES (?, ?)";
        try (PreparedStatement stmtCargo = conn.prepareStatement(queryCargo)) {
            stmtCargo.setString(1, sanitario.getDni());
            stmtCargo.setString(2, sanitario.getCargo());
            stmtCargo.executeUpdate();
        } catch (SQLException e) {
            throw new ConexionException("Error al insertar cargo del sanitario", e);
        }
    }

    /**
     * Inserta un nuevo sanitario gestionando su propia transaccion.
     */
    public void insertar(Sanitario sanitario) {
        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);

            try {
                insertar(conn, sanitario);
                conn.commit();
            } catch (RuntimeException e) {
                rollback(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al insertar sanitario", e);
        }
    }

    /**
     * Inserta los telefonos de un sanitario usando conexion externa.
     */
    public void insertarTelefonos(Connection conn, String dniSanitario, String telefono1, String telefono2) {
        insertarTelefonos(conn, "telefono_sanitario", "dni_san", dniSanitario, telefono1, telefono2);
    }

    /**
     * Inserta los telefonos gestionando su propia conexion.
     */
    public void insertarTelefonos(String dniSanitario, String telefono1, String telefono2) {
        try (Connection conn = ConexionBD.getConexion()) {
            insertarTelefonos(conn, dniSanitario, telefono1, telefono2);
        } catch (SQLException e) {
            throw new ConexionException("Error al insertar telefonos del sanitario", e);
        }
    }

    // ==================== METODOS DE ACTUALIZACION ====================

    /**
     * Actualiza un sanitario usando conexion externa.
     * La contrasena NO se rehashea aqui (viene ya hasheada o sin cambiar).
     *
     * @param conn Conexion gestionada externamente
     * @param sanitario Sanitario con los nuevos datos
     * @param dniOriginal DNI original del sanitario
     * @throws DuplicadoException si el nuevo DNI o email ya existe
     * @throws ValidacionException si el sanitario no existe
     * @throws ConexionException si hay error de conexion
     */
    public void actualizar(Connection conn, Sanitario sanitario, String dniOriginal) {
        //Actualizar tabla sanitario
        String querySanitario = "UPDATE sanitario SET dni_san = ?, nombre_san = ?, " +
                "apellido1_san = ?, apellido2_san = ?, email_san = ?, contrasena_san = ? " +
                "WHERE dni_san = ?";

        try (PreparedStatement stmt = conn.prepareStatement(querySanitario)) {
            stmt.setString(1, sanitario.getDni());
            stmt.setString(2, sanitario.getNombre());
            stmt.setString(3, sanitario.getApellido1());
            stmt.setString(4, sanitario.getApellido2());
            stmt.setString(5, sanitario.getEmail());
            stmt.setString(6, sanitario.getContrasena());
            stmt.setString(7, dniOriginal);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas == 0) {
                throw new ValidacionException("No se encontro el sanitario con DNI: " + dniOriginal, "dni");
            }
        } catch (SQLException e) {
            throw traducirSanitarioSQLException(e);
        }

        //Actualizar cargo
        String queryCargo = "UPDATE sanitario_agrega_sanitario SET cargo = ? WHERE dni_san = ?";
        try (PreparedStatement stmtCargo = conn.prepareStatement(queryCargo)) {
            stmtCargo.setString(1, sanitario.getCargo());
            stmtCargo.setString(2, sanitario.getDni());
            stmtCargo.executeUpdate();
        } catch (SQLException e) {
            throw new ConexionException("Error al actualizar cargo del sanitario", e);
        }
    }

    /**
     * Actualiza un sanitario gestionando su propia transaccion.
     */
    public void actualizar(Sanitario sanitario, String dniOriginal) {
        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);

            try {
                actualizar(conn, sanitario, dniOriginal);
                conn.commit();
            } catch (RuntimeException e) {
                rollback(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al actualizar sanitario", e);
        }
    }

    /**
     * Actualiza los telefonos de un sanitario usando conexion externa.
     */
    public void actualizarTelefonos(Connection conn, String dniSanitario, String telefono1, String telefono2) {
        actualizarTelefonos(conn, "telefono_sanitario", "dni_san", dniSanitario, telefono1, telefono2);
    }

    /**
     * Actualiza los telefonos gestionando su propia transaccion.
     */
    public void actualizarTelefonos(String dniSanitario, String telefono1, String telefono2) {
        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);

            try {
                actualizarTelefonos(conn, dniSanitario, telefono1, telefono2);
                conn.commit();
            } catch (RuntimeException e) {
                rollback(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al actualizar telefonos del sanitario", e);
        }
    }

    // ==================== METODOS DE ELIMINACION ====================

    /**
     * Soft delete: marca un sanitario como inactivo en vez de eliminarlo fisicamente.
     *
     * @param dni DNI del sanitario a desactivar
     * @throws ValidacionException si el sanitario no existe
     * @throws ConexionException si hay error de conexion
     */
    public void eliminar(String dni) {
        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);

            try {
                String softDelete = "UPDATE sanitario SET activo = FALSE, fecha_baja = CURRENT_TIMESTAMP " +
                        "WHERE dni_san = ? AND activo = TRUE";
                try (PreparedStatement stmt = conn.prepareStatement(softDelete)) {
                    stmt.setString(1, dni);
                    int filasAfectadas = stmt.executeUpdate();

                    if (filasAfectadas == 0) {
                        throw new ValidacionException("No se encontro el sanitario activo con DNI: " + dni, "dni");
                    }
                }

                conn.commit();

            } catch (RuntimeException e) {
                rollback(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al desactivar sanitario", e);
        }
    }

    // ==================== METODOS AUXILIARES PRIVADOS ====================

    private Sanitario mapearSanitarioDesdeResultSet(ResultSet rs) throws SQLException {
        Sanitario sanitario = new Sanitario(
                rs.getString("dni_san"),
                rs.getString("nombre_san"),
                rs.getString("apellido1_san") != null ? rs.getString("apellido1_san") : "",
                rs.getString("apellido2_san") != null ? rs.getString("apellido2_san") : "",
                rs.getString("email_san"),
                rs.getString("cargo") != null ? rs.getString("cargo") : "",
                rs.getInt("num_de_pacientes")
        );

        sanitario.setContrasena(rs.getString("contrasena_san"));
        return sanitario;
    }

    private void cargarTelefonosSanitario(Sanitario sanitario) {
        String queryTel = "SELECT telefono FROM telefono_sanitario WHERE dni_san = ? ORDER BY id_telefono LIMIT 2";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(queryTel)) {

            stmt.setString(1, sanitario.getDni());

            try (ResultSet rs = stmt.executeQuery()) {
                int indice = 0;
                while (rs.next() && indice < 2) {
                    String telefono = rs.getString("telefono");
                    if (indice == 0) {
                        sanitario.setTelefono1(telefono);
                    } else {
                        sanitario.setTelefono2(telefono);
                    }
                    indice++;
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al cargar telefonos del sanitario", e);
        }
    }

    /**
     * Traduce SQLExceptions especificas de sanitario a excepciones tipadas
     */
    private RuntimeException traducirSanitarioSQLException(SQLException e) {
        String sqlState = e.getSQLState();
        String mensaje = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (sqlState != null && "23505".equals(sqlState)) {
            if (mensaje.contains("dni_san") || mensaje.contains("sanitario_pkey")) {
                return new DuplicadoException("Ya existe un sanitario con ese DNI", "DNI", e);
            }
            if (mensaje.contains("email_san")) {
                return new DuplicadoException("Ya existe un sanitario con ese email", "email", e);
            }
            return new DuplicadoException("Ya existe un sanitario con esos datos", "datos", e);
        }

        return new ConexionException("Error de base de datos en operacion de sanitario", e);
    }

    // ==================== METODOS DE AUTENTICACION ====================

    /**
     * Autentica un sanitario con DNI y contrasena.
     * Soporta migracion perezosa: si la contrasena almacenada es texto plano
     * y coincide, la rehashea con BCrypt automaticamente.
     *
     * @return Sanitario si las credenciales son correctas, null si no
     */
    public Sanitario autenticar(String dni, String contrasena) {
        //Buscar sanitario solo por DNI (sin filtro de contrasena en SQL)
        String query = "SELECT s.dni_san, s.nombre_san, s.apellido1_san, s.apellido2_san, " +
                "s.email_san, s.num_de_pacientes, s.contrasena_san, sas.cargo " +
                "FROM sanitario s " +
                "LEFT JOIN sanitario_agrega_sanitario sas ON s.dni_san = sas.dni_san " +
                "WHERE s.dni_san = ? AND s.activo = TRUE";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni.toUpperCase());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String hashAlmacenado = rs.getString("contrasena_san");

                    //Verificar con BCrypt (compatible con texto plano via CifradoUtil)
                    if (CifradoUtil.verificarContrasena(contrasena, hashAlmacenado)) {

                        //Migracion perezosa: si era texto plano, rehashear con BCrypt
                        if (!CifradoUtil.esBCrypt(hashAlmacenado)) {
                            migrarContrasenaBCrypt(dni.toUpperCase(), contrasena);
                        }

                        return mapearSanitarioDesdeResultSet(rs);
                    }
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al autenticar sanitario", e);
        }

        return null;
    }

    /**
     * Migra una contrasena de texto plano a BCrypt en la base de datos.
     * Operacion fire-and-forget: si falla, no afecta al login.
     * Usa actualizarHashDirecto() para evitar doble hash.
     */
    private void migrarContrasenaBCrypt(String dni, String contrasenaPlana) {
        try {
            String hash = CifradoUtil.hashContrasena(contrasenaPlana);
            actualizarHashDirecto(dni, hash);
            System.out.println("Migracion BCrypt completada para: " + dni);
        } catch (Exception e) {
            //Fire-and-forget: no interrumpir el login si la migracion falla
            System.err.println("Error en migracion BCrypt para " + dni + ": " + e.getMessage());
        }
    }

    /**
     * Actualiza el hash de contrasena directamente en la BD sin rehashear.
     * Solo para uso interno (migracion perezosa).
     */
    private void actualizarHashDirecto(String dni, String hash) {
        String query = "UPDATE sanitario SET contrasena_san = ? WHERE dni_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, hash);
            stmt.setString(2, dni);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new ConexionException("Error al actualizar hash de contrasena", e);
        }
    }

    /**
     * Verifica si existe un usuario admin en el sistema
     */
    public boolean existeAdmin() {
        String query = "SELECT COUNT(*) FROM sanitario WHERE dni_san = 'ADMIN0000'";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al verificar admin", e);
        }

        return false;
    }

    /**
     * Crea el usuario admin por defecto si no existe.
     * La contrasena "admin" se hashea con BCrypt.
     */
    public void crearAdminSiNoExiste() {
        if (existeAdmin()) {
            return;
        }

        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);

            try {
                //Hashear la contrasena admin con BCrypt
                String hashAdmin = CifradoUtil.hashContrasena("admin");

                String queryInsertSanitario = "INSERT INTO sanitario (dni_san, nombre_san, apellido1_san, " +
                        "apellido2_san, email_san, num_de_pacientes, contrasena_san, activo) " +
                        "VALUES ('ADMIN0000', 'Administrador', 'Sistema', '', 'admin@rehabiapp.com', 0, ?, TRUE)";

                try (PreparedStatement stmt = conn.prepareStatement(queryInsertSanitario)) {
                    stmt.setString(1, hashAdmin);
                    stmt.executeUpdate();
                }

                String queryInsertCargo = "INSERT INTO sanitario_agrega_sanitario (dni_san, cargo) " +
                        "VALUES ('ADMIN0000', 'medico especialista')";

                try (PreparedStatement stmt = conn.prepareStatement(queryInsertCargo)) {
                    stmt.executeUpdate();
                }

                conn.commit();

            } catch (RuntimeException e) {
                rollback(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al crear admin", e);
        }
    }

    /**
     * Cambia la contrasena de un sanitario.
     * La contrasena se recibe en texto plano y se hashea con BCrypt antes de persistirse.
     *
     * @param dni DNI del sanitario
     * @param nuevaContrasena Contrasena en texto plano
     * @throws ValidacionException si el sanitario no existe
     * @throws ConexionException si hay error de conexion
     */
    public void cambiarContrasena(String dni, String nuevaContrasena) {
        String query = "UPDATE sanitario SET contrasena_san = ? WHERE dni_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            //Hashear la contrasena con BCrypt antes de guardar
            stmt.setString(1, CifradoUtil.hashContrasena(nuevaContrasena));
            stmt.setString(2, dni);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas == 0) {
                throw new ValidacionException("No se encontro el sanitario con DNI: " + dni, "dni");
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al cambiar contrasena", e);
        }
    }

    /**
     * Verifica si la contrasena actual es correcta.
     * Compatible con contrasenas BCrypt y texto plano.
     */
    public boolean verificarContrasena(String dni, String contrasena) {
        String query = "SELECT contrasena_san FROM sanitario WHERE dni_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String hashAlmacenado = rs.getString("contrasena_san");
                    return CifradoUtil.verificarContrasena(contrasena, hashAlmacenado);
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al verificar contrasena", e);
        }

        return false;
    }

    /**
     * Actualiza el cargo de un sanitario
     */
    public void actualizarCargo(String dni, String cargo) {
        String query = "UPDATE sanitario_agrega_sanitario SET cargo = ? WHERE dni_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, cargo.toLowerCase());
            stmt.setString(2, dni);

            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas == 0) {
                //Si no existe registro, insertarlo
                insertarCargo(dni, cargo);
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al actualizar cargo", e);
        }
    }

    private void insertarCargo(String dni, String cargo) {
        String query = "INSERT INTO sanitario_agrega_sanitario (dni_san, cargo) VALUES (?, ?)";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);
            stmt.setString(2, cargo.toLowerCase());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new ConexionException("Error al insertar cargo", e);
        }
    }

    /**
     * Elimina todos los telefonos de un sanitario
     */
    public void eliminarTelefonos(String dni) {
        String query = "DELETE FROM telefono_sanitario WHERE dni_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new ConexionException("Error al eliminar telefonos del sanitario", e);
        }
    }

    /**
     * Inserta un telefono para un sanitario
     */
    public void insertarTelefono(String dni, String telefono) {
        String query = "INSERT INTO telefono_sanitario (dni_san, telefono) VALUES (?, ?)";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);
            stmt.setString(2, telefono);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new ConexionException("Error al insertar telefono del sanitario", e);
        }
    }

    /**
     * Obtiene los telefonos de un sanitario como String
     */
    public String obtenerTelefonos(String dni) {
        String query = "SELECT telefono FROM telefono_sanitario WHERE dni_san = ?";
        StringBuilder telefonos = new StringBuilder();

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    if (telefonos.length() > 0) {
                        telefonos.append(", ");
                    }
                    telefonos.append(rs.getString("telefono"));
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al obtener telefonos del sanitario", e);
        }

        return telefonos.toString();
    }

    // ==================== UTILIDADES DE TRANSACCION ====================

    private void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                //Rollback fallido
            }
        }
    }
}
