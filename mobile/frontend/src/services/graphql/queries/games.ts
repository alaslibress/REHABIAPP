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
