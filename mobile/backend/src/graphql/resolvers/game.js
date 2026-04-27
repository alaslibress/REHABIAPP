// Resolvers de sesiones de juego terapeutico
'use strict';

const gameService = require('../../services/gameService');
const { requireAuth } = require('./helpers');

const gameResolvers = {
  Query: {
    async myGameSessions(_parent, { limit, offset }, context) {
      const user = requireAuth(context);
      return gameService.obtenerSesiones(user.sub, context.javaToken, limit, offset);
    },

    async myAssignedGames(_parent, _args, context) {
      const user = requireAuth(context);
      return gameService.obtenerJuegosAsignados(user.sub, context.javaToken);
    },
  },
};

module.exports = gameResolvers;
