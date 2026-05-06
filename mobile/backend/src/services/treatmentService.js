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
  // Campos NO presentes en la respuesta Java actual se rellenan con valores
  // sintieticos deterministas para mantener el contrato con el frontend.
  // Cuando /api Phase 6 enriquezca el endpoint, sustituir los defaults por la
  // respuesta real.
  lista = lista.map((t) => ({
    id: t.codTrat,
    codTrat: t.codTrat,
    name: t.nombreTrat,
    description: t.descripcionTrat || null,
    type: 'TEXT_INSTRUCTION',
    visible: t.visible,
    progressionLevel: t.idNivel || 0,
    disabilityCode: t.codDis || null,
    summary: t.resumen || null,
    materials: Array.isArray(t.materiales) ? t.materiales : [],
    medication: Array.isArray(t.medicacion) ? t.medicacion : [],
    documentUrl: t.urlDocumento || null,
    hasDocument: Boolean(t.tienePdf),
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
