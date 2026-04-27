# PLAN.md — Mobile Backend BFF: Capa GraphQL y Logica de Aplicacion

> **Agente:** Sonnet (Doer) bajo Agent 2 (Mobile)
> **Dominio:** `/mobile/backend/`
> **Prerequisitos:** Leer `/CLAUDE.md` (raiz), `/mobile/backend/CLAUDE.md`, `/mobile/CLAUDE.md`
> **Alcance:** Implementar la capa GraphQL completa del BFF, incluyendo autenticacion, orquestacion, logica de presentacion y estandarizacion de errores.

---

## Contexto General

Este servicio es un **Backend-For-Frontend (BFF)** cuya unica mision es servir a la aplicacion movil de pacientes. No es un backend monolitico. Su responsabilidad se limita a:

1. Exponer una API GraphQL para la app movil (unico punto de entrada).
2. Orquestar peticiones HTTP hacia la API central de Java (`[ SERVICE: API ]`).
3. Generar y validar tokens JWT propios para la sesion del paciente movil.
4. Formatear y filtrar datos para la UI movil (solo los campos necesarios).
5. Estandarizar errores en un formato consumible por el frontend.
6. Inyectar contexto de presentacion (saludo basado en zona horaria).

### Estado Actual del Proyecto

La **Fase A (Infraestructura)** esta COMPLETADA. El servicio ya cuenta con:

- Express 5 configurado y escuchando en puerto 3000
- Endpoint `GET /health` para probes de Kubernetes
- Endpoint `GET /metrics` para Prometheus
- Logger estructurado JSON con pino/pino-http
- Modulo de configuracion con lectura de secretos CSI (`src/config.js`)
- Dockerfile multi-stage (Node.js 20 Alpine, UID 1000, read-only fs)
- Manifiestos K8s base en `/infra/k8s/base/mobile-backend/`

El codigo fuente existente esta en `src/index.js` (entry point) y `src/config.js` (configuracion).

---

## Dependencias Criticas con la API de Java

**ATENCION IMPLEMENTADORES:** La API REST de Java (`/api`) actualmente solo autentica a **sanitarios** (profesionales). Los endpoints de paciente existen para CRUD pero no hay un endpoint de autenticacion de pacientes. La Phase 4 del checklist de `/api/CLAUDE.md` incluye "Mobile-specific endpoints" como pendiente.

**Estrategia de desbloqueo:**

El BFF se implementa completo contra los contratos definidos aqui. Para desarrollo local sin la API de Java disponible, los servicios deben tener un modo mock configurable via `MOCK_API=true` en variables de entorno. Esto permite al equipo movil avanzar sin depender del despliegue de Java.

**Endpoints de la API de Java que el BFF consumira:**

| Metodo | Endpoint Java                                          | Proposito                              |
|--------|--------------------------------------------------------|----------------------------------------|
| POST   | `/api/auth/login`                                      | Autenticar (DNI + contrasena)          |
| POST   | `/api/auth/refresh`                                    | Renovar tokens                         |
| GET    | `/api/pacientes/{dni}`                                 | Perfil del paciente                    |
| GET    | `/api/pacientes/{dniPac}/discapacidades`               | Discapacidades asignadas               |
| GET    | `/api/pacientes/{dniPac}/tratamientos`                 | Tratamientos asignados                 |
| GET    | `/api/catalogo/tratamientos/discapacidad/{codDis}`     | Tratamientos por discapacidad          |
| GET    | `/api/catalogo/niveles-progresion`                     | Niveles de progresion                  |
| GET    | `/api/citas?fecha={fecha}`                             | Citas por fecha                        |
| GET    | `/api/citas/sanitario/{dniSan}`                        | Citas de un sanitario                  |
| POST   | `/api/citas`                                           | Crear cita                             |
| DELETE | `/api/citas?dniPac=X&dniSan=Y&fecha=Z&hora=H`         | Cancelar cita                          |

---

## RESTRICCIONES ABSOLUTAS (Limites de Dominio)

Estas prohibiciones son **inviolables**. Cualquier implementacion que las incumpla sera rechazada automaticamente.

### CERO Bases de Datos

Este backend tiene **estrictamente prohibido**:
- Instalar o importar `pg`, `mongoose`, `sequelize`, `typeorm`, `prisma`, `knex`, `better-sqlite3` o cualquier ORM/driver de base de datos.
- Conectarse a PostgreSQL, MongoDB o cualquier sistema de almacenamiento persistente.
- Toda la persistencia es responsabilidad exclusiva del servicio de Java (`[ SERVICE: API ]`) y del pipeline de datos (`[ SERVICE: DATA ]`).

### CERO Juegos

Los minijuegos Unity estan alojados en AWS S3/CloudFront de forma independiente. Este backend:
- No interactua con los juegos directamente.
- No almacena archivos de juegos.
- No gestiona trafico de juegos.
- Las sesiones de juego se obtienen exclusivamente a traves de la API de Java (que delega en `/data`).

### CERO Logica de Negocio Clinica

Este BFF no toma decisiones clinicas. No calcula niveles de progresion, no valida tratamientos, no aplica reglas medicas. Solo orquesta y formatea. La logica de negocio reside en la API de Java.

### Filesystem de Solo Lectura

El contenedor K8s ejecuta con `readOnlyRootFilesystem: true`. Solo se puede escribir en `/tmp`. No usar `fs.writeFileSync` a ninguna ruta que no sea `/tmp`.

---

## Arquitectura de Carpetas Objetivo

```
src/
|-- index.js                        # Punto de entrada (EXISTE, se modifica)
|-- config.js                       # Configuracion centralizada (EXISTE, se extiende)
|
|-- graphql/
|   |-- typeDefs/
|   |   |-- index.js                # Fusiona todas las definiciones de tipos
|   |   |-- auth.js                 # Tipos y mutations de autenticacion
|   |   |-- patient.js              # Tipos y queries del paciente
|   |   |-- appointment.js          # Tipos, queries y mutations de citas
|   |   |-- treatment.js            # Tipos y queries de tratamientos
|   |   |-- game.js                 # Tipos y queries de sesiones de juego
|   |   |-- common.js               # Tipos compartidos (errores, paginacion, enums)
|   |
|   |-- resolvers/
|       |-- index.js                # Fusiona todos los resolvers
|       |-- auth.js                 # Resolver de login y refresh
|       |-- patient.js              # Resolver de perfil, discapacidades, progreso
|       |-- appointment.js          # Resolver de citas (listar, reservar, cancelar)
|       |-- treatment.js            # Resolver de tratamientos
|       |-- game.js                 # Resolver de sesiones de juego
|
|-- services/
|   |-- apiClient.js                # Cliente HTTP wrapper para la API de Java
|   |-- authService.js              # Logica JWT propia del BFF
|   |-- patientService.js           # Obtiene datos de paciente desde Java API
|   |-- appointmentService.js       # Gestiona citas via Java API
|   |-- treatmentService.js         # Obtiene tratamientos via Java API
|   |-- gameService.js              # Obtiene sesiones de juego via Java API
|
|-- middleware/
|   |-- auth.js                     # Extraccion y validacion JWT del BFF
|   |-- greeting.js                 # Inyeccion de saludo por zona horaria
|   |-- errorFormatter.js           # Formateador global de errores GraphQL
|
|-- utils/
    |-- errors.js                   # Codigos de error y constructor de GraphQLError
    |-- timezone.js                 # Utilidades de zona horaria
```

---

## Phase B: Capa GraphQL (PENDIENTE)

### Step 10: Instalar Dependencias GraphQL

#### 10.1: Dependencias de produccion

```bash
cd mobile/backend
npm install @apollo/server graphql graphql-tag jsonwebtoken
```

| Paquete            | Version minima | Proposito                                    |
|--------------------|----------------|----------------------------------------------|
| `@apollo/server`   | ^4.11          | Servidor GraphQL (Apollo Server 4)           |
| `graphql`          | ^16.9          | Implementacion de referencia de GraphQL      |
| `graphql-tag`      | ^2.12          | Parser de strings GraphQL a DocumentNode     |
| `jsonwebtoken`     | ^9.0           | Generacion y validacion de JWT propios       |

**PROHIBIDO** instalar `express-graphql` (deprecado), `@graphql-tools/schema` (innecesario con Apollo 4), o cualquier paquete de base de datos.

#### 10.2: Verificar package.json

Tras la instalacion, `package.json` debera contener exactamente estas dependencias de produccion:

```json
{
  "dependencies": {
    "@apollo/server": "^4.11.x",
    "express": "^5.2.x",
    "graphql": "^16.9.x",
    "graphql-tag": "^2.12.x",
    "jsonwebtoken": "^9.0.x",
    "pino": "^10.3.x",
    "pino-http": "^11.0.x",
    "prom-client": "^15.1.x"
  }
}
```

#### Checklist Step 10

- [x] `@apollo/server` instalado
- [x] `graphql` y `graphql-tag` instalados
- [x] `jsonwebtoken` instalado
- [x] Ninguna dependencia de base de datos presente en package.json
- [x] `npm ls --depth=0` no muestra warnings de peer dependencies

---

### Step 11: Extender Configuracion (`src/config.js`)

Extender el modulo de configuracion existente para incluir los nuevos parametros necesarios para la capa GraphQL.

#### Nuevas variables de configuracion:

```
JWT_SECRET          -> Clave de firma HMAC-SHA256 para los JWT del BFF
JWT_EXPIRATION_MS   -> Duracion del access token (default: 1800000 = 30 min)
JWT_REFRESH_MS      -> Duracion del refresh token (default: 604800000 = 7 dias)
MOCK_API            -> 'true' para modo mock sin API de Java (default: 'false')
GRAPHQL_PATH        -> Ruta del endpoint GraphQL (default: '/graphql')
```

#### Campos a anadir en module.exports:

