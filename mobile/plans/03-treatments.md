# Plan 03 — Tratamientos (Treatments)

Depends on: Plan 00.

---

## 1. Purpose / UX

Scrollable list of treatment cards grouped by disability. For each disability:
- Section header: disability name + current level badge.
- Treatment cards below (floating):
  - Treatment name (title semibold).
  - Progression level small pill.
  - Collapsible body (default collapsed). Tap header → expand.
  - When expanded:
    - **Resumen** — short paragraph (from `summary`).
    - **Materiales necesarios** — bulleted list.
    - **Medicación** — bulleted list. If empty → hide section.
    - **Descargar PDF** primary button with `document-text-outline` icon. Downloads + opens via `expo-sharing`. Disabled if `documentUrl` null → show text "PDF pendiente".

Header title: `Tratamientos`.

Empty state: `<EmptyState icon="medkit-outline" title="Sin tratamientos" message="No tienes tratamientos asignados todavía." />`.

Pull-to-refresh → `treatmentsStore.fetch()`.

---

## 2. Modals / popups

- **Download progress modal** — shown during PDF download. Contains spinner + "Descargando documento...". Dismisses on completion or error.
- **ErrorPopup** via errorStore:
  - `DOCUMENT_DOWNLOAD_FAILED` (download or sharing failure).
  - `NETWORK_ERROR` on fetch fail.
  - `NO_TREATMENTS_ASSIGNED` treated inline (no popup) — shown as empty state.

---

## 3. Components to add

`src/components/TreatmentCard.tsx`:
- Props: `{ treatment: Treatment; onDownloadPdf: (codTrat: string) => Promise<void>; }`
- Local expanded state.
- Chevron icon rotates on expand.
- Uses `FloatingCard`.

`src/components/DisabilitySection.tsx`:
- Props: `{ disability: Disability; treatments: Treatment[]; onDownloadPdf: ... }`.
- Renders header + list.

`src/components/LevelBadge.tsx`:
- Props: `{ level: number; name?: string; }`. Pill with primary-100 bg, primary-700 text.

`src/components/DownloadProgressModal.tsx`:
- Uses `BackdropModal`. Spinner + text.

---

## 4. Zustand slice — `src/store/treatmentsStore.ts`

```ts
type TreatmentsState = {
  items: Treatment[];
  disabilities: Disability[];   // copied from userStore after hydrate
  loading: boolean;
  hydrated: boolean;
  fetch: () => Promise<void>;
  downloadPdf: (codTrat: string) => Promise<string>;  // returns local file URI
  reset: () => void;
};
```

`fetch()` calls `GET_MY_TREATMENTS` (extended), stores items. On demand also pulls disabilities from `userStore.getState().disabilities` (or adds a new store slice if not present — use userStore which already has fetch for profile + disabilities).

`downloadPdf(codTrat)`:
- Calls `TREATMENT_DOCUMENT_QUERY` — returns base64 or URL.
- If base64 → `FileSystem.writeAsStringAsync(fileUri, base64, { encoding: Base64 })`.
- If URL → `FileSystem.downloadAsync(url, fileUri)`.
- Then `Sharing.shareAsync(fileUri)` to open/share.
- Throws `DOCUMENT_DOWNLOAD_FAILED` on any failure (logged internally).

---

## 5. Types — `src/types/treatments.ts`

Extend existing:

```ts
export type Treatment = {
  id: string;
  codTrat: string;
  name: string;
  description: string | null;
  type: string;               // keep flexible
  visible: boolean;
  progressionLevel: number;
  disabilityCode: string;     // NEW — to group by disability
  summary: string | null;         // NEW
  materials: string[];            // NEW
  medication: string[];           // NEW
  documentUrl: string | null;     // NEW — signed URL or null
  hasDocument: boolean;           // NEW
};

export type Disability = {
  id: string;
  codDis: string;
  name: string;
  description: string | null;
  currentLevel: number;
};
```

---

## 6. Apollo — queries

`src/services/graphql/queries/treatments.ts` — replace:

```ts
export const GET_MY_TREATMENTS = gql`
  query GetMyTreatments {
    myTreatments {
      id
      codTrat
      name
      description
      type
      visible
      progressionLevel
      disabilityCode
      summary
      materials
      medication
      documentUrl
      hasDocument
    }
  }
`;

export const GET_TREATMENT_DOCUMENT = gql`
  query GetTreatmentDocument($codTrat: ID!) {
    treatmentDocument(codTrat: $codTrat) {
      fileName
      mimeType
      base64
      url
    }
  }
`;
```

---

## 7. BFF — typeDefs

`src/graphql/typeDefs/treatment.js` — extend `Treatment` + new `TreatmentDocument`:

```graphql
type Treatment {
  id: ID!
  codTrat: String!
  name: String!
  description: String
  type: String!
  visible: Boolean!
  progressionLevel: Int!
  disabilityCode: String!
  summary: String
  materials: [String!]!
  medication: [String!]!
  documentUrl: String
  hasDocument: Boolean!
}

type TreatmentDocument {
  fileName: String!
  mimeType: String!
  base64: String        # only if inline
  url: String           # only if signed URL
}

extend type Query {
  treatmentDocument(codTrat: ID!): TreatmentDocument!
}
```

Remove `myTreatments(disabilityId, level)` args (simplify). If filtering needed later, add back.

