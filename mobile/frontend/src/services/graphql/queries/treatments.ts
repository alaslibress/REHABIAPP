import { gql } from '@apollo/client';

export const GET_MY_TREATMENTS = gql`
  query GetMyTreatments {
    myTreatments {
      id
      codTrat
      name
      description
      type
      visible
      progressionLevel
      disabilityCode
      summary
      materials
      medication
      documentUrl
      hasDocument
    }
  }
`;

export const GET_TREATMENT_DOCUMENT = gql`
  query GetTreatmentDocument($codTrat: ID!) {
    treatmentDocument(codTrat: $codTrat) {
      fileName
      mimeType
      base64
      url
    }
  }
`;
