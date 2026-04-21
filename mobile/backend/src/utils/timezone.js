// Utilidad de zona horaria usando la API Intl nativa de Node.js 20
// No requiere dependencias externas (moment, luxon, dayjs prohibidos por el plan)
'use strict';

const TIMEZONE_FALLBACK = 'Europe/Madrid';

/**
 * Calcula el saludo apropiado segun la hora local del usuario.
 * Usa Intl.DateTimeFormat nativo de Node.js 20, sin dependencias externas.
 *
 * @param {string} timezone - Zona horaria IANA (ej: 'Europe/Madrid', 'America/Mexico_City')
 * @returns {string} 'Buenos dias', 'Buenas tardes' o 'Buenas noches'
 */
function calcularSaludo(timezone) {
  const zonaValida = validarTimezone(timezone) ? timezone : TIMEZONE_FALLBACK;

  const formatter = new Intl.DateTimeFormat('es-ES', {
    timeZone: zonaValida,
    hour: 'numeric',
    hour12: false,
  });

  const horaLocal = parseInt(formatter.format(new Date()), 10);

  if (horaLocal < 12) {
    return 'Buenos dias';
  } else if (horaLocal < 21) {
    return 'Buenas tardes';
  } else {
    return 'Buenas noches';
  }
}

/**
 * Valida si una cadena es una zona horaria IANA valida.
 *
 * @param {string} timezone
 * @returns {boolean}
 */
function validarTimezone(timezone) {
  if (!timezone || typeof timezone !== 'string') return false;
  try {
    Intl.DateTimeFormat(undefined, { timeZone: timezone });
    return true;
  } catch {
    return false;
  }
}

module.exports = { calcularSaludo, validarTimezone };
