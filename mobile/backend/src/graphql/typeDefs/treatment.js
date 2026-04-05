// TypeDefs de tratamientos del paciente
'use strict';

const { gql } = require('graphql-tag');

const treatmentTypeDefs = gql`
  # Tratamiento terapeutico asignado al paciente
  type Treatment {
    id: ID!
    name: String!
    description: String
    type: String!
    visible: Boolean!
    progressionLevel: Int!
  }

  extend type Query {
    # Tratamientos del paciente, filtrable por discapacidad y nivel de progresion
    myTreatments(disabilityId: ID, level: Int): [Treatment!]!
  }
`;

module.exports = treatmentTypeDefs;