```javascript
module.exports = {
  // ... campos existentes (port, apiBaseUrl, sessionSecret, nodeEnv) ...
  jwtSecret: readSecret('jwt-secret', 'dev-jwt-secret-cambiar-en-produccion'),
  jwtExpirationMs: parseInt(process.env.JWT_EXPIRATION_MS || '1800000', 10),
  jwtRefreshMs: parseInt(process.env.JWT_REFRESH_MS || '604800000', 10),
  mockApi: process.env.MOCK_API === 'true',
  graphqlPath: process.env.GRAPHQL_PATH || '/graphql',
};
```

**IMPORTANTE:** El `jwtSecret` del BFF es DIFERENTE del `jwtSigningKey` de la API de Java. Son dos capas de autenticacion independientes. En produccion, el secreto se lee desde el volumen CSI de AWS Secrets Manager (`/mnt/secrets/jwt-secret`).

#### Checklist Step 11

- [x] `src/config.js` extendido con las 5 nuevas variables
- [x] `readSecret` se reutiliza para `jwt-secret`
- [x] Todos los valores tienen fallback sensato para desarrollo

---

### Step 12: Definir Codigos de Error (`src/utils/errors.js`)

Crear un modulo que centralice todos los codigos de error y proporcione una funcion constructora de GraphQLError estandarizado.

#### Catalogo de Codigos de Error:

El frontend espera errores GraphQL con `extensions.code` coincidiendo con estos valores:

| Codigo                   | HTTP Equiv. | Descripcion                                       |
|--------------------------|-------------|---------------------------------------------------|
| `INVALID_CREDENTIALS`    | 401         | DNI/email o contrasena incorrectos                |
| `ACCOUNT_DEACTIVATED`    | 403         | Cuenta del paciente dada de baja                  |
| `TOKEN_EXPIRED`          | 401         | El access token ha expirado                       |
| `TOKEN_INVALID`          | 401         | Token malformado, firma invalida                  |
| `NETWORK_ERROR`          | 502         | No se puede contactar con la API de Java          |
| `PATIENT_NOT_FOUND`      | 404         | Paciente no existe en el sistema                  |
| `APPOINTMENT_CONFLICT`   | 409         | Horario de cita ya ocupado                        |
| `APPOINTMENT_NOT_FOUND`  | 404         | Cita no encontrada                                |
| `VALIDATION_ERROR`       | 400         | Datos de entrada invalidos                        |
| `INTERNAL_ERROR`         | 500         | Error interno no controlado                       |

#### Estructura del modulo:

El modulo exporta:
1. Un objeto `ERROR_CODES` con las constantes de arriba.
2. Una funcion `crearError(code, mensajeOpcional)` que devuelve un `GraphQLError` con:
   - `message`: descripcion legible en castellano
   - `extensions.code`: el codigo de la tabla
   - `extensions.titulo`: siempre `"Error"`
   - `extensions.subtitulo`: nombre corto del problema en castellano
   - `extensions.texto`: descripcion detallada en castellano

Ejemplo de error construido:

```javascript
// Importar GraphQLError de 'graphql'
const { GraphQLError } = require('graphql');

// El formato de error que el frontend consumira:
new GraphQLError('Credenciales invalidas', {
  extensions: {
    code: 'INVALID_CREDENTIALS',
    titulo: 'Error',
    subtitulo: 'Credenciales invalidas',
    texto: 'El DNI/correo o la contrasena introducidos no son correctos. Por favor, intentelo de nuevo.',
  },
});
```

#### Mapa completo de mensajes (implementar en el modulo):

```
INVALID_CREDENTIALS:
  subtitulo: 'Credenciales invalidas'
  texto: 'El DNI/correo o la contrasena introducidos no son correctos. Por favor, intentelo de nuevo.'

ACCOUNT_DEACTIVATED:
  subtitulo: 'Cuenta desactivada'
  texto: 'Su cuenta ha sido desactivada. Contacte con su centro de rehabilitacion para mas informacion.'

TOKEN_EXPIRED:
  subtitulo: 'Sesion expirada'
  texto: 'Su sesion ha expirado. Por favor, inicie sesion de nuevo.'

TOKEN_INVALID:
  subtitulo: 'Token invalido'
  texto: 'Se ha producido un error de autenticacion. Por favor, inicie sesion de nuevo.'

NETWORK_ERROR:
  subtitulo: 'Error de conexion'
  texto: 'No se ha podido conectar con el servidor. Compruebe su conexion a internet e intentelo de nuevo.'

PATIENT_NOT_FOUND:
  subtitulo: 'Paciente no encontrado'
  texto: 'No se ha encontrado el perfil del paciente. Contacte con su centro de rehabilitacion.'

APPOINTMENT_CONFLICT:
  subtitulo: 'Conflicto de cita'
  texto: 'El horario seleccionado ya esta ocupado. Por favor, elija otro horario.'

APPOINTMENT_NOT_FOUND:
  subtitulo: 'Cita no encontrada'
  texto: 'La cita solicitada no existe o ha sido eliminada.'

VALIDATION_ERROR:
  subtitulo: 'Datos invalidos'
  texto: 'Los datos introducidos no son validos. Por favor, revise los campos e intentelo de nuevo.'

INTERNAL_ERROR:
  subtitulo: 'Error interno'
  texto: 'Se ha producido un error inesperado. Por favor, intentelo de nuevo mas tarde.'
```

#### Checklist Step 12

- [x] `src/utils/errors.js` creado con las 10 definiciones de error
- [x] Funcion `crearError(code, mensajeOpcional)` exportada
- [x] Todos los mensajes en castellano sin tildes (compatibilidad ASCII)
- [x] Cada error incluye `titulo`, `subtitulo`, `texto` en extensions

---

### Step 13: Utilidad de Zona Horaria (`src/utils/timezone.js`)

Crear un modulo que calcule el saludo basado en la zona horaria del dispositivo del paciente.

#### Comportamiento:

1. Recibe un string de zona horaria IANA (ej. `'Europe/Madrid'`, `'America/Mexico_City'`).
2. Calcula la hora local actual en esa zona horaria usando `Intl.DateTimeFormat`.
3. Retorna:
   - `'Buenos dias'` si hora local < 12
   - `'Buenas tardes'` si hora local >= 12 y < 21
   - `'Buenas noches'` si hora local >= 21

#### Exportar:

- `calcularSaludo(timezone)` → string con el saludo
- Si la timezone es invalida o no se proporciona, usar `'Europe/Madrid'` como fallback

**IMPORTANTE:** No instalar `moment-timezone`, `luxon` ni `dayjs` para esto. Node.js 20 incluye soporte nativo de `Intl.DateTimeFormat` con zonas horarias. Usar exclusivamente la API nativa.

#### Checklist Step 13

- [x] `src/utils/timezone.js` creado
- [x] Usa `Intl.DateTimeFormat` nativo de Node.js 20 (sin dependencias externas)
- [x] Tres franjas horarias: manana (<12), tarde (12-20), noche (>=21)
- [x] Fallback a `'Europe/Madrid'` si timezone invalida

---

### Step 14: Cliente HTTP para la API de Java (`src/services/apiClient.js`)

Crear un wrapper sobre `fetch` nativo de Node.js 20 que encapsule todas las llamadas HTTP a la API central de Java.

#### Responsabilidades:

1. **Base URL:** Leer `apiBaseUrl` de `config.js` (default `http://localhost:8080`).
2. **Headers por defecto:** `Content-Type: application/json`, `Accept: application/json`.
3. **Token forwarding:** Aceptar un `javaToken` opcional para inyectar `Authorization: Bearer {token}` en las peticiones autenticadas hacia Java.
4. **Timeout:** 10 segundos por peticion. Si Java no responde en 10s, lanzar error de timeout.
5. **Mapeo de errores:** Traducir codigos HTTP de Java a codigos de error del BFF:

| HTTP Java | Error BFF               |
|-----------|-------------------------|
| 200-299   | Exito (parsear JSON)    |
| 400       | `VALIDATION_ERROR`      |
| 401       | `TOKEN_INVALID`         |
| 403       | `ACCOUNT_DEACTIVATED`   |
| 404       | `PATIENT_NOT_FOUND`     |
| 409       | `APPOINTMENT_CONFLICT`  |
| 500+      | `INTERNAL_ERROR`        |
| Timeout   | `NETWORK_ERROR`         |
| ECONNREFUSED | `NETWORK_ERROR`      |

6. **Logging:** Registrar cada peticion saliente y su resultado con pino (nivel `debug` para exito, `error` para fallos).

#### Metodos exportados:

```javascript
// GET con token opcional
apiClient.get(path, javaToken)

// POST con body y token opcional
apiClient.post(path, body, javaToken)

// PUT con body y token opcional
apiClient.put(path, body, javaToken)

// DELETE con query params y token opcional
apiClient.delete(path, params, javaToken)
```

Todos los metodos devuelven el body JSON parseado en caso de exito, o lanzan un `GraphQLError` construido con `crearError()` en caso de fallo.

#### Modo Mock (`MOCK_API=true`):

Cuando `config.mockApi` es `true`, el apiClient NO realiza peticiones HTTP reales. En su lugar, devuelve datos de prueba estaticos para cada ruta conocida. Esto permite al equipo de frontend desarrollar sin depender de la API de Java.

Datos mock minimos:

- `POST /api/auth/login` → `{ accessToken: 'mock-jwt', refreshToken: 'mock-refresh', rol: 'PATIENT' }`
- `GET /api/pacientes/{dni}` → Paciente de prueba con nombre "Paciente Demo"
- `GET /api/pacientes/{dni}/discapacidades` → Array con 1 discapacidad de ejemplo
- `GET /api/pacientes/{dni}/tratamientos` → Array con 2 tratamientos de ejemplo
- `GET /api/citas?fecha=...` → Array con 1 cita de ejemplo
- `POST /api/citas` → Cita creada de ejemplo
- `DELETE /api/citas?...` → 204 simulado

