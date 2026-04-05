// Resolvers de citas medicas del paciente
'use strict';

const appointmentService = require('../../services/appointmentService');
const { requireAuth } = require('./helpers');

const appointmentResolvers = {
  Query: {
    async myAppointments(_parent, { status, upcoming }, context) {
      const user = requireAuth(context);
      return appointmentService.obtenerCitas(user.sub, context.javaToken, { status, upcoming });
    },
  },

  Mutation: {
    async bookAppointment(_parent, { date, time, practitionerId }, context) {
      const user = requireAuth(context);
      return appointmentService.reservarCita(user.sub, date, time, practitionerId, context.javaToken);
    },

    async cancelAppointment(_parent, { appointmentId }, context) {
      const user = requireAuth(context);
      return appointmentService.cancelarCita(appointmentId, context.javaToken);
    },
  },
};

module.exports = appointmentResolvers;
