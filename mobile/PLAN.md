# PLAN.md -- Depuracion de Login y Mejora de Observabilidad

> Agente: Sonnet (Doer) bajo Agent 2 (Mobile)
> Dominio: /mobile/ (backend + frontend)
> Prerequisitos: Leer CLAUDE.md raiz, mobile, backend, frontend
> Fecha: 2026-04-05

---

## Diagnostico

El login de la app movil falla SIEMPRE con el mensaje generico "Error interno e inesperado, por favor intentalo de nuevo mas tarde", independientemente de las credenciales introducidas. La causa raiz es una cadena de errores que se tragan el contexto original a lo largo de todo el flujo:

1. El `apiClient.js` no gestiona errores de parseo JSON ni diferencia el contexto HTTP 401 (login vs token).
2. El `authService.js` atrapa TODOS los errores del apiClient y los convierte a `INVALID_CREDENTIALS`, perdiendo errores de red, timeouts, y errores del servidor.
3. El `authService.js` guarda tokens en cache con la clave `identifier` (puede ser email) pero los busca con `dniPac` (siempre DNI), provocando cache miss en refresh.
4. El `errorFormatter.js` pierde el contexto original de errores inesperados en los logs.
5. El frontend `parseGraphQLError()` cae al caso generico `INTERNAL_ERROR` cuando el codigo no esta en su mapa.
6. El Apollo `errorLink` del frontend solo usa `console.error` sin estructura.
7. La URL del GraphQL esta hardcodeada a `localhost:3000`, inaccesible desde dispositivos fisicos.
8. El error store no se limpia tras un login exitoso.

El resultado es que cualquier error (incluso credenciales correctas contra un servidor caido) siempre muestra "Error interno" al paciente.

---

## Fase 1: Mejora de Logging del Backend (observabilidad)

Antes de corregir bugs, mejorar los logs para poder diagnosticar problemas en tiempo real.

---

### Tarea 1.1: Logging detallado en apiClient.js

**Archivo:** `/mobile/backend/src/services/apiClient.js`
**Lineas afectadas:** 188-233
**Problema:** Solo se registra "Peticion saliente" con method y URL. No se registra: cuerpo de la peticion (sanitizado), status de respuesta, tiempo de respuesta, Content-Type de la respuesta. Esto hace imposible diagnosticar problemas de comunicacion con la API de Java.
**Solucion:** Anadir logging a nivel debug para peticiones (con cuerpo sanitizado) y a nivel info para respuestas (status + tiempo). Sanitizar passwords del cuerpo antes de loguear.

**Codigo actual (lineas 188-233):**

```javascript
  try {
    logger.debug({ method, url }, 'Peticion saliente a la API de Java');

    const res = await fetch(url, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });

    clearTimeout(timer);

    // 204 No Content — exito sin cuerpo
    if (res.status === 204) {
      logger.debug({ method, url, status: 204 }, 'Respuesta OK (204) de la API de Java');
      return null;
    }

    const data = await res.json();

    if (!res.ok) {
      logger.error({ method, url, status: res.status, data }, 'Error HTTP de la API de Java');
      throw crearError(mapearErrorHttp(res.status));
    }

    logger.debug({ method, url, status: res.status }, 'Respuesta OK de la API de Java');
    return data;

  } catch (err) {
    clearTimeout(timer);

    // El error ya es un GraphQLError construido por nosotros
    if (err.extensions && err.extensions.code) {
      throw err;
    }

    // Timeout o error de red
    if (err.name === 'AbortError') {
      logger.error({ method, url }, 'Timeout en peticion a la API de Java');
      throw crearError('NETWORK_ERROR', 'La API no respondio en el tiempo esperado.');
    }

    // ECONNREFUSED u otros errores de conexion
    logger.error({ method, url, err: err.message }, 'Error de conexion con la API de Java');
    throw crearError('NETWORK_ERROR');
  }
```

**Codigo corregido (reemplazar lineas 188-233):**

```javascript
  // Funcion auxiliar para sanitizar cuerpos de peticion en los logs
  // Elimina contrasenas y tokens para no exponerlos en los registros
  function sanitizarBodyParaLog(body) {
    if (!body) return undefined;
    const copia = { ...body };
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

    // Intentar parsear JSON de forma segura (ver Tarea 2.3 para manejo de errores)
    let data;
    try {
      data = await res.json();
    } catch (parseErr) {
      // La respuesta no es JSON valido (ej: pagina HTML de error 502/503)
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
```

