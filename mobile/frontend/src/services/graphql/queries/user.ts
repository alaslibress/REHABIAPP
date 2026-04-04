import { gql } from '@apollo/client';

export const GET_MY_PROFILE = gql`
  query GetMyProfile {
    me {
      id
      name
      surname
      email
      dni
      phone
      birthDate
      address
      active
    }
  }
`;

export const GET_MY_DISABILITIES = gql`
  query GetMyDisabilities {
    myDisabilities {
      id
      name
      description
      currentLevel
    }
  }
`;

export const GET_MY_PROGRESS = gql`
  query GetMyProgress {
    myProgress {
      totalSessions
      averageScore
      improvementRate
      lastSessionDate
    }
  }
`;
