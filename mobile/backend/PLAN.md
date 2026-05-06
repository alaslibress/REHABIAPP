# PLAN.md — Mobile Backend BFF: Phase G (Frontend ↔ BFF Schema Sync)

> **Author:** Agent 2 Thinker (Opus). PRESCRIPTIVE.
> **Domain:** `/mobile/backend/`.
> **Branch:** `stats-implementation`.
> **Date:** 2026-05-05.
> **Coordinator:** `/mobile/PLAN.md` §2 — Phase G blocks all other mobile work.
> **Pass condition:** `npm test` green AND every Apollo query/mutation issued by `mobile/frontend/src/services/graphql/{queries,mutations}/*.ts` validates against the BFF schema (no `GRAPHQL_VALIDATION_FAILED`).
> **Doer model:** Sonnet. Read every section in order, do NOT skip steps, do NOT improvise.

---

## 0. CONTEXT

The frontend currently emits these GraphQL operations (file → operation):

| File                                                       | Operation                | Status vs. current schema                                              |
|------------------------------------------------------------|--------------------------|------------------------------------------------------------------------|
| `mutations/auth.ts` `LOGIN_MUTATION`                       | `login`                  | OK                                                                     |
| `mutations/auth.ts` `REFRESH_TOKEN_MUTATION`               | `refreshToken`           | OK                                                                     |
| `queries/user.ts` `GET_MY_PROFILE`                         | `me`                     | **MISSING FIELDS** `numSs`, `sexo`, `avatarDataUri`                     |
| `queries/user.ts` `GET_MY_DISABILITIES`                    | `myDisabilities`         | OK                                                                     |
| `queries/user.ts` `GET_MY_PROGRESS`                        | `myProgress`             | **TYPE MISMATCH** (frontend asks for `totalSessions/averageScore/improvementRate/lastSessionDate`; BFF returns `tratamientos[]/lastUpdate`) |
| `queries/treatments.ts` `GET_MY_TREATMENTS`                | `myTreatments`           | **MISSING FIELDS** `codTrat`, `disabilityCode`, `summary`, `materials`, `medication`, `documentUrl`, `hasDocument` |
| `queries/treatments.ts` `GET_TREATMENT_DOCUMENT`           | `treatmentDocument`      | **DOES NOT EXIST**                                                     |
| `queries/progress.ts` `GET_MY_BODY_PART_PROGRESS`          | `myBodyPartProgress`     | **DOES NOT EXIST**                                                     |
| `queries/progress.ts` `GET_BODY_PART_METRICS`              | `bodyPartMetrics`        | **DOES NOT EXIST**                                                     |
| `queries/games.ts` `GET_MY_ASSIGNED_GAMES`                 | `myAssignedGames`        | **DOES NOT EXIST**                                                     |
| `queries/games.ts` `GET_MY_GAME_SESSIONS`                  | `myGameSessions`         | OK                                                                     |
| `queries/appointments.ts` `GET_MY_APPOINTMENTS`            | `myAppointments`         | OK                                                                     |
| `mutations/appointments.ts` `BOOK_APPOINTMENT`             | `bookAppointment`        | OK                                                                     |
| `mutations/appointments.ts` `CANCEL_APPOINTMENT`           | `cancelAppointment`      | OK                                                                     |
| `mutations/appointments.ts` `REQUEST_APPOINTMENT`          | `requestAppointment`     | **DOES NOT EXIST**                                                     |
| `mutations/settings.ts` `REGISTER_DEVICE_TOKEN`            | `registerDeviceToken`    | **DOES NOT EXIST**                                                     |
| `mutations/settings.ts` `UNREGISTER_DEVICE_TOKEN`          | `unregisterDeviceToken`  | **DOES NOT EXIST**                                                     |

The plan below brings the schema in line with every red-marked operation. **The frontend queries are not modified.** All new fields can be served from existing `apiClient.js` mock data plus deterministic synthetic values where the data does not yet exist on the Java side.

---

## 1. NON-NEGOTIABLES (Doer)

- Edit only files under `/mobile/backend/src/` and `/mobile/backend/test/`. Do NOT modify `package.json` (no new deps).
- Spanish comments. No emojis. No diacritics in new strings.
- Each new typedef/resolver/service follows the existing module style: `'use strict';` header, JSDoc on exported functions, `module.exports`.
- After each task, run `npm test` and confirm green BEFORE moving to the next task. If a task introduces a new test, the test MUST pass on first run.
- After every BFF source change, the BFF auto-reloads (`node --watch`). The Doer can re-issue the curl smoke from §11 to verify.
- Do NOT refactor working resolvers. Add fields, don't rename.
- Do NOT remove `availableGames`, `myProgress`, `treatmentPdf`. They are used elsewhere or by older clients — keep them intact and add the new operations alongside.

---

## 2. TASK G.1 — Patient profile: add `numSs`, `sexo`, `avatarDataUri`

### 2.1 Edit `src/graphql/typeDefs/patient.js`

In the `type Patient { ... }` block, **after** the `active: Boolean!` line and **before** `greeting: String`, insert:

```graphql
    # Numero de la Seguridad Social (12 digitos en Espana). Lectura pacientes.
    numSs: String

    # Sexo registrado en la ficha clinica. Coincide con el enum SexoPaciente del API Java.
    sexo: SexoPaciente

    # Avatar codificado en data URI (data:image/png;base64,....). Null si el paciente no ha subido avatar.
    avatarDataUri: String
```

### 2.2 Add `enum SexoPaciente` to `src/graphql/typeDefs/common.js`

Append to the `gql` template at the end of `commonTypeDefs`:

```graphql
  # Sexo registrado en la ficha clinica del paciente
  enum SexoPaciente {
    MASCULINO
    FEMENINO
    OTRO
  }
```

