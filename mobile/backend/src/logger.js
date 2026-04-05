// Logger centralizado del BFF
// Unica instancia de pino para todo el backend — importar desde aqui
// No crear instancias de pino en otros modulos
'use strict';

const pino = require('pino');

const logger = pino({ level: process.env.LOG_LEVEL || 'info' });

module.exports = logger;