**Verificacion:**
1. Ejecutar `npm run dev` con `LOG_LEVEL=debug`.
2. Enviar una peticion de login valida y verificar que aparecen los logs de peticion saliente (con body sanitizado) y respuesta (con status, tiempoMs, contentType).
3. Apagar la API de Java y verificar que aparece el log de error de conexion con tiempoMs y el nombre del error.

---

### Tarea 1.2: Anadir contexto de ruta al mapeo de errores HTTP

**Archivo:** `/mobile/backend/src/services/apiClient.js`
**Lineas afectadas:** 138-147
**Problema:** `mapearErrorHttp(401)` siempre devuelve `TOKEN_INVALID`, pero en el contexto de login, un HTTP 401 significa credenciales invalidas, no token invalido. El mapeo no tiene contexto sobre que endpoint genero el error.
**Solucion:** Anadir un parametro `path` a `mapearErrorHttp` para distinguir endpoints de autenticacion de endpoints de datos.

**Codigo actual (lineas 138-147):**

```javascript
function mapearErrorHttp(status) {
  const mapa = {
    400: 'VALIDATION_ERROR',
    401: 'TOKEN_INVALID',
    403: 'ACCOUNT_DEACTIVATED',
    404: 'PATIENT_NOT_FOUND',
    409: 'APPOINTMENT_CONFLICT',
  };
  return mapa[status] || 'INTERNAL_ERROR';
}
```

**Codigo corregido (reemplazar lineas 138-147):**

```javascript
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
    return esEndpointAuth ? 'INVALID_CREDENTIALS' : 'TOKEN_INVALID';
  }

  const mapa = {
    400: 'VALIDATION_ERROR',
    403: 'ACCOUNT_DEACTIVATED',
    404: 'PATIENT_NOT_FOUND',
    409: 'APPOINTMENT_CONFLICT',
  };
  return mapa[status] || 'INTERNAL_ERROR';
}
```

**NOTA:** La linea donde se llama `mapearErrorHttp` tambien debe actualizarse para pasar `path`. Esto ya esta incluido en el codigo de la Tarea 1.1 (linea `throw crearError(mapearErrorHttp(res.status, path));`).

**Verificacion:**
1. Enviar login con credenciales incorrectas contra la API de Java real.
2. Verificar que el error devuelto al frontend es `INVALID_CREDENTIALS` y no `TOKEN_INVALID`.
3. Enviar una peticion autenticada con un JWT expirado y verificar que devuelve `TOKEN_INVALID`.

---

### Tarea 1.3: Logs descriptivos por tipo de error en el catch de authService

**Archivo:** `/mobile/backend/src/services/authService.js`
**Lineas afectadas:** 66-72
**Problema:** El catch block atrapa todos los errores y los convierte silenciosamente a `INVALID_CREDENTIALS`. No se registra el error original, lo que hace imposible saber si el problema fue de red, de parsing, de timeout, o realmente de credenciales.
**Solucion:** Se aborda en la Tarea 2.1 (correccion del catch block). Los logs se incluyen automaticamente en el nuevo codigo.

---

### Tarea 1.4: Logging mejorado en errorFormatter

**Archivo:** `/mobile/backend/src/middleware/errorFormatter.js`
**Lineas afectadas:** 62-84
**Problema:** El logger de errores internos no incluye detalles utiles como el codigo de error original, la URL que lo genero, o el status HTTP. Solo registra el mensaje y el stack.
**Solucion:** Se aborda en la Tarea 2.4 (correccion del error formatter). Los logs mejorados se incluyen en el nuevo codigo.

---

## Fase 2: Correccion de Bugs del Backend

Corregir los errores que impiden que el flujo de login funcione correctamente.

---

### Tarea 2.1: Fix authService.login() catch block (BUG 1 - CRITICO)

**Archivo:** `/mobile/backend/src/services/authService.js`
**Lineas afectadas:** 60-72
**Problema:** El catch block de `apiClient.post('/api/auth/login', ...)` atrapa TODOS los errores y los convierte a `INVALID_CREDENTIALS`. Esto incluye errores de red (`NETWORK_ERROR`), errores internos del servidor (`INTERNAL_ERROR`), errores de parsing JSON, timeouts, etc. El unico error que se preserva es `ACCOUNT_DEACTIVATED`. Resultado: si la API de Java esta caida, el paciente ve "Credenciales invalidas" en lugar de "Error de conexion".
**Solucion:** Propagar errores del BFF (que ya tienen codigo en `extensions`) directamente. Solo mapear a `INVALID_CREDENTIALS` cuando el error NO sea un error conocido del BFF o cuando no tenga `extensions.code`.

