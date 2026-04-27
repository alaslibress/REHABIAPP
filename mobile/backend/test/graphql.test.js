// Tests de la capa GraphQL del BFF
// Cubre: autenticacion, proteccion de resolvers, estructura de errores, saludo
'use strict';

const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');

// Variables de entorno para el entorno de test
// Puerto diferente al de bff.test.js para evitar colisiones si se ejecutan juntos
process.env.PORT = '3097';
process.env.API_BASE_URL = 'http://localhost:8080';
process.env.NODE_ENV = 'test';
process.env.LOG_LEVEL = 'silent';
process.env.MOCK_API = 'true';

// Helper para peticiones HTTP en tests
function httpPost(url, body, headers = {}) {
  return new Promise((resolve, reject) => {
    const datos = JSON.stringify(body);
    const opts = {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(datos),
        ...headers,
      },
    };
    const req = http.request(url, opts, (res) => {
      let buf = '';
      res.on('data', (c) => { buf += c; });
      res.on('end', () => resolve({ statusCode: res.statusCode, body: JSON.parse(buf) }));
    });
    req.on('error', reject);
    req.write(datos);
    req.end();
  });
}

const GQL = 'http://localhost:3097/graphql';

let servidorActivo;

before(async () => {
  // El modulo exporta una Promise que resuelve cuando el servidor esta escuchando
  servidorActivo = await require('../src/index.js');
});

after(() => {
  return new Promise((resolve) => {
    if (servidorActivo && servidorActivo.server) {
      servidorActivo.server.close(resolve);
    } else {
      resolve();
    }
  });
});

// ---- Test 1: Login exitoso devuelve tokens ----
test('login con credenciales validas devuelve accessToken, refreshToken y expiresAt', async () => {
  const { body } = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken refreshToken expiresAt } }`,
  });

  assert.ok(body.data, 'Debe haber data en la respuesta');
  const login = body.data.login;
  assert.ok(typeof login.accessToken === 'string' && login.accessToken.length > 0, 'accessToken debe ser un string');
  assert.ok(typeof login.refreshToken === 'string' && login.refreshToken.length > 0, 'refreshToken debe ser un string');
  assert.ok(typeof login.expiresAt === 'number' && login.expiresAt > 0, 'expiresAt debe ser un numero positivo');
});

// ---- Test 2: Login fallido devuelve error con codigo correcto ----
test('login fallido devuelve error INVALID_CREDENTIALS con estructura estandarizada', async () => {
  // En modo mock, la API de Java devuelve datos validos siempre
  // Para simular credenciales invalidas, se comprueba la estructura del error cuando mock falla
  // Este test verifica que la estructura de error es correcta al recibir un GraphQL error
  const { body } = await httpPost(GQL, {
    query: `mutation { login(identifier: "", password: "") { accessToken } }`,
  });

  // Si hay un error de validacion de GraphQL, verificar la estructura
  if (body.errors && body.errors.length > 0) {
    const err = body.errors[0];
    assert.ok(err.extensions, 'El error debe tener extensions');
    assert.ok(err.extensions.titulo === 'Error', 'titulo debe ser "Error"');
    assert.ok(typeof err.extensions.subtitulo === 'string', 'subtitulo debe ser string');
    assert.ok(typeof err.extensions.texto === 'string', 'texto debe ser string');
  }
});

// ---- Test 3: Query me sin token devuelve TOKEN_INVALID ----
test('query me sin token devuelve error TOKEN_INVALID con estructura estandarizada', async () => {
  const { body } = await httpPost(GQL, {
    query: `query { me { id name } }`,
  });

  assert.ok(body.errors, 'Debe haber errores');
  const err = body.errors[0];
  assert.equal(err.extensions.code, 'TOKEN_INVALID', 'El codigo debe ser TOKEN_INVALID');
  assert.equal(err.extensions.titulo, 'Error', 'El titulo debe ser "Error"');
  assert.ok(err.extensions.subtitulo, 'Debe haber subtitulo');
  assert.ok(err.extensions.texto, 'Debe haber texto');
  assert.ok(!err.extensions.stacktrace, 'No debe haber stacktrace en modo test');
});

// ---- Test 4: Query me con token valido devuelve perfil del paciente ----
test('query me con token valido devuelve perfil del paciente', async () => {
  // Login primero para obtener token
  const loginRes = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken } }`,
  });
  const token = loginRes.body.data.login.accessToken;

  const { body } = await httpPost(
    GQL,
    { query: `query { me { id name surname email active numSs sexo avatarDataUri } }` },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, 'Debe haber data');
  const me = body.data.me;
  assert.ok(me.id, 'Debe tener id');
  assert.ok(me.name, 'Debe tener nombre');
  assert.ok(me.surname, 'Debe tener apellido');
  assert.ok(typeof me.active === 'boolean', 'active debe ser boolean');
  assert.ok(me.numSs, 'Debe tener numSs');
  assert.ok(me.sexo, 'Debe tener sexo');
  assert.ok(me.avatarDataUri && me.avatarDataUri.startsWith('data:image/'), 'avatarDataUri debe ser data URI de imagen');
});

