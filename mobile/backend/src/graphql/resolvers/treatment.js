// Resolvers de tratamientos del paciente
'use strict';

const treatmentService = require('../../services/treatmentService');
const { requireAuth } = require('./helpers');

const treatmentResolvers = {
  Query: {
    async myTreatments(_parent, { disabilityId, level }, context) {
      const user = requireAuth(context);
      return treatmentService.obtenerTratamientos(user.sub, context.javaToken, { disabilityId, level });
    },
  },
};

module.exports = treatmentResolvers;
