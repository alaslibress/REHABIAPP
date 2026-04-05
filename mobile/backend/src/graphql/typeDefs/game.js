// TypeDefs de sesiones de juego terapeutico
'use strict';

const { gql } = require('graphql-tag');

const gameTypeDefs = gql`
  # Metricas de rendimiento de una sesion de juego
  type GameMetrics {
    accuracy: Float
    reactionTime: Float
    completionRate: Float
  }

  # Sesion de juego terapeutico completada por el paciente
  type GameSession {
    id: ID!
    gameName: String!
    playedAt: String!
    score: Float
    duration: Float
    metrics: GameMetrics
  }

  extend type Query {
    # Historial de sesiones de juego del paciente con paginacion
    myGameSessions(limit: Int, offset: Int): [GameSession!]!
  }
`;

module.exports = gameTypeDefs;
