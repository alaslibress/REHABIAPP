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
  { dniPac: '12345678Z', codTrat: 'TRT001', nombreTrat: 'Ejercicios de movilidad de cadera', visible: true, fechaAsignacion: '2024-03-15T09:00:00' },
  { dniPac: '12345678Z', codTrat: 'TRT002', nombreTrat: 'Electroterapia de baja frecuencia', visible: true, fechaAsignacion: '2024-03-15T09:00:00' },
  { dniPac: '12345678Z', codTrat: 'TRT003', nombreTrat: 'Ejercicios de fortalecimiento lumbar', visible: true, fechaAsignacion: '2024-06-05T11:00:00' },
  { dniPac: '12345678Z', codTrat: 'TRT004', nombreTrat: 'Hidroterapia terapeutica', visible: false, fechaAsignacion: '2024-06-05T11:00:00' },
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
