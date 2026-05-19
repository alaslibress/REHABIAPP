'use strict';
const pino = require('pino');
const config = require('./config');

// Enmascara numeros de telefono en logs por privacidad (RGPD).
// Sustituye los digitos por '*' excepto los ultimos 3.
function enmascararTelefono(tel) {
  if (typeof tel !== 'string' || tel.length < 4) return tel;
  return '*'.repeat(tel.length - 3) + tel.slice(-3);
}

const logger = pino({
  level: config.logLevel,
  redact: {
    paths: ['req.headers.authorization', '*.password', '*.contrasena'],
    censor: '***',
  },
  formatters: {
    log(obj) {
      const copia = { ...obj };
      if (copia.from) copia.from = enmascararTelefono(copia.from);
      if (copia.to) copia.to = enmascararTelefono(copia.to);
      return copia;
    },
  },
});

module.exports = logger;
module.exports.enmascararTelefono = enmascararTelefono;