**Codigo actual (lineas 60-72):**

```javascript
  let javaTokens;
  try {
    javaTokens = await apiClient.post('/api/auth/login', {
      dni: identifier,
      contrasena: password,
    });
  } catch (err) {
    // Propagar errores de red o de Java directamente
    if (err.extensions && err.extensions.code === 'ACCOUNT_DEACTIVATED') {
      throw err;
    }
    throw crearError('INVALID_CREDENTIALS');
  }
```

**Codigo corregido (reemplazar lineas 60-72):**

```javascript
  let javaTokens;
  try {
    javaTokens = await apiClient.post('/api/auth/login', {
      dni: identifier,
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
```

**NOTA:** Tambien es necesario anadir la importacion de `logger` al principio del archivo. Anadir en la linea 8 (despues de `const apiClient = ...`):

**Codigo actual (linea 8):**

```javascript
const apiClient = require('./apiClient');
```

**Codigo corregido (linea 8-9):**

```javascript
const apiClient = require('./apiClient');
const logger = require('../logger');
```

**Verificacion:**
1. Apagar la API de Java y enviar login. Debe devolver `NETWORK_ERROR` (no `INVALID_CREDENTIALS`).
2. Enviar login con credenciales incorrectas contra la API de Java. Debe devolver `INVALID_CREDENTIALS`.
3. Enviar login con cuenta desactivada. Debe devolver `ACCOUNT_DEACTIVATED`.
4. Verificar que los logs contienen el codigo de error y los primeros caracteres del identifier.

---

### Tarea 2.2: Fix cache key mismatch en token cache (BUG 2 - CRITICO)

**Archivo:** `/mobile/backend/src/services/authService.js`
**Lineas afectadas:** 78-85
**Problema:** La funcion `login()` guarda tokens en cache con la clave `identifier` (puede ser email como `admin@rehabiapp.com` o DNI como `12345678Z`). La funcion `refresh()` (linea 109) busca tokens con `dniPac` extraido del payload JWT (`payload.sub`), que siempre es el DNI. Si el usuario hizo login con email, el refresh token falla porque no encuentra la cache: `tokenCache.get('12345678Z')` no existe, solo existe `tokenCache.get('admin@rehabiapp.com')`.
**Solucion:** Despues de generar el par BFF (que decodifica el JWT para obtener el `dniPac`), usar siempre el `dniPac` como clave de la cache. Extraer el DNI del payload del JWT BFF recien generado.

**Codigo actual (lineas 73-86):**

```javascript
  if (!javaTokens || !javaTokens.accessToken) {
    throw crearError('INVALID_CREDENTIALS');
  }

  // Guardar tokens de Java en cache para llamadas internas
  // En produccion, identifier puede ser email — usar sub del JWT Java como clave real
  tokenCache.set(identifier, {
    accessToken: javaTokens.accessToken,
    refreshToken: javaTokens.refreshToken,
  });

  return generarParBff(identifier);
}
```

**Codigo corregido (reemplazar lineas 73-86):**

```javascript
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
```

**Verificacion:**
1. Hacer login con email (`admin@rehabiapp.com`) y verificar que los tokens se guardan con clave DNI.
2. Inmediatamente despues, llamar a `refreshToken` con el refresh token del BFF.
3. Verificar que el refresh encuentra los tokens de Java en cache (no devuelve error).
4. Repetir la prueba haciendo login con DNI directamente para confirmar que sigue funcionando.

---

### Tarea 2.3: Fix JSON parse error handling en apiClient (BUG 3 - ALTO)

**Archivo:** `/mobile/backend/src/services/apiClient.js`
**Lineas afectadas:** 206 (ahora integrado en Tarea 1.1)
**Problema:** `res.json()` se llama sin try-catch. Si la API de Java devuelve HTML (por ejemplo, una pagina de error 502 de nginx), `res.json()` lanza un `SyntaxError` que no se maneja correctamente. Este SyntaxError burbujea hasta el catch generico y se convierte en un error sin codigo del BFF.
**Solucion:** YA INCLUIDA en el codigo corregido de la Tarea 1.1. El bloque try-catch alrededor de `res.json()` maneja SyntaxError de forma explicita, logueando el Content-Type y un fragmento del cuerpo de la respuesta para diagnostico.

