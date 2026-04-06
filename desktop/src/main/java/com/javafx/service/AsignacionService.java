package com.javafx.service;

import com.javafx.Clases.PacienteDiscapacidad;
import com.javafx.Clases.PacienteTratamiento;
import com.javafx.DAO.AsignacionDAO;
import com.javafx.dto.PacienteDiscapacidadRequest;

import java.util.List;

/**
 * Capa de servicio para la asignacion de discapacidades y tratamientos a pacientes.
 * Delega al AsignacionDAO que consume la API REST.
 */
public class AsignacionService {

    private final AsignacionDAO asignacionDAO = new AsignacionDAO();

    public List<PacienteDiscapacidad> listarDiscapacidades(String dniPac) {
        return asignacionDAO.listarDiscapacidades(dniPac);
    }

    public PacienteDiscapacidad asignarDiscapacidad(String dniPac,
                                                     PacienteDiscapacidadRequest request) {
        return asignacionDAO.asignarDiscapacidad(dniPac, request);
    }

    public PacienteDiscapacidad actualizarNivel(String dniPac, String codDis, Integer idNivel) {
        return asignacionDAO.actualizarNivel(dniPac, codDis, idNivel);
    }

    public List<PacienteTratamiento> listarTratamientos(String dniPac) {
        return asignacionDAO.listarTratamientos(dniPac);
    }

    public PacienteTratamiento toggleVisibilidad(String dniPac, String codTrat) {
        return asignacionDAO.toggleVisibilidad(dniPac, codTrat);
    }
}