The enum is shared because future appointments / game telemetry may also reference it.

### 2.3 Edit `src/services/patientService.js` `obtenerPerfil`

Replace the return block (currently lines 26-39) with:

```javascript
  return {
    id: data.dniPac,
    dni: data.dniPac,
    name: data.nombrePac,
    surname: [data.apellido1Pac, data.apellido2Pac].filter(Boolean).join(' '),
    email: data.emailPac || null,
    phone: data.telefonos && data.telefonos.length > 0 ? data.telefonos[0] : null,
    birthDate: data.fechaNacimiento || null,
    address: null,
    active: data.activo,
    // Campos nuevos expuestos a la app movil (Phase G.1).
    // numSs y sexo ya vienen en el mock; avatarDataUri es null hasta que el API Java
    // exponga el campo (sera GET /api/pacientes/{dni}/avatar en una iteracion futura).
    numSs: data.numSs || null,
    sexo: data.sexo || null,
    avatarDataUri: data.avatarDataUri || null,
  };
```

`MOCK_PACIENTE_ADMIN` in `apiClient.js` already has `numSs: '280000000001'` and `sexo: 'MASCULINO'`. No mock change needed.

### 2.4 Test

Append to `test/graphql.test.js` a test case (use the existing pattern of `describe('Query me', ...)`):

```javascript
test('me devuelve numSs, sexo y avatarDataUri', async () => {
  const respuesta = await ejecutarConsulta(
    `query { me { numSs sexo avatarDataUri } }`,
    tokenAdmin
  );
  assert.equal(respuesta.errors, undefined);
  assert.equal(respuesta.data.me.numSs, '280000000001');
  assert.equal(respuesta.data.me.sexo, 'MASCULINO');
  assert.equal(respuesta.data.me.avatarDataUri, null);
});
```

If `ejecutarConsulta` / `tokenAdmin` helpers don't exist with that name, copy the helper used by the closest existing `me` test in the same file.

---

## 3. TASK G.2 — Treatment: add `codTrat`, `disabilityCode`, `summary`, `materials`, `medication`, `documentUrl`, `hasDocument`

### 3.1 Edit `src/graphql/typeDefs/treatment.js`

Replace the `type Treatment` block with:

```graphql
  # Tratamiento terapeutico asignado al paciente
  type Treatment {
    id: ID!
    # Codigo del tratamiento (igual a id en el modelo actual; se mantienen ambos
    # para compatibilidad con el frontend, que los usa indistintamente).
    codTrat: String!
    name: String!
    description: String
    type: String!
    visible: Boolean!
    progressionLevel: Int!
    # Codigo de la discapacidad asociada al tratamiento (FK a Disability.id).
    # Null si el tratamiento no esta vinculado a una discapacidad concreta.
    disabilityCode: String
    # Resumen breve para mostrar en la card de la lista de tratamientos.
    summary: String
    # Materiales necesarios para realizar el tratamiento. Lista vacia si no aplica.
    materials: [String!]!
    # Medicacion asociada al tratamiento (nombre + posologia). Lista vacia si no aplica.
    medication: [String!]!
    # URL absoluta al PDF del protocolo. Null si no hay PDF subido.
    documentUrl: String
    # True si el tratamiento tiene PDF descargable via `treatmentDocument`.
    hasDocument: Boolean!
  }
```

### 3.2 Edit `src/services/treatmentService.js` `obtenerTratamientos`

Replace the mapping block (currently lines 23-30) with:

```javascript
  // Mapeo Java PacienteTratamientoResponse -> GraphQL Treatment
  // Campos NO presentes en la respuesta Java actual se rellenan con valores
  // sintieticos deterministas para mantener el contrato con el frontend.
  // Cuando /api Phase 6 enriquezca el endpoint, sustituir los defaults por la
  // respuesta real (ver TODO marcado abajo).
  lista = lista.map((t) => ({
    id: t.codTrat,
    codTrat: t.codTrat,
    name: t.nombreTrat,
    description: t.descripcionTrat || null,
    type: 'TEXT_INSTRUCTION',
    visible: t.visible,
    progressionLevel: t.idNivel || 0,
    disabilityCode: t.codDis || null,
    summary: t.resumen || null,
    materials: Array.isArray(t.materiales) ? t.materiales : [],
    medication: Array.isArray(t.medicacion) ? t.medicacion : [],
    documentUrl: t.urlDocumento || null,
    hasDocument: Boolean(t.tienePdf),
  }));
```

### 3.3 Update mock data in `src/services/apiClient.js`

Replace `MOCK_TRATAMIENTOS_ADMIN` (lines 52-57) with:

