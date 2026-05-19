// Resolvers de configuracion del dispositivo movil
// Stubs de Phase G.7 — solo log estructurado, sin persistencia.
'use strict';

const { requireAuth } = require('./helpers');

const settingsResolvers = {
  Mutation: {
    async registerDeviceToken(_parent, { token, platform }, context) {
      const user = requireAuth(context);
      context.logger.info(
        { dniPac: user.sub.substring(0, 3) + '***', platform, tokenPrefijo: token.substring(0, 8) + '...' },
        'Token push registrado (stub Phase G.7)'
      );
      return true;
    },

    async unregisterDeviceToken(_parent, { token }, context) {
      const user = requireAuth(context);
      context.logger.info(
        { dniPac: user.sub.substring(0, 3) + '***', tokenPrefijo: token.substring(0, 8) + '...' },
        'Token push eliminado (stub Phase G.7)'
      );
      return true;
    },
  },
};

module.exports = settingsResolvers;
