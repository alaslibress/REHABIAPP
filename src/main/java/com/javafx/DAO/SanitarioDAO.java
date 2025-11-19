package com.javafx.DAO;

import com.javafx.Clases.ConexionBD;
import com.javafx.Clases.Sanitario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//Clase DAO para operaciones de sanitarios con la base de datos
public class SanitarioDAO {
    
    //Metodo para listar todos los sanitarios de la base de datos
    public List<Sanitario> listarTodos() {
        List<Sanitario> listaSanitarios = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            //Obtener conexion
            conn = ConexionBD.getConexion();
            
            //Consulta SQL con JOIN para traer datos completos
            String sql = "SELECT s.dni_san, s.nombre_san, s.apellido1_san, s.apellido2_san, " +
                        "sas.cargo " +
                        "FROM sanitario s " +
                        "LEFT JOIN sanitario_agrega_sanitario sas ON s.dni_san = sas.dni_san " +
                        "ORDER BY s.apellido1_san, s.apellido2_san, s.nombre_san";
            
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            //Recorrer resultados y crear objetos Sanitario
            while (rs.next()) {
                Sanitario sanitario = new Sanitario();
                sanitario.setDni(rs.getString("dni_san"));
                sanitario.setNombre(rs.getString("nombre_san"));
                sanitario.setApellido1(rs.getString("apellido1_san"));
                sanitario.setApellido2(rs.getString("apellido2_san"));
                sanitario.setCargo(rs.getString("cargo"));
                
                listaSanitarios.add(sanitario);
            }
            
            System.out.println("Se cargaron " + listaSanitarios.size() + " sanitarios");
            
        } catch (SQLException e) {
            System.err.println("Error al listar sanitarios: " + e.getMessage());
            e.printStackTrace();
        } finally {
            //Cerrar recursos
            cerrarRecursos(rs, stmt);
        }
        
        return listaSanitarios;
    }
    
    //Metodo para obtener un sanitario completo por DNI
    public Sanitario obtenerPorDNI(String dni) {
        Sanitario sanitario = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement stmtTel = null;
        ResultSet rs = null;
        ResultSet rsTel = null;
        
        try {
            //Obtener conexion
            conn = ConexionBD.getConexion();
            
            //Consulta SQL para obtener datos del sanitario
            String sql = "SELECT s.dni_san, s.nombre_san, s.apellido1_san, s.apellido2_san, " +
                        "s.email_san, s.num_de_pacientes, sas.cargo " +
                        "FROM sanitario s " +
                        "LEFT JOIN sanitario_agrega_sanitario sas ON s.dni_san = sas.dni_san " +
                        "WHERE s.dni_san = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, dni);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                sanitario = new Sanitario();
                sanitario.setDni(rs.getString("dni_san"));
                sanitario.setNombre(rs.getString("nombre_san"));
                sanitario.setApellido1(rs.getString("apellido1_san"));
                sanitario.setApellido2(rs.getString("apellido2_san"));
                sanitario.setEmail(rs.getString("email_san"));
                sanitario.setNumPacientes(rs.getInt("num_de_pacientes"));
                sanitario.setCargo(rs.getString("cargo"));
                
                //Obtener telefonos del sanitario
                String sqlTel = "SELECT telefono FROM telefono_sanitario WHERE dni_san = ? ORDER BY id_telefono LIMIT 2";
                stmtTel = conn.prepareStatement(sqlTel);
                stmtTel.setString(1, dni);
                rsTel = stmtTel.executeQuery();
                
                if (rsTel.next()) {
                    sanitario.setTelefono1(rsTel.getString("telefono"));
                }
                if (rsTel.next()) {
                    sanitario.setTelefono2(rsTel.getString("telefono"));
                }
                
                System.out.println("Sanitario obtenido: " + sanitario.getNombre() + " " + sanitario.getApellidos());
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener sanitario por DNI: " + e.getMessage());
            e.printStackTrace();
        } finally {
            //Cerrar recursos
            cerrarRecursos(rsTel, stmtTel);
            cerrarRecursos(rs, stmt);
        }
        
        return sanitario;
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