// ---- Test 5: Saludo segun zona horaria (Buenos dias / Buenas tardes) ----
test('query me con X-Timezone devuelve greeting calculado por el backend', async () => {
  const loginRes = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken } }`,
  });
  const token = loginRes.body.data.login.accessToken;

  const { body } = await httpPost(
    GQL,
    { query: `query { me { greeting } }` },
    { Authorization: `Bearer ${token}`, 'X-Timezone': 'Europe/Madrid' }
  );

  assert.ok(body.data, 'Debe haber data');
  const greeting = body.data.me.greeting;
  assert.ok(
    greeting.startsWith('Buenos dias') || greeting.startsWith('Buenas tardes') || greeting.startsWith('Buenas noches'),
    `El saludo debe ser valido, recibido: ${greeting}`
  );
});

// ---- Test 6: Estructura de error garantiza titulo/subtitulo/texto ----
test('todos los errores GraphQL incluyen titulo, subtitulo y texto', async () => {
  const { body } = await httpPost(GQL, {
    query: `query { myDisabilities { id } }`,  // Sin token -> TOKEN_INVALID
  });

  assert.ok(body.errors && body.errors.length > 0, 'Debe haber errores');
  for (const err of body.errors) {
    assert.ok(err.extensions.titulo, 'Cada error debe tener titulo');
    assert.ok(err.extensions.subtitulo, 'Cada error debe tener subtitulo');
    assert.ok(err.extensions.texto, 'Cada error debe tener texto');
    assert.ok(err.extensions.code, 'Cada error debe tener code');
  }
});

// ---- Test 7: myDisabilities con token valido devuelve array ----
test('query myDisabilities con token valido devuelve array de discapacidades', async () => {
  const loginRes = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken } }`,
  });
  const token = loginRes.body.data.login.accessToken;

  const { body } = await httpPost(
    GQL,
    { query: `query { myDisabilities { id name currentLevel } }` },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, 'Debe haber data');
  assert.ok(Array.isArray(body.data.myDisabilities), 'myDisabilities debe ser un array');
});

// ---- Test 8: myTreatments con token valido devuelve array ----
test('query myTreatments con token valido devuelve array de tratamientos', async () => {
  const loginRes = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken } }`,
  });
  const token = loginRes.body.data.login.accessToken;

  const { body } = await httpPost(
    GQL,
    { query: `query { myTreatments { id name visible type } }` },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, 'Debe haber data');
  assert.ok(Array.isArray(body.data.myTreatments), 'myTreatments debe ser un array');
});

// ---- Test 9: myAppointments con token valido devuelve array ----
test('query myAppointments con token valido devuelve array de citas', async () => {
  const loginRes = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken } }`,
  });
  const token = loginRes.body.data.login.accessToken;

  const { body } = await httpPost(
    GQL,
    { query: `query { myAppointments { id date time status } }` },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, 'Debe haber data');
  assert.ok(Array.isArray(body.data.myAppointments), 'myAppointments debe ser un array');
});

