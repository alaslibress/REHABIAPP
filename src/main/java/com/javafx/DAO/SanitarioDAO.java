package com.javafx.DAO;

import com.javafx.Clases.Sanitario;
import com.javafx.Clases.ConexionBD;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.ValidacionException;
import com.javafx.service.AuditService;
import com.javafx.util.CifradoUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase DAO para gestionar operaciones de base de datos de Sanitario.
 * Trabaja con las tablas: sanitario, sanitario_agrega_sanitario, telefono_sanitario.
 *
 * Los metodos de escritura lanzan excepciones personalizadas en vez de devolver boolean.
 * El metodo autenticar() sigue devolviendo Sanitario o null (login fallido no es excepcion).
 */
public class SanitarioDAO {

    // ==================== METODOS DE CONSULTA ====================

    /**
     * Lista todos los sanitarios activos con su cargo
     * @return Lista de sanitarios ordenados por nombre
     * @throws ConexionException si hay error de conexion con la BD
     */
    public List<Sanitario> listarTodos() {
        List<Sanitario> sanitarios = new ArrayList<>();

        String query = "SELECT s.dni_san, s.nombre_san, s.apellido1_san, s.apellido2_san, " +
                "s.email_san, s.num_de_pacientes, s.contrasena_san, sas.cargo " +
                "FROM sanitario s " +
                "LEFT JOIN sanitario_agrega_sanitario sas ON s.dni_san = sas.dni_san " +
                "WHERE (s.activo IS NULL OR s.activo = TRUE) " +
                "ORDER BY s.nombre_san";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Sanitario sanitario = mapearSanitarioDesdeResultSet(rs);
                sanitarios.add(sanitario);
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al listar sanitarios", e);
        }

