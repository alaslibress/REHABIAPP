'use strict';
require('dotenv').config();

function leer(name, defecto) {
  const valor = process.env[name];
  if (valor === undefined || valor === '') {
    if (defecto === undefined) {
      throw new Error(`Variable de entorno requerida ausente: ${name}`);
    }
    return defecto;
  }
  return valor;
}

const config = Object.freeze({
  port: parseInt(leer('PORT', '4000'), 10),
  logLevel: leer('LOG_LEVEL', 'info'),

  pg: Object.freeze({
    host: leer('PGHOST'),
    port: parseInt(leer('PGPORT', '5432'), 10),
    database: leer('PGDATABASE'),
    user: leer('PGUSER'),
    password: leer('PGPASSWORD'),
    ssl: leer('PGSSLMODE', 'disable') !== 'disable' ? { rejectUnauthorized: false } : false,
  }),

  ollama: Object.freeze({
    url: leer('OLLAMA_URL', 'http://localhost:11434'),
    model: leer('OLLAMA_MODEL', 'qwen2.5:latest'),
  }),

  whatsapp: Object.freeze({
    hospitalPhoneE164: leer('HOSPITAL_PHONE_E164'),
  }),

  reglas: Object.freeze({
    slotMinutes: parseInt(leer('SLOT_MINUTES', '30'), 10),
    openTime: leer('OPEN_TIME', '09:00'),
    closeTime: leer('CLOSE_TIME', '18:00'),
    workingDays: leer('WORKING_DAYS', '1,2,3,4,5').split(',').map(function (s) { return parseInt(s, 10); }),
    minLeadHours: parseInt(leer('MIN_LEAD_HOURS', '2'), 10),
  }),

  auditActor: leer('AUDIT_ACTOR', 'CHATBOT_BOT'),
});

module.exports = config;
