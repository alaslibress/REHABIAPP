// TypeDefs GraphQL para progreso corporal del paciente
'use strict';

const { gql } = require('graphql-tag');

const progressTypeDefs = gql`
  type BodyPartProgress {
    id: ID!
    name: String!
    hasTreatment: Boolean!
    progressPct: Float!
    improvementPct: Float!
    periodLabel: String!
  }

  type BodyPartMetric {
    date: String!
    score: Float!
    metricType: String!
  }

  extend type Query {
    myBodyPartProgress: [BodyPartProgress!]!
    bodyPartMetrics(bodyPartId: ID!): [BodyPartMetric!]!
  }
`;

module.exports = progressTypeDefs;
