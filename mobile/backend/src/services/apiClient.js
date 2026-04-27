// Cliente HTTP wrapper para la API central de Java
// Usa fetch nativo de Node.js 20 — prohibido usar axios o node-fetch
// Incluye: timeout, mapeo de errores HTTP a GraphQLError, modo mock para desarrollo
'use strict';

const { crearError } = require('../utils/errors');
const config = require('../config');
const logger = require('../logger');

const TIMEOUT_MS = 10000;

// ---- Datos mock del usuario de prueba: Admin RehabiAPP ----
// DNI 12345678Z — valido: 12345678 mod 23 = 14 -> letra Z
const MOCK_PACIENTE_ADMIN = {
  dniPac: '12345678Z',
  dniSan: '87654321B',
  nombrePac: 'Admin',
  apellido1Pac: 'RehabiAPP',
  apellido2Pac: null,
  edadPac: 36,
  emailPac: 'admin@rehabiapp.com',
  numSs: '280000000001',
  sexo: 'MASCULINO',
  fechaNacimiento: '1990-01-01',
  protesis: false,
  activo: true,
  consentimientoRgpd: true,
  telefonos: ['600000000'],
};

const MOCK_DISCAPACIDADES_ADMIN = [
  {
    dniPac: '12345678Z',
    codDis: 'M16',
    nombreDis: 'Coxartrosis',
    idNivel: 2,
    nombreNivel: 'Intermedio',
    fechaAsignacion: '2024-03-10T09:00:00',
    notas: 'Cadera derecha afectada principalmente',
  },
  {
    dniPac: '12345678Z',
    codDis: 'M54',
    nombreDis: 'Lumbalgia cronica',
    idNivel: 1,
    nombreNivel: 'Inicial',
    fechaAsignacion: '2024-06-01T10:30:00',
    notas: null,
  },
];

const MOCK_TRATAMIENTOS_ADMIN = [
  {
    dniPac: '12345678Z', codTrat: 'TRT001', nombreTrat: 'Movilizacion activa de cadera',
    descripcion: null, tipo: 'EXERCISE', visible: true,
    idNivel: 2, nivelNombre: 'Subagudo', codDis: 'M16',
    resumen: 'Ejercicios suaves de rango articular para recuperar movilidad en la cadera derecha.',
    materiales: ['Banda elastica ligera', 'Esterilla de yoga'],
    medicacion: ['Paracetamol 500mg si dolor agudo'],
    tieneDocumento: true, urlDocumento: null,
  },
  {
    dniPac: '12345678Z', codTrat: 'TRT002', nombreTrat: 'Electroterapia de baja frecuencia',
    descripcion: null, tipo: 'TEXT_INSTRUCTION', visible: true,
    idNivel: 2, nivelNombre: 'Subagudo', codDis: 'M16',
    resumen: 'Aplicacion de corriente TENS para aliviar el dolor cronico en la zona de la cadera.',
    materiales: ['Electrodos adhesivos', 'Aparato TENS (proporcionado en clinica)'],
    medicacion: [],
    tieneDocumento: false, urlDocumento: null,
  },
  {
    dniPac: '12345678Z', codTrat: 'TRT003', nombreTrat: 'Fortalecimiento lumbar',
    descripcion: null, tipo: 'EXERCISE', visible: true,
    idNivel: 1, nivelNombre: 'Inicial', codDis: 'M54',
    resumen: 'Serie de ejercicios isometricos para fortalecer la musculatura paravertebral.',
    materiales: ['Pelota de fitball', 'Esterilla'],
    medicacion: ['Ibuprofeno 400mg con comida si necesario'],
    tieneDocumento: true, urlDocumento: null,
  },
  {
    dniPac: '12345678Z', codTrat: 'TRT004', nombreTrat: 'Hidroterapia terapeutica',
    descripcion: null, tipo: 'EXERCISE', visible: false,
    idNivel: 1, nivelNombre: 'Inicial', codDis: 'M54',
    resumen: null, materiales: [], medicacion: [],
    tieneDocumento: false, urlDocumento: null,
  },
];

