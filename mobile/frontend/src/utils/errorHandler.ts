import type { AppError, ErrorCode } from '../types/errors';

// Mapa de codigos de error a mensajes legibles en castellano
const ERROR_MESSAGES: Record<ErrorCode, { subtitle: string; message: string }> = {
  INVALID_CREDENTIALS: {
    subtitle: 'Credenciales invalidas',
    message: 'El DNI/correo o la contrasena introducidos no son correctos. Por favor, intentelo de nuevo.',
  },
  INVALID_IDENTIFIER: {
    subtitle: 'Usuario no encontrado',
    message: 'El DNI o correo introducido no corresponde a ningun paciente registrado. Por favor, verifique sus datos.',
  },
  WRONG_PASSWORD: {
    subtitle: 'Contrasena incorrecta',
    message: 'La contrasena introducida no es correcta. Por favor, intentelo de nuevo.',
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
  NO_GAMES_ASSIGNED: {
    subtitle: 'Sin juegos',
    message: 'No tienes juegos asignados.',
  },
  NO_TREATMENTS_ASSIGNED: {
    subtitle: 'Sin tratamientos',
    message: 'No tienes tratamientos asignados todavia.',
  },
  DOCUMENT_DOWNLOAD_FAILED: {
    subtitle: 'Descarga fallida',
    message: 'No se pudo descargar el documento. Intentalo de nuevo.',
  },
  APPOINTMENT_REQUEST_INVALID_CONTACT: {
    subtitle: 'Contacto invalido',
    message: 'Debes indicar un telefono o email valido para solicitar una cita.',
  },
  NOTIFICATION_PERMISSION_DENIED: {
    subtitle: 'Permiso denegado',
    message: 'Activa las notificaciones desde los ajustes del sistema para recibir recordatorios.',
  },
  BODY_PART_NO_DATA: {
    subtitle: 'Sin datos',
    message: 'Aun no hay metricas para esta zona. Juega para ver tu progreso.',
  },
  APPOINTMENT_REQUEST_CONFLICT: {
    subtitle: 'Horario ocupado',
    message: 'Ya hay una cita para esa fecha y hora. Por favor, elige otro horario.',
  },
};

// Convierte un error GraphQL en un AppError estructurado.
// Sigue una cadena de prioridad para extraer la maxima informacion posible:
// 1. Codigo conocido del BFF en extensions.code -> mensaje del mapa local
// 2. Estructura del BFF (extensions.subtitulo + extensions.texto) -> usar directamente
// 3. Mensaje del error GraphQL sin estructura del BFF -> usar message crudo
// 4. Error de red (networkError) -> mensaje de error de conexion
// 5. Caso generico -> INTERNAL_ERROR
export function parseGraphQLError(error: unknown): AppError {
  // Registrar el codigo y mensaje en desarrollo (sin JSON.stringify para evitar errores con referencias circulares de Apollo)
  if (__DEV__) {
    const code = (error as any)?.graphQLErrors?.[0]?.extensions?.code ?? (error as any)?.networkError?.message ?? 'desconocido';
    console.warn('[parseGraphQLError] Codigo de error recibido:', code);
  }

  // Caso 1 y 2: errores GraphQL del BFF
  if (error && typeof error === 'object' && 'graphQLErrors' in error) {
    const gqlErrors = (error as any).graphQLErrors;
    if (Array.isArray(gqlErrors) && gqlErrors.length > 0) {
      const primerError = gqlErrors[0];
      const code = primerError?.extensions?.code as string | undefined;

      // Caso 1: codigo conocido en el mapa local del frontend
      if (code && ERROR_MESSAGES[code as ErrorCode]) {
        const mapped = ERROR_MESSAGES[code as ErrorCode];
        return {
          title: 'Error',
          subtitle: mapped.subtitle,
          message: mapped.message,
          code: code as ErrorCode,
        };
      }

      // Caso 2: estructura del BFF con subtitulo y texto (codigo desconocido para el frontend)
      if (primerError?.extensions?.subtitulo && primerError?.extensions?.texto) {
        return {
          title: primerError.extensions.titulo || 'Error',
          subtitle: primerError.extensions.subtitulo,
          message: primerError.extensions.texto,
          code: (code as ErrorCode) || 'INTERNAL_ERROR',
        };
      }

      // Caso 3: error GraphQL sin estructura del BFF — usar el mensaje crudo
      if (primerError?.message) {
        return {
          title: 'Error',
          subtitle: 'Error del servidor',
          message: primerError.message,
          code: 'INTERNAL_ERROR',
        };
      }
    }
  }

  // Caso 4: error de red (servidor inalcanzable, timeout, CORS, etc.)
  if (error && typeof error === 'object' && 'networkError' in error) {
    const networkError = (error as any).networkError;
    if (__DEV__) {
      console.warn('[parseGraphQLError] Error de red:', networkError?.message, networkError?.statusCode);
    }
    return {
      title: 'Error',
      subtitle: ERROR_MESSAGES.NETWORK_ERROR.subtitle,
      message: ERROR_MESSAGES.NETWORK_ERROR.message,
      code: 'NETWORK_ERROR',
    };
  }

  // Caso 5: error completamente desconocido
  if (__DEV__) {
    console.warn('[parseGraphQLError] Error no clasificado, devolviendo INTERNAL_ERROR');
  }
  return {
    title: 'Error',
    subtitle: ERROR_MESSAGES.INTERNAL_ERROR.subtitle,
    message: ERROR_MESSAGES.INTERNAL_ERROR.message,
    code: 'INTERNAL_ERROR',
  };
}
