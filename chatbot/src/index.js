'use strict';
const express = require('express');
const config = require('./config');
const logger = require('./logger');
const wa = require('./whatsapp');

async function main() {
  // 1) Servidor HTTP de health para K8s / monitorizacion
  const app = express();
  app.get('/health', function (_req, res) { res.json({ status: 'UP' }); });
  app.listen(config.port, function () {
    logger.info({ port: config.port }, 'HTTP health server escuchando');
  });

  // 2) Cliente WhatsApp (bloquea hasta event 'ready')
  const cliente = wa.crearCliente();
  await cliente.initialize();
}

main().catch(function (err) {
  logger.fatal({ err: err.message, stack: err.stack }, 'Fallo critico de arranque');
  process.exit(1);
});

// Apagado limpio
process.on('SIGTERM', function () { process.exit(0); });
process.on('SIGINT', function () { process.exit(0); });
