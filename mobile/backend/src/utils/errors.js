// Catalogo centralizado de codigos de error del BFF
// Todos los errores que llegan al cliente movil tienen esta estructura garantizada
'use strict';

const { GraphQLError } = require('graphql');

// Codigos de error que el frontend React Native reconoce (types/errors.ts)
const ERROR_CODES = {
  INVALID_CREDENTIALS: 'INVALID_CREDENTIALS',
  INVALID_IDENTIFIER: 'INVALID_IDENTIFIER',
  WRONG_PASSWORD: 'WRONG_PASSWORD',
  ACCOUNT_DEACTIVATED: 'ACCOUNT_DEACTIVATED',
  TOKEN_EXPIRED: 'TOKEN_EXPIRED',
  TOKEN_INVALID: 'TOKEN_INVALID',
  NETWORK_ERROR: 'NETWORK_ERROR',
  PATIENT_NOT_FOUND: 'PATIENT_NOT_FOUND',
  APPOINTMENT_CONFLICT: 'APPOINTMENT_CONFLICT',
  APPOINTMENT_NOT_FOUND: 'APPOINTMENT_NOT_FOUND',
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  INTERNAL_ERROR: 'INTERNAL_ERROR',
};

// Mapa de codigos a mensajes legibles en castellano
const MENSAJES = {
  INVALID_CREDENTIALS: {
    subtitulo: 'Credenciales invalidas',
    texto: 'El DNI/correo o la contrasena introducidos no son correctos. Por favor, intentelo de nuevo.',
  },
  INVALID_IDENTIFIER: {
    subtitulo: 'Usuario no encontrado',
    texto: 'El DNI o correo introducido no corresponde a ningun paciente registrado. Por favor, verifique sus datos.',
  },
  WRONG_PASSWORD: {
    subtitulo: 'Contrasena incorrecta',
    texto: 'La contrasena introducida no es correcta. Por favor, intentelo de nuevo.',
  },
  ACCOUNT_DEACTIVATED: {
    subtitulo: 'Cuenta desactivada',
    texto: 'Su cuenta ha sido desactivada. Contacte con su centro de rehabilitacion para mas informacion.',
  },
  TOKEN_EXPIRED: {
    subtitulo: 'Sesion expirada',
    texto: 'Su sesion ha expirado. Por favor, inicie sesion de nuevo.',
  },
  TOKEN_INVALID: {
    subtitulo: 'Token invalido',
    texto: 'Se ha producido un error de autenticacion. Por favor, inicie sesion de nuevo.',
  },
  NETWORK_ERROR: {
    subtitulo: 'Error de conexion',
    texto: 'No se ha podido conectar con el servidor. Compruebe su conexion a internet e intentelo de nuevo.',
  },
  PATIENT_NOT_FOUND: {
    subtitulo: 'Paciente no encontrado',
    texto: 'No se ha encontrado el perfil del paciente. Contacte con su centro de rehabilitacion.',
  },
  APPOINTMENT_CONFLICT: {
    subtitulo: 'Conflicto de cita',
    texto: 'El horario seleccionado ya esta ocupado. Por favor, elija otro horario.',
  },
  APPOINTMENT_NOT_FOUND: {
    subtitulo: 'Cita no encontrada',
    texto: 'La cita solicitada no existe o ha sido eliminada.',
  },
  VALIDATION_ERROR: {
    subtitulo: 'Datos invalidos',
    texto: 'Los datos introducidos no son validos. Por favor, revise los campos e intentelo de nuevo.',
  },
  INTERNAL_ERROR: {
    subtitulo: 'Error interno',
    texto: 'Se ha producido un error inesperado. Por favor, intentelo de nuevo mas tarde.',
  },
};

/**
 * Construye un GraphQLError estandarizado con la estructura que espera el frontend.
 * Garantiza que extensions siempre incluye: code, titulo, subtitulo, texto.
 *
 * @param {string} code - Codigo del ERROR_CODES
 * @param {string} [mensajePersonalizado] - Mensaje opcional para sobreescribir el texto predeterminado
 * @returns {GraphQLError}
 */
function crearError(code, mensajePersonalizado) {
  const info = MENSAJES[code] || MENSAJES.INTERNAL_ERROR;
  const codigoFinal = MENSAJES[code] ? code : 'INTERNAL_ERROR';

  return new GraphQLError(info.subtitulo, {
    extensions: {
      code: codigoFinal,
      titulo: 'Error',
      subtitulo: info.subtitulo,
      texto: mensajePersonalizado || info.texto,
    },
  });
}

module.exports = { ERROR_CODES, crearError };
