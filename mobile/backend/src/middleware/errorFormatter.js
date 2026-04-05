// Formateador global de errores para Apollo Server
// Garantiza que todos los errores que llegan al cliente movil tienen la estructura estandarizada
// Nunca expone stack traces ni mensajes internos en produccion
'use strict';

const { GraphQLError } = require('graphql');
const { crearError } = require('../utils/errors');
const config = require('../config');
const logger = require('../logger');

/**
 * Funcion formatError de Apollo Server.
 * Se ejecuta antes de enviar cada error al cliente.
 *
 * @param {object} formattedError - Error ya formateado por Apollo
 * @param {unknown} error - Error original
 * @returns {object} Error formateado con estructura garantizada
 */
function errorFormatter(formattedError, error) {
  const originalError = error instanceof GraphQLError ? error.originalError : error;

  // Si el error ya tiene un codigo conocido del BFF, dejarlo pasar tal cual
  if (
    formattedError.extensions &&
    formattedError.extensions.code &&
    formattedError.extensions.titulo
  ) {
    // Registrar errores de negocio en nivel warn (esperados, no bugs)
    logger.warn(
      {
        code: formattedError.extensions.code,
        mensaje: formattedError.message,
        path: formattedError.path,
      },
      'Error GraphQL del BFF enviado al cliente'
    );

    // En produccion, eliminar stacktrace por seguridad
    if (config.nodeEnv === 'production') {
      const { stacktrace: _s, ...extensionsSinStack } = formattedError.extensions;
      return { ...formattedError, extensions: extensionsSinStack };
    }
    return formattedError;
  }

  // Error de validacion de GraphQL (campo no existe, tipo incorrecto, etc.)
  if (
    formattedError.extensions &&
    formattedError.extensions.code === 'GRAPHQL_VALIDATION_FAILED'
  ) {
    logger.warn(
      { mensaje: formattedError.message, path: formattedError.path },
      'Error de validacion GraphQL'
    );
    const err = crearError('VALIDATION_ERROR');
    return {
      message: err.message,
      extensions: err.extensions,
    };
  }

  // Cualquier otro error no controlado -> INTERNAL_ERROR (nivel error: inesperado)
  // Registrar la mayor cantidad de contexto posible para facilitar el diagnostico
  logger.error(
    {
      mensaje: formattedError.message,
      path: formattedError.path,
      codigoOriginal: formattedError.extensions?.code || 'sin_codigo',
      extensiones: formattedError.extensions || {},
      errorOriginalNombre: originalError?.name || 'desconocido',
      errorOriginalMensaje: originalError?.message || 'sin_mensaje',
      stack: originalError?.stack,
    },
    'Error interno no controlado en el BFF'
  );

  const err = crearError('INTERNAL_ERROR');

  const resultado = {
    message: err.message,
    extensions: { ...err.extensions },
  };

  // Solo en desarrollo: incluir detalles de depuracion para el desarrollador
  if (config.nodeEnv !== 'production') {
    if (originalError) {
      resultado.extensions.causa = {
        nombre: originalError.name,
        mensaje: originalError.message,
        codigoOriginal: formattedError.extensions?.code || null,
      };
      resultado.extensions.stacktrace = originalError.stack;
    }
  }

  return resultado;
}

module.exports = errorFormatter;
