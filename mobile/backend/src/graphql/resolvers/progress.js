// Resolvers de progreso corporal del paciente
'use strict';

const progressService = require('../../services/progressService');
const { requireAuth } = require('./helpers');

const progressResolvers = {
  Query: {
    async myBodyPartProgress(_parent, _args, context) {
      const user = requireAuth(context);
      return progressService.obtenerProgresoCorporal(user.sub, context.javaToken);
    },

    async bodyPartMetrics(_parent, { bodyPartId }, context) {
      const user = requireAuth(context);
      return progressService.obtenerMetricasParte(user.sub, bodyPartId, context.javaToken);
    },
  },
};

module.exports = progressResolvers;
