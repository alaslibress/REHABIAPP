import { gql } from '@apollo/client';

export const GET_MY_BODY_PART_PROGRESS = gql`
  query GetMyBodyPartProgress {
    myBodyPartProgress {
      id
      name
      hasTreatment
      progressPct
      improvementPct
      periodLabel
    }
  }
`;

export const GET_BODY_PART_METRICS = gql`
  query GetBodyPartMetrics($bodyPartId: ID!) {
    bodyPartMetrics(bodyPartId: $bodyPartId) {
      date
      score
      metricType
    }
  }
`;
