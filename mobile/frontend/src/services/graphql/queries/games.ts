import { gql } from '@apollo/client';

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
