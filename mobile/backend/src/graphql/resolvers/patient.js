// Resolvers del paciente: perfil, discapacidades y progreso
'use strict';

const patientService = require('../../services/patientService');
const { requireAuth } = require('./helpers');

const patientResolvers = {
  Query: {
    // Perfil del paciente autenticado
    async me(_parent, _args, context) {
      const user = requireAuth(context);
      return patientService.obtenerPerfil(user.sub, context.javaToken);
    },

    // Discapacidades asignadas al paciente autenticado
    async myDisabilities(_parent, _args, context) {
      const user = requireAuth(context);
      return patientService.obtenerDiscapacidades(user.sub, context.javaToken);
    },

    // Resumen de progreso terapeutico
    async myProgress(_parent, _args, context) {
      const user = requireAuth(context);
      return patientService.obtenerProgreso(user.sub, context.javaToken);
    },
  },

  // Field resolver: el saludo se calcula en el middleware y se inyecta en el contexto
  Patient: {
    greeting(_parent, _args, context) {
      const nombre = _parent.name || '';
      const saludo = context.greeting || 'Hola';
      return `${saludo}, ${nombre}`;
    },
  },
};

module.exports = patientResolvers;
