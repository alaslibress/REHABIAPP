// Servicio de sesiones y lanzamiento de videojuegos terapeuticos
// - Historial de sesiones via API Java (delega en /data MongoDB)
// - Lanzamiento de juego: resuelve URL Unity desde el dashboard y firma JWT efimero del BFF
'use strict';

const dashboardService = require('./dashboardService');
const authService = require('./authService');
const apiClient = require('./apiClient');
const { crearError } = require('../utils/errors');

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
  // Cuando se implemente en Java (que delega en /data MongoDB), sustituir por la llamada real.
  return [];
}

/**
 * Prepara el lanzamiento de un videojuego para el paciente.
 * 1) Obtiene el dashboard para localizar el juego desbloqueado por id.
 * 2) Si el juego no esta desbloqueado para este paciente, rechaza la peticion.
 * 3) Genera un JWT efimero (5 min, scope GAMES_PLAY) firmado por el BFF.
 *
 * @param {string} dniPac
 * @param {string|number} idVideojuego
 * @param {string|null} javaToken
 * @returns {Promise<{ urlUnity, ephemeralToken, expiresAt }>}
 */
async function lanzarJuego(dniPac, idVideojuego, javaToken) {
  const juegos = await dashboardService.obtenerJuegosDesbloqueados(dniPac, javaToken);

  // Comparacion como string para ser tolerantes a ID de tipo Long del API o ID de GraphQL
  const juego = juegos.find((j) => String(j.idVideojuego) === String(idVideojuego));

  if (!juego) {
    // El juego no esta desbloqueado o no existe para este paciente
    throw crearError(
      'VALIDATION_ERROR',
      'El videojuego solicitado no esta disponible para este paciente.'
    );
  }

  if (!juego.urlUnity) {
    throw crearError('INTERNAL_ERROR', 'El videojuego no tiene URL configurada.');
  }

  const { token, expiresAt } = authService.generarTokenEfimeroJuego(dniPac, idVideojuego);

  return {
    urlUnity: juego.urlUnity,
    ephemeralToken: token,
    expiresAt,
  };
}

/**
 * Construye la lista de juegos asignados al paciente con la forma exacta que
 * espera el componente GameCard del frontend.
 * En modo mock: deriva de los juegos desbloqueados del dashboard + thumbnails sintieticos.
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @returns {Promise<Array>} AssignedGame[]
 */
async function obtenerJuegosAsignados(dniPac, javaToken) {
  const dashboard = await apiClient.get(`/api/pacientes/${dniPac}/dashboard`, javaToken);
  if (!dashboard || !Array.isArray(dashboard.juegosDesbloqueados)) {
    return [];
  }

  // Derivar dificultad de la posicion del juego para que sea estable y no aleatoria.
  // En produccion vendra del campo `dificultad` del DTO Java cuando exista.
  const DIFICULTADES = ['EASY', 'MEDIUM', 'HARD'];

  return dashboard.juegosDesbloqueados.map((j, idx) => ({
    id: String(j.idVideojuego),
    name: j.nombre,
    description: j.descripcion || `Juego terapeutico para ${j.parteCuerpo || 'rehabilitacion general'}.`,
    thumbnailUrl: j.urlMiniatura || null,
    webglUrl: j.urlUnity || null,
    difficulty: DIFICULTADES[idx % DIFICULTADES.length],
    assignedAt: j.fechaAsignacion || new Date().toISOString(),
  }));
}

module.exports = { obtenerSesiones, lanzarJuego, obtenerJuegosAsignados };
