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
  NO_GAMES_ASSIGNED: 'NO_GAMES_ASSIGNED',
  NO_TREATMENTS_ASSIGNED: 'NO_TREATMENTS_ASSIGNED',
  DOCUMENT_DOWNLOAD_FAILED: 'DOCUMENT_DOWNLOAD_FAILED',
  APPOINTMENT_REQUEST_INVALID_CONTACT: 'APPOINTMENT_REQUEST_INVALID_CONTACT',
  NOTIFICATION_PERMISSION_DENIED: 'NOTIFICATION_PERMISSION_DENIED',
  BODY_PART_NO_DATA: 'BODY_PART_NO_DATA',
  APPOINTMENT_REQUEST_CONFLICT: 'APPOINTMENT_REQUEST_CONFLICT',
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
  NO_GAMES_ASSIGNED: {
    subtitulo: 'Sin juegos',
    texto: 'No tienes juegos asignados.',
  },
  NO_TREATMENTS_ASSIGNED: {
    subtitulo: 'Sin tratamientos',
    texto: 'No tienes tratamientos asignados todavia.',
  },
  DOCUMENT_DOWNLOAD_FAILED: {
    subtitulo: 'Descarga fallida',
    texto: 'No se pudo descargar el documento. Intentalo de nuevo.',
  },
  APPOINTMENT_REQUEST_INVALID_CONTACT: {
    subtitulo: 'Contacto invalido',
    texto: 'Debes indicar un telefono o email valido para solicitar una cita.',
  },
  NOTIFICATION_PERMISSION_DENIED: {
    subtitulo: 'Permiso denegado',
    texto: 'Activa las notificaciones desde los ajustes del sistema.',
  },
  BODY_PART_NO_DATA: {
    subtitulo: 'Sin datos',
    texto: 'Aun no hay metricas para esta zona. Juega para ver tu progreso.',
  },
  APPOINTMENT_REQUEST_CONFLICT: {
    subtitulo: 'Horario ocupado',
    texto: 'Ya hay una cita para esa fecha y hora. Por favor, elige otro horario.',
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
