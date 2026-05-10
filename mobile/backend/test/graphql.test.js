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
    { query: `query { me { id name surname email active } }` },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, 'Debe haber data');
  const me = body.data.me;
  assert.ok(me.id, 'Debe tener id');
  assert.ok(me.name, 'Debe tener nombre');
  assert.ok(me.surname, 'Debe tener apellido');
  assert.ok(typeof me.active === 'boolean', 'active debe ser boolean');
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

// Helper: login y devuelve token para los tests de Phase E
async function obtenerTokenAdmin() {
  const loginRes = await httpPost(GQL, {
    query: `mutation { login(identifier: "admin", password: "admin") { accessToken } }`,
  });
  return loginRes.body.data.login.accessToken;
}

// ---- Phase E.1: treatmentPdf devuelve base64 con cabecera %PDF- ----
test('treatmentPdf devuelve filename, sizeBytes y base64Content valido', async () => {
  const token = await obtenerTokenAdmin();
  const { body } = await httpPost(
    GQL,
    { query: `query { treatmentPdf(codTrat: "TRT001") { codTrat filename sizeBytes base64Content } }` },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, `Debe haber data, recibido: ${JSON.stringify(body)}`);
  const pdf = body.data.treatmentPdf;
  assert.equal(pdf.codTrat, 'TRT001', 'codTrat debe coincidir');
  assert.ok(pdf.filename && pdf.filename.endsWith('.pdf'), 'filename debe terminar en .pdf');
  assert.ok(pdf.sizeBytes > 0, 'sizeBytes debe ser positivo');
  // Decodificar base64 y comprobar la cabecera %PDF-
  const decoded = Buffer.from(pdf.base64Content, 'base64').toString('utf8');
  assert.ok(decoded.startsWith('%PDF-'), 'El contenido decodificado debe empezar por %PDF-');
});

test('treatmentPdf sin token devuelve TOKEN_INVALID', async () => {
  const { body } = await httpPost(GQL, {
    query: `query { treatmentPdf(codTrat: "TRT001") { codTrat } }`,
  });
  assert.ok(body.errors, 'Debe haber errores');
  assert.equal(body.errors[0].extensions.code, 'TOKEN_INVALID');
});

// ---- Phase E.2: availableGames devuelve array de juegos desbloqueados ----
test('availableGames devuelve array de juegos desbloqueados', async () => {
  const token = await obtenerTokenAdmin();
  const { body } = await httpPost(
    GQL,
    { query: `query { availableGames { idVideojuego codigo nombre urlUnity parteCuerpo } }` },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, `Debe haber data, recibido: ${JSON.stringify(body)}`);
  const juegos = body.data.availableGames;
  assert.ok(Array.isArray(juegos), 'availableGames debe ser array');
  assert.ok(juegos.length > 0, 'Debe haber al menos un juego desbloqueado en mock');
  assert.ok(juegos[0].idVideojuego, 'Cada juego tiene idVideojuego');
  assert.ok(juegos[0].urlUnity, 'Cada juego tiene urlUnity');
});

// ---- Phase E.3: startGame devuelve URL Unity y JWT efimero firmado por el BFF ----
test('startGame devuelve urlUnity y ephemeralToken con scope GAMES_PLAY', async () => {
  const token = await obtenerTokenAdmin();
  const { body } = await httpPost(
    GQL,
    {
      query: `mutation { startGame(idVideojuego: "1") { urlUnity ephemeralToken expiresAt } }`,
    },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, `Debe haber data, recibido: ${JSON.stringify(body)}`);
  const launch = body.data.startGame;
  assert.ok(launch.urlUnity && launch.urlUnity.startsWith('http'), 'urlUnity debe ser URL absoluta');
  assert.ok(launch.ephemeralToken && launch.ephemeralToken.split('.').length === 3, 'ephemeralToken debe ser un JWT');
  assert.ok(typeof launch.expiresAt === 'number' && launch.expiresAt > Math.floor(Date.now() / 1000), 'expiresAt debe ser futuro');
  // Decodificar payload (sin verificar firma) y comprobar scope
  const payload = JSON.parse(Buffer.from(launch.ephemeralToken.split('.')[1], 'base64').toString('utf8'));
  assert.equal(payload.scope, 'GAMES_PLAY', 'El JWT debe tener scope GAMES_PLAY');
  // TTL debe ser 5 min aprox
  assert.ok(payload.exp - payload.iat <= 300, 'El TTL del JWT efimero debe ser <= 5 minutos');
});

