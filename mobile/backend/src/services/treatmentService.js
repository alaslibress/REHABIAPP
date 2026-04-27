// Servicio de tratamientos del paciente
// Obtiene y transforma los tratamientos visibles del paciente via la API de Java
'use strict';

const apiClient = require('./apiClient');

/**
 * Transforma un PacienteTratamientoResponse enriquecido al tipo GraphQL Treatment.
 *
 * @param {object} t - Tratamiento de la API de Java (DTO enriquecido)
 * @returns {object} Treatment
 */
function transformarTratamiento(t) {
  return {
    id: t.codTrat,
    codTrat: t.codTrat,
    name: t.nombreTrat,
    description: t.descripcion || null,
    type: t.tipo || 'TEXT_INSTRUCTION',
    visible: t.visible,
    progressionLevel: t.idNivel || 0,
    disabilityCode: t.codDis || '',
    summary: t.resumen || null,
    materials: Array.isArray(t.materiales) ? t.materiales : [],
    medication: Array.isArray(t.medicacion) ? t.medicacion : [],
    documentUrl: t.urlDocumento || null,
    hasDocument: Boolean(t.tieneDocumento),
  };
}

/**
 * Obtiene los tratamientos asignados al paciente.
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @returns {Promise<Array>} Treatment[]
 */
async function obtenerTratamientos(dniPac, javaToken) {
  const data = await apiClient.get(`/api/pacientes/${dniPac}/tratamientos`, javaToken);
  return Array.isArray(data) ? data.map(transformarTratamiento) : [];
}

module.exports = { obtenerTratamientos };
