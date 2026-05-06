// TypeDefs de videojuegos terapeuticos: catalogo desbloqueado, lanzamiento e historial
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

  # Videojuego terapeutico disponible para el paciente.
  # Se construye a partir de los juegos desbloqueados del dashboard.
  type Game {
    idVideojuego: ID!
    codigo: String!
    nombre: String!
    descripcion: String
    codDis: String
    parteCuerpo: String
    urlUnity: String!
  }

  # Datos de lanzamiento de un videojuego: URL Unity y token efimero JWT (5 min, scope GAMES_PLAY)
  type GameSessionLaunch {
    urlUnity: String!
    ephemeralToken: String!
    # Epoch seconds en el que el token efimero expira
    expiresAt: Int!
  }

  # Dificultad asignada al videojuego para este paciente.
  # Calculada a partir del nivel de progresion + sensibilidad clinica.
  enum GameDifficulty {
    EASY
    MEDIUM
    HARD
  }

  # Videojuego asignado al paciente con metadata para la card de la lista.
  type AssignedGame {
    id: ID!
    name: String!
    description: String!
    thumbnailUrl: String
    webglUrl: String
    difficulty: GameDifficulty!
    # Fecha ISO8601 cuando el juego fue asignado al paciente.
    assignedAt: String!
  }

  extend type Query {
    # Historial de sesiones de juego del paciente con paginacion
    myGameSessions(limit: Int, offset: Int): [GameSession!]!

    # Videojuegos desbloqueados para el paciente autenticado
    availableGames: [Game!]!

    # Videojuegos asignados al paciente, formato listo para mostrar en la pestana Juegos.
    myAssignedGames: [AssignedGame!]!
  }

  extend type Mutation {
    # Solicita el lanzamiento de un videojuego.
    # El BFF valida que el juego este desbloqueado y firma un JWT efimero (5 min).
    startGame(idVideojuego: ID!): GameSessionLaunch!
  }
`;

module.exports = gameTypeDefs;