**Verificacion:**
1. Configurar un proxy que devuelva HTML en lugar de JSON para la ruta de login.
2. Verificar que el log muestra `Respuesta de la API de Java no es JSON valido` con el Content-Type, status, y fragmento del cuerpo.
3. Verificar que el error devuelto al frontend es `NETWORK_ERROR` con un mensaje descriptivo.

---

### Tarea 2.4: Fix error formatter pierde contexto (BUG 4 - ALTO)

**Archivo:** `/mobile/backend/src/middleware/errorFormatter.js`
**Lineas afectadas:** 62-84
**Problema:** Cuando ocurre un error inesperado (no controlado por el BFF), el formatter solo registra `formattedError.message`, `path` y `stack`. No incluye: el codigo original del error, el status HTTP que lo genero, la URL del endpoint, ni los detalles de la respuesta. Esto hace imposible diagnosticar errores en produccion.
**Solucion:** Incluir en el log toda la informacion disponible del error original. En modo desarrollo, incluir una extension `causa` con detalles para depuracion.

**Codigo actual (lineas 62-84):**

```javascript
  // Cualquier otro error no controlado -> INTERNAL_ERROR (nivel error: inesperado)
  logger.error(
    {
      mensaje: formattedError.message,
      path: formattedError.path,
      stack: originalError && originalError.stack,
    },
    'Error interno no controlado en el BFF'
  );

  const err = crearError('INTERNAL_ERROR');

  const resultado = {
    message: err.message,
    extensions: { ...err.extensions },
  };

  // Solo en desarrollo: incluir el stack trace para depuracion
  if (config.nodeEnv !== 'production' && originalError && originalError.stack) {
    resultado.extensions.stacktrace = originalError.stack;
  }

  return resultado;
```

**Codigo corregido (reemplazar lineas 62-84):**

```javascript
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
```

**Verificacion:**
1. Forzar un error inesperado (por ejemplo, lanzar un Error generico desde un resolver).
2. Verificar que el log contiene: `codigoOriginal`, `extensiones`, `errorOriginalNombre`, `errorOriginalMensaje`.
3. En modo desarrollo, verificar que la respuesta GraphQL incluye `extensions.causa` con los detalles.
4. En modo produccion (NODE_ENV=production), verificar que `causa` y `stacktrace` NO se incluyen en la respuesta.

---

### Tarea 2.5: Fix HTTP 401 mapping con contexto (BUG 6 - MEDIO)

**Archivo:** `/mobile/backend/src/services/apiClient.js`
**Lineas afectadas:** 138-147 (ahora reemplazado por Tarea 1.2)
**Problema:** YA INCLUIDO en la Tarea 1.2. La funcion `mapearErrorHttp` ahora recibe el parametro `path` para distinguir endpoints de autenticacion.
**Solucion:** Ver Tarea 1.2.

**Verificacion:** Ver verificacion de Tarea 1.2.

---

## Fase 3: Correccion de Bugs del Frontend

Corregir los errores que impiden que el frontend muestre mensajes de error correctos al paciente.

---

### Tarea 3.1: Fix parseGraphQLError() siempre cae a INTERNAL_ERROR (BUG 7 - CRITICO)

**Archivo:** `/mobile/frontend/src/utils/errorHandler.ts`
**Lineas afectadas:** 48-83 (funcion completa)
**Problema:** La funcion comprueba `graphQLErrors[0]?.extensions?.code` y lo busca en `ERROR_MESSAGES`. Si el codigo es `undefined` o no existe en el mapa, cae directamente al caso generico que devuelve INTERNAL_ERROR. Ademas, si hay `graphQLErrors` con un mensaje legible pero sin codigo mapeado, ese mensaje se pierde. Tambien, cuando hay `graphQLErrors` Y `networkError` simultaneamente, el `networkError` nunca se evalua porque el primer if ya salto al caso generico.
**Solucion:** Mejorar la logica de fallback: (1) si hay graphQLErrors con codigo conocido, usarlo; (2) si hay graphQLErrors con codigo desconocido pero con `extensions.subtitulo` y `extensions.texto`, usar esos campos directamente (el backend BFF los envia); (3) si hay graphQLErrors pero sin estructura del BFF, usar el `message` del primer error; (4) si hay networkError, mostrar error de red; (5) como ultimo recurso, INTERNAL_ERROR. Registrar siempre el error completo para depuracion.

**Codigo actual (lineas 48-83):**

```typescript
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
```

**Codigo corregido (reemplazar lineas 48-83):**