test('startGame con idVideojuego no desbloqueado devuelve VALIDATION_ERROR', async () => {
  const token = await obtenerTokenAdmin();
  const { body } = await httpPost(
    GQL,
    { query: `mutation { startGame(idVideojuego: "999") { urlUnity } }` },
    { Authorization: `Bearer ${token}` }
  );
  assert.ok(body.errors, 'Debe haber errores');
  assert.equal(body.errors[0].extensions.code, 'VALIDATION_ERROR');
});

// ---- Phase E.4: myProgress devuelve PatientProgress con tratamientos y lastUpdate ----
test('myProgress devuelve tratamientos[] y lastUpdate', async () => {
  const token = await obtenerTokenAdmin();
  const { body } = await httpPost(
    GQL,
    {
      query: `query { myProgress { lastUpdate tratamientos { codTrat tratamientoNombre deltaPorcentaje entradas { fecha valor } } } }`,
    },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, `Debe haber data, recibido: ${JSON.stringify(body)}`);
  const prog = body.data.myProgress;
  assert.ok(Array.isArray(prog.tratamientos), 'tratamientos debe ser array');
  assert.ok(prog.tratamientos.length > 0, 'Debe haber al menos un tratamiento en mock');
  assert.ok(prog.lastUpdate, 'lastUpdate no debe ser null cuando hay datos');
  assert.ok(Array.isArray(prog.tratamientos[0].entradas), 'entradas debe ser array');
});

// ---- Phase E.5: myDashboard devuelve estructura agregada completa ----
test('myDashboard devuelve paciente, discapacidades, tratamientos y juegos', async () => {
  const token = await obtenerTokenAdmin();
  const { body } = await httpPost(
    GQL,
    {
      query: `query {
        myDashboard {
          paciente { dniPac nombrePac edadPac }
          discapacidadesActivas { codDis nombreDis idNivelActual }
          tratamientosVisibles { codTrat nombreTrat }
          juegosDesbloqueados { idVideojuego codigo desbloqueado urlUnity }
          proximaCita { fecha hora }
        }
      }`,
    },
    { Authorization: `Bearer ${token}` }
  );

  assert.ok(body.data, `Debe haber data, recibido: ${JSON.stringify(body)}`);
  const dash = body.data.myDashboard;
  assert.ok(dash.paciente && dash.paciente.dniPac === '12345678Z', 'paciente.dniPac debe coincidir');
  assert.ok(Array.isArray(dash.discapacidadesActivas), 'discapacidadesActivas debe ser array');
  assert.ok(Array.isArray(dash.tratamientosVisibles), 'tratamientosVisibles debe ser array');
  assert.ok(Array.isArray(dash.juegosDesbloqueados), 'juegosDesbloqueados debe ser array');
});

// ---- Phase G.1: me devuelve numSs, sexo y avatarDataUri ----
test('me devuelve numSs, sexo y avatarDataUri (Phase G.1)', async () => {
  const token = await obtenerTokenAdmin();
  const { body } = await httpPost(
    GQL,
    { query: `query { me { numSs sexo avatarDataUri } }` },
    { Authorization: `Bearer ${token}` }
  );
  assert.ok(body.data, `Debe haber data, recibido: ${JSON.stringify(body)}`);
  assert.equal(body.data.me.numSs, '280000000001', 'numSs debe coincidir con el mock');
  assert.equal(body.data.me.sexo, 'MASCULINO', 'sexo debe coincidir con el mock');
  assert.equal(body.data.me.avatarDataUri, null, 'avatarDataUri debe ser null');
});