---

## 8. BFF — resolvers + services

`src/graphql/resolvers/treatment.js` — update `myTreatments` to pass through new fields; add `treatmentDocument` resolver (`requireAuth` → `documentService.obtenerDocumentoTratamiento`).

New service `src/services/documentService.js`:

```js
async function obtenerDocumentoTratamiento(dniPac, codTrat, javaToken) {
  const res = await apiClient.get(`/api/pacientes/${dniPac}/tratamientos/${codTrat}/documento`, javaToken);
  if (!res) throw crearError('DOCUMENT_DOWNLOAD_FAILED');
  return { fileName: res.fileName, mimeType: res.mimeType, base64: res.base64 ?? null, url: res.url ?? null };
}
```

Extend `treatmentService.js` to request full enriched treatments: `/api/pacientes/{dniPac}/tratamientos` expected to return enriched DTO (see contract).

**Mocks** in `apiClient.js`:
- `/api/pacientes/*/tratamientos` → returns:
  ```js
  [{
    codTrat: 'TRAT-001', nombreTrat: 'Movilización activa hombro',
    descripcion: null, tipo: 'EXERCISE', visible: true,
    idNivel: 2, nivelNombre: 'Subagudo',
    codDis: 'DIS-001',
    resumen: 'Ejercicios suaves de rango articular.',
    materiales: ['Banda elástica ligera', 'Esterilla'],
    medicacion: ['Paracetamol 500mg si dolor'],
    tieneDocumento: true, urlDocumento: null,
  }]
  ```
- `/api/pacientes/*/tratamientos/*/documento` → returns:
  ```js
  { fileName: 'tratamiento-TRAT-001.pdf', mimeType: 'application/pdf',
    base64: '<base64-of-tiny-sample-pdf>', url: null }
  ```
  (ship a tiny "hola mundo" PDF base64 string constant so download flow is testable.)

---

## 9. Java API contract (Agent 1)

- `GET /api/pacientes/{dniPac}/tratamientos` — extend DTO: `{ codTrat, nombreTrat, descripcion, tipo, visible, idNivel, nivelNombre, codDis, resumen, materiales[], medicacion[], tieneDocumento, urlDocumento }`.
- `GET /api/pacientes/{dniPac}/tratamientos/{codTrat}/documento` — new. Returns document: either inline base64 (`{fileName, mimeType, base64}`) or signed URL (`{fileName, mimeType, url}`).

Desktop ERP uploads PDFs + fills `resumen/materiales/medicacion` via a form → stored in `documento_tratamiento` row.

---

## 10. DB gaps (Agent 1)

- **`documento_tratamiento`** — new:
  - `id_doc BIGSERIAL PK`
  - `dni_pac VARCHAR(20) FK paciente CASCADE`
  - `cod_trat VARCHAR(20) FK tratamiento RESTRICT`
  - `resumen TEXT`
  - `materiales TEXT[]` (Postgres array) or json
  - `medicacion TEXT[]`
  - `file_name VARCHAR(200)`
  - `mime_type VARCHAR(100)`
  - `contenido BYTEA` (or `url_s3 TEXT` if moved to S3 — decide at /data level)
  - `fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW()`
  - `fecha_actualizacion TIMESTAMP`
  - Unique `(dni_pac, cod_trat)` — one active doc per patient-treatment.

If stored as BYTEA, restrict size (say, 5 MB). If S3, add `url_s3` and use signed URLs.

Envers audit `documento_tratamiento_audit` (omit BYTEA from audit).

---

## 11. Error codes

`NETWORK_ERROR`, `DOCUMENT_DOWNLOAD_FAILED`, `INTERNAL_ERROR`, `TOKEN_EXPIRED`. Empty list handled inline (not an error).

---

## 12. Edge cases

- `hasDocument=false` → button replaced by "PDF pendiente" disabled state.
- `materials` or `medication` empty arrays → hide that subsection.
- PDF too big / network timeout → `DOCUMENT_DOWNLOAD_FAILED`.
- Expand/collapse retained in component state — resets on tab unmount (acceptable for MVP).
- Multiple disabilities → multiple sections.

---

## 13. Files touched

Frontend:
- `app/(tabs)/treatments.tsx` — full rewrite.
- `src/components/TreatmentCard.tsx`, `DisabilitySection.tsx`, `LevelBadge.tsx`, `DownloadProgressModal.tsx` — new.
- `src/store/treatmentsStore.ts` — new.
- `src/services/graphql/queries/treatments.ts` — replace.
- `src/types/treatments.ts` — extend.

Backend:
- `src/graphql/typeDefs/treatment.js` — extend.
- `src/graphql/resolvers/treatment.js` — extend.
- `src/services/treatmentService.js` — extend.
- `src/services/documentService.js` — new.
- `src/services/apiClient.js` — mocks.
- `test/graphql.test.js` — add test for `treatmentDocument` + enriched `myTreatments`.

---

## 14. Acceptance

1. Tratamientos tab shows 1+ disability sections with treatment cards.
2. Expand/collapse works. Sections hide when arrays empty.
3. Descargar PDF → progress modal → opens share sheet with mock PDF.
4. Failure path (simulate apiClient to return null) → `DOCUMENT_DOWNLOAD_FAILED` popup.
5. Empty treatments → empty state.
6. Dark mode renders correctly.
7. BFF tests pass.