```javascript
const MOCK_TRATAMIENTOS_ADMIN = [
  {
    dniPac: '12345678Z',
    codTrat: 'TRT001',
    nombreTrat: 'Ejercicios de movilidad de cadera',
    descripcionTrat: 'Serie de ejercicios para recuperar el rango articular de la cadera operada.',
    visible: true,
    fechaAsignacion: '2024-03-15T09:00:00',
    idNivel: 2,
    codDis: 'M16',
    resumen: 'Tres series de 10 repeticiones, dos veces al dia.',
    materiales: ['Esterilla', 'Cinta elastica baja resistencia'],
    medicacion: [],
    urlDocumento: 'https://api.rehabiapp.local/tratamientos/TRT001/pdf',
    tienePdf: true,
  },
  {
    dniPac: '12345678Z',
    codTrat: 'TRT002',
    nombreTrat: 'Electroterapia de baja frecuencia',
    descripcionTrat: 'Sesiones de TENS para alivio del dolor lumbar.',
    visible: true,
    fechaAsignacion: '2024-03-15T09:00:00',
    idNivel: 1,
    codDis: 'M54',
    resumen: 'Sesion de 20 minutos en clinica, 2 veces por semana.',
    materiales: ['Equipo TENS clinico'],
    medicacion: ['Paracetamol 1g si dolor irruptivo'],
    urlDocumento: null,
    tienePdf: false,
  },
  {
    dniPac: '12345678Z',
    codTrat: 'TRT003',
    nombreTrat: 'Ejercicios de fortalecimiento lumbar',
    descripcionTrat: 'Plan de fortalecimiento progresivo del core.',
    visible: true,
    fechaAsignacion: '2024-06-05T11:00:00',
    idNivel: 1,
    codDis: 'M54',
    resumen: 'Ejercicios isometricos diarios. Aumentar carga semanalmente.',
    materiales: ['Esterilla', 'Pelota suiza'],
    medicacion: [],
    urlDocumento: 'https://api.rehabiapp.local/tratamientos/TRT003/pdf',
    tienePdf: true,
  },
  {
    dniPac: '12345678Z',
    codTrat: 'TRT004',
    nombreTrat: 'Hidroterapia terapeutica',
    descripcionTrat: 'Sesiones de piscina (oculto al paciente — pendiente de aprobacion).',
    visible: false,
    fechaAsignacion: '2024-06-05T11:00:00',
    idNivel: 0,
    codDis: 'M16',
    resumen: null,
    materiales: [],
    medicacion: [],
    urlDocumento: null,
    tienePdf: false,
  },
];
```

### 3.4 Test

Add to `test/graphql.test.js`:

```javascript
test('myTreatments incluye los nuevos campos del Phase G', async () => {
  const respuesta = await ejecutarConsulta(
    `query {
      myTreatments {
        id codTrat name disabilityCode summary materials medication documentUrl hasDocument
      }
    }`,
    tokenAdmin
  );
  assert.equal(respuesta.errors, undefined);
  const trt001 = respuesta.data.myTreatments.find((t) => t.codTrat === 'TRT001');
  assert.equal(trt001.disabilityCode, 'M16');
  assert.equal(trt001.hasDocument, true);
  assert.deepEqual(trt001.materials, ['Esterilla', 'Cinta elastica baja resistencia']);
});
```

---

## 4. TASK G.3 — Treatment document download (`treatmentDocument`)

The frontend `treatmentsStore.downloadPdf` (line 56-87) calls `GET_TREATMENT_DOCUMENT` with `codTrat` and expects `{ fileName, mimeType, base64, url }`. The existing `treatmentPdf` resolver returns a different shape and is also kept for backward compatibility.

### 4.1 Append to `src/graphql/typeDefs/treatment.js`

Above `extend type Query { ... }`, add the new payload type:

```graphql
  # Documento descargable asociado a un tratamiento (PDF de protocolo, video, etc.)
  type TreatmentDocument {
    # Nombre sugerido al guardar el archivo en el dispositivo.
    fileName: String!
    # MIME type. Para PDFs siempre 'application/pdf'.
    mimeType: String!
    # Contenido en base64 (incluye el PDF entero). Null si el frontend debe usar `url`.
    base64: String
    # URL absoluta al recurso. Null si el contenido va en `base64`.
    url: String
  }
```

Inside `extend type Query { ... }` (the SAME extend that already has `myTreatments` and `treatmentPdf`), add:

```graphql
    # Descarga el documento del tratamiento. Para PDFs <=10MB se devuelve `base64`;
    # para tamanos mayores se devuelve `url` (no implementado aun en mock).
    treatmentDocument(codTrat: ID!): TreatmentDocument
```

### 4.2 Add resolver in `src/graphql/resolvers/treatment.js`

Inside `Query: { ... }` add a new resolver method **after** `treatmentPdf`:

```javascript
    async treatmentDocument(_parent, { codTrat }, context) {
      requireAuth(context);
      const pdf = await treatmentPdfService.obtenerPdfTratamiento(String(codTrat), context.javaToken);
      if (!pdf) return null;
      return {
        fileName: pdf.filename,
        mimeType: 'application/pdf',
        base64: pdf.base64Content,
        url: null,
      };
    },
```

The `treatmentPdfService.obtenerPdfTratamiento` already exists. No service change needed.

### 4.3 Test

```javascript
test('treatmentDocument devuelve fileName + mimeType + base64', async () => {
  const respuesta = await ejecutarConsulta(
    `query { treatmentDocument(codTrat: "TRT001") { fileName mimeType base64 url } }`,
    tokenAdmin
  );
  assert.equal(respuesta.errors, undefined);
  assert.equal(respuesta.data.treatmentDocument.fileName, 'TRT001.pdf');
  assert.equal(respuesta.data.treatmentDocument.mimeType, 'application/pdf');
  assert.match(respuesta.data.treatmentDocument.base64, /^JVBERi/);
  assert.equal(respuesta.data.treatmentDocument.url, null);
});
```

---

## 5. TASK G.4 — Body-part progress (NEW: `myBodyPartProgress` + `bodyPartMetrics`)

The frontend `BodyDiagram` component renders 15 body parts (`HEAD`, `NECK`, `TORSO`, `LEFT_SHOULDER`, ...) with hit-test overlay. `progressStore.fetch` calls `GET_MY_BODY_PART_PROGRESS`; tapping a part calls `loadMetrics` → `GET_BODY_PART_METRICS`.

### 5.1 Create `src/graphql/typeDefs/bodyProgress.js`

