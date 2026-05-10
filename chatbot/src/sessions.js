'use strict';

// Sesiones en memoria. Un cierre del servicio reinicia toda la conversacion —
// aceptable para MVP. La clave es el numero E.164 sin '+' (igual que from de WA).
//
// ConversationState = {
//   historial: [{role:'user'|'assistant', content:string}],
//   pacienteDni: string|null,
//   pendienteIdentificarDni: boolean,
//   ultimaInteraccion: number  // ms desde epoch
// }

const TTL_MS = 30 * 60 * 1000; // 30 minutos sin actividad → reset

const sesiones = new Map();

function obtener(phoneE164) {
  let s = sesiones.get(phoneE164);
  if (!s || (Date.now() - s.ultimaInteraccion > TTL_MS)) {
    s = {
      historial: [],
      pacienteDni: null,
      pendienteIdentificarDni: false,
      ultimaInteraccion: Date.now(),
    };
    sesiones.set(phoneE164, s);
  }
  return s;
}

function actualizar(phoneE164, parche) {
  const s = obtener(phoneE164);
  Object.assign(s, parche, { ultimaInteraccion: Date.now() });
  sesiones.set(phoneE164, s);
}

function anadirTurno(phoneE164, role, content) {
  const s = obtener(phoneE164);
  s.historial.push({ role, content });
  // Truncar a los ultimos 10 turnos para no inflar el contexto del LLM
  if (s.historial.length > 10) s.historial = s.historial.slice(-10);
  s.ultimaInteraccion = Date.now();
}

function reset(phoneE164) {
  sesiones.delete(phoneE164);
}

module.exports = { obtener, actualizar, anadirTurno, reset };