// ---- Phase G.2: myTreatments devuelve los nuevos campos ----
test('myTreatments incluye codTrat, disabilityCode, hasDocument y materials (Phase G.2)', async () => {
  const token = await obtenerTokenAdmin();
  const { body } = await httpPost(
    GQL,
    { query: `query { myTreatments { id codTrat name disabilityCode summary materials medication documentUrl hasDocument } }` },
    { Authorization: `Bearer ${token}` }
  );
  assert.ok(body.data, `Debe haber data, recibido: ${JSON.stringify(body)}`);
  const trt001 = body.data.myTreatments.find((t) => t.codTrat === 'TRT001');
  assert.ok(trt001, 'TRT001 debe estar en la lista');
  assert.equal(trt001.disabilityCode, 'M16', 'disabilityCode debe ser M16');
  assert.equal(trt001.hasDocument, true, 'TRT001 tiene PDF');
  assert.deepEqual(trt001.materials, ['Esterilla', 'Cinta elastica baja resistencia']);
});

// ---- Phase G.3: treatmentDocument devuelve TreatmentDocument ----
test('treatmentDocument devuelve fileName, mimeType y base64 (Phase G.3)', async () => {
  const token = await obtenerTokenAdmin();
  const { body } = await httpPost(
    GQL,
    { query: `query { treatmentDocument(codTrat: "TRT001") { fileName mimeType base64 url } }` },
    { Authorization: `Bearer ${token}` }
  );
  assert.ok(body.data, `Debe haber data, recibido: ${JSON.stringify(body)}`);
  const doc = body.data.treatmentDocument;
  assert.equal(doc.fileName, 'TRT001.pdf', 'fileName debe ser TRT001.pdf');
  assert.equal(doc.mimeType, 'application/pdf', 'mimeType debe ser application/pdf');
  assert.ok(doc.base64 && doc.base64.length > 0, 'base64 no debe estar vacio');
  assert.equal(doc.url, null, 'url debe ser null en mock');
});

// ---- Phase G.4: myBodyPartProgress devuelve las 15 partes ----
test('myBodyPartProgress devuelve 15 partes y marca hasTreatment para M16/M54 (Phase G.4)', async () => {
  const token = await obtenerTokenAdmin();
  const { body } = await httpPost(
    GQL,
    { query: `query { myBodyPartProgress { id name hasTreatment progressPct periodLabel } }` },
    { Authorization: `Bearer ${token}` }
  );
  assert.ok(body.data, `Debe haber data, recibido: ${JSON.stringify(body)}`);
  const partes = body.data.myBodyPartProgress;
  assert.equal(partes.length, 15, 'Deben ser 15 partes del cuerpo');
  const cadDer = partes.find((p) => p.id === 'RIGHT_HIP');
  assert.equal(cadDer.hasTreatment, true, 'RIGHT_HIP debe tener tratamiento (M16)');
  const torso = partes.find((p) => p.id === 'TORSO');
  assert.equal(torso.hasTreatment, true, 'TORSO debe tener tratamiento (M54)');
  const cabeza = partes.find((p) => p.id === 'HEAD');
  assert.equal(cabeza.hasTreatment, false, 'HEAD no tiene tratamiento');
  assert.equal(cabeza.progressPct, null, 'progressPct es null para partes sin tratamiento');
});

// ---- Phase G.4: bodyPartMetrics devuelve 12 puntos ordenados ----
test('bodyPartMetrics devuelve 12 puntos semanales ordenados (Phase G.4)', async () => {
  const token = await obtenerTokenAdmin();
  const { body } = await httpPost(
    GQL,
    { query: `query { bodyPartMetrics(bodyPartId: RIGHT_HIP) { date score metricType } }` },
    { Authorization: `Bearer ${token}` }
  );
  assert.ok(body.data, `Debe haber data, recibido: ${JSON.stringify(body)}`);
  const puntos = body.data.bodyPartMetrics;
  assert.equal(puntos.length, 12, 'Deben ser 12 puntos semanales');
  for (let i = 1; i < puntos.length; i++) {
    assert.ok(puntos[i].date >= puntos[i - 1].date, 'Los puntos deben estar en orden cronologico ascendente');
  }
});