        return sanitarios;
    }

    /**
     * Busca sanitarios que coincidan con el texto en cualquier campo
     * @param texto Texto a buscar
     * @return Lista de sanitarios que coinciden
     * @throws ConexionException si hay error de conexion con la BD
     */
    public List<Sanitario> buscarPorTexto(String texto) {
        List<Sanitario> sanitarios = new ArrayList<>();

        String query = "SELECT s.dni_san, s.nombre_san, s.apellido1_san, s.apellido2_san, " +
                "s.email_san, s.num_de_pacientes, s.contrasena_san, sas.cargo " +
                "FROM sanitario s " +
                "LEFT JOIN sanitario_agrega_sanitario sas ON s.dni_san = sas.dni_san " +
                "WHERE (s.activo IS NULL OR s.activo = TRUE) AND (" +
                "LOWER(s.dni_san) LIKE ? OR LOWER(s.nombre_san) LIKE ? OR " +
                "LOWER(s.apellido1_san) LIKE ? OR LOWER(s.apellido2_san) LIKE ? OR " +
                "LOWER(s.email_san) LIKE ?) ORDER BY s.nombre_san";

        String patron = "%" + texto.toLowerCase() + "%";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, patron);
            stmt.setString(2, patron);
            stmt.setString(3, patron);
            stmt.setString(4, patron);
            stmt.setString(5, patron);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Sanitario sanitario = mapearSanitarioDesdeResultSet(rs);
                    sanitarios.add(sanitario);
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al buscar sanitarios", e);
        }

        return sanitarios;
    }

    /**
     * Busca un sanitario por su DNI
     * @param dni DNI del sanitario
     * @return Sanitario encontrado o null si no existe
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
     * @param dni DNI del sanitario
     * @return Sanitario con todos sus datos o null si no existe
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
     * @throws ConexionException si hay error de conexion con la BD
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
            throw new ConexionException("Error al verificar DNI", e);
        }

        return false;
    }

    /**
     * Verifica si existe un sanitario con el email especificado
     * @throws ConexionException si hay error de conexion con la BD
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
            throw new ConexionException("Error al verificar email", e);
        }

        return false;
    }

    /**
     * Verifica si existe un email excluyendo un DNI especifico
     * @throws ConexionException si hay error de conexion con la BD
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
            throw new ConexionException("Error al verificar email", e);
        }

        return false;
    }

    // ==================== METODOS DE INSERCION ====================

    /**
     * Inserta un nuevo sanitario en la base de datos
     * @param sanitario Sanitario a insertar
     * @throws DuplicadoException si el DNI o email ya existe
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void insertar(Sanitario sanitario) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            insertar(conn, sanitario);

            conn.commit();

        } catch (SQLException e) {
            hacerRollback(conn);
            throw traducirSQLException(e);

        } finally {
            cerrarConexion(conn);
        }
    }

    /**
     * Inserta un sanitario usando conexion externa (para transacciones compuestas)
     */
    public void insertar(Connection conn, Sanitario sanitario) throws SQLException {
        //Insertar en tabla sanitario
        String querySanitario = "INSERT INTO sanitario (dni_san, nombre_san, apellido1_san, " +
                "apellido2_san, email_san, num_de_pacientes, contrasena_san) " +
                "VALUES (?, ?, ?, ?, ?, 0, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(querySanitario)) {
            stmt.setString(1, sanitario.getDni());
            stmt.setString(2, sanitario.getNombre());
            stmt.setString(3, sanitario.getApellido1());
            stmt.setString(4, sanitario.getApellido2());
            stmt.setString(5, sanitario.getEmail());
            //Hashear contrasena con BCrypt antes de almacenar
            stmt.setString(6, CifradoUtil.hashContrasena(sanitario.getContrasena()));
            stmt.executeUpdate();
        }

        //Insertar cargo en tabla sanitario_agrega_sanitario
        String queryCargo = "INSERT INTO sanitario_agrega_sanitario (dni_san, cargo) VALUES (?, ?)";
        try (PreparedStatement stmtCargo = conn.prepareStatement(queryCargo)) {
            stmtCargo.setString(1, sanitario.getDni());
            stmtCargo.setString(2, sanitario.getCargo());
            stmtCargo.executeUpdate();
        }
    }

    /**
     * Inserta los telefonos de un sanitario
     * @param dniSanitario DNI del sanitario
     * @param telefono1 Primer telefono
     * @param telefono2 Segundo telefono (opcional)
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void insertarTelefonos(String dniSanitario, String telefono1, String telefono2) {
        try (Connection conn = ConexionBD.getConexion()) {
            insertarTelefonos(conn, dniSanitario, telefono1, telefono2);
        } catch (SQLException e) {
            throw new ConexionException("Error al insertar telefonos", e);
        }
    }

    /**
     * Inserta telefonos usando conexion externa (para transacciones compuestas)
     */
    public void insertarTelefonos(Connection conn, String dniSanitario, String telefono1, String telefono2) throws SQLException {
        String query = "INSERT INTO telefono_sanitario (dni_san, telefono) VALUES (?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            if (telefono1 != null && !telefono1.trim().isEmpty()) {
                stmt.setString(1, dniSanitario);
                stmt.setString(2, telefono1.trim());
                stmt.executeUpdate();
            }

            if (telefono2 != null && !telefono2.trim().isEmpty()) {
                stmt.setString(1, dniSanitario);
                stmt.setString(2, telefono2.trim());
                stmt.executeUpdate();
            }
        }
    }

    // ==================== METODOS DE ACTUALIZACION ====================

    /**
     * Actualiza los datos de un sanitario existente
     * @param sanitario Sanitario con los nuevos datos
     * @param dniOriginal DNI original del sanitario
     * @throws DuplicadoException si el nuevo DNI o email ya existe
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void actualizar(Sanitario sanitario, String dniOriginal) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

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
                stmt.executeUpdate();
            }

            //Actualizar cargo
            String queryCargo = "UPDATE sanitario_agrega_sanitario SET cargo = ? WHERE dni_san = ?";
            try (PreparedStatement stmtCargo = conn.prepareStatement(queryCargo)) {
                stmtCargo.setString(1, sanitario.getCargo());
                stmtCargo.setString(2, sanitario.getDni());
                stmtCargo.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            hacerRollback(conn);
            throw traducirSQLException(e);

        } finally {
            cerrarConexion(conn);
        }
    }

    /**
     * Actualiza los telefonos de un sanitario
     * @param dniSanitario DNI del sanitario
     * @param telefono1 Primer telefono
     * @param telefono2 Segundo telefono
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void actualizarTelefonos(String dniSanitario, String telefono1, String telefono2) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            //Eliminar telefonos existentes
            String queryEliminar = "DELETE FROM telefono_sanitario WHERE dni_san = ?";
            try (PreparedStatement stmtEliminar = conn.prepareStatement(queryEliminar)) {
                stmtEliminar.setString(1, dniSanitario);
                stmtEliminar.executeUpdate();
            }

            //Insertar nuevos telefonos
            String queryInsertar = "INSERT INTO telefono_sanitario (dni_san, telefono) VALUES (?, ?)";
            try (PreparedStatement stmtInsertar = conn.prepareStatement(queryInsertar)) {

                if (telefono1 != null && !telefono1.trim().isEmpty()) {
                    stmtInsertar.setString(1, dniSanitario);
                    stmtInsertar.setString(2, telefono1.trim());
                    stmtInsertar.executeUpdate();
                }

                if (telefono2 != null && !telefono2.trim().isEmpty()) {
                    stmtInsertar.setString(1, dniSanitario);
                    stmtInsertar.setString(2, telefono2.trim());
                    stmtInsertar.executeUpdate();
                }
            }

            conn.commit();

        } catch (SQLException e) {
            hacerRollback(conn);
            throw new ConexionException("Error al actualizar telefonos", e);

        } finally {
            cerrarConexion(conn);
        }
    }

    // ==================== METODOS DE ELIMINACION ====================

    /**
     * Realiza una baja logica (soft delete) de un sanitario.
     * @param dni DNI del sanitario a dar de baja
     * @throws ConexionException si hay error de conexion con la BD
     * @throws ValidacionException si el sanitario no existe
     */
    public void eliminar(String dni) {
        String softDelete = "UPDATE sanitario SET activo = FALSE, fecha_baja = CURRENT_TIMESTAMP WHERE dni_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(softDelete)) {

            stmt.setString(1, dni);
            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas == 0) {
                throw new ValidacionException("No se encontro el sanitario con DNI: " + dni, "dni");
            }

            System.out.println("Sanitario dado de baja (soft delete): " + dni);

        } catch (SQLException e) {
            throw new ConexionException("Error al dar de baja sanitario", e);
        }
    }

    // ==================== METODOS AUXILIARES PRIVADOS ====================

    private Sanitario mapearSanitarioDesdeResultSet(ResultSet rs) throws SQLException {
        String dni = rs.getString("dni_san");
        String nombre = rs.getString("nombre_san");
        String apellido1 = rs.getString("apellido1_san");
        String apellido2 = rs.getString("apellido2_san");
        String email = rs.getString("email_san");
        int numPacientes = rs.getInt("num_de_pacientes");
        String contrasena = rs.getString("contrasena_san");
        String cargo = rs.getString("cargo");

        Sanitario sanitario = new Sanitario(
                dni,
                nombre,
                apellido1 != null ? apellido1 : "",
                apellido2 != null ? apellido2 : "",
                email,
                cargo != null ? cargo : "",
                numPacientes
        );

        sanitario.setContrasena(contrasena);

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
            System.err.println("Error al cargar telefonos del sanitario: " + e.getMessage());
        }
    }

    /**
     * Traduce una SQLException a la excepcion personalizada correspondiente
     */
    private RuntimeException traducirSQLException(SQLException e) {
        String sqlState = e.getSQLState();
        String mensaje = e.getMessage();

        if (sqlState != null && "23505".equals(sqlState)) {
            String campo = detectarCampoDuplicado(mensaje);
            return new DuplicadoException("Ya existe un registro con ese " + campo, campo, e);
        }

        if (sqlState != null && "23503".equals(sqlState)) {
            return new ValidacionException("Referencia invalida: " + mensaje, "clave_foranea", e);
        }

        return new ConexionException("Error de base de datos: " + mensaje, e);
    }

    private String detectarCampoDuplicado(String mensaje) {
        if (mensaje == null) return "campo desconocido";
        String mensajeLower = mensaje.toLowerCase();

        if (mensajeLower.contains("dni_san") || mensajeLower.contains("sanitario_pkey")) {
            return "DNI";
        }
        if (mensajeLower.contains("email_san") || mensajeLower.contains("email")) {
            return "email";
        }

        return "campo duplicado";
    }

    private void hacerRollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }
        }
    }

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

    // ==================== METODOS DE AUTENTICACION ====================

    /**
     * Autentica un sanitario con DNI y contrasena.
     * Devuelve null si las credenciales son incorrectas (no es excepcion).
     * Lanza ConexionException si hay error de BD.
     *
     * @param dni DNI del sanitario
     * @param contrasena Contrasena del sanitario
     * @return Sanitario si las credenciales son correctas, null si no
     * @throws ConexionException si hay error de conexion con la BD
     */
    public Sanitario autenticar(String dni, String contrasena) {
        String query = "SELECT s.dni_san, s.nombre_san, s.apellido1_san, s.apellido2_san, " +
                "s.email_san, s.num_de_pacientes, s.contrasena_san, sas.cargo " +
                "FROM sanitario s " +
                "LEFT JOIN sanitario_agrega_sanitario sas ON s.dni_san = sas.dni_san " +
                "WHERE s.dni_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni.toUpperCase());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String hashAlmacenado = rs.getString("contrasena_san");

                    if (CifradoUtil.verificarContrasena(contrasena, hashAlmacenado)) {
                        Sanitario sanitario = mapearSanitarioDesdeResultSet(rs);

                        //Migracion perezosa: si la contrasena estaba en texto plano, rehashear
                        if (!CifradoUtil.esBCrypt(hashAlmacenado)) {
                            migrarContrasenaBCrypt(dni.toUpperCase(), contrasena);
                        }

                        System.out.println("Autenticacion exitosa para: " + sanitario.getDni());
                        return sanitario;
                    }
                }
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al autenticar", e);
        }

        System.out.println("Autenticacion fallida para DNI: " + dni);
        return null;
    }

    /**
     * Migra una contrasena de texto plano a BCrypt tras login exitoso
     */
    private void migrarContrasenaBCrypt(String dni, String contrasenaPlana) {
        String query = "UPDATE sanitario SET contrasena_san = ? WHERE dni_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, CifradoUtil.hashContrasena(contrasenaPlana));
            stmt.setString(2, dni);
            stmt.executeUpdate();
            System.out.println("Contrasena migrada a BCrypt para: " + dni);

        } catch (SQLException e) {
            //No bloquear el login si falla la migracion
            System.err.println("Error al migrar contrasena a BCrypt: " + e.getMessage());
        }
    }

    /**
     * Verifica si existe el usuario admin en el sistema
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
     * Crea el usuario admin por defecto si no existe
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void crearAdminSiNoExiste() {
        if (existeAdmin()) {
            System.out.println("Usuario admin ya existe");
            return;
        }

        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            //1. Insertar sanitario admin con contrasena hasheada con BCrypt
            String queryInsertSanitario = "INSERT INTO sanitario (dni_san, nombre_san, apellido1_san, " +
                    "apellido2_san, email_san, num_de_pacientes, contrasena_san) " +
                    "VALUES ('ADMIN0000', 'Administrador', 'Sistema', '', 'admin@rehabiapp.com', 0, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(queryInsertSanitario)) {
                stmt.setString(1, CifradoUtil.hashContrasena("admin"));
                stmt.executeUpdate();
            }

            //2. Asignar cargo medico especialista al admin
            String queryInsertCargo = "INSERT INTO sanitario_agrega_sanitario (dni_san, cargo) " +
                    "VALUES ('ADMIN0000', 'medico especialista')";

            try (PreparedStatement stmt = conn.prepareStatement(queryInsertCargo)) {
                stmt.executeUpdate();
            }

            conn.commit();
            System.out.println("Usuario admin creado correctamente");

        } catch (SQLException e) {
            hacerRollback(conn);
            throw new ConexionException("Error al crear admin", e);

        } finally {
            cerrarConexion(conn);
        }
    }

    /**
     * Cambia la contrasena de un sanitario
     * @param dni DNI del sanitario
     * @param nuevaContrasena Nueva contrasena
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void cambiarContrasena(String dni, String nuevaContrasena) {
        String query = "UPDATE sanitario SET contrasena_san = ? WHERE dni_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, CifradoUtil.hashContrasena(nuevaContrasena));
            stmt.setString(2, dni);

            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas > 0) {
                AuditService.cambioContrasena(dni);
            }

        } catch (SQLException e) {
            throw new ConexionException("Error al cambiar contrasena", e);
        }
    }

    /**
     * Verifica si la contrasena actual es correcta
     * @param dni DNI del sanitario
     * @param contrasena Contrasena a verificar
     * @return true si la contrasena es correcta
     * @throws ConexionException si hay error de conexion con la BD
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
     * @throws ConexionException si hay error de conexion con la BD
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
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void eliminarTelefonos(String dni) {
        String query = "DELETE FROM telefono_sanitario WHERE dni_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new ConexionException("Error al eliminar telefonos", e);
        }
    }

    /**
     * Inserta un telefono para un sanitario
     * @throws ConexionException si hay error de conexion con la BD
     */
    public void insertarTelefono(String dni, String telefono) {
        String query = "INSERT INTO telefono_sanitario (dni_san, telefono) VALUES (?, ?)";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);
            stmt.setString(2, telefono);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new ConexionException("Error al insertar telefono", e);
        }
    }

    /**
     * Obtiene los telefonos de un sanitario como String separados por coma
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
            throw new ConexionException("Error al obtener telefonos", e);
        }

        return telefonos.toString();
    }
}
