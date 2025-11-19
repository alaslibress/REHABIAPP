package com.javafx.DAO;

import com.javafx.Clases.ConexionBD;
import com.javafx.Clases.Paciente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//Clase DAO para operaciones de pacientes con la base de datos
public class PacienteDAO {
    
    //Metodo para listar todos los pacientes de la base de datos
    public List<Paciente> listarTodos() {
        List<Paciente> listaPacientes = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            //Obtener conexion
            conn = ConexionBD.getConexion();
            
            //Consulta SQL simple para traer datos de pacientes
            String sql = "SELECT dni_pac, nombre_pac, apellido1_pac, apellido2_pac, protesis " +
                        "FROM paciente " +
                        "ORDER BY apellido1_pac, apellido2_pac, nombre_pac";
            
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            //Recorrer resultados y crear objetos Paciente
            while (rs.next()) {
                Paciente paciente = new Paciente();
                paciente.setDni(rs.getString("dni_pac"));
                paciente.setNombre(rs.getString("nombre_pac"));
                paciente.setApellido1(rs.getString("apellido1_pac"));
                paciente.setApellido2(rs.getString("apellido2_pac"));
                paciente.setProtesis(rs.getInt("protesis"));
                
                listaPacientes.add(paciente);
            }
            
            System.out.println("Se cargaron " + listaPacientes.size() + " pacientes");
            
        } catch (SQLException e) {
            System.err.println("Error al listar pacientes: " + e.getMessage());
            e.printStackTrace();
        } finally {
            //Cerrar recursos
            cerrarRecursos(rs, stmt);
        }
        
        return listaPacientes;
    }
    
    //Metodo para obtener un paciente completo por DNI
    public Paciente obtenerPorDNI(String dni) {
        Paciente paciente = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement stmtTel = null;
        PreparedStatement stmtDir = null;
        ResultSet rs = null;
        ResultSet rsTel = null;
        ResultSet rsDir = null;
        
        try {
            //Obtener conexion
            conn = ConexionBD.getConexion();
            
            //Consulta SQL para obtener datos del paciente
            String sql = "SELECT p.dni_pac, p.nombre_pac, p.apellido1_pac, p.apellido2_pac, " +
                        "p.email_pac, p.num_ss, p.edad_pac, p.protesis, " +
                        "p.discapacidad_pac, p.tratamiento_pac, p.estado_tratamiento, p.id_direccion " +
                        "FROM paciente p " +
                        "WHERE p.dni_pac = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, dni);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                paciente = new Paciente();
                paciente.setDni(rs.getString("dni_pac"));
                paciente.setNombre(rs.getString("nombre_pac"));
                paciente.setApellido1(rs.getString("apellido1_pac"));
                paciente.setApellido2(rs.getString("apellido2_pac"));
                paciente.setEmail(rs.getString("email_pac"));
                paciente.setNumSS(rs.getString("num_ss"));
                paciente.setEdad(rs.getInt("edad_pac"));
                paciente.setProtesis(rs.getInt("protesis"));
                paciente.setDiscapacidad(rs.getString("discapacidad_pac"));
                paciente.setTratamiento(rs.getString("tratamiento_pac"));
                paciente.setEstadoPaciente(rs.getString("estado_tratamiento"));
                
                //Obtener direccion del paciente
                int idDireccion = rs.getInt("id_direccion");
                String sqlDir = "SELECT d.calle, d.numero, d.piso, d.cp, " +
                               "c.nombre_localidad, l.provincia " +
                               "FROM direccion d " +
                               "JOIN cp c ON d.cp = c.cp " +
                               "JOIN localidad l ON c.nombre_localidad = l.nombre_localidad " +
                               "WHERE d.id_direccion = ?";
                stmtDir = conn.prepareStatement(sqlDir);
                stmtDir.setInt(1, idDireccion);
                rsDir = stmtDir.executeQuery();
                
                if (rsDir.next()) {
                    String direccion = rsDir.getString("calle") + " " + rsDir.getString("numero");
                    if (rsDir.getString("piso") != null) {
                        direccion += ", " + rsDir.getString("piso");
                    }
                    direccion += " - " + rsDir.getString("cp") + " " + rsDir.getString("nombre_localidad");
                    paciente.setDireccion(direccion);
                }
                
                //Obtener telefonos del paciente
                String sqlTel = "SELECT telefono FROM telefono_paciente WHERE dni_pac = ? ORDER BY id_telefono LIMIT 2";
                stmtTel = conn.prepareStatement(sqlTel);
                stmtTel.setString(1, dni);
                rsTel = stmtTel.executeQuery();
                
                if (rsTel.next()) {
                    paciente.setTelefono1(rsTel.getString("telefono"));
                }
                if (rsTel.next()) {
                    paciente.setTelefono2(rsTel.getString("telefono"));
                }
                
                System.out.println("Paciente obtenido: " + paciente.getNombre() + " " + paciente.getApellidos());
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener paciente por DNI: " + e.getMessage());
            e.printStackTrace();
        } finally {
            //Cerrar recursos
            cerrarRecursos(rsDir, stmtDir);
            cerrarRecursos(rsTel, stmtTel);
            cerrarRecursos(rs, stmt);
        }
        
        return paciente;
    }
    
    //Metodo auxiliar para cerrar recursos
    private void cerrarRecursos(ResultSet rs, PreparedStatement stmt) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            System.err.println("Error al cerrar recursos: " + e.getMessage());
        }
    }
}
