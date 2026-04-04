import { gql } from '@apollo/client';

export const GET_MY_TREATMENTS = gql`
  query GetMyTreatments($disabilityId: ID, $level: Int) {
    myTreatments(disabilityId: $disabilityId, level: $level) {
      id
      name
      description
      type
      visible
      progressionLevel
    }
  }
`;