#### Checklist Step 14

- [x] `src/services/apiClient.js` creado
- [x] Usa `fetch` nativo de Node.js 20 (sin `axios`, sin `node-fetch`)
- [x] Timeout de 10 segundos implementado con `AbortController`
- [x] Mapeo completo de errores HTTP a errores GraphQL
- [x] Modo mock implementado para desarrollo local
- [x] Logger pino integrado para tracing de peticiones

---

### Step 15: Servicio de Autenticacion (`src/services/authService.js`)

Crear el servicio que gestiona la autenticacion del paciente y la emision de JWT propios del BFF.

#### Arquitectura de Tokens (Dual-Token)

El BFF mantiene **dos capas de autenticacion independientes**:

```
App Movil <--[JWT BFF]--> BFF Node.js <--[JWT Java]--> API Java
```

1. **JWT del BFF** (emitido por este servicio):
   - Firmado con `config.jwtSecret` usando HMAC-SHA256
   - Payload del access token: `{ sub: dniPac, tipo: 'access', iat, exp }`
   - Payload del refresh token: `{ sub: dniPac, tipo: 'refresh', iat, exp }`
   - Access token TTL: 30 minutos (`config.jwtExpirationMs`)
   - Refresh token TTL: 7 dias (`config.jwtRefreshMs`)

2. **JWT de Java** (obtenido de la API central):
   - Se almacena en un `Map` en memoria indexado por `dniPac`
   - Se usa internamente para las llamadas HTTP hacia la API de Java
   - Se renueva automaticamente cuando caduca

**NOTA SOBRE ESCALABILIDAD:** El Map en memoria es adecuado para el MVP. En produccion con multiples pods K8s, se migrara a Redis. Si el pod se reinicia, los pacientes deben re-autenticarse. Esto es aceptable.

#### Flujo de Login:

```
1. App envia: login(identifier, password)
2. BFF recibe identifier (puede ser DNI o email)
3. BFF llama a Java API: POST /api/auth/login { dni: identifier, contrasena: password }
   - Si identifier contiene '@', es email -> BFF necesita resolver DNI primero (pendiente endpoint Java)
   - Por ahora, asumir que identifier ES el DNI
4. Java responde: { accessToken, refreshToken, rol }
5. BFF almacena tokens de Java en Map: tokenCache.set(dniPac, { accessToken, refreshToken })
6. BFF genera su propio JWT pair:
   - bffAccessToken = jwt.sign({ sub: dniPac, tipo: 'access' }, jwtSecret, { expiresIn: '30m' })
   - bffRefreshToken = jwt.sign({ sub: dniPac, tipo: 'refresh' }, jwtSecret, { expiresIn: '7d' })
7. BFF retorna al movil: { accessToken: bffAccessToken, refreshToken: bffRefreshToken, expiresAt: timestamp }
```

#### Flujo de Refresh:

```
1. App envia: refreshToken(refreshToken)
2. BFF valida el refresh token del BFF (verificar firma + tipo === 'refresh' + no expirado)
3. BFF extrae dniPac del token
4. BFF comprueba si tiene token Java en cache para ese DNI
5. Si el token Java esta cerca de caducar, BFF llama: POST /api/auth/refresh { refreshToken: javaRefresh }
6. BFF genera nuevo par JWT BFF
7. BFF retorna al movil: { accessToken, refreshToken, expiresAt }
```

#### Metodos exportados:

```javascript
// Autentica paciente y devuelve tokens BFF
authService.login(identifier, password) -> { accessToken, refreshToken, expiresAt }

// Renueva tokens BFF
authService.refresh(refreshToken) -> { accessToken, refreshToken, expiresAt }

// Obtiene el token de Java para un DNI (usado internamente por otros servicios)
authService.obtenerTokenJava(dniPac) -> string | null

// Valida un access token BFF y extrae el payload
authService.validarToken(token) -> { sub, tipo }
```

#### Checklist Step 15

- [x] `src/services/authService.js` creado
- [x] Generacion de JWT con `jsonwebtoken` (HMAC-SHA256)
- [x] Cache en memoria (`Map`) para tokens de Java
- [x] Flujo de login completo (movil -> BFF -> Java -> BFF -> movil)
- [x] Flujo de refresh completo
- [x] Validacion de tokens con manejo de errores (expirado, invalido)
- [x] `expiresAt` devuelto como Unix timestamp en segundos

---

### Step 16: Servicios de Dominio (`src/services/`)

Crear un servicio por cada dominio funcional. Cada servicio encapsula las llamadas HTTP a la API de Java usando `apiClient` y transforma los datos al formato esperado por GraphQL.

**Regla fundamental:** Estos servicios son puentes de traduccion. No contienen logica de negocio clinica. Solo mapean campos, filtran datos innecesarios para la UI, y consolidan multiples llamadas REST en un unico resultado.

#### 16.1: `src/services/patientService.js`

Metodos:

**`obtenerPerfil(dniPac, javaToken)`**
- Llama a: `GET /api/pacientes/{dniPac}`
- Transforma la respuesta Java `PacienteResponse` al tipo GraphQL `Patient`:

| Campo Java (PacienteResponse) | Campo GraphQL (Patient) | Transformacion                              |
|-------------------------------|-------------------------|---------------------------------------------|
| `dniPac`                      | `id`                    | Directo (usar DNI como ID unico)            |
| `dniPac`                      | `dni`                   | Directo                                     |
| `nombrePac`                   | `name`                  | Directo                                     |
| `apellido1Pac + apellido2Pac` | `surname`               | Concatenar con espacio                       |
| `emailPac`                    | `email`                 | Directo                                     |
| `telefonos[0]`                | `phone`                 | Primer telefono del array, o null            |
| `fechaNacimiento`             | `birthDate`             | Formato ISO string (ya viene asi de Java)    |
| *(no disponible)*             | `address`               | Null (la direccion es un objeto relacional)  |
| `activo`                      | `active`                | Directo                                     |

**CRITICO:** Los campos clinicos (`alergias`, `antecedentes`, `medicacionActual`) que devuelve Java **NO se exponen** al movil. El BFF los filtra. La app movil del paciente no necesita ver estos campos sensibles.

**`obtenerDiscapacidades(dniPac, javaToken)`**
- Llama a: `GET /api/pacientes/{dniPac}/discapacidades`
- Transforma `PacienteDiscapacidadResponse[]` al tipo GraphQL `Disability[]`:

| Campo Java                    | Campo GraphQL           | Transformacion             |
|-------------------------------|-------------------------|----------------------------|
| `codDis`                      | `id`                    | Directo                   |
| `nombreDis`                   | `name`                  | Directo                   |
| *(no disponible)*             | `description`           | Null (no viene en el DTO) |
| `idNivel`                     | `currentLevel`          | Directo                   |

**`obtenerProgreso(dniPac, javaToken)`**
- **Endpoint Java pendiente.** Este endpoint aun no existe en la API central.
- En modo mock: devolver datos de ejemplo.
- En produccion: se implementara cuando el pipeline `/data` exponga estadisticas agregadas via la API de Java.
- Tipo de retorno `ProgressSummary`: `{ totalSessions, averageScore, improvementRate, lastSessionDate }`

#### 16.2: `src/services/treatmentService.js`

Metodos:

**`obtenerTratamientos(dniPac, javaToken, filtros)`**
- Llama a: `GET /api/pacientes/{dniPac}/tratamientos`
- Si `filtros.disabilityId` presente: filtrar en el BFF por discapacidad
- Si `filtros.level` presente: filtrar en el BFF por nivel de progresion
- Transforma `PacienteTratamientoResponse[]` al tipo GraphQL `Treatment[]`:

| Campo Java                    | Campo GraphQL           | Transformacion             |
|-------------------------------|-------------------------|----------------------------|
| `codTrat`                     | `id`                    | Directo                   |
| `nombreTrat`                  | `name`                  | Directo                   |
| *(no disponible)*             | `description`           | Null (no viene en el DTO) |
| *(no disponible)*             | `type`                  | `'TEXT_INSTRUCTION'` (default) |
| `visible`                     | `visible`               | Directo                   |
| *(no disponible)*             | `progressionLevel`      | 0 (default, pendiente de enriquecimiento) |

**NOTA:** El DTO `PacienteTratamientoResponse` de Java incluye: `dniPac`, `codTrat`, `nombreTrat`, `visible`, `fechaAsignacion`. Faltan `description`, `type` y `progressionLevel`. Para completar estos campos, se necesitaria una segunda llamada a `GET /api/catalogo/tratamientos/{codTrat}`. Por ahora, usar valores default. El enriquecimiento completo se implementara cuando la API de Java aplane estos datos en un unico endpoint optimizado para movil.

#### 16.3: `src/services/appointmentService.js`

Metodos:

**`obtenerCitas(dniPac, javaToken, filtros)`**
- Llama a: `GET /api/citas?fecha={hoy}` (para citas proximas) o multiples fechas
- Filtra las citas que corresponden al `dniPac`
- Si `filtros.upcoming === true`: solo citas con fecha >= hoy
- Si `filtros.status` presente: filtrar por estado (SCHEDULED, COMPLETED, CANCELLED)
- Transforma `CitaResponse[]` al tipo GraphQL `Appointment[]`:

| Campo Java                    | Campo GraphQL            | Transformacion                          |
|-------------------------------|--------------------------|-----------------------------------------|
| `dniPac+dniSan+fecha+hora`    | `id`                     | Componer ID: `${dniPac}_${dniSan}_${fecha}_${hora}` |
| `fechaCita`                   | `date`                   | Formato ISO string                      |
| `horaCita`                    | `time`                   | Formato HH:mm string                    |
| `dniSan`                      | `practitionerName`       | Requiere 2a llamada a sanitario (o devolver DNI) |
| *(no disponible)*             | `practitionerSpecialty`  | Null (pendiente)                        |
| *(derivado)*                  | `status`                 | `'SCHEDULED'` (default, Java no tiene campo estado) |
| *(no disponible)*             | `notes`                  | Null (las citas Java no tienen notas)   |

