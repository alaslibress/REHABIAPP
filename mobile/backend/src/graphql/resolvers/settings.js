// Resolvers para ajustes: registro y eliminacion de tokens de notificacion
'use strict';

const { requireAuth } = require('./helpers');
const notificationService = require('../../services/notificationService');

module.exports = {
  Mutation: {
    // Registra el token Expo Push del dispositivo del paciente
    registerDeviceToken: async function (_parent, args, context) {
      const user = requireAuth(context);
      const { token, platform } = args;

      if (!token || !platform) {
        const { crearError } = require('../../utils/errors');
        throw crearError('VALIDATION_ERROR');
      }

      await notificationService.registrarToken(user.sub, token, platform, context.javaToken);
      return true;
    },

    // Elimina (desactiva) el token de notificaciones del dispositivo
    unregisterDeviceToken: async function (_parent, args, context) {
      requireAuth(context);
      const { token } = args;

      await notificationService.eliminarToken(token, context.javaToken);
      return true;
    },
  },
};
