package com.javafx.service;

import com.javafx.Clases.PacienteDiscapacidad;
import com.javafx.Clases.PacienteTratamiento;
import com.javafx.Clases.Tratamiento;
import com.javafx.DAO.AsignacionDAO;
import com.javafx.DAO.CatalogoDAO;
import com.javafx.dto.PacienteDiscapacidadRequest;
import com.javafx.excepcion.ConexionException;

import java.util.List;

/**
 * Servicio clinico para gestionar discapacidades y tratamientos de pacientes.
 * Adaptador entre los DAOs de asignacion y los controladores JavaFX.
 *
 * Algunos metodos dependen de endpoints de la API que aun no existen
 * (marcados con TODO). Lanzaran UnsupportedOperationException hasta que
 * los endpoints esten implementados en el Agent 1 (API).
 */
public class PacienteClinicoService {

    private final AsignacionDAO asignacionDAO = new AsignacionDAO();
    private final CatalogoDAO catalogoDAO = new CatalogoDAO();

    // ==================== DISCAPACIDADES ====================

    /**
     * Lista las discapacidades asignadas al paciente con sus niveles de progresion.
     */
    public List<PacienteDiscapacidad> listarDiscapacidadesPaciente(String dniPac) {
        return asignacionDAO.listarDiscapacidades(dniPac);
    }

    /**
     * Asigna una discapacidad al paciente.
     * Nivel inicial: 1 (agudo). Notas vacias por defecto.
     */
    public void asignarDiscapacidad(String dniPac, String codDis) {
        PacienteDiscapacidadRequest request = new PacienteDiscapacidadRequest(codDis, 1, "");
        asignacionDAO.asignarDiscapacidad(dniPac, request);
    }

    /**
     * Desasigna una discapacidad del paciente.
     */
    public void desasignarDiscapacidad(String dniPac, String codDis) {
        asignacionDAO.desasignarDiscapacidad(dniPac, codDis);
    }

    /**
     * Cambia el nivel de progresion de una discapacidad asignada al paciente.
     */
    public void cambiarNivel(String dniPac, String codDis, int nuevoNivel) {
        asignacionDAO.actualizarNivel(dniPac, codDis, nuevoNivel);
    }

    /**
     * Actualiza las notas clinicas de una discapacidad asignada al paciente.
     */
    public void actualizarNotas(String dniPac, String codDis, String notas) {
        asignacionDAO.actualizarNotas(dniPac, codDis, notas);
    }

    // ==================== TRATAMIENTOS ====================

    /**
     * Lista los tratamientos del paciente para una discapacidad concreta.
     * Filtra los tratamientos del paciente por los vinculados a la discapacidad indicada.
     */
    public List<PacienteTratamiento> listarTratamientosPaciente(String dniPac, String codDis) {
        // La API devuelve todos los tratamientos del paciente. Filtramos localmente
        // cruzando con los tratamientos del catalogo de la discapacidad.
        List<PacienteTratamiento> todos = asignacionDAO.listarTratamientos(dniPac);
        List<Tratamiento> tratamientosDiscapacidad = catalogoDAO.listarTratamientosPorDiscapacidad(codDis);

        List<String> codigosDiscapacidad = tratamientosDiscapacidad.stream()
            .map(Tratamiento::getCodTrat)
            .toList();

        return todos.stream()
            .filter(t -> codigosDiscapacidad.contains(t.getCodTrat()))
            .toList();
    }

    /**
     * Obtiene los tratamientos del catalogo disponibles para asignar al paciente
     * para una discapacidad concreta (los que aun no tiene asignados).
     */
    public List<Tratamiento> obtenerTratamientosDisponibles(String dniPac, String codDis) {
        List<Tratamiento> delCatalogo = catalogoDAO.listarTratamientosPorDiscapacidad(codDis);
        List<PacienteTratamiento> asignados = asignacionDAO.listarTratamientos(dniPac);

        List<String> codigosAsignados = asignados.stream()
            .map(PacienteTratamiento::getCodTrat)
            .toList();

        return delCatalogo.stream()
            .filter(t -> !codigosAsignados.contains(t.getCodTrat()))
            .toList();
    }

    /**
     * Alterna la visibilidad de un tratamiento del paciente.
     */
    public void toggleVisibilidad(String dniPac, String codTrat) {
        asignacionDAO.toggleVisibilidad(dniPac, codTrat);
    }

    /**
     * Asigna un tratamiento al paciente con visibilidad inicial activada.
     */
    public void asignarTratamiento(String dniPac, String codTrat) {
        asignacionDAO.asignarTratamiento(dniPac, codTrat);
    }

    /**
     * Desasigna un tratamiento del paciente.
     */
    public void desasignarTratamiento(String dniPac, String codTrat) {
        asignacionDAO.desasignarTratamiento(dniPac, codTrat);
    }
}
