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

  extend type Mutation {
    # Reservar una nueva cita medica
    bookAppointment(date: String!, time: String!, practitionerId: ID!): Appointment!

    # Cancelar una cita existente por su ID
    cancelAppointment(appointmentId: ID!): Appointment!
  }
`;

module.exports = appointmentTypeDefs;