**NOTA IMPORTANTE:** La API de Java no tiene un endpoint para listar citas de un PACIENTE. Solo lista por fecha (`GET /api/citas?fecha=`) o por sanitario (`GET /api/citas/sanitario/{dniSan}`). El BFF necesitara obtener las citas y filtrar en memoria por `dniPac`, o solicitar al equipo de Java un endpoint `GET /api/citas/paciente/{dniPac}`. Documentar esta limitacion como dependencia.

**`reservarCita(dniPac, fecha, hora, practitionerId, javaToken)`**
- Llama a: `POST /api/citas` con body `{ dniPac, dniSan: practitionerId, fechaCita: fecha, horaCita: hora }`
- Retorna la cita creada transformada al formato GraphQL

**`cancelarCita(appointmentId, javaToken)`**
- Descomponer `appointmentId` en sus 4 componentes (dniPac, dniSan, fecha, hora)
- Llama a: `DELETE /api/citas?dniPac=X&dniSan=Y&fecha=Z&hora=H`
- Retorna `{ id, status: 'CANCELLED' }`

#### 16.4: `src/services/gameService.js`

Metodos:

**`obtenerSesiones(dniPac, javaToken, limit, offset)`**
- **Endpoint Java pendiente.** El pipeline `/data` almacena sesiones de juego en MongoDB, pero no hay endpoint expuesto en la API de Java para obtenerlas por paciente.
- En modo mock: devolver sesiones de ejemplo
- En produccion: se implementara cuando la API de Java exponga `GET /api/pacientes/{dniPac}/sesiones-juego`

#### Checklist Step 16

- [x] `src/services/patientService.js` creado con `obtenerPerfil`, `obtenerDiscapacidades`, `obtenerProgreso`
- [x] `src/services/treatmentService.js` creado con `obtenerTratamientos`
- [x] `src/services/appointmentService.js` creado con `obtenerCitas`, `reservarCita`, `cancelarCita`
- [x] `src/services/gameService.js` creado con `obtenerSesiones`
- [x] Todos los servicios usan `apiClient` (nunca `fetch` directo)
- [x] Mapeo de campos documentado en comentarios del codigo
- [x] Modo mock funcional para todos los servicios

---

### Step 17: Definir TypeDefs (Esquema GraphQL)

Los TypeDefs definen el contrato publico del BFF hacia la app movil. Cada archivo exporta un string de esquema GraphQL usando `gql` de `graphql-tag`.

#### 17.1: `src/graphql/typeDefs/common.js`

```graphql
# Codigos de error estandarizados (usados en extensions.code de GraphQLError)
enum ErrorCode {
  INVALID_CREDENTIALS
  ACCOUNT_DEACTIVATED
  TOKEN_EXPIRED
  TOKEN_INVALID
  NETWORK_ERROR
  PATIENT_NOT_FOUND
  APPOINTMENT_CONFLICT
  APPOINTMENT_NOT_FOUND
  VALIDATION_ERROR
  INTERNAL_ERROR
}

# Estado de una cita medica
enum AppointmentStatus {
  SCHEDULED
  COMPLETED
  CANCELLED
}
```

#### 17.2: `src/graphql/typeDefs/auth.js`

```graphql
# Respuesta de autenticacion con tokens JWT
type AuthPayload {
  accessToken: String!
  refreshToken: String!
  expiresAt: Int!
}

type Mutation {
  # Autenticacion del paciente con DNI/email y contrasena
  login(identifier: String!, password: String!): AuthPayload!

  # Renovar tokens usando el refresh token
  refreshToken(refreshToken: String!): AuthPayload!
}
```

#### 17.3: `src/graphql/typeDefs/patient.js`

```graphql
# Perfil del paciente (solo campos seguros para la app movil)
type Patient {
  id: ID!
  name: String!
  surname: String!
  email: String
  dni: String!
  phone: String
  birthDate: String
  address: String
  active: Boolean!
}

# Discapacidad asignada al paciente con su nivel actual
type Disability {
  id: ID!
  name: String!
  description: String
  currentLevel: Int!
}

# Resumen de progreso terapeutico del paciente
type ProgressSummary {
  totalSessions: Int!
  averageScore: Float
  improvementRate: Float
  lastSessionDate: String
}

type Query {
  # Perfil del paciente autenticado
  me: Patient!

  # Discapacidades asignadas al paciente autenticado
  myDisabilities: [Disability!]!

  # Resumen de progreso global del paciente
  myProgress: ProgressSummary!
}
```

#### 17.4: `src/graphql/typeDefs/treatment.js`

```graphql
# Tratamiento asignado al paciente
type Treatment {
  id: ID!
  name: String!
  description: String
  type: String!
  visible: Boolean!
  progressionLevel: Int!
}

type Query {
  # Tratamientos del paciente, filtrable por discapacidad y nivel
  myTreatments(disabilityId: ID, level: Int): [Treatment!]!
}
```

#### 17.5: `src/graphql/typeDefs/appointment.js`

```graphql
# Cita medica del paciente
type Appointment {
  id: ID!
  date: String!
  time: String!
  practitionerName: String!
  practitionerSpecialty: String
  status: AppointmentStatus!
  notes: String
}

type Query {
  # Citas del paciente, filtrable por estado y proximas
  myAppointments(status: AppointmentStatus, upcoming: Boolean): [Appointment!]!
}

type Mutation {
  # Reservar una nueva cita
  bookAppointment(date: String!, time: String!, practitionerId: ID!): Appointment!

  # Cancelar una cita existente
  cancelAppointment(appointmentId: ID!): Appointment!
}
```

#### 17.6: `src/graphql/typeDefs/game.js`

```graphql
# Metricas de una sesion de juego terapeutico
type GameMetrics {
  accuracy: Float
  reactionTime: Float
  completionRate: Float
}

# Sesion de juego terapeutico completada
type GameSession {
  id: ID!
  gameName: String!
  playedAt: String!
  score: Float
  duration: Float
  metrics: GameMetrics
}

type Query {
  # Sesiones de juego del paciente con paginacion
  myGameSessions(limit: Int, offset: Int): [GameSession!]!
}
```

#### 17.7: `src/graphql/typeDefs/index.js`

Este modulo importa todos los typeDefs y los fusiona en un unico array para Apollo Server.

- Importar los 6 archivos de typeDefs
- Exportar un array: `[commonTypeDefs, authTypeDefs, patientTypeDefs, treatmentTypeDefs, appointmentTypeDefs, gameTypeDefs]`

Apollo Server 4 acepta un array de DocumentNode y los fusiona automaticamente, incluyendo la extension de `Query` y `Mutation` entre multiples archivos.

#### Checklist Step 17

- [x] `src/graphql/typeDefs/common.js` creado con enums
- [x] `src/graphql/typeDefs/auth.js` creado con AuthPayload y mutations login/refresh
- [x] `src/graphql/typeDefs/patient.js` creado con Patient, Disability, ProgressSummary, queries me/myDisabilities/myProgress
- [x] `src/graphql/typeDefs/treatment.js` creado con Treatment y query myTreatments
- [x] `src/graphql/typeDefs/appointment.js` creado con Appointment, queries y mutations
- [x] `src/graphql/typeDefs/game.js` creado con GameSession, GameMetrics, query myGameSessions
- [x] `src/graphql/typeDefs/index.js` creado, fusiona todos los typeDefs
- [x] El esquema completo coincide exactamente con las queries/mutations definidas en el frontend (`/mobile/frontend/src/services/graphql/`)

---

### Step 18: Implementar Resolvers

Los resolvers implementan la logica de cada query y mutation. Cada resolver recibe el contexto con el usuario autenticado y delega al servicio correspondiente.

#### Firma de un resolver GraphQL:

```javascript
// (parent, args, context, info)
// context contiene: { user: { sub, tipo }, javaToken, greeting, logger }
```

#### 18.1: `src/graphql/resolvers/auth.js`

```javascript
Mutation: {
  // login(identifier, password) -> AuthPayload
  // - NO requiere contexto autenticado (es el punto de entrada)
  // - Llama a authService.login(identifier, password)
  // - En caso de error: lanzar crearError('INVALID_CREDENTIALS')

  // refreshToken(refreshToken) -> AuthPayload
  // - NO requiere contexto autenticado
  // - Llama a authService.refresh(refreshToken)
  // - En caso de error: lanzar crearError('TOKEN_EXPIRED') o crearError('TOKEN_INVALID')
}
```

#### 18.2: `src/graphql/resolvers/patient.js`

```javascript
Query: {
  // me() -> Patient
  // - REQUIERE autenticacion (context.user debe existir)
  // - Obtiene dniPac de context.user.sub
  // - Llama a patientService.obtenerPerfil(dniPac, context.javaToken)

  // myDisabilities() -> [Disability]
  // - REQUIERE autenticacion
  // - Llama a patientService.obtenerDiscapacidades(dniPac, context.javaToken)

  // myProgress() -> ProgressSummary
  // - REQUIERE autenticacion
  // - Llama a patientService.obtenerProgreso(dniPac, context.javaToken)
}
```

#### 18.3: `src/graphql/resolvers/treatment.js`

```javascript
Query: {
  // myTreatments(disabilityId?, level?) -> [Treatment]
  // - REQUIERE autenticacion
  // - Llama a treatmentService.obtenerTratamientos(dniPac, javaToken, { disabilityId, level })
}
```

#### 18.4: `src/graphql/resolvers/appointment.js`