```javascript
// TypeDefs del progreso por parte del cuerpo
// Alimenta el componente BodyDiagram + ProgressChartModal del frontend
'use strict';

const { gql } = require('graphql-tag');

const bodyProgressTypeDefs = gql`
  # Identificadores de partes del cuerpo. Coincide 1:1 con frontend types/progress.ts BodyPartId.
  enum BodyPartId {
    HEAD
    NECK
    TORSO
    LEFT_SHOULDER
    RIGHT_SHOULDER
    LEFT_ARM
    RIGHT_ARM
    LEFT_HAND
    RIGHT_HAND
    LEFT_HIP
    RIGHT_HIP
    LEFT_LEG
    RIGHT_LEG
    LEFT_FOOT
    RIGHT_FOOT
  }

  # Resumen de progreso por parte del cuerpo (para el BodyDiagram clickable)
  type BodyPartProgress {
    id: BodyPartId!
    # Etiqueta legible en castellano (p.ej. "Cadera derecha")
    name: String!
    # True si el paciente tiene tratamiento activo afectando esta parte
    hasTreatment: Boolean!
    # Progreso acumulado [0..100]. Null si no hay datos suficientes.
    progressPct: Float
    # Mejora porcentual contra baseline [-100..+100]. Null si no hay baseline.
    improvementPct: Float
    # Etiqueta del periodo agregado (p.ej. "Ultimas 4 semanas").
    periodLabel: String!
  }

  # Punto de la serie temporal para una parte del cuerpo
  type BodyPartMetric {
    # Fecha en ISO8601 'YYYY-MM-DD'.
    date: String!
    # Valor numerico (escala depende de metricType — el frontend lo muestra tal cual).
    score: Float!
    # Identificador de la metrica (p.ej. 'angulo_flexion', 'fuerza_isometrica', 'dolor_evaluado').
    metricType: String!
  }

  extend type Query {
    # Resumen de progreso por parte del cuerpo del paciente autenticado.
    # Util para pintar el BodyDiagram con colores segun progressPct.
    myBodyPartProgress: [BodyPartProgress!]!

    # Serie temporal de una parte del cuerpo concreta. Usado por ProgressChartModal.
    bodyPartMetrics(bodyPartId: BodyPartId!): [BodyPartMetric!]!
  }
`;

module.exports = bodyProgressTypeDefs;
```

### 5.2 Register the new typeDef in `src/graphql/typeDefs/index.js`

Replace the file with:

```javascript
// Fusiona todos los TypeDefs del esquema GraphQL del BFF
// Apollo Server 4 acepta un array de DocumentNode
'use strict';

const commonTypeDefs = require('./common');
const authTypeDefs = require('./auth');
const patientTypeDefs = require('./patient');
const treatmentTypeDefs = require('./treatment');
const appointmentTypeDefs = require('./appointment');
const gameTypeDefs = require('./game');
const dashboardTypeDefs = require('./dashboard');
const bodyProgressTypeDefs = require('./bodyProgress');
const settingsTypeDefs = require('./settings');

// El orden importa: common primero (define enums usados por los demas)
module.exports = [
  commonTypeDefs,
  authTypeDefs,
  patientTypeDefs,
  treatmentTypeDefs,
  appointmentTypeDefs,
  gameTypeDefs,
  dashboardTypeDefs,
  bodyProgressTypeDefs,
  settingsTypeDefs,
];
```

(`settings` is created in §8 — register it now to avoid touching this file twice.)

### 5.3 Create `src/services/bodyProgressService.js`

```javascript
// Servicio de progreso por parte del cuerpo
// En modo mock: deriva los datos de las discapacidades + tratamientos del paciente.
// En produccion: consumira un endpoint enriquecido de /api/pacientes/{dni}/progreso/body-parts
// que aun no existe (a implementar en /api Phase 12).
'use strict';

const apiClient = require('./apiClient');

// Mapa codDis -> partes del cuerpo afectadas. Espejo de la logica que /desktop usa
// para colorear el diagrama. Lista cerrada — anadir nuevos codigos cuando aparezcan.
const PARTES_POR_DISCAPACIDAD = {
  M16: ['LEFT_HIP', 'RIGHT_HIP'],
  M54: ['TORSO'],
  M75: ['LEFT_SHOULDER', 'RIGHT_SHOULDER'],
  G56: ['LEFT_HAND', 'RIGHT_HAND'],
};

// Etiqueta legible por parte del cuerpo. Usada como `name` en el GraphQL response.
const ETIQUETAS_PARTE = {
  HEAD: 'Cabeza',
  NECK: 'Cuello',
  TORSO: 'Espalda y tronco',
  LEFT_SHOULDER: 'Hombro izquierdo',
  RIGHT_SHOULDER: 'Hombro derecho',
  LEFT_ARM: 'Brazo izquierdo',
  RIGHT_ARM: 'Brazo derecho',
  LEFT_HAND: 'Mano izquierda',
  RIGHT_HAND: 'Mano derecha',
  LEFT_HIP: 'Cadera izquierda',
  RIGHT_HIP: 'Cadera derecha',
  LEFT_LEG: 'Pierna izquierda',
  RIGHT_LEG: 'Pierna derecha',
  LEFT_FOOT: 'Pie izquierdo',
  RIGHT_FOOT: 'Pie derecho',
};

const TODAS_LAS_PARTES = Object.keys(ETIQUETAS_PARTE);

/**
 * Calcula el resumen de progreso por parte del cuerpo del paciente.
 * Itera sobre TODAS_LAS_PARTES para garantizar que el frontend recibe el set completo
 * (BodyDiagram pinta partes sin tratamiento en gris claro).
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @returns {Promise<Array>} BodyPartProgress[]
 */
async function obtenerProgresoPorParte(dniPac, javaToken) {
  const discapacidades = await apiClient.get(`/api/pacientes/${dniPac}/discapacidades`, javaToken);
  const lista = Array.isArray(discapacidades) ? discapacidades : [];

  // Set de partes del cuerpo afectadas por las discapacidades activas
  const partesAfectadas = new Set();
  for (const d of lista) {
    const partes = PARTES_POR_DISCAPACIDAD[d.codDis] || [];
    for (const p of partes) partesAfectadas.add(p);
  }

  // Hash deterministico para que el progreso mock sea estable entre llamadas.
  // Real /api Phase 12 sustituira esto por valores agregados de MongoDB.
  function hashEstable(parte) {
    let h = 0;
    for (const c of `${dniPac}-${parte}`) h = (h * 31 + c.charCodeAt(0)) >>> 0;
    return h;
  }

  return TODAS_LAS_PARTES.map((parte) => {
    const tieneTratamiento = partesAfectadas.has(parte);
    const seed = hashEstable(parte);
    return {
      id: parte,
      name: ETIQUETAS_PARTE[parte],
      hasTreatment: tieneTratamiento,
      progressPct: tieneTratamiento ? Number((40 + (seed % 60)).toFixed(1)) : null,
      improvementPct: tieneTratamiento ? Number((-5 + (seed % 30)).toFixed(1)) : null,
      periodLabel: tieneTratamiento ? 'Ultimas 4 semanas' : 'Sin datos',
    };
  });
}

/**
 * Devuelve la serie temporal de una parte del cuerpo concreta.
 * Mock: 12 puntos semanales con tendencia ligeramente positiva (deterministica por parte+dni).
 *
 * @param {string} dniPac
 * @param {string} bodyPartId
 * @param {string|null} _javaToken
 * @returns {Promise<Array>} BodyPartMetric[]
 */
async function obtenerMetricasPorParte(dniPac, bodyPartId, _javaToken) {
  // Hash determinista para que la grafica sea estable entre llamadas (no cambia en cada refresh).
  let seed = 0;
  for (const c of `${dniPac}-${bodyPartId}`) seed = (seed * 31 + c.charCodeAt(0)) >>> 0;

  const hoy = new Date();
  const puntos = [];
  for (let i = 11; i >= 0; i -= 1) {
    const fecha = new Date(hoy.getTime() - i * 7 * 24 * 60 * 60 * 1000);
    const isoDate = fecha.toISOString().slice(0, 10);
    // Score crece con el tiempo (i menor = mas reciente = mayor score), con ruido.
    const base = 50 + (11 - i) * 3;
    const ruido = ((seed >> i) & 0x07) - 3;
    puntos.push({
      date: isoDate,
      score: Number((base + ruido).toFixed(1)),
      metricType: 'angulo_flexion',
    });
  }
  return puntos;
}

module.exports = { obtenerProgresoPorParte, obtenerMetricasPorParte };
```

