// Fusiona todos los resolvers del BFF en un unico objeto para Apollo Server
'use strict';

const authResolvers = require('./auth');
const patientResolvers = require('./patient');
const treatmentResolvers = require('./treatment');
const appointmentResolvers = require('./appointment');
const gameResolvers = require('./game');

const resolvers = {
  Query: {
    ...patientResolvers.Query,
    ...treatmentResolvers.Query,
    ...appointmentResolvers.Query,
    ...gameResolvers.Query,
  },
  Mutation: {
    ...authResolvers.Mutation,
    ...appointmentResolvers.Mutation,
  },
  // Field resolvers de tipos especificos
  Patient: patientResolvers.Patient,
};

module.exports = resolvers;