```javascript
Query: {
  // myAppointments(status?, upcoming?) -> [Appointment]
  // - REQUIERE autenticacion
  // - Llama a appointmentService.obtenerCitas(dniPac, javaToken, { status, upcoming })
}

Mutation: {
  // bookAppointment(date, time, practitionerId) -> Appointment
  // - REQUIERE autenticacion
  // - Llama a appointmentService.reservarCita(dniPac, fecha, hora, practitionerId, javaToken)

  // cancelAppointment(appointmentId) -> Appointment
  // - REQUIERE autenticacion
  // - Llama a appointmentService.cancelarCita(appointmentId, javaToken)
}
```

#### 18.5: `src/graphql/resolvers/game.js`

```javascript
Query: {
  // myGameSessions(limit?, offset?) -> [GameSession]
  // - REQUIERE autenticacion
  // - Llama a gameService.obtenerSesiones(dniPac, javaToken, limit, offset)
}
```

#### 18.6: `src/graphql/resolvers/index.js`

Fusiona todos los resolvers usando merge profundo:

```javascript
// Importar todos los resolvers individuales
// Fusionar Query y Mutation de todos los modulos en un unico objeto
// Exportar el objeto fusionado
```

Ejemplo de merge manual (sin lodash):

```javascript
const resolvers = {
  Query: {
    ...patientResolvers.Query,
    ...treatmentResolvers.Query,
    ...appointmentResolvers.Query,
    ...gameResolvers.Query,
  },
  Mutation: {
    ...authResolvers.Mutation,
    ...appointmentResolvers.Mutation,
  },
};
```

#### Proteccion de resolvers autenticados:

Todos los resolvers excepto `login` y `refreshToken` deben verificar que `context.user` existe. Si no existe, lanzar `crearError('TOKEN_INVALID')`.

Implementar esto como una funcion helper `requireAuth(context)` que se invoca al inicio de cada resolver protegido:

```javascript
function requireAuth(context) {
  if (!context.user) {
    throw crearError('TOKEN_INVALID');
  }
  return context.user;
}
```

#### Checklist Step 18

- [x] `src/graphql/resolvers/auth.js` creado con login y refreshToken
- [x] `src/graphql/resolvers/patient.js` creado con me, myDisabilities, myProgress
- [x] `src/graphql/resolvers/treatment.js` creado con myTreatments
- [x] `src/graphql/resolvers/appointment.js` creado con myAppointments, bookAppointment, cancelAppointment
- [x] `src/graphql/resolvers/game.js` creado con myGameSessions
- [x] `src/graphql/resolvers/index.js` creado, fusiona todos los resolvers
- [x] Funcion `requireAuth(context)` implementada y usada en todos los resolvers protegidos
- [x] Cada resolver delega al servicio correspondiente (nunca llama a apiClient directamente)

---

### Step 19: Middleware de Autenticacion (`src/middleware/auth.js`)

Este middleware se ejecuta en cada peticion GraphQL para extraer y validar el JWT del BFF desde la cabecera `Authorization`.

#### Comportamiento:

1. Lee la cabecera `Authorization: Bearer <token>` de la peticion HTTP.
2. Si no hay cabecera: `user = null` (la peticion es anonima, solo login/refresh funcionaran).
3. Si hay cabecera:
   a. Extrae el token (quitar prefijo `Bearer `).
   b. Llama a `authService.validarToken(token)`.
   c. Si el token es valido: `user = { sub: dniPac, tipo: 'access' }`.
   d. Si el token ha expirado: `user = null` (no lanzar error aqui, los resolvers lo manejan).
   e. Si el token es invalido/malformado: `user = null`.
4. Obtiene el token de Java para ese usuario: `javaToken = authService.obtenerTokenJava(dniPac)`.
5. Retorna el objeto de contexto: `{ user, javaToken, logger }`.

#### Integracion con Apollo Server 4:

Este middleware se conecta a traves de la funcion `context` de Apollo Server:

```javascript
// En la configuracion de Apollo Server
context: async ({ req }) => {
  const authContext = await authMiddleware(req);
  const greeting = greetingMiddleware(req);
  return { ...authContext, greeting };
}
```

#### Checklist Step 19

- [x] `src/middleware/auth.js` creado
- [x] Extrae token de cabecera Authorization
- [x] Valida token usando authService.validarToken
- [x] Manejo silencioso de tokens ausentes/invalidos (sin lanzar error)
- [x] Inyecta `user` y `javaToken` en el contexto de Apollo

---

### Step 20: Middleware de Saludo (`src/middleware/greeting.js`)

Este middleware lee la zona horaria del dispositivo del paciente y calcula el saludo apropiado.

#### Comportamiento:

1. Lee la cabecera `X-Timezone` de la peticion HTTP (ej. `'Europe/Madrid'`).
2. Si no esta presente, usa fallback `'Europe/Madrid'`.
3. Llama a `calcularSaludo(timezone)` de `src/utils/timezone.js`.
4. Retorna el string de saludo (ej. `'Buenos dias'`).

#### Uso en resolvers:

El saludo esta disponible en `context.greeting`. Los resolvers que lo necesiten (como `me`) pueden incluirlo en la respuesta. Opcionalmente, se puede exponer como un campo adicional en el tipo `Patient` o como una query separada.

**Recomendacion arquitectonica:** Exponer `greeting` como campo del tipo `Patient` via un field resolver:

```graphql
type Patient {
  # ... campos existentes ...
  greeting: String    # Saludo calculado por el backend
}
```

El field resolver de `Patient.greeting` simplemente devuelve `context.greeting + ' ' + parent.name`.

#### Cabecera en el frontend:

El equipo de frontend debe anadir la cabecera `X-Timezone` en el cliente Apollo. Referencia para el frontend:

```typescript
// En authLink o en un link dedicado
const timezoneLink = setContext(async (_, prevContext) => ({
  ...prevContext,
  headers: {
    ...prevContext.headers,
    'X-Timezone': Intl.DateTimeFormat().resolvedOptions().timeZone,
  },
}));
```

#### Checklist Step 20

- [x] `src/middleware/greeting.js` creado
- [x] Lee cabecera `X-Timezone` de la peticion
- [x] Usa `calcularSaludo()` de `src/utils/timezone.js`
- [x] Fallback a `'Europe/Madrid'`
- [x] Inyecta `greeting` en contexto de Apollo

---

### Step 21: Formateador Global de Errores (`src/middleware/errorFormatter.js`)

Este modulo configura la funcion `formatError` de Apollo Server para garantizar que todos los errores que llegan al cliente tengan la estructura estandarizada.

#### Comportamiento:

1. Recibe el error formateado por Apollo y el error original.
2. Si el error original es un `GraphQLError` con `extensions.code` conocido: lo deja pasar tal cual.
3. Si es un error desconocido o una excepcion de Node.js: lo transforma a `INTERNAL_ERROR`.
4. **NUNCA** expone stack traces, mensajes internos de Node.js, o detalles de la API de Java al cliente movil.
5. En modo `development` (`config.nodeEnv === 'development'`): incluye el stack trace en `extensions.stacktrace` para depuracion.
6. En modo `production`: el `extensions.stacktrace` se elimina siempre.

#### Estructura garantizada de cada error en la respuesta:

```json
{
  "errors": [
    {
      "message": "Descripcion legible en castellano",
      "extensions": {
        "code": "CODIGO_DEL_ERROR",
        "titulo": "Error",
        "subtitulo": "Nombre corto del problema",
        "texto": "Descripcion detallada en castellano"
      }
    }
  ]
}
```

#### Checklist Step 21

- [x] `src/middleware/errorFormatter.js` creado
- [x] Todos los errores desconocidos se convierten a INTERNAL_ERROR
- [x] Nunca se exponen stack traces en produccion
- [x] Stack traces disponibles en desarrollo
- [x] La estructura `titulo/subtitulo/texto` se garantiza en todos los errores

---

### Step 22: Integrar Apollo Server con Express (`src/index.js`)

Modificar el punto de entrada existente para montar Apollo Server 4 sobre Express.

#### Apollo Server 4 + Express:

Apollo Server 4 se integra con Express mediante `expressMiddleware`:

```javascript
const { ApolloServer } = require('@apollo/server');
const { expressMiddleware } = require('@apollo/server/express4');
```

#### Orden de montaje en Express:

```javascript
// 1. Middlewares base (YA EXISTEN)
app.use(express.json());
app.use(pinoHttp({ logger }));

// 2. Endpoints de infraestructura (YA EXISTEN, fuera de GraphQL)
app.get('/health', ...);
app.get('/metrics', ...);

// 3. Apollo Server GraphQL (NUEVO)
const server = new ApolloServer({
  typeDefs,                    // Del Step 17
  resolvers,                   // Del Step 18
  formatError: errorFormatter, // Del Step 21
  introspection: config.nodeEnv !== 'production',
});

await server.start();

app.use(
  config.graphqlPath,          // '/graphql' por defecto
  express.json(),
  expressMiddleware(server, {
    context: async ({ req }) => {
      const authCtx = await authMiddleware(req);
      const greeting = greetingMiddleware(req);
      return { ...authCtx, greeting, logger };
    },
  })
);
```

#### Cambios en el entry point:

1. Convertir el modulo a una funcion asincrona auto-ejecutable (`async IIFE`) porque `server.start()` es asincrono.
2. Los endpoints `/health` y `/metrics` siguen montados como rutas Express normales ANTES de Apollo.
3. Apollo se monta solo en la ruta `/graphql`.
4. El `app.listen()` se ejecuta DESPUES de `server.start()`.

#### Introspection:

- **Desarrollo:** Habilitado (`introspection: true`). Permite explorar el esquema con GraphQL Playground, Apollo Sandbox, etc.
- **Produccion:** Deshabilitado (`introspection: false`). Por seguridad, no exponer el esquema completo.

#### CORS:

El frontend movil se conecta desde `localhost` en desarrollo. Configurar CORS en el middleware de Apollo:

