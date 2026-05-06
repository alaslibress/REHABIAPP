// Resolvers de videojuegos terapeuticos
'use strict';

const gameService = require('../../services/gameService');
const dashboardService = require('../../services/dashboardService');
const { requireAuth } = require('./helpers');

const gameResolvers = {
  Query: {
    async myGameSessions(_parent, { limit, offset }, context) {
      const user = requireAuth(context);
      return gameService.obtenerSesiones(user.sub, context.javaToken, limit, offset);
    },

    async availableGames(_parent, _args, context) {
      const user = requireAuth(context);
      return dashboardService.obtenerJuegosDesbloqueados(user.sub, context.javaToken);
    },

    // Juegos con la forma esperada por el frontend GameCard (Phase G.5)
    async myAssignedGames(_parent, _args, context) {
      const user = requireAuth(context);
      return gameService.obtenerJuegosAsignados(user.sub, context.javaToken);
    },
  },

  Mutation: {
    async startGame(_parent, { idVideojuego }, context) {
      const user = requireAuth(context);
      return gameService.lanzarJuego(user.sub, idVideojuego, context.javaToken);
    },
  },
};

module.exports = gameResolvers;