```typescript
// Convierte un error GraphQL en un AppError estructurado.
// Sigue una cadena de prioridad para extraer la maxima informacion posible:
// 1. Codigo conocido del BFF en extensions.code -> mensaje del mapa local
// 2. Estructura del BFF (extensions.subtitulo + extensions.texto) -> usar directamente
// 3. Mensaje del error GraphQL sin estructura del BFF -> usar message crudo
// 4. Error de red (networkError) -> mensaje de error de conexion
// 5. Caso generico -> INTERNAL_ERROR
export function parseGraphQLError(error: unknown): AppError {
  // Registrar el error completo en la consola para depuracion durante desarrollo
  if (__DEV__) {
    console.warn('[parseGraphQLError] Error recibido:', JSON.stringify(error, null, 2));
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
    // Registrar detalles del error de red para depuracion
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
```

**Verificacion:**
1. Enviar login con credenciales invalidas. Verificar que el popup muestra "Credenciales invalidas" (no "Error interno").
2. Apagar el backend BFF y enviar login. Verificar que muestra "Error de conexion".
3. Forzar un error sin estructura del BFF (por ejemplo, un error de validacion de GraphQL). Verificar que muestra el mensaje del servidor.
4. En modo desarrollo (__DEV__), verificar que `console.warn` muestra los detalles del error en la terminal de Metro.

---

### Tarea 3.2: Fix Apollo errorLink sin estructura (BUG 8 - ALTO)

**Archivo:** `/mobile/frontend/src/services/graphql/client.ts`
**Lineas afectadas:** 33-52
**Problema:** El `errorLink` usa `console.error` con concatenacion de strings. No incluye el nombre de la operacion de forma facil de filtrar, no muestra las variables de la peticion (sanitizadas), y no registra el `statusCode` del networkError. Esto dificulta la depuracion en la terminal de Metro.
**Solucion:** Reemplazar con logging estructurado que incluya nombre de operacion, variables sanitizadas, codigo de error, y statusCode. Usar `console.warn` en lugar de `console.error` para errores GraphQL (que son errores de negocio esperados) y reservar `console.error` para errores de red (inesperados).

**Codigo actual (lineas 33-52):**

```typescript
const errorLink = onError(function ({ graphQLErrors, networkError, operation }) {
  if (graphQLErrors) {
    for (const err of graphQLErrors) {
      const code = err.extensions?.code ?? 'SIN_CODIGO';
      const path = err.path ? err.path.join(' > ') : operation.operationName;
      console.error(
        `[GraphQL] ${code} en "${path}"\n` +
        `  Mensaje : ${err.message}\n` +
        `  Subtitulo: ${err.extensions?.subtitulo ?? '-'}\n` +
        `  Texto    : ${err.extensions?.texto ?? '-'}`
      );
    }
  }
  if (networkError) {
    console.error(
      `[Red] Error de conexion en "${operation.operationName}"\n` +
      `  ${networkError.message}`
    );
  }
});
```

**Codigo corregido (reemplazar lineas 33-52):**

```typescript
// Sanitizar variables de la operacion para los logs (ocultar contrasenas)
function sanitizarVariables(variables: Record<string, any> | undefined): Record<string, any> | undefined {
  if (!variables) return undefined;
  const copia = { ...variables };
  if (copia.password) copia.password = '***';
  if (copia.contrasena) copia.contrasena = '***';
  if (copia.refreshToken) copia.refreshToken = '***TOKEN***';
  return copia;
}

// Link de errores globales — logging estructurado para depuracion en Metro
const errorLink = onError(function ({ graphQLErrors, networkError, operation }) {
  const operacion = operation.operationName || 'operacion_anonima';
  const variables = sanitizarVariables(operation.variables);

  if (graphQLErrors) {
    for (const err of graphQLErrors) {
      const code = err.extensions?.code ?? 'SIN_CODIGO';
      // Errores de negocio (credenciales, validacion) son warn, no error
      console.warn(
        `[GraphQL] ${code} | operacion="${operacion}" | mensaje="${err.message}" | ` +
        `subtitulo="${err.extensions?.subtitulo ?? '-'}" | ` +
        `texto="${err.extensions?.texto ?? '-'}" | ` +
        `variables=${JSON.stringify(variables)}`
      );
    }
  }

  if (networkError) {
    // Extraer statusCode si existe (errores HTTP devuelven statusCode)
    const statusCode = 'statusCode' in networkError ? (networkError as any).statusCode : 'N/A';
    console.error(
      `[Red] Error de conexion | operacion="${operacion}" | ` +
      `status=${statusCode} | mensaje="${networkError.message}" | ` +
      `variables=${JSON.stringify(variables)}`
    );
  }
});
```

