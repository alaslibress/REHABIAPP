# RehabiAPP Mobile BFF — GraphQL API

Backend-For-Frontend de la app movil de pacientes. Apollo Server 4 sobre Express 5.

Endpoint GraphQL: `POST /graphql`. Autenticacion via cabecera `Authorization: Bearer <accessToken>` (JWT del BFF, distinto del JWT de la API Java). Cabecera opcional `X-Timezone` para el saludo.

Para detalles de arquitectura ver `CLAUDE.md`. Para el plan operativo ver `PLAN.md`.

## Phase E — Treatment PDF, juegos y dashboard

Las queries y mutaciones añadidas en Phase E proxifican nuevos endpoints del API Java (`/api`) y los exponen al frontend movil via GraphQL.

### Query `treatmentPdf(codTrat: String!): TreatmentPdfPayload`

Descarga el PDF de protocolo de un tratamiento. El BFF llama a `GET /api/tratamientos/{cod}/pdf` (que devuelve binario `application/pdf`), serializa los bytes a base64 y los devuelve. Limite duro de 10 MB.

```graphql
query {
  treatmentPdf(codTrat: "TRT001") {
    codTrat
    filename       # "TRT001.pdf"
    sizeBytes      # tamano en bytes
    base64Content  # decodificar y escribir a expo-file-system
  }
}
```

Errores: `TOKEN_INVALID`, `VALIDATION_ERROR` (sin PDF asociado o supera 10 MB), `NETWORK_ERROR`.

### Query `availableGames: [Game!]!`

Lista de videojuegos terapeuticos desbloqueados para el paciente autenticado. Construida a partir de `juegosDesbloqueados` del dashboard, filtrando los `desbloqueado=true`.

```graphql
query {
  availableGames {
    idVideojuego
    codigo
    nombre
    descripcion
    codDis
    parteCuerpo
    urlUnity
  }
}
```

### Mutation `startGame(idVideojuego: ID!): GameSessionLaunch!`

Solicita el lanzamiento de un videojuego. El BFF:

1. Verifica que el videojuego este en la lista de desbloqueados del paciente.
2. Firma un JWT efimero (5 minutos, scope `GAMES_PLAY`) con la clave del BFF.
3. Devuelve la URL Unity, el token efimero y `expiresAt` en epoch seconds.

```graphql
mutation {
  startGame(idVideojuego: "1") {
    urlUnity         # URL absoluta del juego Unity en S3/CloudFront
    ephemeralToken   # JWT, inyectar como query param en la WebView
    expiresAt        # epoch seconds (TTL = 300s desde ahora)
  }
}
```

Payload del JWT efimero: `{ sub: dniPac, tipo: 'access', scope: 'GAMES_PLAY', idVideojuego, iat, exp }`.

Errores: `TOKEN_INVALID`, `VALIDATION_ERROR` (juego no desbloqueado o no existe).

### Query `myProgress: PatientProgress!`

Progreso del paciente agrupado por tratamiento. Proxy a `GET /api/pacientes/{dni}/progreso` (que delega en el pipeline `/data` MongoDB). Estructura compatible con `react-native-chart-kit`.

```graphql
query {
  myProgress {
    lastUpdate           # ISO timestamp del punto mas reciente
    tratamientos {
      codTrat
      tratamientoNombre
      parteCuerpo
      metricaNombre
      baselineValor
      baselineFecha
      currentValor
      currentFecha
      deltaPorcentaje
      entradas { fecha valor }
    }
  }
}
```

> Cambio de contrato: en Phase 4 `myProgress` devolvia `ProgressSummary`. Ahora devuelve `PatientProgress` con la estructura por tratamiento.

### Query `myDashboard: Dashboard!`

Vista agregada del dashboard. Proxy a `GET /api/pacientes/{dni}/dashboard`.

```graphql
query {
  myDashboard {
    paciente { dniPac nombrePac apellido1Pac apellido2Pac edadPac }
    discapacidadesActivas { codDis nombreDis idNivelActual nombreNivelActual ordenNivelActual }
    tratamientosVisibles { codTrat nombreTrat idNivel ordenNivel }
    juegosDesbloqueados { idVideojuego codigo nombre urlUnity parteCuerpo desbloqueado }
    ultimaSesionJuego { idSesion nombreJuego fechaInicio duracionSegundos score }
    proximaCita { dniSanitario fecha hora }
  }
}
```

## Modo mock

`MOCK_API=true` activa datos sinteticos en `apiClient.js` (paciente `12345678Z` / password `admin`). Util para desarrollo local sin la API Java arrancada. Incluye mocks para dashboard, progreso y descarga de PDF (cabecera `%PDF-` valida).

## Tests

```bash
npm test   # node:test (sin jest)
```

Cubre los 5 nuevos resolvers de Phase E (treatmentPdf, availableGames, startGame, myProgress, myDashboard) mas los flujos previos.
