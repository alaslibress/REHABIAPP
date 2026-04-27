// TypeDefs del paciente: perfil, discapacidades y progreso
// Los campos clinicos sensibles (alergias, antecedentes) NO se exponen aqui
'use strict';

const { gql } = require('graphql-tag');

const patientTypeDefs = gql`
  # Perfil publico del paciente — solo campos seguros para la app movil
  type Patient {
    id: ID!
    name: String!
    surname: String!
    email: String
    dni: String!
    phone: String
    birthDate: String
    address: String
    active: Boolean!
    numSs: String
    sexo: String
    # Foto de perfil como data URI (base64) — null si no hay foto
    avatarDataUri: String
    # Saludo calculado por el backend segun la hora local del paciente
    greeting: String
  }

  # Discapacidad asignada al paciente con su nivel de progresion actual
  type Disability {
    id: ID!
    name: String!
    description: String
    currentLevel: Int!
  }

  # Resumen de progreso terapeutico del paciente
  type ProgressSummary {
    totalSessions: Int!
    averageScore: Float
    improvementRate: Float
    lastSessionDate: String
  }

  extend type Query {
    # Perfil del paciente autenticado
    me: Patient!

    # Discapacidades asignadas al paciente autenticado
    myDisabilities: [Disability!]!

    # Resumen de progreso terapeutico global del paciente
    myProgress: ProgressSummary!
  }
`;

module.exports = patientTypeDefs;