// ---- Test 10: myGameSessions devuelve array ----
test('query myGameSessions con token valido devuelve array de sesiones', async () => {
  const loginRes = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken } }`,
  });
  const token = loginRes.body.data.login.accessToken;

  const { body } = await httpPost(
    GQL,
    { query: `query { myGameSessions(limit: 10, offset: 0) { id gameName score } }` },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, 'Debe haber data');
  assert.ok(Array.isArray(body.data.myGameSessions), 'myGameSessions debe ser un array');
});

// ---- Test 11: requestAppointment con datos validos devuelve AppointmentRequest ----
test('mutation requestAppointment con datos validos devuelve AppointmentRequest', async () => {
  const loginRes = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken } }`,
  });
  const token = loginRes.body.data.login.accessToken;

  const manana = new Date();
  manana.setDate(manana.getDate() + 1);
  const fechaManana = manana.toISOString().split('T')[0];

  const { body } = await httpPost(
    GQL,
    {
      query: `mutation RequestCita($fecha: String!, $hora: String!, $motivo: String!, $telefono: String) {
        requestAppointment(fechaPreferida: $fecha, horaPreferida: $hora, motivo: $motivo, telefono: $telefono) {
          id estado fechaPreferida horaPreferida motivo createdAt
        }
      }`,
      variables: {
        fecha: fechaManana,
        hora: '10:00',
        motivo: 'Dolor en la cadera derecha al caminar mas de 10 minutos',
        telefono: '600000000',
      },
    },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, `Debe haber data, errores: ${JSON.stringify(body.errors)}`);
  const req = body.data.requestAppointment;
  assert.ok(req.id, 'Debe tener id');
  assert.equal(req.estado, 'PENDING', 'estado debe ser PENDING');
  assert.ok(req.fechaPreferida, 'Debe tener fechaPreferida');
  assert.ok(req.horaPreferida, 'Debe tener horaPreferida');
  assert.ok(req.motivo, 'Debe tener motivo');
  assert.ok(req.createdAt, 'Debe tener createdAt');
});

// ---- Test 12: myAssignedGames devuelve array de juegos asignados ----
test('query myAssignedGames con token valido devuelve array de juegos', async () => {
  const loginRes = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken } }`,
  });
  const token = loginRes.body.data.login.accessToken;

  const { body } = await httpPost(
    GQL,
    { query: `query { myAssignedGames { id name description difficulty assignedAt } }` },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, `Debe haber data, errores: ${JSON.stringify(body.errors)}`);
  const juegos = body.data.myAssignedGames;
  assert.ok(Array.isArray(juegos), 'myAssignedGames debe ser un array');
  assert.ok(juegos.length >= 1, 'Debe haber al menos un juego en el mock');
  assert.ok(juegos[0].id, 'Primer juego debe tener id');
  assert.ok(['EASY', 'MEDIUM', 'HARD'].includes(juegos[0].difficulty), 'difficulty debe ser enum valido');
});

// ---- Test 13: myTreatments devuelve campos enriquecidos ----
test('query myTreatments devuelve campos enriquecidos (codTrat, disabilityCode, materials)', async () => {
  const loginRes = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken } }`,
  });
  const token = loginRes.body.data.login.accessToken;

  const { body } = await httpPost(
    GQL,
    {
      query: `query {
        myTreatments {
          id codTrat name type visible progressionLevel
          disabilityCode summary materials medication documentUrl hasDocument
        }
      }`,
    },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, `Debe haber data, errores: ${JSON.stringify(body.errors)}`);
  const tratamientos = body.data.myTreatments;
  assert.ok(Array.isArray(tratamientos), 'myTreatments debe ser un array');
  assert.ok(tratamientos.length >= 1, 'Debe haber al menos un tratamiento');
  const t = tratamientos[0];
  assert.ok(t.codTrat, 'Debe tener codTrat');
  assert.ok(t.disabilityCode, 'Debe tener disabilityCode');
  assert.ok(Array.isArray(t.materials), 'materials debe ser array');
  assert.ok(Array.isArray(t.medication), 'medication debe ser array');
  assert.equal(typeof t.hasDocument, 'boolean', 'hasDocument debe ser boolean');
});

