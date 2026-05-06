// Servicio de autenticacion del BFF
// Gestiona los JWT propios del BFF (diferentes de los JWT de la API de Java)
// Cache en memoria de tokens Java para llamadas internas (Map<dniPac, javaTokens>)
'use strict';

const jwt = require('jsonwebtoken');
const config = require('../config');
const apiClient = require('./apiClient');
const { crearError } = require('../utils/errors');
const logger = require('../logger');

// Cache en memoria: dniPac -> { accessToken, refreshToken }
// En produccion con multiples pods K8s, migrar a Redis
const tokenCache = new Map();

// ---- Credenciales mock para desarrollo local ----
// Permite usar admin/admin@rehabiapp.com/12345678Z con password "admin"
const MOCK_CREDENCIALES = {
  admin:                { dniPac: '12345678Z', password: 'admin' },
  'admin@rehabiapp.com': { dniPac: '12345678Z', password: 'admin' },
  '12345678Z':           { dniPac: '12345678Z', password: 'admin' },
};

/**
 * Resuelve el DNI del paciente a partir de un identificador mock.
 * Solo activo cuando config.mockApi === true.
 * Distingue entre identificador no encontrado y contrasena incorrecta.
 *
 * @param {string} identifier
 * @param {string} password
 * @returns {string} dniPac
 * @throws GraphQLError si el identificador no existe o la contrasena no coincide
 */
function resolverMockCredenciales(identifier, password) {
  const entrada = MOCK_CREDENCIALES[identifier];

  // El DNI o correo no corresponde a ningun paciente registrado
  if (!entrada) {
    logger.warn(
      { identifier: identifier.substring(0, 3) + '***' },
      'Intento de login con identificador no registrado'
    );
    throw crearError('INVALID_IDENTIFIER');
  }

  // El identificador existe pero la contrasena no coincide
  if (entrada.password !== password) {
    logger.warn(
      { dniPac: entrada.dniPac.substring(0, 3) + '***' },
      'Intento de login con contrasena incorrecta'
    );
    throw crearError('WRONG_PASSWORD');
  }

  return entrada.dniPac;
}

/**
 * Autentica al paciente contra la API de Java y genera un par de tokens JWT del BFF.
 *
 * @param {string} identifier - DNI o email del paciente
 * @param {string} password - Contrasena en texto plano
 * @returns {{ accessToken, refreshToken, expiresAt }}
 */
async function login(identifier, password) {
  // En modo mock: validar credenciales localmente y resolver al DNI canonico
  if (config.mockApi) {
    const dniPac = resolverMockCredenciales(identifier, password);
    // Simular tokens de Java en cache (el apiClient mock devuelve tokens ficticios)
    const javaTokens = await apiClient.post('/api/auth/login-paciente', { identifier, contrasena: password });
    tokenCache.set(dniPac, {
      accessToken: javaTokens.accessToken,
      refreshToken: javaTokens.refreshToken,
    });
    return generarParBff(dniPac);
  }

  // Modo real: autenticar contra POST /api/auth/login-paciente del API Java.
  // Este endpoint acepta DNI o email en el campo `identifier` y devuelve
  // un JWT con rol=PATIENT (implementado en /api Phase 12.5).
  let javaTokens;
  try {
    javaTokens = await apiClient.post('/api/auth/login-paciente', {
      identifier,
      contrasena: password,
    });
  } catch (err) {
    // Si el error ya es un GraphQLError del BFF con codigo conocido, propagarlo tal cual.
    // Esto preserva NETWORK_ERROR, INTERNAL_ERROR, INVALID_CREDENTIALS, ACCOUNT_DEACTIVATED, etc.
    // El apiClient ya mapea los codigos HTTP a codigos del BFF correctamente.
    if (err.extensions && err.extensions.code) {
      logger.warn(
        { code: err.extensions.code, identifier: identifier.substring(0, 3) + '***' },
        'Error en login propagado desde apiClient'
      );
      throw err;
    }

    // Error inesperado sin estructura del BFF — loguear y lanzar INTERNAL_ERROR
    logger.error(
      { errorNombre: err.name, errorMensaje: err.message },
      'Error inesperado en authService.login()'
    );
    throw crearError('INTERNAL_ERROR');
  }

  if (!javaTokens || !javaTokens.accessToken) {
    throw crearError('INVALID_CREDENTIALS');
  }

  // Resolver el DNI canonico del paciente.
  // Si el identifier ya es un DNI, se usa directamente.
  // Si es un email, extraer el DNI del token JWT de Java (campo sub o dniPac).
  let dniPac = identifier;
  if (javaTokens.dniPac) {
    // La API de Java devuelve el DNI en la respuesta de login
    dniPac = javaTokens.dniPac;
  } else {
    // Intentar decodificar el JWT de Java para extraer el DNI del subject
    try {
      const payloadJava = jwt.decode(javaTokens.accessToken);
      if (payloadJava && payloadJava.sub) {
        dniPac = payloadJava.sub;
      }
    } catch {
      // Si falla la decodificacion, usar el identifier original como fallback
      logger.warn(
        { identifier: identifier.substring(0, 3) + '***' },
        'No se pudo extraer DNI del JWT de Java, usando identifier como clave de cache'
      );
    }
  }

  // Guardar tokens de Java en cache usando SIEMPRE el DNI como clave.
  // Esto garantiza que refresh() (que busca por dniPac del JWT BFF) encuentre los tokens.
  tokenCache.set(dniPac, {
    accessToken: javaTokens.accessToken,
    refreshToken: javaTokens.refreshToken,
  });

  logger.debug(
    { dniPac: dniPac.substring(0, 3) + '***', cacheSize: tokenCache.size },
    'Tokens de Java almacenados en cache'
  );

  return generarParBff(dniPac);
}

