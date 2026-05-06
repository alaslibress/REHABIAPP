// TypeDefs de configuracion del dispositivo movil
// Las mutaciones registran/eliminan tokens de notificaciones push (APNs/FCM).
// En esta iteracion son stubs — la entrega real sera Phase 5.3 del checklist mobile.
'use strict';

const { gql } = require('graphql-tag');

const settingsTypeDefs = gql`
  extend type Mutation {
    # Registra el token push del dispositivo. Devuelve true si se acepto.
    # Stub en esta iteracion: el BFF lo loguea pero no persiste — pending APNs/FCM wiring.
    registerDeviceToken(token: String!, platform: String!): Boolean!

    # Elimina el token push del dispositivo. Devuelve true si se acepto.
    unregisterDeviceToken(token: String!): Boolean!
  }
`;

module.exports = settingsTypeDefs;