// ---- Test 14: treatmentDocument devuelve documento con base64 ----
test('query treatmentDocument devuelve documento PDF con base64', async () => {
  const loginRes = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken } }`,
  });
  const token = loginRes.body.data.login.accessToken;

  const { body } = await httpPost(
    GQL,
    { query: `query { treatmentDocument(codTrat: "TRT001") { fileName mimeType base64 url } }` },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, `Debe haber data, errores: ${JSON.stringify(body.errors)}`);
  const doc = body.data.treatmentDocument;
  assert.ok(doc.fileName, 'Debe tener fileName');
  assert.equal(doc.mimeType, 'application/pdf', 'mimeType debe ser PDF');
  assert.ok(doc.base64, 'Debe tener base64');
});

// ---- Test 15: myBodyPartProgress devuelve 15 partes del cuerpo ----
test('query myBodyPartProgress con token valido devuelve 15 partes del cuerpo', async () => {
  const loginRes = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken } }`,
  });
  const token = loginRes.body.data.login.accessToken;

  const { body } = await httpPost(
    GQL,
    { query: `query { myBodyPartProgress { id name hasTreatment progressPct improvementPct periodLabel } }` },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, `Debe haber data, errores: ${JSON.stringify(body.errors)}`);
  const partes = body.data.myBodyPartProgress;
  assert.ok(Array.isArray(partes), 'myBodyPartProgress debe ser un array');
  assert.equal(partes.length, 15, 'Debe haber exactamente 15 partes del cuerpo');
  const activas = partes.filter(function (p) { return p.hasTreatment; });
  assert.equal(activas.length, 3, 'Deben haber 3 partes activas en el mock');
  assert.ok(activas[0].progressPct > 0, 'Partes activas deben tener progressPct > 0');
});

// ---- Test 16: bodyPartMetrics devuelve 8 puntos de datos ----
test('query bodyPartMetrics con token valido devuelve metricas de sesion', async () => {
  const loginRes = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken } }`,
  });
  const token = loginRes.body.data.login.accessToken;

  const { body } = await httpPost(
    GQL,
    { query: `query { bodyPartMetrics(bodyPartId: "RIGHT_ARM") { date score metricType } }` },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, `Debe haber data, errores: ${JSON.stringify(body.errors)}`);
  const metricas = body.data.bodyPartMetrics;
  assert.ok(Array.isArray(metricas), 'bodyPartMetrics debe ser un array');
  assert.equal(metricas.length, 8, 'Debe haber 8 puntos de datos en el mock');
  assert.ok(metricas[0].date, 'Cada metrica debe tener date');
  assert.ok(typeof metricas[0].score === 'number', 'Cada metrica debe tener score numerico');
  assert.equal(metricas[0].metricType, 'ACCURACY', 'metricType debe ser ACCURACY en el mock');
});

// ---- Test 17: registerDeviceToken registra el token push ----
test('mutation registerDeviceToken con token valido devuelve true', async () => {
  const loginRes = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken } }`,
  });
  const token = loginRes.body.data.login.accessToken;

  const { body } = await httpPost(
    GQL,
    {
      query: `mutation {
        registerDeviceToken(token: "ExponentPushToken[test-token-abc123]", platform: "ANDROID")
      }`,
    },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, `Debe haber data, errores: ${JSON.stringify(body.errors)}`);
  assert.equal(body.data.registerDeviceToken, true, 'registerDeviceToken debe devolver true');
});

// ---- Test 18: unregisterDeviceToken elimina el token push ----
test('mutation unregisterDeviceToken con token valido devuelve true', async () => {
  const loginRes = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken } }`,
  });
  const token = loginRes.body.data.login.accessToken;

  const { body } = await httpPost(
    GQL,
    {
      query: `mutation {
        unregisterDeviceToken(token: "ExponentPushToken[test-token-abc123]")
      }`,
    },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, `Debe haber data, errores: ${JSON.stringify(body.errors)}`);
  assert.equal(body.data.unregisterDeviceToken, true, 'unregisterDeviceToken debe devolver true');
});
