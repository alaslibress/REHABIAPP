package com.javafx.DAO;

import com.javafx.Clases.Sanitario;
import com.javafx.Clases.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase DAO (Data Access Object) para gestionar operaciones de base de datos
 * relacionadas con la entidad Sanitario
 * Trabaja con las tablas: sanitario, sanitario_agrega_sanitario, telefono_sanitario
 */
public class SanitarioDAO {

    // ==================== METODOS DE CONSULTA ====================

    /**
     * Lista todos los sanitarios de la base de datos con su cargo
     * @return Lista de sanitarios ordenados por nombre
     */
    public List<Sanitario> listarTodos() {
        List<Sanitario> sanitarios = new ArrayList<>();

        //Consulta con JOIN para obtener el cargo desde la tabla sanitario_agrega_sanitario
        String query = "SELECT s.dni_san, s.nombre_san, s.apellido1_san, s.apellido2_san, " +
                "s.email_san, s.num_de_pacientes, s.contrasena_san, sas.cargo " +
                "FROM sanitario s " +
                "LEFT JOIN sanitario_agrega_sanitario sas ON s.dni_san = sas.dni_san " +
                "ORDER BY s.nombre_san";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            //Recorrer resultados y crear objetos Sanitario
            while (rs.next()) {
                Sanitario sanitario = mapearSanitarioDesdeResultSet(rs);
                sanitarios.add(sanitario);
            }

        } catch (SQLException e) {
            System.err.println("Error al listar sanitarios: " + e.getMessage());
        }

