// Servicio de notificaciones push — gestiona tokens de dispositivo
// Pendiente de implementacion real en el API Java (tabla token_dispositivo)
'use strict';

const apiClient = require('./apiClient');
const logger = require('../logger');

/**
 * Registra o actualiza el token Expo Push del dispositivo del paciente.
 * Mientras el API Java no tenga la tabla token_dispositivo, funciona en modo mock.
 *
 * @param {string} dniPac - DNI del paciente
 * @param {string} token - Token Expo Push (ExponentPushToken[...])
 * @param {string} platform - Plataforma (ANDROID | IOS | WEB)
 * @param {string|null} javaToken - JWT para el API Java
 */
async function registrarToken(dniPac, token, platform, javaToken) {
  try {
    await apiClient.post(
      `/api/pacientes/${dniPac}/device-tokens`,
      { token, platform },
      javaToken
    );
    logger.info({ dniPac, platform }, 'Token de dispositivo registrado');
  } catch (err) {
    // No propagar el error — un fallo en el registro de token no debe bloquear el login
    logger.warn({ dniPac, error: err.message }, 'Error al registrar token de dispositivo');
  }
}

/**
 * Desactiva el token de notificaciones de un dispositivo.
 *
 * @param {string} token - Token Expo Push a desactivar
 * @param {string|null} javaToken - JWT para el API Java
 */
async function eliminarToken(token, javaToken) {
  try {
    await apiClient.delete('/api/device-tokens', { token }, javaToken);
    logger.info({ token: token.substring(0, 20) + '...' }, 'Token de dispositivo eliminado');
  } catch (err) {
    logger.warn({ error: err.message }, 'Error al eliminar token de dispositivo');
  }
}

module.exports = { registrarToken, eliminarToken };