```javascript
const cors = require('cors');
// No instalar cors como dependencia nueva; Express 5 soporta CORS nativamente
// Alternativamente, usar las cabeceras directamente:
app.use(config.graphqlPath, (req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Timezone');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  if (req.method === 'OPTIONS') return res.sendStatus(204);
  next();
});
```

**NOTA:** En produccion, reemplazar `*` con el dominio de la app movil o usar la politica del ingress de K8s.

#### Checklist Step 22

- [x] `@apollo/server` importado e inicializado en `src/index.js`
- [x] `expressMiddleware` monta Apollo en `/graphql`
- [x] Funcion `context` integra auth middleware + greeting middleware
- [x] `server.start()` se ejecuta antes de `app.listen()`
- [x] `/health` y `/metrics` siguen funcionando fuera de GraphQL
- [x] Introspection habilitado solo en desarrollo
- [x] CORS configurado con cabecera `X-Timezone` permitida
- [x] Entry point convertido a async IIFE
- [x] Los exports `{ app, server }` se mantienen para los tests

---

### Step 23: Tests

Extender el archivo de tests existente (`test/bff.test.js`) y crear tests adicionales para la capa GraphQL.

#### 23.1: Tests de infraestructura (ya existen en `test/bff.test.js`)

Los tests existentes (health, metrics, config) deben seguir pasando sin modificacion.

#### 23.2: Nuevo archivo `test/graphql.test.js`

Tests para la capa GraphQL usando peticiones HTTP directas al endpoint `/graphql`:

**Tests de autenticacion:**
- `POST /graphql` con mutation `login` y credenciales validas (modo mock) → retorna `accessToken`, `refreshToken`, `expiresAt`
- `POST /graphql` con mutation `login` y credenciales invalidas → error con `extensions.code === 'INVALID_CREDENTIALS'`
- `POST /graphql` con query `me` sin token → error con `extensions.code === 'TOKEN_INVALID'`
- `POST /graphql` con query `me` y token valido → retorna perfil del paciente

**Tests de estructura de errores:**
- Verificar que todos los errores GraphQL incluyen `extensions.titulo`, `extensions.subtitulo`, `extensions.texto`
- Verificar que no se filtran stack traces en modo produccion (setear `NODE_ENV=production`)

**Tests de saludo:**
- `POST /graphql` con cabecera `X-Timezone: Europe/Madrid` a las 9:00 → `greeting` contiene `'Buenos dias'`
- `POST /graphql` sin cabecera `X-Timezone` → greeting usa fallback `'Europe/Madrid'`

**Tests de queries autenticadas (modo mock):**
- `myDisabilities` → retorna array de discapacidades
- `myTreatments` → retorna array de tratamientos
- `myAppointments` → retorna array de citas

#### 23.3: Ejecutar tests

```bash
npm test    # node --test 'test/*.test.js'
```

Todos los tests deben ejecutarse con `MOCK_API=true` y `LOG_LEVEL=silent`.

#### Checklist Step 23

- [x] Tests existentes en `test/bff.test.js` siguen pasando
- [x] `test/graphql.test.js` creado
- [x] Tests de login exito/fallo
- [x] Tests de proteccion de queries sin token
- [x] Tests de estructura de errores estandarizada
- [x] Tests de saludo por zona horaria
- [x] Tests de queries en modo mock
- [x] `npm test` pasa al 100%

---

### Step 24: Actualizacion de ConfigMap de Kubernetes

Actualizar el ConfigMap existente en `/infra/k8s/base/mobile-backend/configmap.yaml` para incluir las nuevas variables:

```yaml
data:
  NODE_ENV: "production"
  PORT: "3000"
  API_BASE_URL: "http://rehabiapp-api.rehabiapp-api.svc.cluster.local:8080"
  LOG_LEVEL: "info"
  GRAPHQL_PATH: "/graphql"
  JWT_EXPIRATION_MS: "1800000"
  JWT_REFRESH_MS: "604800000"
  MOCK_API: "false"
```

El `JWT_SECRET` NO va en el ConfigMap. Se monta como secreto via CSI SecretProviderClass en `/mnt/secrets/jwt-secret`. Esto debe configurarse en el overlay de AWS.

#### Checklist Step 24

- [x] ConfigMap actualizado con las nuevas variables de entorno
- [x] `JWT_SECRET` NO incluido en ConfigMap (solo en secretos CSI)
- [x] `MOCK_API` establecido a `'false'` en produccion

---

## Resumen de Dependencias con Otros Equipos

| Dependencia | Equipo Responsable | Estado | Impacto |
|-------------|-------------------|--------|---------|
| Endpoint de autenticacion de pacientes | Agent 1 (API Java) | PENDIENTE | Login de la app movil no funciona sin mock |
| Endpoint `GET /api/citas/paciente/{dniPac}` | Agent 1 (API Java) | PENDIENTE | Citas del paciente requieren filtrado en BFF |
| Endpoint de sesiones de juego por paciente | Agent 1 (API Java + Data) | PENDIENTE | Pantalla de juegos no funciona sin mock |
| Endpoint de progreso/estadisticas del paciente | Agent 1 (API Java + Data) | PENDIENTE | Resumen de progreso no funciona sin mock |
| Nombre del sanitario en respuesta de citas | Agent 1 (API Java) | PENDIENTE | Citas muestran DNI en lugar de nombre |
| Cabecera `X-Timezone` en Apollo Client | Agent 2 (Frontend) | PENDIENTE | Saludo usa fallback hasta que se implemente |

---

## Checklist Global

### Fase A: Infraestructura Base (COMPLETADA)

- [x] Step 1: Proyecto Node.js inicializado con Express
- [x] Step 2: Endpoint /health retorna 200 OK
- [x] Step 3: Logging estructurado JSON con pino
- [x] Step 4: Endpoint /metrics para Prometheus
- [x] Step 5: Utilidad de lectura de secretos CSI
- [x] Step 6: Compatibilidad filesystem read-only
- [x] Step 7: Dockerfile compatible
- [x] Step 8: .dockerignore creado
- [x] Step 9: Containerizacion y manifiestos K8s

### Fase B: Capa GraphQL (PENDIENTE)

- [x] Step 10: Dependencias GraphQL instaladas
- [x] Step 11: Configuracion extendida con JWT y GraphQL
- [x] Step 12: Codigos de error centralizados
- [x] Step 13: Utilidad de zona horaria
- [x] Step 14: Cliente HTTP para API de Java
- [x] Step 15: Servicio de autenticacion con JWT propio
- [x] Step 16: Servicios de dominio (patient, treatment, appointment, game)
- [x] Step 17: TypeDefs GraphQL completos
- [x] Step 18: Resolvers GraphQL completos
- [x] Step 19: Middleware de autenticacion JWT
- [x] Step 20: Middleware de saludo por zona horaria
- [x] Step 21: Formateador global de errores
- [x] Step 22: Integracion Apollo Server + Express
- [x] Step 23: Tests (infraestructura + GraphQL)
- [x] Step 24: ConfigMap K8s actualizado

### Fase C: Correccion de errores de login y observabilidad en desarrollo

- [x] Step 25: Corregir puerto del frontend GraphQL (CRITICO)
- [x] Step 26: Configurar script `npm run dev` con MOCK_API activado
- [x] Step 27: Activar pino-pretty en modo desarrollo
- [x] Step 28: Eliminar instancias duplicadas de pino en el backend
- [x] Step 29: Verificar flujo completo login admin en terminal

### Fase D: Login inalcanzable desde dispositivo movil

- [ ] Step 30: Resolver URL dinamica del backend con expo-constants (CRITICO)
- [ ] Step 31: Mover fetchProfile fuera del handler de login
- [ ] Step 32: Parsear errores en userStore.fetchProfile
- [ ] Step 33: Verificar flujo completo login desde dispositivo movil

---

## Phase C: Correccion de Errores de Login y Observabilidad (PENDIENTE)

### Diagnostico del Agente 2 Pensador (2026-04-04)

Se ha reportado un error al iniciar sesion con el usuario `admin` cuyo sintoma es:
**el error no se refleja en la terminal del backend ni del frontend.**

Tras revisar el flujo completo desde el boton "Iniciar Sesion" hasta el mock del BFF, se identificaron **cuatro defectos encadenados** que, combinados, provocan que el error sea invisible:

#### Defecto 1 — Puerto incorrecto en el frontend (CAUSA RAIZ)

**Archivo:** `/mobile/frontend/src/services/graphql/client.ts`, linea 10

```javascript
const GRAPHQL_URI = 'http://localhost:4000/graphql';
```

El backend escucha en el **puerto 3000** (definido en `config.js` y en el ConfigMap de K8s). El frontend apunta al **puerto 4000**, que no tiene ningun proceso escuchando.

**Consecuencia:** Apollo Client nunca llega a conectar con el BFF. La mutacion `login` produce un error de red (ECONNREFUSED o similar) que es capturado por el `onError` link del frontend, pero como el backend nunca recibe la peticion, no hay log alguno en la terminal del backend. En la terminal de Metro/Expo aparece un `[Red] Error de conexion en "Login"` pero puede confundirse con ruido de red del entorno Expo.

#### Defecto 2 — Script `npm run dev` no activa el modo mock

**Archivo:** `/mobile/backend/package.json`, linea 8

```json
"dev": "node --watch src/index.js"
```

Sin la variable `MOCK_API=true`, `config.mockApi` evalua a `false` (comparacion estricta `process.env.MOCK_API === 'true'` en `config.js`). Esto provoca que `authService.login()` intente autenticar contra la API de Java real en `http://localhost:8080`, que no esta levantada en desarrollo local. El resultado es un `NETWORK_ERROR` o `INVALID_CREDENTIALS` lanzado dentro de un `try/catch` que si se loguea — pero solo si el frontend consiguiera conectar, cosa que no ocurre por el Defecto 1.

#### Defecto 3 — Logs JSON en bruto no son legibles en terminal

