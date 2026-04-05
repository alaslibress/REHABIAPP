// Middleware de autenticacion JWT para Apollo Server
// Extrae y valida el JWT del BFF desde la cabecera Authorization de cada peticion
'use strict';

const authService = require('../services/authService');

/**
 * Extrae el usuario del JWT del BFF desde la cabecera Authorization.
 * Si el token es invalido o no existe, user queda como null (sin lanzar error).
 * Los resolvers protegidos son los responsables de rechazar peticiones sin usuario.
 *
 * @param {object} req - Objeto Request de Express
 * @returns {{ user: object|null, javaToken: string|null }}
 */
async function authMiddleware(req) {
  const authHeader = req.headers['authorization'];

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return { user: null, javaToken: null };
  }

  const token = authHeader.slice(7); // Eliminar prefijo 'Bearer '

  try {
    const payload = authService.validarToken(token);

    // Solo los access tokens son validos para peticiones a resolvers
    if (payload.tipo !== 'access') {
      return { user: null, javaToken: null };
    }

    const javaToken = authService.obtenerTokenJava(payload.sub);

    return { user: payload, javaToken };
  } catch {
    // Token invalido o expirado — dejar que los resolvers manejen el error
    return { user: null, javaToken: null };
  }
}

module.exports = authMiddleware;
