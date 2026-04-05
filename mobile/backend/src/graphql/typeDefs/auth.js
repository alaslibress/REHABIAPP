// TypeDefs de autenticacion: mutations login y refreshToken
'use strict';

const { gql } = require('graphql-tag');

const authTypeDefs = gql`
  # Respuesta de autenticacion con par de tokens JWT del BFF
  type AuthPayload {
    accessToken: String!
    refreshToken: String!
    # Unix timestamp en segundos cuando expira el access token
    expiresAt: Int!
  }

  extend type Mutation {
    # Autenticacion del paciente con DNI/email y contrasena
    login(identifier: String!, password: String!): AuthPayload!

    # Renovar par de tokens usando el refresh token actual
    refreshToken(refreshToken: String!): AuthPayload!
  }
`;

module.exports = authTypeDefs;