### 5.4 Create `src/graphql/resolvers/bodyProgress.js`

```javascript
// Resolvers de progreso por parte del cuerpo
'use strict';

const bodyProgressService = require('../../services/bodyProgressService');
const { requireAuth } = require('./helpers');

const bodyProgressResolvers = {
  Query: {
    async myBodyPartProgress(_parent, _args, context) {
      const user = requireAuth(context);
      return bodyProgressService.obtenerProgresoPorParte(user.sub, context.javaToken);
    },

    async bodyPartMetrics(_parent, { bodyPartId }, context) {
      const user = requireAuth(context);
      return bodyProgressService.obtenerMetricasPorParte(user.sub, bodyPartId, context.javaToken);
    },
  },
};

module.exports = bodyProgressResolvers;
```

### 5.5 Register the resolver in `src/graphql/resolvers/index.js`

Replace the file with:

```javascript
// Fusiona todos los resolvers del BFF en un unico objeto para Apollo Server
'use strict';

const authResolvers = require('./auth');
const patientResolvers = require('./patient');
const treatmentResolvers = require('./treatment');
const appointmentResolvers = require('./appointment');
const gameResolvers = require('./game');
const dashboardResolvers = require('./dashboard');
const bodyProgressResolvers = require('./bodyProgress');
const settingsResolvers = require('./settings');

const resolvers = {
  Query: {
    ...patientResolvers.Query,
    ...treatmentResolvers.Query,
    ...appointmentResolvers.Query,
    ...gameResolvers.Query,
    ...dashboardResolvers.Query,
    ...bodyProgressResolvers.Query,
  },
  Mutation: {
    ...authResolvers.Mutation,
    ...appointmentResolvers.Mutation,
    ...gameResolvers.Mutation,
    ...settingsResolvers.Mutation,
  },
  Patient: patientResolvers.Patient,
};

module.exports = resolvers;
```

### 5.6 Test

```javascript
test('myBodyPartProgress devuelve las 15 partes y marca hasTreatment para M16/M54', async () => {
  const respuesta = await ejecutarConsulta(
    `query { myBodyPartProgress { id name hasTreatment progressPct periodLabel } }`,
    tokenAdmin
  );
  assert.equal(respuesta.errors, undefined);
  const partes = respuesta.data.myBodyPartProgress;
  assert.equal(partes.length, 15);
  const cadDer = partes.find((p) => p.id === 'RIGHT_HIP');
  assert.equal(cadDer.hasTreatment, true);
  const torso = partes.find((p) => p.id === 'TORSO');
  assert.equal(torso.hasTreatment, true);
  const cabeza = partes.find((p) => p.id === 'HEAD');
  assert.equal(cabeza.hasTreatment, false);
  assert.equal(cabeza.progressPct, null);
});

test('bodyPartMetrics devuelve 12 puntos semanales ordenados', async () => {
  const respuesta = await ejecutarConsulta(
    `query { bodyPartMetrics(bodyPartId: RIGHT_HIP) { date score metricType } }`,
    tokenAdmin
  );
  assert.equal(respuesta.errors, undefined);
  const puntos = respuesta.data.bodyPartMetrics;
  assert.equal(puntos.length, 12);
  // Orden cronologico ascendente
  for (let i = 1; i < puntos.length; i += 1) {
    assert.ok(puntos[i].date >= puntos[i - 1].date);
  }
});
```

