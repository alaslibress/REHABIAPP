// Servicio de sesiones de juego terapeutico y juegos asignados
// Obtiene el historial de sesiones via la API de Java (que delega en el pipeline /data MongoDB)
// DEPENDENCIA PENDIENTE: endpoints de juegos por paciente en la API de Java
'use strict';

const apiClient = require('./apiClient');

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

/**
 * Transforma un juego de la API de Java al tipo GraphQL AssignedGame.
 *
 * @param {object} juego - Juego de la API de Java
 * @returns {object} AssignedGame
 */
function mapAssignedGame(juego) {
  return {
    id: juego.idJuego,
    name: juego.nombreJuego,
    description: juego.descripcion || '',
    thumbnailUrl: juego.urlThumbnail || null,
    webglUrl: juego.urlWebgl || null,
    difficulty: juego.dificultad || 'EASY',
    assignedAt: juego.fechaAsignacion,
  };
}

/**
 * Obtiene los juegos terapeuticos asignados al paciente.
 * DEPENDENCIA PENDIENTE: GET /api/pacientes/{dniPac}/juegos en la API de Java.
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @returns {Promise<Array>} AssignedGame[]
 */
async function obtenerJuegosAsignados(dniPac, javaToken) {
  const data = await apiClient.get(`/api/pacientes/${dniPac}/juegos`, javaToken);
  return Array.isArray(data) ? data.map(mapAssignedGame) : [];
}

module.exports = { obtenerSesiones, obtenerJuegosAsignados };
