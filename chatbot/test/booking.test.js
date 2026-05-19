'use strict';
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');

// Stubs — sustituyen los modulos de infra antes de require('./booking')
// para que los tests no necesiten Postgres ni Ollama.

// Stub de db
const dbStub = {
  buscarPacientePorTelefono: async function () {
    return { dni_pac: '12345678Z', dni_san: '87654321B', nombre_pac: 'Admin', apellido1_pac: 'RehabiAPP', activo: true };
  },
  buscarPacientePorDni: async function () { return null; },
  slotDisponible: async function () { return true; },
  insertarCita: async function () { return true; },
  registrarAudit: async function () {},
};

// Stub de llm — devuelve intent 'book' con fecha y hora listas
const llmStubBook = {
  extraerIntencion: async function () {
    return { intent: 'book', fecha: '2026-06-15', hora: '10:00', respuesta_usuario: 'Confirmo.' };
  },
};

// Stub de llm — devuelve intent 'book' con sabado (dia no laborable)
const llmStubSabado = {
  extraerIntencion: async function () {
    return { intent: 'book', fecha: '2026-06-13', hora: '10:00', respuesta_usuario: 'Confirmo.' };
  },
};

// Inyeccion de mocks via Module._cache para CommonJS
const Module = require('module');

function inyectarMock(ruta, mock) {
  const rutaResuelta = require.resolve(ruta);
  Module._cache[rutaResuelta] = { id: rutaResuelta, filename: rutaResuelta, loaded: true, exports: mock };
}

function limpiarMock(ruta) {
  const rutaResuelta = require.resolve(ruta);
  delete Module._cache[rutaResuelta];
}

// Tambien necesitamos stub de config para que booking.js no falle al leer .env
inyectarMock('../src/config', {
  reglas: {
    workingDays: [1, 2, 3, 4, 5],
    openTime: '09:00',
    closeTime: '18:00',
    slotMinutes: 30,
    minLeadHours: 0, // sin antelacion minima en tests para no depender de la hora actual
  },
  auditActor: 'CHATBOT_BOT',
});

inyectarMock('../src/logger', {
  info: function () {},
  warn: function () {},
  error: function () {},
  fatal: function () {},
});

// ---- Test 1: mensaje con intent book, slot libre → responde "Cita confirmada" ----
test('manejarMensaje confirma cita cuando slot esta libre', async function () {
  inyectarMock('../src/db', dbStub);
  inyectarMock('../src/llm', llmStubBook);

  // Limpiar cache de booking + sessions para que tomen los nuevos stubs
  limpiarMock('../src/booking');
  limpiarMock('../src/sessions');

  const booking = require('../src/booking');
  const respuesta = await booking.manejarMensaje('34999111222', 'cita 15 de junio 10:00');

  assert.ok(respuesta.includes('Cita confirmada'), `Esperaba "Cita confirmada" en respuesta; recibido: "${respuesta}"`);

  limpiarMock('../src/db');
  limpiarMock('../src/llm');
  limpiarMock('../src/booking');
  limpiarMock('../src/sessions');
});

// ---- Test 2: fecha en sabado → responde FUERA_HORARIO ----
test('manejarMensaje rechaza cita en sabado con FUERA_HORARIO', async function () {
  inyectarMock('../src/db', dbStub);
  inyectarMock('../src/llm', llmStubSabado);
  limpiarMock('../src/booking');
  limpiarMock('../src/sessions');

  const booking = require('../src/booking');
  const { ERROR_REPLIES } = require('../src/promptTemplates');
  const respuesta = await booking.manejarMensaje('34999111333', 'cita el sabado 10:00');

  assert.equal(respuesta, ERROR_REPLIES.FUERA_HORARIO,
    `Esperaba FUERA_HORARIO; recibido: "${respuesta}"`);

  limpiarMock('../src/db');
  limpiarMock('../src/llm');
  limpiarMock('../src/booking');
  limpiarMock('../src/sessions');
});
