// TypeDefs de citas medicas del paciente
'use strict';

const { gql } = require('graphql-tag');

const appointmentTypeDefs = gql`
  # Cita medica del paciente con su sanitario asignado
  type Appointment {
    id: ID!
    date: String!
    time: String!
    practitionerName: String!
    practitionerSpecialty: String
    status: AppointmentStatus!
    notes: String
  }

  # Estado de una solicitud de cita enviada por el paciente
  enum AppointmentRequestStatus { PENDING CONFIRMED REJECTED }

  # Solicitud de cita enviada por el paciente al centro
  type AppointmentRequest {
    id: ID!
    fechaPreferida: String!
    horaPreferida: String!
    motivo: String!
    estado: AppointmentRequestStatus!
    createdAt: String!
  }

  extend type Query {
    # Citas del paciente, filtrable por estado y por proximas
    myAppointments(status: AppointmentStatus, upcoming: Boolean): [Appointment!]!
  }

  extend type Mutation {
    # Reservar una nueva cita medica
    bookAppointment(date: String!, time: String!, practitionerId: ID!): Appointment!

    # Cancelar una cita existente por su ID
    cancelAppointment(appointmentId: ID!): Appointment!

    # Solicitar una cita nueva (el sanitario la confirmara posteriormente)
    requestAppointment(
      fechaPreferida: String!
      horaPreferida: String!
      motivo: String!
      telefono: String
      email: String
    ): AppointmentRequest!
  }
`;

module.exports = appointmentTypeDefs;
