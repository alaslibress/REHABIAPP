import { gql } from '@apollo/client';

export const GET_MY_ASSIGNED_GAMES = gql`
  query GetMyAssignedGames {
    myAssignedGames {
      id
      name
      description
      thumbnailUrl
      webglUrl
      difficulty
      assignedAt
    }
  }
`;

// Solicita al BFF la URL y el token efimero para lanzar un videojuego.
// El BFF valida que el paciente lo tenga desbloqueado y firma un JWT corto (5 min).
export const START_GAME = gql`
  mutation StartGame($idVideojuego: ID!) {
    startGame(idVideojuego: $idVideojuego) {
      urlUnity
      ephemeralToken
      expiresAt
    }
  }
`;

export const GET_MY_GAME_SESSIONS = gql`
  query GetMyGameSessions($limit: Int, $offset: Int) {
    myGameSessions(limit: $limit, offset: $offset) {
      id
      gameName
      playedAt
      score
      duration
      metrics {
        accuracy
        reactionTime
        completionRate
      }
    }
  }
`;