const MOCK_CITAS_ADMIN = [
  { dniPac: '12345678Z', dniSan: '87654321B', fechaCita: '2026-04-10', horaCita: '10:00:00' },
  { dniPac: '12345678Z', dniSan: '87654321B', fechaCita: '2026-04-17', horaCita: '11:30:00' },
  { dniPac: '12345678Z', dniSan: '87654321B', fechaCita: '2026-04-24', horaCita: '09:00:00' },
];

// Indice de pacientes mock por DNI
const MOCK_PACIENTES = {
  '12345678Z': MOCK_PACIENTE_ADMIN,
};
const MOCK_DISCAPACIDADES = {
  '12345678Z': MOCK_DISCAPACIDADES_ADMIN,
};
const MOCK_TRATAMIENTOS = {
  '12345678Z': MOCK_TRATAMIENTOS_ADMIN,
};
// Las citas se filtran por dniPac en appointmentService — aqui van todas juntas
const MOCK_TODAS_CITAS = [...MOCK_CITAS_ADMIN];

/**
 * Resuelve la respuesta mock para una peticion dada.
 * Soporta rutas dinamicas con DNI (ej: /api/pacientes/{dni}/discapacidades).
 *
 * @param {string} method
 * @param {string} path - Ruta sin query string
 * @returns {any} Datos mock o null
 */
