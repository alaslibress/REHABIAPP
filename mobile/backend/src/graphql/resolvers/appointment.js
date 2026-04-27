// Resolvers de citas medicas del paciente
'use strict';

const appointmentService = require('../../services/appointmentService');
const { requireAuth } = require('./helpers');
const { crearError } = require('../../utils/errors');

// Expresiones regulares para validacion de formato
const REGEX_FECHA = /^\d{4}-\d{2}-\d{2}$/;
const REGEX_HORA = /^\d{2}:\d{2}$/;

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

    async requestAppointment(_parent, { fechaPreferida, horaPreferida, motivo, telefono, email }, context) {
      const user = requireAuth(context);

      // Validar que al menos telefono o email esten presentes
      const telTrim = (telefono || '').trim();
      const emailTrim = (email || '').trim();
      if (!telTrim && !emailTrim) {
        throw crearError('APPOINTMENT_REQUEST_INVALID_CONTACT');
      }

      // Validar formato de fecha YYYY-MM-DD
      if (!REGEX_FECHA.test(fechaPreferida)) {
        throw crearError('VALIDATION_ERROR', 'La fecha debe tener formato YYYY-MM-DD.');
      }

      // Validar formato de hora HH:mm
      if (!REGEX_HORA.test(horaPreferida)) {
        throw crearError('VALIDATION_ERROR', 'La hora debe tener formato HH:mm.');
      }

      // Validar que la fecha no sea pasada
      const hoy = new Date().toISOString().split('T')[0];
      if (fechaPreferida < hoy) {
        throw crearError('VALIDATION_ERROR', 'La fecha preferida debe ser futura.');
      }

      // Validar longitud minima del motivo
      if (!motivo || motivo.trim().length < 10) {
        throw crearError('VALIDATION_ERROR', 'El motivo debe tener al menos 10 caracteres.');
      }

      return appointmentService.solicitarCita(
        user.sub,
        { fechaPreferida, horaPreferida, motivo: motivo.trim(), telefono: telTrim || null, email: emailTrim || null },
        context.javaToken
      );
    },
  },
};

module.exports = appointmentResolvers;