/**
 * Renueva el par de tokens BFF a partir de un refresh token valido.
 *
 * @param {string} refreshToken - Refresh token del BFF
 * @returns {{ accessToken, refreshToken, expiresAt }}
 */
async function refresh(refreshToken) {
  let payload;
  try {
    payload = jwt.verify(refreshToken, config.jwtSecret);
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      throw crearError('TOKEN_EXPIRED');
    }
    throw crearError('TOKEN_INVALID');
  }

  if (payload.tipo !== 'refresh') {
    throw crearError('TOKEN_INVALID');
  }

  const dniPac = payload.sub;
  const tokensJava = tokenCache.get(dniPac);

  // Si tenemos tokens de Java en cache, renovarlos tambien
  if (tokensJava && tokensJava.refreshToken) {
    try {
      // Renovar usando el endpoint especifico de pacientes — /api/auth/refresh
      // solo maneja sanitarios (H.3, /api Phase 13).
      const nuevosJava = await apiClient.post('/api/auth/refresh-paciente', {
        refreshToken: tokensJava.refreshToken,
      });
      if (nuevosJava && nuevosJava.accessToken) {
        tokenCache.set(dniPac, {
          accessToken: nuevosJava.accessToken,
          refreshToken: nuevosJava.refreshToken,
        });
      }
    } catch {
      // Si falla la renovacion Java, continuar con el cache existente
      // El paciente seguira autenticado en el BFF hasta que el token Java expire
    }
  }

  return generarParBff(dniPac);
}

/**
 * Valida un access token del BFF y retorna el payload.
 *
 * @param {string} token - Access token del BFF
 * @returns {{ sub, tipo }}
 */
function validarToken(token) {
  try {
    const payload = jwt.verify(token, config.jwtSecret);
    return payload;
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      throw crearError('TOKEN_EXPIRED');
    }
    throw crearError('TOKEN_INVALID');
  }
}

/**
 * Obtiene el token de Java en cache para un paciente.
 * Usado internamente por los servicios de dominio para llamadas a la API de Java.
 *
 * @param {string} dniPac
 * @returns {string|null} JWT de la API de Java o null si no esta en cache
 */
function obtenerTokenJava(dniPac) {
  const tokens = tokenCache.get(dniPac);
  return tokens ? tokens.accessToken : null;
}

/**
 * Genera un par de tokens JWT del BFF (access + refresh).
 *
 * @param {string} dniPac
 * @returns {{ accessToken, refreshToken, expiresAt }}
 */
function generarParBff(dniPac) {
  const ahora = Math.floor(Date.now() / 1000);
  const expAccess = ahora + Math.floor(config.jwtExpirationMs / 1000);

  const accessToken = jwt.sign(
    { sub: dniPac, tipo: 'access' },
    config.jwtSecret,
    { expiresIn: Math.floor(config.jwtExpirationMs / 1000) }
  );

  const refreshToken = jwt.sign(
    { sub: dniPac, tipo: 'refresh' },
    config.jwtSecret,
    { expiresIn: Math.floor(config.jwtRefreshMs / 1000) }
  );

  return {
    accessToken,
    refreshToken,
    expiresAt: expAccess,
  };
}

/**
 * Genera un JWT efimero corto (5 min) para lanzar un videojuego Unity.
 * Scope GAMES_PLAY — restringe el uso a la sesion de juego.
 * Firmado con la misma clave del BFF para evitar gestion de claves adicional.
 *
 * @param {string} dniPac
 * @param {string|number} idVideojuego
 * @returns {{ token: string, expiresAt: number }} expiresAt en epoch seconds
 */
function generarTokenEfimeroJuego(dniPac, idVideojuego) {
  const TTL_SEGUNDOS = 5 * 60; // 5 minutos
  const ahora = Math.floor(Date.now() / 1000);
  const expiresAt = ahora + TTL_SEGUNDOS;

  const token = jwt.sign(
    {
      sub: dniPac,
      tipo: 'access',
      scope: 'GAMES_PLAY',
      idVideojuego: String(idVideojuego),
    },
    config.jwtSecret,
    { expiresIn: TTL_SEGUNDOS }
  );

  return { token, expiresAt };
}

module.exports = {
  login,
  refresh,
  validarToken,
  obtenerTokenJava,
  generarTokenEfimeroJuego,
};