---

## 6. TASK G.5 — Assigned games (NEW: `myAssignedGames`)

The frontend `gamesStore.fetch` calls `GET_MY_ASSIGNED_GAMES` and expects fields `id`, `name`, `description`, `thumbnailUrl`, `webglUrl`, `difficulty (EASY|MEDIUM|HARD)`, `assignedAt`. The existing `availableGames` returns a different shape — keep it for backward compatibility and add the new query.

### 6.1 Append to `src/graphql/typeDefs/game.js`

Inside the existing `gameTypeDefs` template, after the existing `type Game { ... }` block, add:

```graphql
  # Dificultad asignada al videojuego para este paciente.
  # Calculada a partir del nivel de progresion + sensibilidad clinica.
  enum GameDifficulty {
    EASY
    MEDIUM
    HARD
  }

  # Videojuego asignado al paciente con metadata para la card de la lista.
  type AssignedGame {
    id: ID!
    name: String!
    description: String!
    thumbnailUrl: String
    webglUrl: String
    difficulty: GameDifficulty!
    # Fecha ISO8601 cuando el juego fue asignado al paciente.
    assignedAt: String!
  }
```

Inside `extend type Query { ... }` (the same one that already has `myGameSessions` and `availableGames`), add:

```graphql
    # Videojuegos asignados al paciente, formato listo para mostrar en la pestana Juegos.
    myAssignedGames: [AssignedGame!]!
```

### 6.2 Edit `src/graphql/resolvers/game.js`

Add a new resolver in `Query: { ... }` after `availableGames`:

```javascript
    async myAssignedGames(_parent, _args, context) {
      const user = requireAuth(context);
      return gameService.obtenerJuegosAsignados(user.sub, context.javaToken);
    },
```

### 6.3 Edit `src/services/gameService.js`

Add the new function (do NOT modify existing functions):

```javascript
/**
 * Construye la lista de juegos asignados al paciente con la forma exacta que
 * espera el componente GameCard del frontend.
 * En modo mock: deriva de los juegos desbloqueados del dashboard + thumbnails sintieticos.
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @returns {Promise<Array>} AssignedGame[]
 */
async function obtenerJuegosAsignados(dniPac, javaToken) {
  const dashboard = await apiClient.get(`/api/pacientes/${dniPac}/dashboard`, javaToken);
  if (!dashboard || !Array.isArray(dashboard.juegosDesbloqueados)) {
    return [];
  }

  // Derivar dificultad de la fecha de asignacion para que sea estable y no aleatoria.
  // En produccion vendra del campo `dificultad` del DTO Java cuando exista.
  const DIFICULTADES = ['EASY', 'MEDIUM', 'HARD'];

  return dashboard.juegosDesbloqueados.map((j, idx) => ({
    id: String(j.idVideojuego),
    name: j.nombre,
    description: j.descripcion || `Juego terapeutico para ${j.parteCuerpo || 'rehabilitacion general'}.`,
    thumbnailUrl: j.urlMiniatura || null,
    webglUrl: j.urlUnity || null,
    difficulty: DIFICULTADES[idx % DIFICULTADES.length],
    assignedAt: j.fechaAsignacion || new Date().toISOString(),
  }));
}

module.exports = {
  // ... lo que ya estuviera exportado
  obtenerJuegosAsignados,
};
```

> **Important:** preserve existing exports. Read the current `module.exports` block first, then merge `obtenerJuegosAsignados` into it. Do NOT replace.

### 6.4 Test

```javascript
test('myAssignedGames mapea juegosDesbloqueados a AssignedGame', async () => {
  const respuesta = await ejecutarConsulta(
    `query { myAssignedGames { id name description webglUrl difficulty assignedAt } }`,
    tokenAdmin
  );
  assert.equal(respuesta.errors, undefined);
  assert.ok(respuesta.data.myAssignedGames.length >= 1);
  const j = respuesta.data.myAssignedGames[0];
  assert.equal(j.id, '1');
  assert.equal(j.name, 'Mover la cadera');
  assert.equal(j.webglUrl, 'https://games.rehabiapp.com/hip-01');
  assert.ok(['EASY', 'MEDIUM', 'HARD'].includes(j.difficulty));
});
```

---

## 7. TASK G.6 — Appointment request (NEW: `requestAppointment`)

The frontend `AppointmentRequestForm` component submits `REQUEST_APPOINTMENT` with `{ fechaPreferida, horaPreferida, motivo, telefono?, email? }` and expects `{ id, fechaPreferida, horaPreferida, motivo, estado, createdAt }`.

### 7.1 Edit `src/graphql/typeDefs/appointment.js`

Append a new type and a new mutation field:

```graphql
  # Estado de una solicitud de cita en revision por la clinica
  enum AppointmentRequestStatus {
    PENDING
    APPROVED
    REJECTED
    CANCELLED
  }

  # Solicitud de cita enviada por el paciente desde el formulario libre.
  # NO es una cita confirmada — la clinica la revisa antes de crear el Appointment.
  type AppointmentRequest {
    id: ID!
    fechaPreferida: String!
    horaPreferida: String!
    motivo: String!
    estado: AppointmentRequestStatus!
    createdAt: String!
  }

  extend type Mutation {
    # Solicita una cita al equipo clinico (no es una reserva confirmada).
    # La clinica recibe la solicitud por canal interno y, si la aprueba, llama a
    # bookAppointment internamente. Devuelve el registro de la solicitud.
    requestAppointment(
      fechaPreferida: String!
      horaPreferida: String!
      motivo: String!
      telefono: String
      email: String
    ): AppointmentRequest!
  }
```