**Archivo:** `/mobile/backend/package.json`

pino genera JSON estructurado por defecto:
```
{"level":40,"time":1712345678000,"code":"INVALID_CREDENTIALS","msg":"Error GraphQL del BFF enviado al cliente"}
```

Este formato es correcto para Kubernetes (Fluentd/Loki lo parsea), pero en desarrollo local es practicamente ilegible. El proyecto ya tiene `pino-pretty` como devDependency pero el script `dev` no lo utiliza. Si un error llega al backend, el desarrollador no lo ve porque la linea JSON pasa desapercibida entre el resto del output.

#### Defecto 4 — Instancias duplicadas de pino

Los archivos `errorFormatter.js` y `apiClient.js` crean cada uno su propio `const logger = pino(...)` independiente del logger de `index.js`. Esto no causa el bug directamente, pero fragmenta la configuracion de logging: si se cambia el LOG_LEVEL en una instancia, las demas no se enteran. Ademas, cada instancia abre su propio stream de escritura a stdout.

---

### Step 25: Corregir puerto del frontend GraphQL (CRITICO)

**Objetivo:** Que el Apollo Client del frontend apunte al mismo puerto donde el BFF escucha.

**Archivo a modificar:** `/mobile/frontend/src/services/graphql/client.ts`

**Cambio exacto:**

```javascript
// ANTES (linea 10):
const GRAPHQL_URI = 'http://localhost:4000/graphql';

// DESPUES:
const GRAPHQL_URI = 'http://localhost:3000/graphql';
```

**Justificacion:** El BFF escucha en el puerto 3000 (valor por defecto en `config.js`, confirmado en el ConfigMap de K8s). El puerto 4000 no corresponde a ningun servicio del ecosistema.

**Nota para el implementador:** No crear un sistema de variables de entorno ni constantes de configuracion para esto. Es un literal que debe coincidir con el puerto del backend. En produccion la URL la inyecta el sistema de build de Expo, pero eso es scope de la Fase 5 (no ahora).

---

### Step 26: Configurar script `npm run dev` con MOCK_API activado

**Objetivo:** Que el script de desarrollo active el modo mock automaticamente, sin que el desarrollador tenga que recordar poner la variable manualmente.

**Archivo a modificar:** `/mobile/backend/package.json`

**Cambio exacto en la seccion scripts:**

```json
"dev": "MOCK_API=true LOG_LEVEL=debug node --watch src/index.js | npx pino-pretty"
```

Este unico cambio resuelve tres problemas de golpe:
1. `MOCK_API=true` — activa los datos mock sin necesidad de la API de Java.
2. `LOG_LEVEL=debug` — en desarrollo, mostrar todos los mensajes de debug (peticiones mock entrantes/salientes, tokens generados, etc.).
3. `| npx pino-pretty` — convierte el JSON de pino en output coloreado y legible para humanos.

**Verificacion:** Tras el cambio, ejecutar `npm run dev` y confirmar que la linea de arranque muestra:

```
[HH:MM:SS] INFO: BFF mobile-backend iniciado
    port: 3000
    env: "development"
    mockApi: true
```

**No modificar el script `start`** — ese es para produccion y debe mantener el JSON crudo para Kubernetes.

---

### Step 27: Activar pino-pretty en modo desarrollo (ya resuelto en Step 26)

Este step queda cubierto por el pipe `| npx pino-pretty` del Step 26. El implementador solo debe verificar que `pino-pretty` sigue siendo devDependency en `package.json` (actualmente lo es: `"pino-pretty": "^13.1.3"`).

No se necesita codigo adicional. Marcar como completado si el Step 26 funciona correctamente.

---

### Step 28: Eliminar instancias duplicadas de pino en el backend

**Objetivo:** Centralizar el logging en una unica instancia de pino compartida por todo el backend.

**Problema actual:** Los archivos `src/middleware/errorFormatter.js` y `src/services/apiClient.js` crean cada uno `const logger = pino({ level: process.env.LOG_LEVEL || 'info' })`. Esto significa que hay 3 instancias independientes de pino (la de `index.js`, la de `errorFormatter.js` y la de `apiClient.js`).

**Solucion:**

1. **Crear un modulo de logger compartido** en `src/logger.js`:

```javascript
// Logger centralizado del BFF
// Unica instancia de pino para todo el backend — importar desde aqui
'use strict';

const pino = require('pino');

const logger = pino({ level: process.env.LOG_LEVEL || 'info' });

module.exports = logger;
```

2. **Reemplazar las instancias locales** en estos archivos:

- `src/index.js`: sustituir `const logger = pino({ level: ... })` por `const logger = require('./logger')`
- `src/middleware/errorFormatter.js`: sustituir `const pino = require('pino'); ... const logger = pino(...)` por `const logger = require('../logger')`
- `src/services/apiClient.js`: sustituir `const pino = require('pino'); ... const logger = pino(...)` por `const logger = require('../logger')`

3. **Eliminar** los `require('pino')` locales que queden sin usar en esos tres archivos.

4. **No tocar** el `require('pino-http')` en `index.js` — ese es un middleware de Express que necesita su propia configuracion y debe seguir recibiendo el logger compartido como opcion:

```javascript
const logger = require('./logger');
app.use(pinoHttp({ logger }));
```

**Resultado esperado:** Una sola instancia de pino controla todo el output. Si se cambia LOG_LEVEL, afecta a todo uniformemente.

---

### Step 29: Verificar flujo completo login admin en terminal

**Objetivo:** Confirmar que, tras los steps 25-28, el login con el usuario admin funciona y que tanto exitos como errores son visibles en las terminales.

**Procedimiento (el implementador debe ejecutar, NO automatizar):**

1. Arrancar el backend:
```bash
cd mobile/backend
npm run dev
```
Confirmar que aparece `mockApi: true` en la linea de arranque.

2. En otra terminal, probar login exitoso:
```bash
curl -s -X POST http://localhost:3000/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"mutation { login(identifier: \"admin\", password: \"admin\") { accessToken refreshToken expiresAt } }"}' | jq .
```
Confirmar que devuelve `data.login.accessToken` (string JWT) y que la terminal del backend muestra el log de la peticion mock a nivel debug.

3. Probar login fallido (contrasena incorrecta):
```bash
curl -s -X POST http://localhost:3000/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"mutation { login(identifier: \"admin\", password: \"mala\") { accessToken } }"}' | jq .
```
Confirmar que devuelve un error con `extensions.code: "INVALID_CREDENTIALS"` y que la terminal del backend muestra:
```
WARN: Error GraphQL del BFF enviado al cliente
    code: "INVALID_CREDENTIALS"
```

4. Ejecutar los tests:
```bash
npm test
```
Confirmar que siguen pasando los 16 tests (6 infra + 10 GraphQL).

5. Si el frontend esta disponible, arrancar Expo y probar el login desde la app:
```bash
cd mobile/frontend
npm start
```
Al hacer login con `admin`/`admin`, confirmar que no aparecen errores en la terminal de Metro y que la app navega al dashboard. Al hacer login con contrasena incorrecta, confirmar que aparece el popup de error Y que el log `[GraphQL] INVALID_CREDENTIALS en "Login"` se muestra en la terminal de Metro.

**Criterio de exito:** Los 5 puntos del procedimiento pasan. Marcar como completado.

---

## Phase D: Login Inalcanzable desde Dispositivo Movil (PENDIENTE)

### Diagnostico del Agente 2 Pensador (2026-04-04)

El usuario reporta que **no puede iniciar sesion desde la app movil**. El backend funciona correctamente (verificado con curl en localhost). El problema es exclusivamente del frontend y su capacidad de conectar con el backend.

Se identificaron **3 defectos** que, combinados, impiden el login y ocultan los errores:

#### Defecto 1 — URL `localhost` inalcanzable desde dispositivos moviles (CAUSA RAIZ)

**Archivo:** `/mobile/frontend/src/services/graphql/client.ts`, linea 10

```typescript
const GRAPHQL_URI = 'http://localhost:3000/graphql';
```

`localhost` en un telefono fisico resuelve a **el propio telefono**, no a la maquina de desarrollo. En un emulador Android, `localhost` resuelve al emulador, no al host. Solo funciona en iOS Simulator (comparte stack de red con macOS) y en el navegador web.

| Plataforma | `localhost` funciona | URL que debe usarse |
|------------|---------------------|---------------------|
| Navegador web | Si | `localhost:3000` |
| iOS Simulator | Si | `localhost:3000` |
| Android Emulator | **No** | `10.0.2.2:3000` |
| Telefono fisico (WiFi) | **No** | IP LAN del PC (ej: `192.168.1.X:3000`) |

**Consecuencia:** Apollo Client envia la mutacion `login` a una direccion inalcanzable. La conexion TCP falla inmediatamente (ECONNREFUSED) o se queda colgada hasta timeout. El usuario ve el spinner de carga indefinidamente o un error de red generico.

**Solucion:** Usar `expo-constants` para detectar automaticamente la IP del servidor de desarrollo de Expo (que ya es la IP LAN del PC). Expo CLI la inyecta en `Constants.expoConfig.hostUri` al arrancar. En produccion, usar la URL real del backend.

#### Defecto 2 — Condicion de carrera: AuthGuard navega antes de que fetchProfile termine

**Archivos:**
- `/mobile/frontend/app/(auth)/login.tsx`, linea 37-38
- `/mobile/frontend/app/_layout.tsx`, linea 30-33 (AuthGuard)

```typescript
// login.tsx — flujo actual:
await login(credentials);        // 1. Exito → isAuthenticated = true
await fetchProfile();            // 2. Pero AuthGuard ya navego a /(tabs)
```

Cuando `login()` termina con exito, `authStore` actualiza `isAuthenticated = true`. El `AuthGuard` en `_layout.tsx` observa este cambio via `useEffect` y ejecuta `router.replace('/(tabs)')` **inmediatamente**, desmontando el componente `LoginScreen`. En ese momento, `fetchProfile()` todavia esta ejecutandose. Hay dos consecuencias:

