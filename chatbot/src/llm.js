'use strict';
const { Ollama } = require('ollama');
const config = require('./config');
const logger = require('./logger');
const { SYSTEM_PROMPT } = require('./promptTemplates');

const cliente = new Ollama({ host: config.ollama.url });

// Construye el array de mensajes del chat para Ollama:
// [system, ...historial, user]
function construirMensajes(historial, nuevoMensaje) {
  const hoy = new Date().toISOString().slice(0, 10);
  const sistema = SYSTEM_PROMPT.replace('{{HOY_ISO}}', hoy);
  const turnos = historial.map(function (turno) {
    return { role: turno.role, content: turno.content };
  });
  return [
    { role: 'system', content: sistema },
    ...turnos,
    { role: 'user', content: nuevoMensaje },
  ];
}

// Llama a Qwen y parsea la respuesta JSON. Si el modelo devuelve algo no parseable,
// devolvemos un fallback con intent='other'.
async function extraerIntencion(historial, nuevoMensaje) {
  const mensajes = construirMensajes(historial, nuevoMensaje);
  let raw;
  try {
    const respuesta = await cliente.chat({
      model: config.ollama.model,
      messages: mensajes,
      format: 'json',
      stream: false,
      options: { temperature: 0.2 },
    });
    raw = respuesta.message.content;
  } catch (err) {
    logger.error({ err: err.message }, 'Fallo llamada a Ollama');
    return {
      intent: 'other',
      fecha: null,
      hora: null,
      respuesta_usuario: 'Lo siento, ahora mismo no puedo procesar tu mensaje. Por favor, llama al hospital.',
    };
  }

  try {
    const parsed = JSON.parse(raw);
    return {
      intent: parsed.intent || 'other',
      fecha: typeof parsed.fecha === 'string' ? parsed.fecha : null,
      hora: typeof parsed.hora === 'string' ? parsed.hora : null,
      respuesta_usuario: typeof parsed.respuesta_usuario === 'string' ? parsed.respuesta_usuario : '',
    };
  } catch (err) {
    logger.warn({ raw }, 'Qwen devolvio JSON invalido — fallback');
    return {
      intent: 'other',
      fecha: null,
      hora: null,
      respuesta_usuario: 'No te he entendido bien. Por favor, indicame la fecha (DD/MM/AAAA) y la hora (HH:MM).',
    };
  }
}

module.exports = { extraerIntencion };
