// Fusiona todos los TypeDefs del esquema GraphQL del BFF
// Apollo Server 4 acepta un array de DocumentNode
'use strict';

const commonTypeDefs = require('./common');
const authTypeDefs = require('./auth');
const patientTypeDefs = require('./patient');
const treatmentTypeDefs = require('./treatment');
const appointmentTypeDefs = require('./appointment');
const gameTypeDefs = require('./game');
const settingsTypeDefs = require('./settings');
const progressTypeDefs = require('./progress');

// El orden importa: common primero (define enums usados por los demas)
module.exports = [
  commonTypeDefs,
  authTypeDefs,
  patientTypeDefs,
  treatmentTypeDefs,
  appointmentTypeDefs,
  gameTypeDefs,
  settingsTypeDefs,
  progressTypeDefs,
];
