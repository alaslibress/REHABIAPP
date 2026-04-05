// Servicio de tratamientos del paciente
// Obtiene y filtra los tratamientos visibles del paciente via la API de Java
'use strict';

const apiClient = require('./apiClient');

/**
 * Obtiene los tratamientos asignados al paciente, con filtros opcionales.
 * El campo visible=false indica tratamientos ocultados por el sanitario.
 * El BFF aplica los filtros en memoria cuando la API de Java no los soporta directamente.
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @param {{ disabilityId?: string, level?: number }} filtros
 * @returns {Promise<Array>} Treatment[]
 */
async function obtenerTratamientos(dniPac, javaToken, filtros = {}) {
  const data = await apiClient.get(`/api/pacientes/${dniPac}/tratamientos`, javaToken);
  let lista = Array.isArray(data) ? data : [];

  // Mapeo Java PacienteTratamientoResponse -> GraphQL Treatment
  // Faltan: description, type, progressionLevel (pendiente enriquecimiento desde catalogo)
  lista = lista.map((t) => ({
    id: t.codTrat,
    name: t.nombreTrat,
    description: null,            // No viene en PacienteTratamientoResponse
    type: 'TEXT_INSTRUCTION',     // Default — pendiente distincion por tipo en API Java
    visible: t.visible,
    progressionLevel: 0,          // Pendiente vinculacion con nivel de progresion del paciente
  }));

  // Filtrar en el BFF por nivel de progresion si se solicita
  if (filtros.level !== undefined && filtros.level !== null) {
    lista = lista.filter((t) => t.progressionLevel === filtros.level);
  }

  // DEPENDENCIA PENDIENTE: filtrado por disabilityId requiere endpoint enriquecido en Java
  // Por ahora devolver la lista completa cuando se filtra por discapacidad

  return lista;
}

module.exports = { obtenerTratamientos };
