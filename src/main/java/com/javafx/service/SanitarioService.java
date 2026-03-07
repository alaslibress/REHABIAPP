package com.javafx.service;

import com.javafx.Clases.Sanitario;
import com.javafx.DAO.SanitarioDAO;

import java.util.List;
import java.util.Optional;

/**
 * Capa de servicio para operaciones de Sanitario
 * Actua como wrapper del SanitarioDAO proporcionando una interfaz simplificada
 */
public class SanitarioService {

    private final SanitarioDAO sanitarioDAO;

    /**
     * Constructor que inicializa el DAO
     */
    public SanitarioService() {
        this.sanitarioDAO = new SanitarioDAO();
    }

    /**
     * Lista todos los sanitarios
     * @return Lista de todos los sanitarios
     */
    public List<Sanitario> listarTodos() {
        return sanitarioDAO.listarTodos();
    }

    /**
     * Busca un sanitario por su DNI
     * @param dni DNI del sanitario
     * @return Optional con el sanitario si existe, vacio si no
     */
    public Optional<Sanitario> buscarPorDni(String dni) {
        Sanitario sanitario = sanitarioDAO.buscarPorDni(dni);
        return Optional.ofNullable(sanitario);
    }

    /**
     * Inserta un nuevo sanitario con sus telefonos
     * @param sanitario Sanitario a insertar
     * @param tel1 Primer telefono
     * @param tel2 Segundo telefono
     * @return true si la insercion fue exitosa
     */
    public boolean insertar(Sanitario sanitario, String tel1, String tel2) {
        boolean sanitarioInsertado = sanitarioDAO.insertar(sanitario);

        if (sanitarioInsertado) {
            sanitarioDAO.insertarTelefonos(sanitario.getDni(), tel1, tel2);
            return true;
        }

        return false;
    }

    /**
     * Actualiza un sanitario existente con sus telefonos
     * @param sanitario Sanitario con los nuevos datos
     * @param dniOriginal DNI original del sanitario
     * @param tel1 Primer telefono
     * @param tel2 Segundo telefono
     * @return true si la actualizacion fue exitosa
     */
    public boolean actualizar(Sanitario sanitario, String dniOriginal, String tel1, String tel2) {
        boolean sanitarioActualizado = sanitarioDAO.actualizar(sanitario, dniOriginal);

        if (sanitarioActualizado) {
            sanitarioDAO.actualizarTelefonos(sanitario.getDni(), tel1, tel2);
            return true;
        }

        return false;
    }

    /**
     * Elimina un sanitario
     * @param dni DNI del sanitario a eliminar
     * @return true si la eliminacion fue exitosa
     */
    public boolean eliminar(String dni) {
        return sanitarioDAO.eliminar(dni);
    }
}
