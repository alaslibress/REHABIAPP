'use strict';
const { Client, LocalAuth } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');
const logger = require('./logger');
const booking = require('./booking');

// Construye un cliente whatsapp-web.js con auth persistente.
// Primera ejecucion: imprime QR en terminal — escanear desde la app de WhatsApp
// del numero del hospital. Despues, .wwebjs_auth/ guarda la sesion.
function crearCliente() {
  const cliente = new Client({
    authStrategy: new LocalAuth({ clientId: 'rehabiapp-chatbot' }),
    puppeteer: {
      args: ['--no-sandbox', '--disable-setuid-sandbox'],
    },
  });

  cliente.on('qr', function (qr) {
    logger.info('Escanea este QR con la app de WhatsApp del numero del hospital:');
    qrcode.generate(qr, { small: true });
  });

  cliente.on('ready', function () {
    logger.info('WhatsApp client listo. Esperando mensajes.');
  });

  cliente.on('auth_failure', function (msg) {
    logger.error({ msg }, 'Fallo de autenticacion WhatsApp — borra .wwebjs_auth/ y reinicia');
  });

  cliente.on('disconnected', function (motivo) {
    logger.warn({ motivo }, 'WhatsApp desconectado — proceso terminara, supervisor lo reinicia');
    process.exit(1);
  });

  cliente.on('message', async function (msg) {
    // Ignorar grupos, broadcasts, mensajes propios y stickers / media sin texto
    if (msg.fromMe) return;
    if (msg.from.endsWith('@g.us')) return;
    if (msg.from === 'status@broadcast') return;
    if (!msg.body || msg.body.trim() === '') return;

    // msg.from formato '34628678888@c.us' — extraer E.164 sin '+'
    const phoneE164 = msg.from.split('@')[0];
    logger.info({ from: phoneE164, len: msg.body.length }, 'Mensaje recibido');

    try {
      const respuesta = await booking.manejarMensaje(phoneE164, msg.body);
      await msg.reply(respuesta);
    } catch (err) {
      logger.error({ err: err.message, from: phoneE164 }, 'Error manejando mensaje');
      await msg.reply('Ha habido un problema. Por favor, intentalo de nuevo en unos minutos.').catch(function () {});
    }
  });

  return cliente;
}

module.exports = { crearCliente };
