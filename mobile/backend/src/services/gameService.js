// Servicio de sesiones de juego terapeutico
// Obtiene el historial de sesiones via la API de Java (que delega en el pipeline /data MongoDB)
// DEPENDENCIA PENDIENTE: endpoint de sesiones de juego por paciente en la API de Java
'use strict';

/**
 * Obtiene el historial de sesiones de juego terapeutico del paciente.
 * Endpoint pendiente en la API de Java — devuelve array vacio hasta que este disponible.
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @param {number} limit - Numero maximo de sesiones (default 20)
 * @param {number} offset - Desplazamiento para paginacion (default 0)
 * @returns {Promise<Array>} GameSession[]
 */
async function obtenerSesiones(dniPac, javaToken, limit = 20, offset = 0) {
  // DEPENDENCIA PENDIENTE: GET /api/pacientes/{dniPac}/sesiones-juego
  // Cuando se implemente en Java (que delega en /data MongoDB), sustituir por la llamada real:
  //
  // const data = await apiClient.get(
  //   `/api/pacientes/${dniPac}/sesiones-juego?limit=${limit}&offset=${offset}`,
  //   javaToken
  // );
  // return (Array.isArray(data) ? data : []).map(transformarSesion);

  return [];
}

module.exports = { obtenerSesiones };