**Verificacion:**
1. Enviar login con credenciales invalidas y verificar que Metro muestra un `console.warn` con el codigo, operacion, y variables (password oculta).
2. Apagar el BFF y enviar login. Verificar que Metro muestra un `console.error` con statusCode y mensaje del error de red.
3. Verificar que las contrasenas NO aparecen en los logs.

---

### Tarea 3.3: Fix URL hardcodeada a localhost:3000 (BUG 9 - ALTO)

**Archivo:** `/mobile/frontend/src/services/graphql/client.ts`
**Lineas afectadas:** 9-10
**Problema:** La URL `http://localhost:3000/graphql` esta hardcodeada. `localhost` se resuelve al propio dispositivo movil, no al ordenador de desarrollo. En un telefono fisico o emulador Android, la peticion nunca llega al BFF. Android usa `10.0.2.2` para acceder al host y los dispositivos fisicos necesitan la IP LAN real.
**Solucion:** Usar `expo-constants` para detectar automaticamente la URL correcta. En desarrollo, Expo proporciona la IP del servidor de desarrollo a traves de `Constants.expoConfig.hostUri`. En produccion, usar la URL configurada. Esto permite que funcione en emuladores Y dispositivos fisicos sin cambios.

**Codigo actual (lineas 1-14):**

```typescript
import { ApolloClient, InMemoryCache, createHttpLink, from } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import { onError } from '@apollo/client/link/error';
import * as SecureStore from 'expo-secure-store';
import type { AuthToken } from '../../types/auth';

const TOKEN_KEY = 'auth_token';

// URL base del backend BFF — cambiar segun entorno
const GRAPHQL_URI = 'http://localhost:3000/graphql';

const httpLink = createHttpLink({
  uri: GRAPHQL_URI,
});
```

**Codigo corregido (reemplazar lineas 1-14):**

```typescript
import { ApolloClient, InMemoryCache, createHttpLink, from } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import { onError } from '@apollo/client/link/error';
import * as SecureStore from 'expo-secure-store';
import Constants from 'expo-constants';
import { Platform } from 'react-native';
import type { AuthToken } from '../../types/auth';

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
```

**NOTA:** Es necesario verificar que `expo-constants` esta instalado en el proyecto. Ejecutar:

```bash
cd /mobile/frontend && npx expo install expo-constants
```

Si ya esta instalado (probablemente si, porque es parte del stack de Expo), no hace falta instalarlo.

**Verificacion:**
1. Ejecutar `npx expo start` y verificar que en la terminal de Metro aparece `[Apollo] URL del BFF resuelta: http://192.168.X.X:3000/graphql` con la IP LAN correcta.
2. Abrir la app en un emulador Android y verificar que las peticiones llegan al BFF.
3. Abrir la app en un dispositivo fisico conectado a la misma red WiFi y verificar que las peticiones llegan al BFF.
4. Si se define `EXPO_PUBLIC_API_URL=https://api.rehabiapp.com`, verificar que usa esa URL en lugar de la local.

---

### Tarea 3.4: Limpiar error store tras login exitoso (BUG 10 - MEDIO)

**Archivo:** `/mobile/frontend/src/store/authStore.ts`
**Lineas afectadas:** 30-31
**Problema:** Despues de un login exitoso, el `authStore` establece `isAuthenticated: true` e `isLoading: false`, pero el `errorStore` puede tener un error pendiente de un intento de login fallido anterior. Este error persistente puede mostrarse al usuario en la siguiente pantalla si el `ErrorPopup` sigue montado.
**Solucion:** Importar `useErrorStore` y llamar a `hideError()` tras el login exitoso. Tambien limpiar el error del propio `authStore` (ya se hace con `error: null` en el `set` del try, pero tambien asegurar que el `errorStore` global se limpia).

**Codigo actual (lineas 1-37):**

```typescript
import { create } from 'zustand';
import * as SecureStore from 'expo-secure-store';
import type { AuthState, AuthToken, LoginCredentials } from '../types/auth';
import type { AppError } from '../types/errors';
import { client } from '../services/graphql/client';
import { LOGIN_MUTATION } from '../services/graphql/mutations/auth';
import { parseGraphQLError } from '../utils/errorHandler';

// Clave para almacenamiento seguro de tokens
const TOKEN_KEY = 'auth_token';

export const useAuthStore = create<AuthState>(function (set) {
  return {
    token: null,
    isAuthenticated: false,
    isLoading: true,
    error: null,

    login: async function (credentials: LoginCredentials): Promise<void> {
      set({ isLoading: true, error: null });
      try {
        const { data } = await client.mutate({
          mutation: LOGIN_MUTATION,
          variables: {
            identifier: credentials.identifier,
            password: credentials.password,
          },
        });
        const token: AuthToken = data.login;
        await SecureStore.setItemAsync(TOKEN_KEY, JSON.stringify(token));
        set({ token, isAuthenticated: true, isLoading: false });
      } catch (err) {
        const appError: AppError = parseGraphQLError(err);
        set({ error: appError, isLoading: false });
        throw appError;
      }
    },
```