1. El perfil del paciente (`patient`) queda como `null` en el `userStore`. La pantalla de inicio muestra "Paciente" como nombre generico en lugar de "Admin".
2. Si `fetchProfile()` falla (por ejemplo porque el `me` query devuelve un error), el `catch` intenta ejecutar `showError(err)` en un componente ya desmontado, lo que puede producir un warning de React o un error silencioso.

**Solucion:** Cargar el perfil DESPUES de la navegacion, no antes. Mover `fetchProfile()` fuera del `handleLogin()` y ejecutarlo en la pantalla de inicio `(tabs)/index.tsx` con un `useEffect`.

#### Defecto 3 — `fetchProfile()` lanza errores crudos sin parsear

**Archivo:** `/mobile/frontend/src/store/userStore.ts`, lineas 16-18

```typescript
} catch (err) {
  set({ isLoading: false });
  throw err;  // Error crudo de Apollo, no parseado
}
```

A diferencia de `authStore.login()` que usa `parseGraphQLError(err)` para convertir el error de Apollo en un `AppError` con `{title, subtitle, message, code}`, el `userStore.fetchProfile()` lanza el error tal cual. Cuando este error llega al `catch` de `login.tsx` y se pasa a `showError(err)`, el `ErrorPopup` intenta acceder a `error.title`, `error.subtitle` y `error.message` — propiedades que **no existen** en un `ApolloError` crudo. El popup muestra campos vacios o `undefined`.

**Solucion:** Parsear el error en `userStore` igual que en `authStore`.

---

### Step 30: Resolver URL dinamica del backend con expo-constants (CRITICO)

**Objetivo:** Que el Apollo Client del frontend conecte automaticamente al backend correcto independientemente de la plataforma (emulador, telefono fisico, web).

**Archivo a modificar:** `/mobile/frontend/src/services/graphql/client.ts`

**Verificar dependencia:** `expo-constants` viene incluido por defecto en proyectos Expo. Si por alguna razon no esta, instalar con `npx expo install expo-constants`.

**Cambio exacto:** Reemplazar las lineas 1-14 del archivo por:

```typescript
import { ApolloClient, InMemoryCache, createHttpLink, from } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import { onError } from '@apollo/client/link/error';
import * as SecureStore from 'expo-secure-store';
import Constants from 'expo-constants';
import type { AuthToken } from '../../types/auth';

const TOKEN_KEY = 'auth_token';

// URL del backend BFF — se resuelve automaticamente segun la plataforma:
// - En desarrollo: usa la IP del servidor Expo (misma IP LAN del PC)
// - En produccion: usar la URL real del backend (cambiar el fallback)
const DEV_HOST = Constants.expoConfig?.hostUri?.split(':')[0] ?? 'localhost';
const GRAPHQL_URI = __DEV__
  ? `http://${DEV_HOST}:3000/graphql`
  : 'https://mobile-bff.rehabiapp.com/graphql';

const httpLink = createHttpLink({
  uri: GRAPHQL_URI,
});
```

**Como funciona:**

1. `Constants.expoConfig.hostUri` contiene la IP y puerto del servidor de desarrollo de Expo (ej: `192.168.1.42:8081`).
2. `split(':')[0]` extrae solo la IP: `192.168.1.42`.
3. Se construye la URL del BFF con esa IP: `http://192.168.1.42:3000/graphql`.
4. Esta IP es la misma en la que corre el backend (`npm run dev`), porque ambos procesos corren en la misma maquina.
5. `__DEV__` es una variable global de React Native que es `true` en desarrollo y `false` en builds de produccion.
6. Si `hostUri` no esta disponible (caso raro), cae a `localhost` como antes — funciona al menos en web y iOS Simulator.

**El resto del archivo (authLink, errorLink, ApolloClient) NO se modifica.** Solo cambian las lineas de import y construccion de la URL.

**Nota:** NO usar variables de entorno manuales (`EXPO_PUBLIC_*`). La solucion con `expo-constants` es automatica y no requiere configuracion del desarrollador.

---

### Step 31: Mover fetchProfile fuera del handler de login

**Objetivo:** Eliminar la condicion de carrera entre AuthGuard y fetchProfile.

**Principio:** El login solo debe encargarse de autenticar. La carga de datos del paciente es responsabilidad de la pantalla que los necesita.

#### 31.1: Simplificar handleLogin en login.tsx

**Archivo a modificar:** `/mobile/frontend/app/(auth)/login.tsx`

**Cambio en la funcion `handleLogin`:**

```typescript
async function handleLogin() {
  if (!identifier.trim() || !password.trim()) return;

  const credentials: LoginCredentials = {
    identifier: identifier.trim(),
    password: password,
  };

  try {
    await login(credentials);
    // La navegacion se gestiona automaticamente por el AuthGuard
    // El perfil se carga en la pantalla de inicio
  } catch (err: any) {
    const appError = parseGraphQLError(err);
    showError(appError);
  }
}
```

**Cambios clave:**
1. Eliminar `await fetchProfile()` — ya no se llama aqui.
2. Eliminar `const fetchProfile = useUserStore(...)` del componente (import innecesario).
3. Eliminar `import { useUserStore }` si ya no se usa en el componente.
4. Envolver `err` con `parseGraphQLError()` antes de pasarlo a `showError()` para garantizar que siempre es un `AppError`.
5. Anadir `import { parseGraphQLError } from '../../src/utils/errorHandler';` al principio del archivo.

#### 31.2: Cargar perfil en la pantalla de inicio

**Archivo a modificar:** `/mobile/frontend/app/(tabs)/index.tsx`

**Anadir al principio del componente `HomeScreen`, justo despues de la linea donde se obtiene `patient` del store:**

```typescript
import { useEffect } from 'react';
// ... otros imports existentes ...

export default function HomeScreen() {
  const router = useRouter();
  const patient = useUserStore(function (s) { return s.patient; });
  const fetchProfile = useUserStore(function (s) { return s.fetchProfile; });
  const patientName = patient?.name ?? 'Paciente';

  // Cargar perfil del paciente al montar la pantalla de inicio
  useEffect(function () {
    if (!patient) {
      fetchProfile().catch(function (err) {
        console.error('[Inicio] Error al cargar perfil:', err);
      });
    }
  }, []);

  // ... resto del componente sin cambios ...
}
```

**Logica:** Si no hay datos de paciente en el store (primera carga), se llama a `fetchProfile()`. Si falla, se loguea el error en consola pero no se bloquea la pantalla — el usuario ve "Paciente" como nombre y puede reintentar navegando a la pestana de perfil.

---

### Step 32: Parsear errores en userStore.fetchProfile

**Objetivo:** Que todos los errores lanzados por los stores tengan la estructura `AppError` que el `ErrorPopup` espera.

**Archivo a modificar:** `/mobile/frontend/src/store/userStore.ts`

**Cambio en el catch de fetchProfile:**

```typescript
import { create } from 'zustand';
import type { UserState } from '../types/user';
import { client } from '../services/graphql/client';
import { GET_MY_PROFILE } from '../services/graphql/queries/user';
import { parseGraphQLError } from '../utils/errorHandler';

export const useUserStore = create<UserState>(function (set) {
  return {
    patient: null,
    isLoading: false,

    fetchProfile: async function (): Promise<void> {
      set({ isLoading: true });
      try {
        const { data } = await client.query({ query: GET_MY_PROFILE });
        set({ patient: data.me, isLoading: false });
      } catch (err) {
        set({ isLoading: false });
        throw parseGraphQLError(err);
      }
    },

    clearProfile: function (): void {
      set({ patient: null });
    },
  };
});
```

**Cambio unico:** Reemplazar `throw err;` por `throw parseGraphQLError(err);` y anadir el import de `parseGraphQLError`.

---

### Step 33: Verificar flujo completo login desde dispositivo movil

**Objetivo:** Confirmar que el login funciona desde un telefono fisico conectado al mismo WiFi.

**Procedimiento:**

1. Arrancar el backend:
```bash
cd mobile/backend
npm run dev
```
Confirmar que muestra `mockApi: true` y el puerto 3000.

2. Arrancar el frontend:
```bash
cd mobile/frontend
npm start
```
Expo mostrara un QR y una URL tipo `exp://192.168.X.X:8081`. Anotar la IP.

3. Abrir la app en el movil (escanear QR con Expo Go). En la terminal de Metro, confirmar que aparece la IP resuelta:
```
Logs for your project will appear below...
```

4. En la pantalla de login, introducir `admin` / `admin` y pulsar "Iniciar Sesion".

5. **Resultado esperado:**
   - La terminal del backend muestra la peticion `POST /graphql` con estado 200.
   - La app navega a la pantalla de inicio con globos flotantes.
   - El saludo muestra "Buenos dias, Admin" (o tardes/noches segun la hora).
   - No hay errores en la terminal de Metro.

6. Probar login con contrasena incorrecta (`admin` / `mala`):
   - La terminal del backend muestra `WARN: Error GraphQL del BFF ... INVALID_CREDENTIALS`.
   - La terminal de Metro muestra `[GraphQL] INVALID_CREDENTIALS en "Login"`.
   - La app muestra un popup con "Credenciales invalidas".

7. Ejecutar los tests del backend (no deben romperse):
```bash
cd mobile/backend
npm test
```

**Criterio de exito:** Los 7 puntos pasan. Marcar como completado.

---

## Nota Sprint Progreso (2026-04-27)

Smoke test PDF tras 8.5 GET documento. Sin nuevos endpoints en este sprint.

- [ ] Smoke test manual: `treatmentDocument` GraphQL query devuelve PDF binario una vez `/api/pacientes/{dni}/tratamientos/{cod}/documento` esté implementado por /api Phase 8.

---
