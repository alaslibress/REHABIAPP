// Servicio del dashboard agregado del paciente
// Consume GET /api/pacientes/{dni}/dashboard del API Java y lo transforma
// al esquema GraphQL Dashboard expuesto al movil.
'use strict';

const apiClient = require('./apiClient');
const { crearError } = require('../utils/errors');

/**
 * Obtiene el dashboard agregado del paciente.
 * El API Java construye este DTO a partir de varias entidades (paciente,
 * discapacidades activas, tratamientos visibles, juegos desbloqueados,
 * ultima sesion de juego y proxima cita).
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @returns {Promise<object>} Dashboard
 */
async function obtenerDashboard(dniPac, javaToken) {
  const data = await apiClient.get(`/api/pacientes/${dniPac}/dashboard`, javaToken);
  if (!data) {
    throw crearError('PATIENT_NOT_FOUND');
  }
  // El payload del API ya tiene la forma esperada por el esquema GraphQL,
  // los nombres de campo se mapean directamente (camelCase).
  return data;
}

/**
 * Obtiene solo los videojuegos desbloqueados del paciente a partir del dashboard.
 * Filtra los que tengan desbloqueado=true.
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @returns {Promise<Array>} Game[]
 */
async function obtenerJuegosDesbloqueados(dniPac, javaToken) {
  const dashboard = await obtenerDashboard(dniPac, javaToken);
  const juegos = Array.isArray(dashboard.juegosDesbloqueados) ? dashboard.juegosDesbloqueados : [];
  // Mapeo JuegoDesbloqueadoDto -> Game (GraphQL).
  // Solo devolvemos los efectivamente desbloqueados (desbloqueado=true).
  return juegos
    .filter((j) => j && j.desbloqueado)
    .map((j) => ({
      idVideojuego: j.idVideojuego,
      codigo: j.codigo,
      nombre: j.nombre,
      // descripcion no viene en JuegoDesbloqueadoDto del API; se enviara null
      descripcion: j.descripcion || null,
      // codDis no viene en el dashboard; se omite si no esta presente
      codDis: j.codDis || null,
      parteCuerpo: j.parteCuerpo || null,
      urlUnity: j.urlUnity,
    }));
}

module.exports = { obtenerDashboard, obtenerJuegosDesbloqueados };
