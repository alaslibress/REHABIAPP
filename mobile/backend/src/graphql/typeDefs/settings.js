// TypeDefs para ajustes de usuario: tokens de notificacion push
'use strict';

const { gql } = require('graphql-tag');

module.exports = gql`
  extend type Mutation {
    registerDeviceToken(token: String!, platform: String!): Boolean!
    unregisterDeviceToken(token: String!): Boolean!
  }
`;