**Codigo corregido (reemplazar lineas 1-37):**

```typescript
import { create } from 'zustand';
import * as SecureStore from 'expo-secure-store';
import type { AuthState, AuthToken, LoginCredentials } from '../types/auth';
import type { AppError } from '../types/errors';
import { client } from '../services/graphql/client';
import { LOGIN_MUTATION } from '../services/graphql/mutations/auth';
import { parseGraphQLError } from '../utils/errorHandler';
import { useErrorStore } from './errorStore';

// Clave para almacenamiento seguro de tokens
const TOKEN_KEY = 'auth_token';

export const useAuthStore = create<AuthState>(function (set) {
  return {
    token: null,
    isAuthenticated: false,
    isLoading: true,
    error: null,

    login: async function (credentials: LoginCredentials): Promise<void> {
      set({ isLoading: true, error: null });
      try {
        const { data } = await client.mutate({
          mutation: LOGIN_MUTATION,
          variables: {
            identifier: credentials.identifier,
            password: credentials.password,
          },
        });
        const token: AuthToken = data.login;
        await SecureStore.setItemAsync(TOKEN_KEY, JSON.stringify(token));
        // Limpiar cualquier error previo del error store global
        useErrorStore.getState().hideError();
        set({ token, isAuthenticated: true, isLoading: false, error: null });
      } catch (err) {
        const appError: AppError = parseGraphQLError(err);
        set({ error: appError, isLoading: false });
        throw appError;
      }
    },
```

**NOTA:** `useErrorStore.getState()` es una llamada valida de Zustand que accede al estado fuera de un componente React. No requiere estar dentro de un hook.

**Verificacion:**
1. Hacer login con credenciales incorrectas. Verificar que aparece el popup de error.
2. Cerrar el popup y hacer login con credenciales correctas.
3. Verificar que NO aparece ningun error residual despues del login exitoso.
4. Navegar a la pantalla principal y verificar que el `errorStore` esta limpio (`currentError: null, isVisible: false`).

---

## Fase 4: Verificacion integral

Despues de aplicar todas las correcciones, ejecutar las siguientes pruebas para validar el flujo completo.

---

### Tarea 4.1: Test login con credenciales validas (modo mock)

**Prerrequisitos:** Backend BFF corriendo con `MOCK_API=true`.

**Pasos:**
1. Iniciar el backend: `cd /mobile/backend && MOCK_API=true LOG_LEVEL=debug npm run dev`
2. Iniciar el frontend: `cd /mobile/frontend && npx expo start`
3. En la app, introducir DNI `12345678Z` y contrasena `admin`.
4. Pulsar "Iniciar Sesion".

**Resultado esperado:**
- Backend logs: peticion de login recibida, tokens generados, cache actualizada con clave `12345678Z`.
- Frontend: navegacion automatica a la pantalla principal (Dashboard).
- No aparece ningun popup de error.

**Repetir con email:** Usar `admin@rehabiapp.com` y contrasena `admin`. Mismo resultado esperado.

---

### Tarea 4.2: Test login con credenciales invalidas (modo mock)

**Prerrequisitos:** Backend BFF corriendo con `MOCK_API=true`.

**Pasos:**
1. En la app, introducir DNI `99999999X` y contrasena `wrong`.
2. Pulsar "Iniciar Sesion".

**Resultado esperado:**
- Backend logs: error `INVALID_CREDENTIALS` en el resolver de login.
- Frontend: popup de error con titulo "Error", subtitulo "Credenciales invalidas", y texto "El DNI/correo o la contrasena introducidos no son correctos..."
- Terminal Metro: `console.warn` con `[GraphQL] INVALID_CREDENTIALS | operacion="Login" | ...`
- El boton vuelve a estar activo (isLoading = false).

---

### Tarea 4.3: Test login con servidor BFF caido

**Prerrequisitos:** Backend BFF NO corriendo.

