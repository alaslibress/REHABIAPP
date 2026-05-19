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
    # Numero de la Seguridad Social (12 digitos en Espana). Lectura pacientes.
    numSs: String
    # Sexo registrado en la ficha clinica. Coincide con el enum SexoPaciente del API Java.
    sexo: SexoPaciente
    # Avatar codificado en data URI (data:image/png;base64,....). Null si el paciente no ha subido avatar.
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

  # Punto en la serie temporal de una metrica
  type ProgressEntry {
    fecha: String!
    valor: Float!
  }

  # Progreso del paciente en un tratamiento concreto
  type TreatmentProgress {
    codTrat: String!
    tratamientoNombre: String
    parteCuerpo: String
    metricaNombre: String
    baselineValor: Float
    baselineFecha: String
    currentValor: Float
    currentFecha: String
    deltaPorcentaje: Float
    entradas: [ProgressEntry!]!
  }

  # Vista global del progreso del paciente, lista para alimentar
  # los graficos de react-native-chart-kit en el movil.
  type PatientProgress {
    tratamientos: [TreatmentProgress!]!
    # Fecha del punto mas reciente entre todos los tratamientos
    lastUpdate: String
  }

  # Resumen plano del progreso para tarjetas de bienvenida
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

    # Progreso terapeutico del paciente, agrupado por tratamiento
    myProgress: PatientProgress!

    # Resumen agregado del progreso terapeutico (orientado a tarjetas de bienvenida).
    # Distinto de myProgress (series por tratamiento) y de
    # myBodyPartProgress (mapa de cuerpo). Este es el formato plano para el dashboard.
    myProgressSummary: ProgressSummary
  }
`;

module.exports = patientTypeDefs;
