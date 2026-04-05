// Funciones de ayuda compartidas entre resolvers
'use strict';

const { crearError } = require('../../utils/errors');

/**
 * Verifica que el contexto tenga un usuario autenticado.
 * Lanza TOKEN_INVALID si no hay usuario en el contexto.
 *
 * @param {object} context - Contexto de Apollo Server
 * @returns {{ sub, tipo }} Payload del JWT del BFF
 */
function requireAuth(context) {
  if (!context.user) {
    throw crearError('TOKEN_INVALID');
  }
  return context.user;
}

module.exports = { requireAuth };
