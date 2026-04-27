// TypeDefs de tratamientos del paciente
'use strict';

const { gql } = require('graphql-tag');

const treatmentTypeDefs = gql`
  # Tratamiento terapeutico asignado al paciente con campos enriquecidos
  type Treatment {
    id: ID!
    codTrat: String!
    name: String!
    description: String
    type: String!
    visible: Boolean!
    progressionLevel: Int!
    disabilityCode: String!
    summary: String
    materials: [String!]!
    medication: [String!]!
    documentUrl: String
    hasDocument: Boolean!
  }

  # Documento PDF asociado a un tratamiento
  type TreatmentDocument {
    fileName: String!
    mimeType: String!
    base64: String
    url: String
  }

  extend type Query {
    # Tratamientos del paciente
    myTreatments: [Treatment!]!

    # Documento PDF de un tratamiento especifico
    treatmentDocument(codTrat: ID!): TreatmentDocument!
  }
`;

module.exports = treatmentTypeDefs;