### 7.2 Edit `src/graphql/resolvers/appointment.js` and `src/services/appointmentService.js`

Add to the resolver (in `Mutation: { ... }`):

```javascript
    async requestAppointment(_parent, args, context) {
      const user = requireAuth(context);
      return appointmentService.solicitarCita(user.sub, args, context.javaToken);
    },
```

Add to the service (preserve existing exports — merge):

```javascript
/**
 * Crea una solicitud de cita (no una cita confirmada).
 * En mock: genera un id sintieticamente y devuelve estado PENDING.
 * En produccion: POST /api/citas/solicitudes (endpoint pendiente en /api Phase 12).
 *
 * @param {string} dniPac
 * @param {{ fechaPreferida, horaPreferida, motivo, telefono?, email? }} args
 * @param {string|null} _javaToken
 * @returns {Promise<object>} AppointmentRequest
 */
async function solicitarCita(dniPac, args, _javaToken) {
  // Validacion ligera (la API Java validara con mas detalle cuando se conecte real).
  if (!args.motivo || args.motivo.trim().length < 5) {
    const { crearError } = require('../utils/errors');
    throw crearError('VALIDATION_ERROR');
  }

  // Id sintietico estable: dni + timestamp.
  const id = `REQ-${dniPac}-${Date.now()}`;
  return {
    id,
    fechaPreferida: args.fechaPreferida,
    horaPreferida: args.horaPreferida,
    motivo: args.motivo,
    estado: 'PENDING',
    createdAt: new Date().toISOString(),
  };
}

module.exports = {
  // ... lo que ya estuviera exportado
  solicitarCita,
};
```

### 7.3 Test

```javascript
test('requestAppointment devuelve estado PENDING y respeta los argumentos', async () => {
  const respuesta = await ejecutarConsulta(
    `mutation {
       requestAppointment(
         fechaPreferida: "2026-06-10",
         horaPreferida: "10:30",
         motivo: "Revision de cadera",
         telefono: "600000000"
       ) { id fechaPreferida horaPreferida motivo estado createdAt }
     }`,
    tokenAdmin
  );
  assert.equal(respuesta.errors, undefined);
  const r = respuesta.data.requestAppointment;
  assert.match(r.id, /^REQ-12345678Z-/);
  assert.equal(r.fechaPreferida, '2026-06-10');
  assert.equal(r.estado, 'PENDING');
});
```

---

## 8. TASK G.7 — Push device token stubs (`registerDeviceToken`, `unregisterDeviceToken`)

Frontend uses these in the settings flow. We stub them returning `Boolean!` (the schema expects `Boolean` per the mutation usage in `mutations/settings.ts`).

### 8.1 Create `src/graphql/typeDefs/settings.js`

```javascript
// TypeDefs de configuracion del dispositivo movil
// Las mutaciones registran/eliminan tokens de notificaciones push (APNs/FCM).
// En esta iteracion son stubs — la entrega real sera Phase 5.3 del checklist mobile.
'use strict';

const { gql } = require('graphql-tag');

const settingsTypeDefs = gql`
  extend type Mutation {
    # Registra el token push del dispositivo. Devuelve true si se acepto.
    # Stub en esta iteracion: el BFF lo loguea pero no persiste — pending APNs/FCM wiring.
    registerDeviceToken(token: String!, platform: String!): Boolean!

    # Elimina el token push del dispositivo. Devuelve true si se acepto.
    unregisterDeviceToken(token: String!): Boolean!
  }
`;

module.exports = settingsTypeDefs;
```

### 8.2 Create `src/graphql/resolvers/settings.js`

```javascript
// Resolvers de configuracion del dispositivo movil
// Stubs de Phase G.7 — solo log estructurado, sin persistencia.
'use strict';

const { requireAuth } = require('./helpers');

const settingsResolvers = {
  Mutation: {
    async registerDeviceToken(_parent, { token, platform }, context) {
      const user = requireAuth(context);
      context.logger.info(
        { dniPac: user.sub.substring(0, 3) + '***', platform, tokenPrefijo: token.substring(0, 8) + '...' },
        'Token push registrado (stub Phase G.7)'
      );
      return true;
    },

    async unregisterDeviceToken(_parent, { token }, context) {
      const user = requireAuth(context);
      context.logger.info(
        { dniPac: user.sub.substring(0, 3) + '***', tokenPrefijo: token.substring(0, 8) + '...' },
        'Token push eliminado (stub Phase G.7)'
      );
      return true;
    },
  },
};

module.exports = settingsResolvers;
```

(Already wired in `resolvers/index.js` and `typeDefs/index.js` per §5.2 / §5.5.)

### 8.3 Test

```javascript
test('registerDeviceToken devuelve true (stub)', async () => {
  const respuesta = await ejecutarConsulta(
    `mutation { registerDeviceToken(token: "ExpoTokExample123", platform: "android") }`,
    tokenAdmin
  );
  assert.equal(respuesta.errors, undefined);
  assert.equal(respuesta.data.registerDeviceToken, true);
});
```

---

## 9. TASK G.8 — Backwards-compatible `myProgressSummary`

The frontend file `queries/user.ts` exports `GET_MY_PROGRESS` with `{ totalSessions, averageScore, improvementRate, lastSessionDate }`. It is **not used** by any current store (`progressStore` uses `myBodyPartProgress`). Still, to avoid a future regression and to keep the schema lossless, expose both shapes by adding a new query `myProgressSummary` and leaving `myProgress` untouched.

### 9.1 Edit `src/graphql/typeDefs/patient.js`

Inside `extend type Query { ... }` add (do NOT remove existing):

```graphql
    # Resumen agregado del progreso terapeutico (orientado a tarjetas de bienvenida).
    # Distinto del `myProgress` (que devuelve series por tratamiento) y del
    # `myBodyPartProgress` (que devuelve mapa de cuerpo). Este es el formato
    # plano usado por el dashboard.
    myProgressSummary: ProgressSummary
```

