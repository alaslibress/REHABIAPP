// Middleware de saludo basado en zona horaria del dispositivo
// Lee la cabecera X-Timezone y calcula el saludo apropiado
'use strict';

const { calcularSaludo } = require('../utils/timezone');

/**
 * Extrae la zona horaria de la cabecera X-Timezone y calcula el saludo.
 * Fallback a 'Europe/Madrid' si la cabecera no existe o es invalida.
 *
 * @param {object} req - Objeto Request de Express
 * @returns {string} Saludo: 'Buenos dias', 'Buenas tardes' o 'Buenas noches'
 */
function greetingMiddleware(req) {
  const timezone = req.headers['x-timezone'] || 'Europe/Madrid';
  return calcularSaludo(timezone);
}

module.exports = greetingMiddleware;
