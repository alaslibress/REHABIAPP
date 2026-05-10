'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const sessions = require('../src/sessions');

test('obtener crea sesion nueva si no existe', function () {
  const s = sessions.obtener('34999000001');
  assert.deepEqual(s.historial, []);
  assert.equal(s.pacienteDni, null);
  assert.equal(s.pendienteIdentificarDni, false);
});

test('anadirTurno agrega turno al historial', function () {
  sessions.anadirTurno('34999000002', 'user', 'Hola');
  sessions.anadirTurno('34999000002', 'assistant', 'Hola, como puedo ayudarte?');
  const s = sessions.obtener('34999000002');
  assert.equal(s.historial.length, 2);
  assert.equal(s.historial[0].role, 'user');
  assert.equal(s.historial[1].role, 'assistant');
});

test('anadirTurno trunca a 10 turnos', function () {
  for (let i = 0; i < 12; i++) {
    sessions.anadirTurno('34999000003', 'user', `msg ${i}`);
  }
  const s = sessions.obtener('34999000003');
  assert.equal(s.historial.length, 10);
});

test('actualizar modifica campos de la sesion', function () {
  sessions.obtener('34999000004');
  sessions.actualizar('34999000004', { pacienteDni: '12345678Z' });
  const s = sessions.obtener('34999000004');
  assert.equal(s.pacienteDni, '12345678Z');
});

test('reset elimina la sesion', function () {
  sessions.obtener('34999000005');
  sessions.reset('34999000005');
  const s = sessions.obtener('34999000005');
  assert.equal(s.pacienteDni, null, 'Tras reset la sesion debe ser nueva');
});