Add the `type ProgressSummary` block above `extend type Query`:

```graphql
  # Resumen plano del progreso para tarjetas de bienvenida
  type ProgressSummary {
    totalSessions: Int!
    averageScore: Float
    improvementRate: Float
    lastSessionDate: String
  }
```

### 9.2 Edit `src/graphql/resolvers/patient.js`

Add to `Query: { ... }`:

```javascript
    async myProgressSummary(_parent, _args, context) {
      const user = requireAuth(context);
      return patientService.obtenerProgreso(user.sub, context.javaToken);
    },
```

`patientService.obtenerProgreso` already exists and returns the right shape (`{ totalSessions, averageScore, improvementRate, lastSessionDate }`) — see `patientService.js` line 70-79.

### 9.3 Frontend follow-up (DO NOT IMPLEMENT HERE)

The frontend should be migrated to call `myProgressSummary` if/when the welcome card is built. That migration belongs to `mobile/frontend/PLAN.md` and is **out of scope** for Phase G. The currently unused `GET_MY_PROGRESS` constant stays in `queries/user.ts` — no edit.

---

## 10. CONFIGURATION CHECK

The Doer MUST verify these env conditions BEFORE running tests:

| Variable     | Expected      | Where                                    |
|--------------|---------------|------------------------------------------|
| `MOCK_API`   | `true`        | `mobile/backend/.env` or `npm run dev` already sets it |
| `LOG_LEVEL`  | `debug`       | for development; `info` for tests        |
| `PORT`       | `3000`        | default                                  |
| `NODE_ENV`   | `development` | default                                  |

`npm run dev` script in `package.json` already sets `MOCK_API=true LOG_LEVEL=debug`. No action needed.

---

## 11. VERIFICATION (smoke + tests)

After completing G.1 to G.8, run:

```bash
cd /home/alaslibres/DAM/RehabiAPP/mobile/backend
npm test
```

Expected: All tests pass (the existing 16 + the 8 new = 24 tests minimum).

Then start the BFF and run the curl smoke test:

```bash
cd /home/alaslibres/DAM/RehabiAPP/mobile/backend && npm run dev
# In a second terminal:

# 1) Login
TOKEN=$(curl -s http://localhost:3000/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { login(identifier: \"admin\", password: \"admin\") { accessToken } }"}' \
  | python3 -c 'import sys, json; print(json.load(sys.stdin)["data"]["login"]["accessToken"])')

echo "TOKEN=$TOKEN"

# 2) me with new fields
curl -s http://localhost:3000/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"query":"{ me { numSs sexo avatarDataUri } }"}'

# 3) myTreatments with new fields
curl -s http://localhost:3000/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"query":"{ myTreatments { id codTrat name disabilityCode hasDocument materials } }"}'

# 4) myBodyPartProgress
curl -s http://localhost:3000/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"query":"{ myBodyPartProgress { id name hasTreatment progressPct } }"}'

# 5) bodyPartMetrics
curl -s http://localhost:3000/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"query":"{ bodyPartMetrics(bodyPartId: RIGHT_HIP) { date score metricType } }"}'

# 6) myAssignedGames
curl -s http://localhost:3000/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"query":"{ myAssignedGames { id name webglUrl difficulty } }"}'

# 7) treatmentDocument
curl -s http://localhost:3000/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"query":"{ treatmentDocument(codTrat: \"TRT001\") { fileName mimeType base64 } }"}' | head -c 200

# 8) requestAppointment
curl -s http://localhost:3000/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"query":"mutation { requestAppointment(fechaPreferida: \"2026-06-10\", horaPreferida: \"10:30\", motivo: \"Revision\") { id estado createdAt } }"}'

# 9) registerDeviceToken
curl -s http://localhost:3000/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"query":"mutation { registerDeviceToken(token: \"ExpoTokExample\", platform: \"android\") }"}'
```

Each call MUST return `data` with no `errors` field.

---

## 12. CHECKLIST (mark `[x]` after each verifies green)

- [x] G.1 `me` exposes `numSs`, `sexo`, `avatarDataUri`. Test added.
- [x] G.2 `myTreatments` exposes `codTrat`, `disabilityCode`, `summary`, `materials`, `medication`, `documentUrl`, `hasDocument`. Test added.
- [x] G.3 `treatmentDocument(codTrat: ID!)` query implemented. Test added.
- [x] G.4 `myBodyPartProgress` + `bodyPartMetrics(bodyPartId)` queries implemented with mock service. Tests added.
- [x] G.5 `myAssignedGames` query implemented mapping from dashboard. Test added.
- [x] G.6 `requestAppointment` mutation + `AppointmentRequest` type implemented. Test added.
- [x] G.7 `registerDeviceToken` + `unregisterDeviceToken` stubs implemented. Test added.
- [x] G.8 `myProgressSummary` query implemented backed by existing `obtenerProgreso`. (Optional test — skipped per plan.)
- [x] `npm test` — 31/31 green (6 bff + 25 graphql).
- [x] Curl smoke (§11) — 9/9 llamadas devuelven `data` sin `errors`.
- [x] CLAUDE.md `/mobile/backend/CLAUDE.md` updated: Phase G section with all `[x]`.
- [x] Engram saved with title `mobile BFF Phase G — schema sync DONE`.

---

## 13. AFTER G — checkpoints

After this plan turns green, the Doer SHALL stop at Checkpoint A in `/mobile/PLAN.md` §5 and request the developer's sign-off before touching the frontend. **Do not** start frontend work until the developer approves.

---

*This file is the single source of truth for BFF Phase G. Phases A-E (legacy) live in git history.*