function resolverMock(method, path) {
  // POST /api/auth/login y /api/auth/refresh -> siempre exito (la validacion de credenciales
  // se hace en authService antes de llegar aqui en modo mock)
  if (method === 'POST' && path === '/api/auth/login') {
    return { accessToken: 'mock-java-access-token', refreshToken: 'mock-java-refresh-token', rol: 'PATIENT' };
  }
  if (method === 'POST' && path === '/api/auth/refresh') {
    return { accessToken: 'mock-java-access-token-nuevo', refreshToken: 'mock-java-refresh-token-nuevo', rol: 'PATIENT' };
  }

  // GET /api/pacientes/{dni}
  const matchPerfil = path.match(/^\/api\/pacientes\/([^/]+)$/);
  if (matchPerfil && method === 'GET') {
    return MOCK_PACIENTES[matchPerfil[1]] || null;
  }

  // GET /api/pacientes/{dni}/discapacidades
  const matchDis = path.match(/^\/api\/pacientes\/([^/]+)\/discapacidades$/);
  if (matchDis && method === 'GET') {
    return MOCK_DISCAPACIDADES[matchDis[1]] || [];
  }

  // GET /api/pacientes/{dni}/tratamientos
  const matchTrat = path.match(/^\/api\/pacientes\/([^/]+)\/tratamientos$/);
  if (matchTrat && method === 'GET') {
    return MOCK_TRATAMIENTOS[matchTrat[1]] || [];
  }

  // GET /api/citas (por fecha — el servicio filtrara por dniPac en memoria)
  if (method === 'GET' && path.startsWith('/api/citas')) {
    return MOCK_TODAS_CITAS;
  }

  // POST /api/citas
  if (method === 'POST' && path === '/api/citas') {
    return MOCK_CITAS_ADMIN[0]; // Devolver primera cita como ejemplo de creacion
  }

  // DELETE /api/citas
  if (method === 'DELETE' && path === '/api/citas') {
    return null; // 204 simulado
  }

  // POST /api/pacientes/{dni}/device-tokens (registro de token push)
  const matchDeviceToken = path.match(/^\/api\/pacientes\/([^/]+)\/device-tokens$/);
  if (matchDeviceToken && method === 'POST') {
    return { ok: true }; // Mock — tabla token_dispositivo pendiente en el API Java
  }

  // DELETE /api/device-tokens (eliminacion de token push)
  if (method === 'DELETE' && path === '/api/device-tokens') {
    return null; // 204 simulado
  }

  // GET /api/pacientes/{dni}/tratamientos/{codTrat}/documento (documento PDF del tratamiento)
  const matchDocumento = path.match(/^\/api\/pacientes\/([^/]+)\/tratamientos\/([^/]+)\/documento$/);
  if (matchDocumento && method === 'GET') {
    // PDF minimo "Hola Mundo" — suficiente para probar el flujo de descarga
    const MOCK_PDF_BASE64 = Buffer.from(
      '%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n' +
      '2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n' +
      '3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]>>endobj\n' +
      'xref\n0 4\n0000000000 65535 f\n0000000009 00000 n\n' +
      '0000000058 00000 n\n0000000115 00000 n\n' +
      'trailer<</Size 4/Root 1 0 R>>\nstartxref\n190\n%%EOF'
    ).toString('base64');
    return {
      fileName: `tratamiento-${matchDocumento[2]}.pdf`,
      mimeType: 'application/pdf',
      base64: MOCK_PDF_BASE64,
      url: null,
    };
  }

  // GET /api/pacientes/{dni}/juegos (juegos terapeuticos asignados)
  const matchJuegos = path.match(/^\/api\/pacientes\/([^/]+)\/juegos$/);
  if (matchJuegos && method === 'GET') {
    return [
      { idJuego: 'j1', nombreJuego: 'Alcanza la estrella', descripcion: 'Ejercicio de alcance del brazo derecho.', urlThumbnail: null, urlWebgl: 'https://games.rehabiapp.com/star-reach', dificultad: 'EASY', fechaAsignacion: '2026-04-10' },
      { idJuego: 'j2', nombreJuego: 'Ritmo de pasos', descripcion: 'Coordinacion de piernas siguiendo el compas.', urlThumbnail: null, urlWebgl: 'https://games.rehabiapp.com/step-rhythm', dificultad: 'MEDIUM', fechaAsignacion: '2026-04-12' },
    ];
  }

  // POST /api/pacientes/{dni}/solicitudes-cita (solicitud de cita del paciente)
  const matchSolicitud = path.match(/^\/api\/pacientes\/([^/]+)\/solicitudes-cita$/);
  if (matchSolicitud && method === 'POST') {
    return {
      id: `req-${Date.now()}`,
      fechaPreferida: null,
      horaPreferida: null,
      motivo: null,
      estado: 'PENDING',
      createdAt: new Date().toISOString(),
    };
  }

  // GET /api/pacientes/{dni}/foto (foto de perfil como base64)
  const matchFoto = path.match(/^\/api\/pacientes\/([^/]+)\/foto$/);
  if (matchFoto && method === 'GET') {
    // PNG 8x8 solido azul primario (#2563EB) — suficiente para probar el flujo de avatar
    return {
      base64: 'iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAIAAABLbSncAAAAJElEQVQI12P4z8BQz0AEYBjVMKphVMOohlENoxpGNYxqIBkAALsAAQFaxqIAAAAASUVORK5CYII=',
      mimeType: 'image/png',
    };
  }

  // GET /api/pacientes/{dni}/progreso/partes-cuerpo
  const matchProgreso = path.match(/^\/api\/pacientes\/([^/]+)\/progreso\/partes-cuerpo$/);
  if (matchProgreso && method === 'GET') {
    return [
      { id: 'HEAD', name: 'Cabeza', hasTreatment: false, progressPct: 0, improvementPct: 0, periodLabel: '4 semanas' },
      { id: 'NECK', name: 'Cuello', hasTreatment: false, progressPct: 0, improvementPct: 0, periodLabel: '4 semanas' },
      { id: 'TORSO', name: 'Torso', hasTreatment: false, progressPct: 0, improvementPct: 0, periodLabel: '4 semanas' },
      { id: 'LEFT_SHOULDER', name: 'Hombro izquierdo', hasTreatment: false, progressPct: 0, improvementPct: 0, periodLabel: '4 semanas' },
      { id: 'RIGHT_SHOULDER', name: 'Hombro derecho', hasTreatment: true, progressPct: 62, improvementPct: 18, periodLabel: '4 semanas' },
      { id: 'LEFT_ARM', name: 'Brazo izquierdo', hasTreatment: false, progressPct: 0, improvementPct: 0, periodLabel: '4 semanas' },
      { id: 'RIGHT_ARM', name: 'Brazo derecho', hasTreatment: true, progressPct: 75, improvementPct: 25, periodLabel: '4 semanas' },
      { id: 'LEFT_HAND', name: 'Mano izquierda', hasTreatment: false, progressPct: 0, improvementPct: 0, periodLabel: '4 semanas' },
      { id: 'RIGHT_HAND', name: 'Mano derecha', hasTreatment: false, progressPct: 0, improvementPct: 0, periodLabel: '4 semanas' },
      { id: 'LEFT_HIP', name: 'Cadera izquierda', hasTreatment: false, progressPct: 0, improvementPct: 0, periodLabel: '4 semanas' },
      { id: 'RIGHT_HIP', name: 'Cadera derecha', hasTreatment: false, progressPct: 0, improvementPct: 0, periodLabel: '4 semanas' },
      { id: 'LEFT_LEG', name: 'Pierna izquierda', hasTreatment: false, progressPct: 0, improvementPct: 0, periodLabel: '4 semanas' },
      { id: 'RIGHT_LEG', name: 'Pierna derecha', hasTreatment: true, progressPct: 45, improvementPct: 10, periodLabel: '4 semanas' },
      { id: 'LEFT_FOOT', name: 'Pie izquierdo', hasTreatment: false, progressPct: 0, improvementPct: 0, periodLabel: '4 semanas' },
      { id: 'RIGHT_FOOT', name: 'Pie derecho', hasTreatment: false, progressPct: 0, improvementPct: 0, periodLabel: '4 semanas' },
    ];
  }

  // GET /api/pacientes/{dni}/progreso/partes-cuerpo/{bodyPartId}/metricas
  const matchMetricas = path.match(/^\/api\/pacientes\/([^/]+)\/progreso\/partes-cuerpo\/([^/]+)\/metricas/);
  if (matchMetricas && method === 'GET') {
    // 8 puntos de datos semanales ascendentes — simula mejora progresiva
    const hoy = new Date();
    return Array.from({ length: 8 }, function (_, i) {
      const fecha = new Date(hoy);
      fecha.setDate(hoy.getDate() - (7 - i) * 7);
      return {
        date: fecha.toISOString().split('T')[0],
        score: 40 + Math.round((38 / 7) * i),
        metricType: 'ACCURACY',
      };
    });
  }

  return null;
}