        return sanitarios;
    }

    /**
     * Busca sanitarios que coincidan con el texto en cualquier campo
     * @param texto Texto a buscar en dni, nombre, apellidos o email
     * @return Lista de sanitarios que coinciden con la busqueda
     */
    public List<Sanitario> buscarPorTexto(String texto) {
        List<Sanitario> sanitarios = new ArrayList<>();

        //Busqueda en multiples campos usando LIKE con patron
        String query = "SELECT s.dni_san, s.nombre_san, s.apellido1_san, s.apellido2_san, " +
                "s.email_san, s.num_de_pacientes, s.contrasena_san, sas.cargo " +
                "FROM sanitario s " +
                "LEFT JOIN sanitario_agrega_sanitario sas ON s.dni_san = sas.dni_san " +
                "WHERE LOWER(s.dni_san) LIKE ? OR LOWER(s.nombre_san) LIKE ? OR " +
                "LOWER(s.apellido1_san) LIKE ? OR LOWER(s.apellido2_san) LIKE ? OR " +
                "LOWER(s.email_san) LIKE ? ORDER BY s.nombre_san";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            //Preparar patron de busqueda con comodines
            String patron = "%" + texto.toLowerCase() + "%";
            stmt.setString(1, patron);
            stmt.setString(2, patron);
            stmt.setString(3, patron);
            stmt.setString(4, patron);
            stmt.setString(5, patron);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Sanitario sanitario = mapearSanitarioDesdeResultSet(rs);
                sanitarios.add(sanitario);
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar sanitarios: " + e.getMessage());
        }

        return sanitarios;
    }

    /**
     * Busca un sanitario por su DNI
     * @param dni DNI del sanitario a buscar
     * @return Sanitario encontrado o null si no existe
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
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapearSanitarioDesdeResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar sanitario por DNI: " + e.getMessage());
        }

        return null;
    }

    /**
     * Obtiene un sanitario completo por DNI incluyendo sus telefonos
     * @param dni DNI del sanitario
     * @return Sanitario con todos sus datos o null si no existe
     */
    public Sanitario obtenerPorDNI(String dni) {
        //Primero obtener datos basicos del sanitario
        Sanitario sanitario = buscarPorDni(dni);

        if (sanitario != null) {
            //Obtener telefonos del sanitario desde la tabla telefono_sanitario
            cargarTelefonosSanitario(sanitario);
        }

        return sanitario;
    }

    /**
     * Verifica si existe un sanitario con el DNI especificado
     * @param dni DNI a verificar
     * @return true si existe, false en caso contrario
     */
    public boolean existeDni(String dni) {
        String query = "SELECT COUNT(*) FROM sanitario WHERE dni_san = ?";

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
     * Verifica si existe un sanitario con el email especificado
     * @param email Email a verificar
     * @return true si existe, false en caso contrario
     */
    public boolean existeEmail(String email) {
        String query = "SELECT COUNT(*) FROM sanitario WHERE email_san = ?";

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
     * Util para validar al actualizar un sanitario
     * @param email Email a verificar
     * @param dniExcluir DNI del sanitario a excluir de la busqueda
     * @return true si existe otro sanitario con ese email
     */
    public boolean existeEmailExcluyendoDni(String email, String dniExcluir) {
        String query = "SELECT COUNT(*) FROM sanitario WHERE email_san = ? AND dni_san != ?";

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

    // ==================== METODOS DE INSERCION ====================

    /**
     * Inserta un nuevo sanitario en la base de datos
     * Realiza la insercion en las tablas sanitario y sanitario_agrega_sanitario
     * @param sanitario Sanitario a insertar
     * @return true si la insercion fue exitosa
     */
    public boolean insertar(Sanitario sanitario) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

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
                stmt.setString(6, sanitario.getContrasena());
                stmt.executeUpdate();
            }

            //Insertar cargo en tabla sanitario_agrega_sanitario
            String queryCargo = "INSERT INTO sanitario_agrega_sanitario (dni_san, cargo) VALUES (?, ?)";
            try (PreparedStatement stmtCargo = conn.prepareStatement(queryCargo)) {
                stmtCargo.setString(1, sanitario.getDni());
                stmtCargo.setString(2, sanitario.getCargo());
                stmtCargo.executeUpdate();
            }

            //Confirmar transaccion
            conn.commit();
            return true;

        } catch (SQLException e) {
            //Revertir cambios en caso de error
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error al hacer rollback: " + ex.getMessage());
                }
            }
            System.err.println("Error al insertar sanitario: " + e.getMessage());
            return false;

        } finally {
            //Restaurar autocommit
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
     * Inserta los telefonos de un sanitario
     * @param dniSanitario DNI del sanitario
     * @param telefono1 Primer telefono (puede ser null o vacio)
     * @param telefono2 Segundo telefono (puede ser null o vacio)
     * @return true si la insercion fue exitosa
     */
    public boolean insertarTelefonos(String dniSanitario, String telefono1, String telefono2) {
        String query = "INSERT INTO telefono_sanitario (dni_san, telefono) VALUES (?, ?)";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            //Insertar telefono 1 si no esta vacio
            if (telefono1 != null && !telefono1.trim().isEmpty()) {
                stmt.setString(1, dniSanitario);
                stmt.setString(2, telefono1.trim());
                stmt.executeUpdate();
            }

            //Insertar telefono 2 si no esta vacio
            if (telefono2 != null && !telefono2.trim().isEmpty()) {
                stmt.setString(1, dniSanitario);
                stmt.setString(2, telefono2.trim());
                stmt.executeUpdate();
            }

            return true;

        } catch (SQLException e) {
            System.err.println("Error al insertar telefonos: " + e.getMessage());
            return false;
        }
    }

    // ==================== METODOS DE ACTUALIZACION ====================

    /**
     * Actualiza los datos de un sanitario existente
     * @param sanitario Sanitario con los nuevos datos
     * @param dniOriginal DNI original del sanitario (por si se modifica)
     * @return true si la actualizacion fue exitosa
     */
    public boolean actualizar(Sanitario sanitario, String dniOriginal) {
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

            //Actualizar cargo en tabla sanitario_agrega_sanitario
            String queryCargo = "UPDATE sanitario_agrega_sanitario SET cargo = ? WHERE dni_san = ?";
            try (PreparedStatement stmtCargo = conn.prepareStatement(queryCargo)) {
                stmtCargo.setString(1, sanitario.getCargo());
                stmtCargo.setString(2, sanitario.getDni());
                stmtCargo.executeUpdate();
            }

            //Confirmar transaccion
            conn.commit();
            return true;

        } catch (SQLException e) {
            //Revertir cambios en caso de error
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error al hacer rollback: " + ex.getMessage());
                }
            }
            System.err.println("Error al actualizar sanitario: " + e.getMessage());
            return false;

        } finally {
            //Restaurar autocommit
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
     * Actualiza los telefonos de un sanitario
     * Primero elimina los telefonos existentes y luego inserta los nuevos
     * @param dniSanitario DNI del sanitario
     * @param telefono1 Primer telefono
     * @param telefono2 Segundo telefono
     * @return true si la actualizacion fue exitosa
     */
    public boolean actualizarTelefonos(String dniSanitario, String telefono1, String telefono2) {
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

    // ==================== METODOS DE ELIMINACION ====================

    /**
     * Elimina un sanitario de la base de datos
     * Las tablas relacionadas se eliminan automaticamente por ON DELETE CASCADE
     * @param dni DNI del sanitario a eliminar
     * @return true si la eliminacion fue exitosa
     */
    public boolean eliminar(String dni) {
        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            //Eliminar el sanitario (las FK con CASCADE eliminan telefonos y cargo)
            String deleteSanitario = "DELETE FROM sanitario WHERE dni_san = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteSanitario)) {
                stmt.setString(1, dni);
                int filasAfectadas = stmt.executeUpdate();

                if (filasAfectadas > 0) {
                    conn.commit();
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
            System.err.println("Error al eliminar sanitario: " + e.getMessage());
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

    // ==================== METODOS AUXILIARES PRIVADOS ====================

    /**
     * Mapea un ResultSet a un objeto Sanitario
     * Metodo auxiliar para evitar duplicacion de codigo
     * @param rs ResultSet posicionado en una fila valida
     * @return Objeto Sanitario con los datos del ResultSet
     * @throws SQLException Si hay error al leer el ResultSet
     */
    private Sanitario mapearSanitarioDesdeResultSet(ResultSet rs) throws SQLException {
        String dni = rs.getString("dni_san");
        String nombre = rs.getString("nombre_san");
        String apellido1 = rs.getString("apellido1_san");
        String apellido2 = rs.getString("apellido2_san");
        String email = rs.getString("email_san");
        int numPacientes = rs.getInt("num_de_pacientes");
        String contrasena = rs.getString("contrasena_san");
        String cargo = rs.getString("cargo");

        //Crear objeto sanitario con los datos obtenidos
        Sanitario sanitario = new Sanitario(
                dni,
                nombre,
                apellido1 != null ? apellido1 : "",
                apellido2 != null ? apellido2 : "",
                email,
                cargo != null ? cargo : "",
                numPacientes
        );

        //Asignar contrasena
        sanitario.setContrasena(contrasena);

        return sanitario;
    }

    /**
     * Carga los telefonos de un sanitario desde la base de datos
     * @param sanitario Sanitario al que se le asignaran los telefonos
     */
    private void cargarTelefonosSanitario(Sanitario sanitario) {
        String queryTel = "SELECT telefono FROM telefono_sanitario WHERE dni_san = ? ORDER BY id_telefono LIMIT 2";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(queryTel)) {

            stmt.setString(1, sanitario.getDni());
            ResultSet rs = stmt.executeQuery();

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

        } catch (SQLException e) {
            System.err.println("Error al cargar telefonos del sanitario: " + e.getMessage());
        }
    }

    // ==================== METODOS DE AUTENTICACION ====================

    /**
     * Autentica un sanitario con DNI y contraseña
     * @param dni DNI del sanitario
     * @param contrasena Contraseña del sanitario
     * @return Sanitario si las credenciales son correctas, null si no
     */
    public Sanitario autenticar(String dni, String contrasena) {
        String query = "SELECT s.dni_san, s.nombre_san, s.apellido1_san, s.apellido2_san, " +
                "s.email_san, s.num_de_pacientes, s.contrasena_san, sas.cargo " +
                "FROM sanitario s " +
                "LEFT JOIN sanitario_agrega_sanitario sas ON s.dni_san = sas.dni_san " +
                "WHERE s.dni_san = ? AND s.contrasena_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni.toUpperCase());
            stmt.setString(2, contrasena);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Sanitario sanitario = mapearSanitarioDesdeResultSet(rs);
                System.out.println("Autenticacion exitosa para: " + sanitario.getDni());
                return sanitario;
            }

        } catch (SQLException e) {
            System.err.println("Error al autenticar: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Autenticacion fallida para DNI: " + dni);
        return null;
    }

    /**
     * Verifica si existe un usuario admin en el sistema
     * @return true si existe el usuario admin
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
            System.err.println("Error al verificar admin: " + e.getMessage());
        }

        return false;
    }

    /**
     * Crea el usuario admin por defecto si no existe
     * @return true si se creo correctamente o ya existia
     */
    public boolean crearAdminSiNoExiste() {
        if (existeAdmin()) {
            System.out.println("Usuario admin ya existe");
            return true;
        }

        Connection conn = null;

        try {
            conn = ConexionBD.getConexion();
            conn.setAutoCommit(false);

            //1. Insertar sanitario admin
            String queryInsertSanitario = "INSERT INTO sanitario (dni_san, nombre_san, apellido1_san, " +
                    "apellido2_san, email_san, num_de_pacientes, contrasena_san) " +
                    "VALUES ('ADMIN0000', 'Administrador', 'Sistema', '', 'admin@rehabiapp.com', 0, 'admin')";

            try (PreparedStatement stmt = conn.prepareStatement(queryInsertSanitario)) {
                stmt.executeUpdate();
            }

            //2. Asignar cargo medico especialista al admin (en minusculas segun CHECK)
            String queryInsertCargo = "INSERT INTO sanitario_agrega_sanitario (dni_san, cargo) " +
                    "VALUES ('ADMIN0000', 'medico especialista')";

            try (PreparedStatement stmt = conn.prepareStatement(queryInsertCargo)) {
                stmt.executeUpdate();
            }

            conn.commit();
            System.out.println("Usuario admin creado correctamente");
            return true;

        } catch (SQLException e) {
            System.err.println("Error al crear admin: " + e.getMessage());
            e.printStackTrace();

            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error al hacer rollback: " + ex.getMessage());
                }
            }
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
     * Cambia la contraseña de un sanitario
     * @param dni DNI del sanitario
     * @param nuevaContrasena Nueva contraseña
     * @return true si se cambio correctamente
     */
    public boolean cambiarContrasena(String dni, String nuevaContrasena) {
        String query = "UPDATE sanitario SET contrasena_san = ? WHERE dni_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, nuevaContrasena);
            stmt.setString(2, dni);

            int filasAfectadas = stmt.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("Error al cambiar contraseña: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Verifica si la contraseña actual es correcta
     * @param dni DNI del sanitario
     * @param contrasena Contraseña a verificar
     * @return true si la contraseña es correcta
     */
    public boolean verificarContrasena(String dni, String contrasena) {
        String query = "SELECT COUNT(*) FROM sanitario WHERE dni_san = ? AND contrasena_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);
            stmt.setString(2, contrasena);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("Error al verificar contraseña: " + e.getMessage());
        }

        return false;
    }

    /**
     * Actualiza el cargo de un sanitario en la tabla sanitario_agrega_sanitario
     * @param dni DNI del sanitario
     * @param cargo Nuevo cargo (medico especialista o enfermero)
     * @return true si se actualizo correctamente
     */
    public boolean actualizarCargo(String dni, String cargo) {
        String query = "UPDATE sanitario_agrega_sanitario SET cargo = ? WHERE dni_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, cargo.toLowerCase());
            stmt.setString(2, dni);

            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas == 0) {
                //Si no existe registro, insertarlo
                return insertarCargo(dni, cargo);
            }

            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar cargo: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inserta el cargo de un sanitario si no existe
     * @param dni DNI del sanitario
     * @param cargo Cargo a insertar
     * @return true si se inserto correctamente
     */
    private boolean insertarCargo(String dni, String cargo) {
        String query = "INSERT INTO sanitario_agrega_sanitario (dni_san, cargo) VALUES (?, ?)";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);
            stmt.setString(2, cargo.toLowerCase());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al insertar cargo: " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina todos los telefonos de un sanitario
     * @param dni DNI del sanitario
     * @return true si se eliminaron correctamente
     */
    public boolean eliminarTelefonos(String dni) {
        String query = "DELETE FROM telefono_medico WHERE dni_san = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);
            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Error al eliminar telefonos: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inserta un telefono para un sanitario
     * @param dni DNI del sanitario
     * @param telefono Numero de telefono
     * @return true si se inserto correctamente
     */
    public boolean insertarTelefono(String dni, String telefono) {
        String query = "INSERT INTO telefono_medico (dni_san, telefono) VALUES (?, ?)";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);
            stmt.setString(2, telefono);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al insertar telefono: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene los telefonos de un sanitario
     * @param dni DNI del sanitario
     * @return String con los telefonos separados por coma
     */
    public String obtenerTelefonos(String dni) {
        String query = "SELECT telefono FROM telefono_medico WHERE dni_san = ?";
        StringBuilder telefonos = new StringBuilder();

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                if (telefonos.length() > 0) {
                    telefonos.append(", ");
                }
                telefonos.append(rs.getString("telefono"));
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener telefonos: " + e.getMessage());
        }

        return telefonos.toString();
    }
}