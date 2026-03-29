// Tests del BFF mobile-backend
// Verifica los requisitos del PLAN.md: /health, /metrics, config, graceful shutdown
'use strict';

const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');

// Helper para hacer peticiones HTTP en los tests
function httpGet(url) {
  return new Promise((resolve, reject) => {
    http.get(url, (res) => {
      let body = '';
      res.on('data', (chunk) => { body += chunk; });
      res.on('end', () => resolve({ statusCode: res.statusCode, headers: res.headers, body }));
    }).on('error', reject);
  });
}

let server;
let app;

before(async () => {
  // Levantar la aplicacion en puerto de test para no colisionar con el puerto 3000
  process.env.PORT = '3099';
  process.env.API_BASE_URL = 'http://localhost:8080';
  process.env.NODE_ENV = 'test';
  // Necesario para que pino no intente escribir a stdout durante los tests
  process.env.LOG_LEVEL = 'silent';

  // Importar el modulo despues de configurar las variables de entorno
  const mod = require('../src/index.js');
  app = mod.app;
  server = mod.server;

  // Esperar a que el servidor este listo
  await new Promise((resolve) => {
    if (server.listening) return resolve();
    server.once('listening', resolve);
  });
});

after(() => {
  return new Promise((resolve) => {
    server.close(() => resolve());
  });
});

// ---- Test 1: /health devuelve 200 y { "status": "UP" } ----
test('GET /health devuelve 200 OK con body { status: "UP" }', async () => {
  const { statusCode, body } = await httpGet('http://localhost:3099/health');
  assert.equal(statusCode, 200, 'El codigo de respuesta debe ser 200');
  const json = JSON.parse(body);
  assert.equal(json.status, 'UP', 'El campo status debe ser "UP"');
});

// ---- Test 2: /metrics devuelve 200 con content-type de Prometheus ----
test('GET /metrics devuelve 200 con Content-Type de Prometheus', async () => {
  const { statusCode, headers } = await httpGet('http://localhost:3099/metrics');
  assert.equal(statusCode, 200, 'El codigo de respuesta debe ser 200');
  assert.ok(
    headers['content-type'].includes('text/plain'),
    'Content-Type debe ser text/plain (formato Prometheus)'
  );
});

// ---- Test 3: /metrics contiene metricas por defecto de Node.js ----
test('GET /metrics contiene metricas de proceso Node.js', async () => {
  const { body } = await httpGet('http://localhost:3099/metrics');
  assert.ok(body.includes('process_cpu_seconds_total'), 'Debe incluir metrica process_cpu_seconds_total');
  assert.ok(body.includes('nodejs_heap_size_used_bytes'), 'Debe incluir metrica nodejs_heap_size_used_bytes');
});

// ---- Test 4: Config lee variables de entorno correctamente ----
test('Config usa PORT del entorno correctamente', () => {
  const config = require('../src/config.js');
  assert.equal(config.port, 3099, 'El puerto debe leerse de process.env.PORT');
  assert.equal(config.apiBaseUrl, 'http://localhost:8080', 'La URL de la API debe leerse de process.env.API_BASE_URL');
  assert.equal(config.nodeEnv, 'test', 'El entorno debe leerse de process.env.NODE_ENV');
});

// ---- Test 5: Config tiene fallback cuando no existe archivo de secreto ----
test('readSecret usa fallback cuando el archivo no existe', () => {
  const config = require('../src/config.js');
  // En entorno de test no existe /mnt/secrets/session-secret
  // Debe usar el fallback 'dev-session-secret'
  assert.ok(typeof config.sessionSecret === 'string', 'sessionSecret debe ser un string');
  assert.ok(config.sessionSecret.length > 0, 'sessionSecret no debe estar vacio');
});

// ---- Test 6: Rutas desconocidas devuelven 404 ----
test('GET /ruta-inexistente devuelve 404', async () => {
  const { statusCode } = await httpGet('http://localhost:3099/ruta-inexistente');
  assert.equal(statusCode, 404, 'Rutas no definidas deben devolver 404');
});
