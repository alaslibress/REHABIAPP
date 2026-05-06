// Resolvers del dashboard agregado del paciente
'use strict';

const dashboardService = require('../../services/dashboardService');
const { requireAuth } = require('./helpers');

const dashboardResolvers = {
  Query: {
    async myDashboard(_parent, _args, context) {
      const user = requireAuth(context);
      return dashboardService.obtenerDashboard(user.sub, context.javaToken);
    },
  },
};

module.exports = dashboardResolvers;
