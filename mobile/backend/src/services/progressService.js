// Servicio de progreso corporal del paciente
// Obtiene el mapa de partes del cuerpo y sus metricas desde la API de Java
'use strict';

const apiClient = require('./apiClient');

/**
 * Obtiene el progreso por partes del cuerpo del paciente.
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @returns {Promise<Array>} BodyPartProgress[]
 */
async function obtenerProgresoCorporal(dniPac, javaToken) {
  const data = await apiClient.get(
    `/api/pacientes/${dniPac}/progreso/partes-cuerpo`,
    javaToken
  );
  return Array.isArray(data) ? data : [];
}

/**
 * Obtiene las metricas de sesiones para una parte del cuerpo especifica.
 *
 * @param {string} dniPac
 * @param {string} bodyPartId
 * @param {string|null} javaToken
 * @returns {Promise<Array>} BodyPartMetric[]
 */
async function obtenerMetricasParte(dniPac, bodyPartId, javaToken) {
  const data = await apiClient.get(
    `/api/pacientes/${dniPac}/progreso/partes-cuerpo/${bodyPartId}/metricas?limit=30`,
    javaToken
  );
  return Array.isArray(data) ? data : [];
}

module.exports = { obtenerProgresoCorporal, obtenerMetricasParte };
