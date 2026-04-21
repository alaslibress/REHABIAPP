// Resolvers de autenticacion: login y refreshToken
// Son los unicos resolvers que no requieren contexto autenticado
'use strict';

const authService = require('../../services/authService');

const authResolvers = {
  Mutation: {
    // Autenticar paciente con DNI/email y contrasena
    async login(_parent, { identifier, password }) {
      return authService.login(identifier, password);
    },

    // Renovar par de tokens usando el refresh token
    async refreshToken(_parent, { refreshToken }) {
      return authService.refresh(refreshToken);
    },
  },
};

module.exports = authResolvers;
