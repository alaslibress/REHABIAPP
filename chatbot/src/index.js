'use strict';
const fs = require('fs');
const path = require('path');
const express = require('express');
const config = require('./config');
const logger = require('./logger');

// Limpia Singleton* huerfanos del run anterior — evita el loop crash-restart
// de Chromium "profile appears to be in use by another Chromium process".
// Se ejecuta antes de cargar whatsapp.js para que Chrome arranque limpio.
(function limpiarLocksChromium() {
  const sessionDir = '/app/.wwebjs_auth/session-rehabiapp-chatbot';
  for (const nombre of ['SingletonLock', 'SingletonSocket', 'SingletonCookie', 'lockfile']) {
    try {
      fs.unlinkSync(path.join(sessionDir, nombre));
      logger.warn({ nombre }, 'lock Chromium huerfano eliminado');
    } catch (_e) {
      // no existe, OK
    }
  }
})();

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
