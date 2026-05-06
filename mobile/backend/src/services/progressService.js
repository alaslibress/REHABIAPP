// Servicio de progreso del paciente
// Consume GET /api/pacientes/{dni}/progreso (proxiado al pipeline /data MongoDB)
// Devuelve estructura compatible con react-native-chart-kit en el movil.
'use strict';

const apiClient = require('./apiClient');

/**
 * Obtiene la lista de progreso por tratamiento del paciente y la envuelve
 * en el tipo PatientProgress del esquema GraphQL.
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @returns {Promise<{ tratamientos: Array, lastUpdate: string|null }>}
 */
async function obtenerProgresoPaciente(dniPac, javaToken) {
  const data = await apiClient.get(`/api/pacientes/${dniPac}/progreso`, javaToken);
  const lista = Array.isArray(data) ? data : [];

  // Calcular lastUpdate como el currentFecha mas reciente entre todos los tratamientos
  let lastUpdate = null;
  for (const t of lista) {
    if (t && t.currentFecha) {
      if (!lastUpdate || new Date(t.currentFecha) > new Date(lastUpdate)) {
        lastUpdate = t.currentFecha;
      }
    }
  }

  return {
    tratamientos: lista,
    lastUpdate,
  };
}

module.exports = { obtenerProgresoPaciente };