/**
 * Mapea el codigo HTTP de la API de Java al codigo de error del BFF.
 * Distingue entre endpoints de autenticacion y endpoints de datos
 * para mapear correctamente el HTTP 401.
 *
 * @param {number} status - Codigo HTTP de la respuesta
 * @param {string} path - Ruta del endpoint que genero el error
 * @returns {string} codigo de ERROR_CODES
 */
function mapearErrorHttp(status, path) {
  // Para endpoints de autenticacion, un 401 significa credenciales invalidas
  const esEndpointAuth = path && (
    path.startsWith('/api/auth/login') ||
    path.startsWith('/api/auth/register')
  );

  if (status === 401) {
    return esEndpointAuth ? 'WRONG_PASSWORD' : 'TOKEN_INVALID';
  }

  if (status === 404) {
    // En endpoints de autenticacion, un 404 significa que el usuario no existe
    return esEndpointAuth ? 'INVALID_IDENTIFIER' : 'PATIENT_NOT_FOUND';
  }

  const mapa = {
    400: 'VALIDATION_ERROR',
    403: 'ACCOUNT_DEACTIVATED',
    409: 'APPOINTMENT_CONFLICT',
  };
  return mapa[status] || 'INTERNAL_ERROR';
}

/**
 * Ejecuta una peticion HTTP con timeout usando fetch nativo de Node.js 20.
 *
 * @param {string} method - Metodo HTTP
 * @param {string} path - Ruta relativa (ej: '/api/pacientes/123')
 * @param {object|null} body - Cuerpo JSON para POST/PUT
 * @param {string|null} javaToken - JWT de la API de Java para autenticacion interna
 * @param {object|null} params - Query params para DELETE
 * @returns {Promise<object|null>} Respuesta JSON o null para 204
 */
