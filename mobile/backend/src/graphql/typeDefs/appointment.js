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

  extend type Query {
    # Citas del paciente, filtrable por estado y por proximas
    myAppointments(status: AppointmentStatus, upcoming: Boolean): [Appointment!]!
  }

  # Estado de una solicitud de cita en revision por la clinica
  enum AppointmentRequestStatus {
    PENDING
    APPROVED
    REJECTED
    CANCELLED
  }

  # Solicitud de cita enviada por el paciente desde el formulario libre.
  # NO es una cita confirmada — la clinica la revisa antes de crear el Appointment.
  type AppointmentRequest {
    id: ID!
    fechaPreferida: String!
    horaPreferida: String!
    motivo: String!
    estado: AppointmentRequestStatus!
    createdAt: String!
  }

  extend type Mutation {
    # Reservar una nueva cita medica
    bookAppointment(date: String!, time: String!, practitionerId: ID!): Appointment!

    # Cancelar una cita existente por su ID
    cancelAppointment(appointmentId: ID!): Appointment!

    # Solicita una cita al equipo clinico (no es una reserva confirmada).
    # La clinica recibe la solicitud por canal interno y, si la aprueba, llama a
    # bookAppointment internamente. Devuelve el registro de la solicitud.
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
