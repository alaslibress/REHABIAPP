import type { AppError, ErrorCode } from '../types/errors';

// Mapa de codigos de error a mensajes legibles en castellano
const ERROR_MESSAGES: Record<ErrorCode, { subtitle: string; message: string }> = {
  INVALID_CREDENTIALS: {
    subtitle: 'Credenciales invalidas',
    message: 'El DNI/correo o la contrasena introducidos no son correctos. Por favor, intentelo de nuevo.',
  },
  ACCOUNT_DEACTIVATED: {
    subtitle: 'Cuenta desactivada',
    message: 'Su cuenta ha sido desactivada. Contacte con su centro de rehabilitacion para mas informacion.',
  },
  TOKEN_EXPIRED: {
    subtitle: 'Sesion expirada',
    message: 'Su sesion ha expirado. Por favor, inicie sesion de nuevo.',
  },
  TOKEN_INVALID: {
    subtitle: 'Token invalido',
    message: 'Se ha producido un error de autenticacion. Por favor, inicie sesion de nuevo.',
  },
  NETWORK_ERROR: {
    subtitle: 'Error de conexion',
    message: 'No se ha podido conectar con el servidor. Compruebe su conexion a internet e intentelo de nuevo.',
  },
  PATIENT_NOT_FOUND: {
    subtitle: 'Paciente no encontrado',
    message: 'No se ha encontrado el perfil del paciente. Contacte con su centro de rehabilitacion.',
  },
  APPOINTMENT_CONFLICT: {
    subtitle: 'Conflicto de cita',
    message: 'El horario seleccionado ya esta ocupado. Por favor, elija otro horario.',
  },
  APPOINTMENT_NOT_FOUND: {
    subtitle: 'Cita no encontrada',
    message: 'La cita solicitada no existe o ha sido eliminada.',
  },
  VALIDATION_ERROR: {
    subtitle: 'Datos invalidos',
    message: 'Los datos introducidos no son validos. Por favor, revise los campos e intentelo de nuevo.',
  },
  INTERNAL_ERROR: {
    subtitle: 'Error interno',
    message: 'Se ha producido un error inesperado. Por favor, intentelo de nuevo mas tarde.',
  },
};

// Convierte un error GraphQL en un AppError estructurado
export function parseGraphQLError(error: unknown): AppError {
  // Intentar extraer el codigo del error GraphQL
  if (error && typeof error === 'object' && 'graphQLErrors' in error) {
    const gqlErrors = (error as any).graphQLErrors;
    if (Array.isArray(gqlErrors) && gqlErrors.length > 0) {
      const code = gqlErrors[0]?.extensions?.code as ErrorCode;
      const mapped = ERROR_MESSAGES[code];
      if (mapped) {
        return {
          title: 'Error',
          subtitle: mapped.subtitle,
          message: mapped.message,
          code: code,
        };
      }
    }
  }

  // Error de red
  if (error && typeof error === 'object' && 'networkError' in error) {
    return {
      title: 'Error',
      subtitle: ERROR_MESSAGES.NETWORK_ERROR.subtitle,
      message: ERROR_MESSAGES.NETWORK_ERROR.message,
      code: 'NETWORK_ERROR',
    };
  }

  // Error generico
  return {
    title: 'Error',
    subtitle: ERROR_MESSAGES.INTERNAL_ERROR.subtitle,
    message: ERROR_MESSAGES.INTERNAL_ERROR.message,
    code: 'INTERNAL_ERROR',
  };
}
