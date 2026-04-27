import { ApolloClient, InMemoryCache, createHttpLink, from } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import { onError } from '@apollo/client/link/error';
import * as SecureStore from 'expo-secure-store';
import Constants from 'expo-constants';
import { Platform } from 'react-native';
import type { AuthToken } from '../../types/auth';
import { useAuthStore } from '../../store/authStore';

const TOKEN_KEY = 'auth_token';

// Puerto del BFF (debe coincidir con PORT en /mobile/backend/src/config.js)
const BFF_PORT = 3000;

/**
 * Resuelve la URL del BFF automaticamente segun el entorno:
 * - Produccion: variable de entorno EXPO_PUBLIC_API_URL
 * - Desarrollo con Expo: extrae la IP del servidor de desarrollo desde hostUri
 * - Android emulador fallback: 10.0.2.2 (IP especial del host en el emulador de Android)
 * - iOS simulador fallback: localhost (comparte red con el host)
 */
function resolverUrlGraphQL(): string {
  // En produccion, usar la URL configurada por variable de entorno
  const urlProduccion = Constants.expoConfig?.extra?.apiUrl
    || process.env.EXPO_PUBLIC_API_URL;
  if (urlProduccion) {
    return `${urlProduccion}/graphql`;
  }

  // En desarrollo: extraer la IP del servidor Expo desde hostUri
  // hostUri tiene formato "192.168.1.100:8081" (IP:puerto_expo)
  const hostUri = Constants.expoConfig?.hostUri;
  if (hostUri) {
    const ip = hostUri.split(':')[0];
    if (ip) {
      return `http://${ip}:${BFF_PORT}/graphql`;
    }
  }

  // Fallback segun plataforma
  if (Platform.OS === 'android') {
    // 10.0.2.2 es la IP especial para acceder al host desde el emulador de Android
    return `http://10.0.2.2:${BFF_PORT}/graphql`;
  }

  // iOS simulador o web: localhost funciona porque comparten red con el host
  return `http://localhost:${BFF_PORT}/graphql`;
}

const GRAPHQL_URI = resolverUrlGraphQL();

// Registrar la URL resuelta en desarrollo para facilitar la depuracion
if (__DEV__) {
  console.log(`[Apollo] URL del BFF resuelta: ${GRAPHQL_URI}`);
}

const httpLink = createHttpLink({
  uri: GRAPHQL_URI,
});

// Link de autenticacion: adjunta el JWT y la zona horaria a cada peticion
const authLink = setContext(async function (_request, previousContext) {
  const tokenJson = await SecureStore.getItemAsync(TOKEN_KEY);
  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;

  if (!tokenJson) {
    return {
      ...previousContext,
      headers: { ...previousContext.headers, 'X-Timezone': timezone },
    };
  }

  const token: AuthToken = JSON.parse(tokenJson);
  return {
    ...previousContext,
    headers: {
      ...previousContext.headers,
      Authorization: `Bearer ${token.accessToken}`,
      'X-Timezone': timezone,
    },
  };
});

// Sanitizar variables de la operacion para los logs (ocultar contrasenas)
function sanitizarVariables(variables: Record<string, any> | undefined): Record<string, any> | undefined {
  if (!variables) return undefined;
  const copia = { ...variables };
  if (copia.password) copia.password = '***';
  if (copia.contrasena) copia.contrasena = '***';
  if (copia.refreshToken) copia.refreshToken = '***TOKEN***';
  return copia;
}

// Codigos del BFF que implican que la sesion ya no es valida
const SESSION_EXPIRED_CODES = new Set(['UNAUTHENTICATED', 'TOKEN_EXPIRED', 'TOKEN_INVALID']);

// Evita ejecutar el cierre de sesion mas de una vez en una rafaga de errores
let cierreEnCurso = false;

async function cerrarSesionPorExpiracion(): Promise<void> {
  if (cierreEnCurso) return;
  cierreEnCurso = true;
  try {
    await useAuthStore.getState().logout();
  } catch {
    // Si logout falla (p.ej. SecureStore no disponible), forzamos el reset minimo
  } finally {
    // Permitir nuevos cierres tras el siguiente tick — la navegacion ya se ha disparado
    setTimeout(function () { cierreEnCurso = false; }, 1000);
  }
}

// Link de errores globales — logging estructurado + auto-logout por sesion expirada
const errorLink = onError(function ({ graphQLErrors, networkError, operation }) {
  const operacion = operation.operationName || 'operacion_anonima';
  const variables = sanitizarVariables(operation.variables);

  if (graphQLErrors) {
    let sesionExpirada = false;
    for (const err of graphQLErrors) {
      const code = err.extensions?.code ?? 'SIN_CODIGO';
      if (SESSION_EXPIRED_CODES.has(code as string)) {
        sesionExpirada = true;
      }
      console.warn(
        `[GraphQL] ${code} | operacion="${operacion}" | mensaje="${err.message}" | ` +
        `subtitulo="${err.extensions?.subtitulo ?? '-'}" | ` +
        `texto="${err.extensions?.texto ?? '-'}" | ` +
        `variables=${JSON.stringify(variables)}`
      );
    }
    if (sesionExpirada) {
      // Disparar cierre de sesion fuera del flujo de la peticion
      void cerrarSesionPorExpiracion();
    }
  }

  if (networkError) {
    const statusCode = 'statusCode' in networkError ? (networkError as any).statusCode : 'N/A';
    if (statusCode === 401) {
      void cerrarSesionPorExpiracion();
    }
    console.error(
      `[Red] Error de conexion | operacion="${operacion}" | ` +
      `status=${statusCode} | mensaje="${networkError.message}" | ` +
      `variables=${JSON.stringify(variables)}`
    );
  }
});

export const client = new ApolloClient({
  link: from([errorLink, authLink, httpLink]),
  cache: new InMemoryCache(),
});
