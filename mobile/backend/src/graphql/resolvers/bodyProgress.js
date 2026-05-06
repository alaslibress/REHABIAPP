// Resolvers de progreso por parte del cuerpo
'use strict';

const bodyProgressService = require('../../services/bodyProgressService');
const { requireAuth } = require('./helpers');

const bodyProgressResolvers = {
  Query: {
    async myBodyPartProgress(_parent, _args, context) {
      const user = requireAuth(context);
      return bodyProgressService.obtenerProgresoPorParte(user.sub, context.javaToken);
    },

    async bodyPartMetrics(_parent, { bodyPartId }, context) {
      const user = requireAuth(context);
      return bodyProgressService.obtenerMetricasPorParte(user.sub, bodyPartId, context.javaToken);
    },
  },
};

module.exports = bodyProgressResolvers;