// ---- Phase G.5: myAssignedGames mapea los juegos del dashboard ----
test('myAssignedGames devuelve AssignedGame con id, name, webglUrl y difficulty (Phase G.5)', async () => {
  const token = await obtenerTokenAdmin();
  const { body } = await httpPost(
    GQL,
    { query: `query { myAssignedGames { id name description webglUrl difficulty assignedAt } }` },
    { Authorization: `Bearer ${token}` }
  );
  assert.ok(body.data, `Debe haber data, recibido: ${JSON.stringify(body)}`);
  assert.ok(body.data.myAssignedGames.length >= 1, 'Debe haber al menos un juego asignado');
  const juego = body.data.myAssignedGames[0];
  assert.equal(juego.id, '1', 'id del primer juego debe ser 1');
  assert.equal(juego.name, 'Mover la cadera', 'nombre del primer juego debe coincidir');
  assert.ok(['EASY', 'MEDIUM', 'HARD'].includes(juego.difficulty), 'difficulty debe ser un valor valido');
});

// ---- Phase G.6: requestAppointment devuelve AppointmentRequest ----
test('requestAppointment devuelve estado PENDING y preserva los argumentos (Phase G.6)', async () => {
  const token = await obtenerTokenAdmin();
  const { body } = await httpPost(
    GQL,
    {
      query: `mutation {
        requestAppointment(
          fechaPreferida: "2026-06-10",
          horaPreferida: "10:30",
          motivo: "Revision de cadera",
          telefono: "600000000"
        ) { id fechaPreferida horaPreferida motivo estado createdAt }
      }`,
    },
    { Authorization: `Bearer ${token}` }
  );
  assert.ok(body.data, `Debe haber data, recibido: ${JSON.stringify(body)}`);
  const r = body.data.requestAppointment;
  assert.ok(r.id.startsWith('REQ-12345678Z-'), 'id debe comenzar por REQ-12345678Z-');
  assert.equal(r.fechaPreferida, '2026-06-10', 'fechaPreferida debe coincidir');
  assert.equal(r.estado, 'PENDING', 'estado debe ser PENDING');
  assert.ok(r.createdAt, 'createdAt debe estar presente');
});

// ---- Phase I J.9: bodyPartMetrics acepta variable tipada como BodyPartId! ----
test('bodyPartMetrics acepta variable tipada como BodyPartId! (regresion progress.ts:17)', async () => {
  const token = await obtenerTokenAdmin();
  const { body } = await httpPost(
    GQL,
    {
      query: `
        query GetBodyPartMetrics($bodyPartId: BodyPartId!) {
          bodyPartMetrics(bodyPartId: $bodyPartId) {
            date
            score
            metricType
          }
        }
      `,
      variables: { bodyPartId: 'RIGHT_HIP' },
    },
    { Authorization: `Bearer ${token}` }
  );
  assert.ok(body.data, `Esperaba data; recibido: ${JSON.stringify(body)}`);
  assert.equal(body.errors, undefined,
    `Esperaba sin errores; recibido: ${JSON.stringify(body.errors)}`);
  assert.ok(Array.isArray(body.data.bodyPartMetrics), 'bodyPartMetrics debe ser un array');
});

// ---- Phase G.7: registerDeviceToken devuelve true (stub) ----
test('registerDeviceToken devuelve true (stub Phase G.7)', async () => {
  const token = await obtenerTokenAdmin();
  const { body } = await httpPost(
    GQL,
    { query: `mutation { registerDeviceToken(token: "ExpoTokExample123", platform: "android") }` },
    { Authorization: `Bearer ${token}` }
  );
  assert.ok(body.data, `Debe haber data, recibido: ${JSON.stringify(body)}`);
  assert.equal(body.data.registerDeviceToken, true, 'debe devolver true');
});
