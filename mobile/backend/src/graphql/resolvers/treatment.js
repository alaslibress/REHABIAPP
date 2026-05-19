// Resolvers de tratamientos del paciente
'use strict';

const treatmentService = require('../../services/treatmentService');
const treatmentPdfService = require('../../services/treatmentPdfService');
const { requireAuth } = require('./helpers');

const treatmentResolvers = {
  Query: {
    async myTreatments(_parent, { disabilityId, level }, context) {
      const user = requireAuth(context);
      return treatmentService.obtenerTratamientos(user.sub, context.javaToken, { disabilityId, level });
    },

    async treatmentPdf(_parent, { codTrat }, context) {
      requireAuth(context);
      return treatmentPdfService.obtenerPdfTratamiento(codTrat, context.javaToken);
    },

    // Descarga el documento del tratamiento con la forma esperada por el frontend (Phase G.3)
    async treatmentDocument(_parent, { codTrat }, context) {
      requireAuth(context);
      const pdf = await treatmentPdfService.obtenerPdfTratamiento(String(codTrat), context.javaToken);
      if (!pdf) return null;
      return {
        fileName: pdf.filename,
        mimeType: 'application/pdf',
        base64: pdf.base64Content,
        url: null,
      };
    },
  },
};

module.exports = treatmentResolvers;
