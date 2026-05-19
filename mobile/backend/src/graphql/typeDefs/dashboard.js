// TypeDefs del dashboard agregado del paciente
// Espejo del DTO DashboardResponse del API Java
'use strict';

const { gql } = require('graphql-tag');

const dashboardTypeDefs = gql`
  # Resumen identificativo del paciente para el dashboard
  type DashboardPatientSummary {
    dniPac: String!
    nombrePac: String!
    apellido1Pac: String!
    apellido2Pac: String
    edadPac: Int
  }

  # Discapacidad activa con su nivel de progresion actual
  type DashboardActiveDisability {
    codDis: String!
    nombreDis: String!
    idNivelActual: Int
    nombreNivelActual: String
    ordenNivelActual: Int
  }

  # Tratamiento visible para el paciente en el dashboard
  type DashboardVisibleTreatment {
    codTrat: String!
    nombreTrat: String!
    idNivel: Int
    ordenNivel: Int
  }

  # Videojuego potencialmente disponible para el paciente
  type DashboardUnlockedGame {
    idVideojuego: ID!
    codigo: String!
    nombre: String!
    urlUnity: String
    parteCuerpo: String
    desbloqueado: Boolean!
  }

  # Resumen de la ultima sesion de juego del paciente
  type DashboardLastGameSession {
    idSesion: String
    nombreJuego: String
    fechaInicio: String
    duracionSegundos: Int
    score: Float
  }

  # Resumen de la proxima cita del paciente
  type DashboardNextAppointment {
    dniSanitario: String!
    fecha: String!
    hora: String!
  }

  # Vista agregada del dashboard del paciente
  type Dashboard {
    paciente: DashboardPatientSummary!
    discapacidadesActivas: [DashboardActiveDisability!]!
    tratamientosVisibles: [DashboardVisibleTreatment!]!
    juegosDesbloqueados: [DashboardUnlockedGame!]!
    ultimaSesionJuego: DashboardLastGameSession
    proximaCita: DashboardNextAppointment
  }

  extend type Query {
    # Dashboard agregado del paciente autenticado
    myDashboard: Dashboard!
  }
`;

module.exports = dashboardTypeDefs;
