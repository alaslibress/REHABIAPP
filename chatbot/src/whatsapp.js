'use strict';
const { Client, LocalAuth } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');
const logger = require('./logger');
const booking = require('./booking');

// ============================================================
// WORKAROUND temporal — whatsapp-web.js 1.34.7 + WhatsApp Web actual
// Bug: "Execution context was destroyed, most likely because of a navigation"
//      en Client.inject() linea 146 — el pupPage.evaluate del poll inicial
//      muere si WA Web hace navegacion durante esos 30s.
// Fix: pre-espera con waitForFunction (resiliente a navegacion) + reintento
//      del inject completo si vuelve a navegar a media inyeccion.
// Eliminar cuando salga release ww.js > 1.34.7 con el parche oficial.
// Documentado en engram: decision/wwjs-inject-execution-context-workaround
// ============================================================
const ClientProto = require('whatsapp-web.js/src/Client').prototype;
if (!ClientProto.__injectPatched) {
  const origInject = ClientProto.inject;
  ClientProto.inject = async function patchedInject() {
    await this.pupPage
      .waitForFunction('window.Debug?.VERSION !== undefined', {
        timeout: 60000,
        polling: 500,
      })
      .catch(function () {});

    let lastErr;
    for (let intento = 0; intento < 5; intento++) {
      try {
        return await origInject.call(this);
      } catch (e) {
        const msg = String((e && e.message) || e);
        if (/Execution context was destroyed|context destroyed|navigation/i.test(msg)) {
          lastErr = e;
          logger.warn({ intento: intento + 1 }, 'inject() murio por navegacion, reintentando');
          await this.pupPage
            .waitForFunction('window.Debug?.VERSION !== undefined', {
              timeout: 30000,
              polling: 500,
            })
            .catch(function () {});
          await new Promise(function (r) { setTimeout(r, 1500); });
          continue;
        }
        throw e;
      }
    }
    throw lastErr || new Error('inject retries exhausted');
  };
  ClientProto.__injectPatched = true;
}

// Construye un cliente whatsapp-web.js con auth persistente.
// Primera ejecucion: imprime QR en terminal — escanear desde la app de WhatsApp
// del numero del hospital. Despues, .wwebjs_auth/ guarda la sesion.
// Pinea la version de WhatsApp Web para evitar el error "Execution context was destroyed"
// que aparece cuando WA actualiza su frontend y rompe la inyeccion de whatsapp-web.js.
// Mantener este HTML actualizado periodicamente desde wppconnect-team/wa-version.
const WA_WEB_VERSION_URL =
  'https://raw.githubusercontent.com/wppconnect-team/wa-version/main/html/2.3000.1039749116-alpha.html';

function crearCliente() {
  const cliente = new Client({
    authStrategy: new LocalAuth({ clientId: 'rehabiapp-chatbot' }),
    puppeteer: {
      args: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-blink-features=AutomationControlled',
        '--disable-dev-shm-usage',
      ],
    },
    webVersionCache: {
      type: 'remote',
      remotePath: WA_WEB_VERSION_URL,
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