**Pasos:**
1. Asegurarse de que el BFF esta apagado (kill del proceso).
2. En la app, introducir cualquier credencial y pulsar "Iniciar Sesion".

**Resultado esperado:**
- Frontend: popup de error con subtitulo "Error de conexion" y texto "No se ha podido conectar con el servidor..."
- Terminal Metro: `console.error` con `[Red] Error de conexion | operacion="Login" | ...`
- NO debe mostrar "Error interno" ni "Credenciales invalidas".

---

### Tarea 4.4: Test refresh de token (modo mock)

**Prerrequisitos:** Backend BFF corriendo con `MOCK_API=true`.

**Pasos:**
1. Hacer login con email `admin@rehabiapp.com`.
2. Copiar el `refreshToken` de la respuesta.
3. Enviar la mutacion `refreshToken` con ese token (via GraphiQL o curl).

**Resultado esperado:**
- Backend logs: cache hit para `12345678Z` (NO para `admin@rehabiapp.com`).
- Respuesta: nuevo par de tokens con `expiresAt` actualizado.
- Si la cache se busca con email, devolveria error — esto valida la correccion del BUG 2.

**Comando curl de ejemplo:**
```bash
curl -s http://localhost:3000/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { refreshToken(refreshToken: \"TOKEN_AQUI\") { accessToken refreshToken expiresAt } }"}'
```

---

### Tarea 4.5: Verificar que los logs contienen informacion util

**Prerrequisitos:** Backend BFF corriendo con `LOG_LEVEL=debug`.

**Verificar que aparecen los siguientes campos en los logs:**

| Escenario | Campos requeridos en el log |
|-----------|---------------------------|
| Peticion saliente | `method`, `url`, `body` (sanitizado) |
| Respuesta OK | `method`, `url`, `status`, `tiempoMs`, `contentType` |
| Error HTTP | `method`, `url`, `status`, `contentType`, `tiempoMs`, `data` |
| Error de conexion | `method`, `url`, `tiempoMs`, `errorNombre`, `errorMensaje` |
| Timeout | `method`, `url`, `tiempoMs`, `timeoutMs` |
| Error de parseo JSON | `method`, `url`, `status`, `contentType`, `errorParseo`, `cuerpoTruncado` |
| Login propagado | `code`, `identifier` (truncado) |
| Cache actualizada | `dniPac` (truncado), `cacheSize` |
| Error interno (formatter) | `codigoOriginal`, `extensiones`, `errorOriginalNombre`, `errorOriginalMensaje` |

**Test rapido para todos los escenarios:**
```bash
# 1. Login exitoso (mock)
curl -s http://localhost:3000/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { login(identifier: \"admin\", password: \"admin\") { accessToken } }"}'

# 2. Login fallido (mock, credenciales incorrectas)
curl -s http://localhost:3000/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { login(identifier: \"admin\", password: \"wrong\") { accessToken } }"}'

# 3. Query sin autenticacion (debe devolver TOKEN_INVALID)
curl -s http://localhost:3000/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ me { dniPac nombrePac } }"}'
```

---

## Resumen de archivos modificados

| Archivo | Tareas | Bugs corregidos |
|---------|--------|-----------------|
| `/mobile/backend/src/services/apiClient.js` | 1.1, 1.2, 2.3, 2.5 | BUG 3, BUG 5, BUG 6 |
| `/mobile/backend/src/services/authService.js` | 1.3, 2.1, 2.2 | BUG 1, BUG 2 |
| `/mobile/backend/src/middleware/errorFormatter.js` | 1.4, 2.4 | BUG 4 |
| `/mobile/frontend/src/utils/errorHandler.ts` | 3.1 | BUG 7 |
| `/mobile/frontend/src/services/graphql/client.ts` | 3.2, 3.3 | BUG 8, BUG 9 |
| `/mobile/frontend/src/store/authStore.ts` | 3.4 | BUG 10 |

## Orden de implementacion recomendado

1. **Primero el backend** (Fase 1 + Fase 2): los bugs del backend son la causa raiz. Corregirlos permite que el frontend reciba errores correctos.
2. **Despues el frontend** (Fase 3): una vez que el backend envia errores correctos, el frontend puede mostrarlos.
3. **Finalmente verificacion** (Fase 4): ejecutar los tests end-to-end para confirmar que todo funciona.

Dentro de cada fase, el orden de tareas es el indicado. Las tareas 1.1 y 1.2 modifican `apiClient.js` y deben aplicarse juntas. Las tareas 2.1 y 2.2 modifican `authService.js` y deben aplicarse juntas.
