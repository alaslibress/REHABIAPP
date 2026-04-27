// Resolvers de tratamientos del paciente
'use strict';

const treatmentService = require('../../services/treatmentService');
const documentService = require('../../services/documentService');
const { requireAuth } = require('./helpers');

const treatmentResolvers = {
  Query: {
    async myTreatments(_parent, _args, context) {
      const user = requireAuth(context);
      return treatmentService.obtenerTratamientos(user.sub, context.javaToken);
    },

    async treatmentDocument(_parent, { codTrat }, context) {
      const user = requireAuth(context);
      return documentService.obtenerDocumentoTratamiento(user.sub, codTrat, context.javaToken);
    },
  },
};

module.exports = treatmentResolvers;