async function peticion(method, path, body, javaToken, params) {
  // Modo mock: devolver datos dinamicos sin peticion real
  if (config.mockApi) {
    const sinParams = path.split('?')[0];
    const resultado = resolverMock(method, sinParams);
    logger.debug({ method, path, mock: true }, 'Peticion mock a la API de Java');
    return resultado;
  }

  // Construir URL con query params
  let url = `${config.apiBaseUrl}${path}`;
  if (params) {
    const qs = new URLSearchParams(params).toString();
    url = `${url}?${qs}`;
  }

  // Headers de la peticion
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };
  if (javaToken) {
    headers['Authorization'] = `Bearer ${javaToken}`;
  }

  // AbortController para timeout
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);

  // Funcion auxiliar para sanitizar cuerpos de peticion en los logs
  // Elimina contrasenas y tokens para no exponerlos en los registros
  function sanitizarBodyParaLog(cuerpo) {
    if (!cuerpo) return undefined;
    const copia = { ...cuerpo };
    if (copia.contrasena) copia.contrasena = '***';
    if (copia.password) copia.password = '***';
    if (copia.refreshToken) copia.refreshToken = '***TOKEN***';
    return copia;
  }

  const inicioMs = Date.now();

  try {
    logger.debug(
      { method, url, body: sanitizarBodyParaLog(body) },
      'Peticion saliente a la API de Java'
    );

    const res = await fetch(url, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });

    clearTimeout(timer);
    const tiempoMs = Date.now() - inicioMs;
    const contentType = res.headers.get('content-type') || 'desconocido';

    // 204 No Content — exito sin cuerpo
    if (res.status === 204) {
      logger.info(
        { method, url, status: 204, tiempoMs, contentType },
        'Respuesta OK (204) de la API de Java'
      );
      return null;
    }

    // Intentar parsear JSON de forma segura
    // Si la API devuelve HTML (ej: pagina de error 502 de nginx), res.json() falla
    let data;
    try {
      data = await res.json();
    } catch (parseErr) {
      const textoRespuesta = await res.text().catch(() => '(no se pudo leer el cuerpo)');
      logger.error(
        {
          method,
          url,
          status: res.status,
          contentType,
          tiempoMs,
          errorParseo: parseErr.message,
          cuerpoTruncado: textoRespuesta.substring(0, 500),
        },
        'Respuesta de la API de Java no es JSON valido'
      );
      throw crearError(
        'NETWORK_ERROR',
        `La API devolvio una respuesta no valida (status ${res.status}, tipo ${contentType}).`
      );
    }

    if (!res.ok) {
      logger.error(
        { method, url, status: res.status, contentType, tiempoMs, data },
        'Error HTTP de la API de Java'
      );
      throw crearError(mapearErrorHttp(res.status, path));
    }

    logger.info(
      { method, url, status: res.status, tiempoMs, contentType },
      'Respuesta OK de la API de Java'
    );
    return data;

  } catch (err) {
    clearTimeout(timer);
    const tiempoMs = Date.now() - inicioMs;

    // El error ya es un GraphQLError construido por nosotros — propagarlo tal cual
    if (err.extensions && err.extensions.code) {
      throw err;
    }

    // Timeout (AbortController)
    if (err.name === 'AbortError') {
      logger.error(
        { method, url, tiempoMs, timeoutMs: TIMEOUT_MS },
        'Timeout en peticion a la API de Java'
      );
      throw crearError('NETWORK_ERROR', 'La API no respondio en el tiempo esperado.');
    }

    // ECONNREFUSED, ENOTFOUND u otros errores de conexion de bajo nivel
    logger.error(
      { method, url, tiempoMs, errorNombre: err.name, errorMensaje: err.message },
      'Error de conexion con la API de Java'
    );
    throw crearError('NETWORK_ERROR');
  }
}

const apiClient = {
  get: (path, javaToken) => peticion('GET', path, null, javaToken, null),
  post: (path, body, javaToken) => peticion('POST', path, body, javaToken, null),
  put: (path, body, javaToken) => peticion('PUT', path, body, javaToken, null),
  delete: (path, params, javaToken) => peticion('DELETE', path, null, javaToken, params),
};

module.exports = apiClient;
