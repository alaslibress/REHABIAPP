# Plan 04 — Progreso (Progress)

Depends on: Plan 00.

---

## 1. Purpose / UX

Screen shows a front-view SVG human body. Body parts:
- **Active** (patient has treatment/disability attached) → filled with `primary-600`, tappable, soft shadow on press.
- **Inactive** → filled with `border` grey, non-tappable (opacity 0.5).

Tapping an active body part opens a modal with:
- Title: body part name (e.g. "Brazo derecho"). 
- Line chart (`react-native-chart-kit` LineChart): X axis = dates, Y axis = normalized score 0–100. Data from recent game sessions tied to that body part.
- Progress bar: average normalized 0–100%. Use custom `<View>` with width % and `bg-primary-600`.
- Text: `"Has mejorado un {deltaPct}% en las últimas {period}."` — where `deltaPct` = improvement rate from BFF; `period` = "4 semanas" default.
- Close button top-right (`close-outline`).

Header title: `Progreso`.

Empty state (no active body parts): `<EmptyState icon="body-outline" title="Sin zonas activas" message="No tienes tratamientos asignados a zonas del cuerpo." />`.

Empty modal state (no metrics for tapped part): inline text in modal "Sin datos todavía. Juega para ver tu progreso." + progress bar at 0%.

---

## 2. Modals / popups

- **ProgressChartModal** — main tap target.
- **ErrorPopup**:
  - `BODY_PART_NO_DATA` used internally but rendered inline in modal (NOT popup) to avoid blocking.
  - `NETWORK_ERROR` popup on fetch fail.

---

## 3. Components to add

`src/components/BodyDiagram.tsx`:
- Props: `{ parts: BodyPartProgress[]; onPressPart: (part: BodyPartProgress) => void; }`
- Renders one `<Svg viewBox="0 0 300 600">` with `<Path>` children for each anatomical region.
- Static SVG paths bundled — list of body parts: `HEAD`, `NECK`, `TORSO`, `LEFT_SHOULDER`, `RIGHT_SHOULDER`, `LEFT_ARM`, `RIGHT_ARM`, `LEFT_HAND`, `RIGHT_HAND`, `LEFT_HIP`, `RIGHT_HIP`, `LEFT_LEG`, `RIGHT_LEG`, `LEFT_FOOT`, `RIGHT_FOOT`. 15 regions.
- Each `<Path>` tapped via `onPress` from `react-native-svg` (wrap with `<G>` + Pressable overlay if needed).
- Fill colour computed from `parts[partId].hasTreatment`.

Ship the SVG path data as a constant map `BODY_PART_PATHS` in `src/components/bodyPaths.ts` (doer fills with a simple schematic — boxes/rounded rectangles per region, doesn't have to be anatomically realistic, just identifiable). Order: render torso first, limbs on top.

`src/components/ProgressChartModal.tsx`:
- Props: `{ visible; onClose; bodyPart: BodyPartProgress | null; metrics: BodyPartMetric[]; loadingMetrics; }`.
- On open, parent triggers `progressStore.loadMetrics(bodyPart.id)`.

`src/components/ProgressBar.tsx`:
- Props: `{ value: number /* 0..100 */; }`. Track bg-border, fill bg-primary-600, rounded-full, height 8dp.

---

## 4. Zustand slice — `src/store/progressStore.ts`

```ts
type ProgressState = {
  bodyParts: BodyPartProgress[];       // coloured map
  metricsByPart: Record<string, BodyPartMetric[]>;
  loadingMetrics: boolean;
  hydrated: boolean;
  fetch: () => Promise<void>;
  loadMetrics: (bodyPartId: string) => Promise<void>;
  reset: () => void;
};
```

`fetch()` → `GET_MY_BODY_PART_PROGRESS` → stores bodyParts.

`loadMetrics(id)` → `GET_BODY_PART_METRICS(id)` → stores in `metricsByPart[id]`. Cached; don't refetch if already present (unless pull-to-refresh).

---

## 5. Types — `src/types/progress.ts` (new)

```ts
export type BodyPartId =
  | 'HEAD' | 'NECK' | 'TORSO'
  | 'LEFT_SHOULDER' | 'RIGHT_SHOULDER'
  | 'LEFT_ARM' | 'RIGHT_ARM'
  | 'LEFT_HAND' | 'RIGHT_HAND'
  | 'LEFT_HIP' | 'RIGHT_HIP'
  | 'LEFT_LEG' | 'RIGHT_LEG'
  | 'LEFT_FOOT' | 'RIGHT_FOOT';

export type BodyPartProgress = {
  id: BodyPartId;
  name: string;            // localized (Spanish)
  hasTreatment: boolean;
  progressPct: number;     // 0..100, 0 if no data
  improvementPct: number;  // -100..100, delta last period
  periodLabel: string;     // e.g. '4 semanas'
};

export type BodyPartMetric = {
  date: string;            // YYYY-MM-DD
  score: number;           // 0..100 normalized
  metricType: string;      // e.g. 'ACCURACY'
};
```

---

## 6. Apollo — queries

`src/services/graphql/queries/progress.ts` (new):

```ts
export const GET_MY_BODY_PART_PROGRESS = gql`
  query GetMyBodyPartProgress {
    myBodyPartProgress {
      id
      name
      hasTreatment
      progressPct
      improvementPct
      periodLabel
    }
  }
`;

export const GET_BODY_PART_METRICS = gql`
  query GetBodyPartMetrics($bodyPartId: ID!) {
    bodyPartMetrics(bodyPartId: $bodyPartId) {
      date
      score
      metricType
    }
  }
`;
```

---

## 7. BFF — typeDefs

`src/graphql/typeDefs/progress.js` (new):

```graphql
type BodyPartProgress {
  id: ID!
  name: String!
  hasTreatment: Boolean!
  progressPct: Float!
  improvementPct: Float!
  periodLabel: String!
}

type BodyPartMetric {
  date: String!
  score: Float!
  metricType: String!
}

extend type Query {
  myBodyPartProgress: [BodyPartProgress!]!
  bodyPartMetrics(bodyPartId: ID!): [BodyPartMetric!]!
}
```

---

## 8. BFF — resolvers + service

`src/graphql/resolvers/progress.js` (new) — both resolvers `requireAuth`, call `progressService`.

`src/services/progressService.js` (new):
- `obtenerProgresoCorporal(dniPac, javaToken)`: GET `/api/pacientes/{dniPac}/progreso/partes-cuerpo`.
- `obtenerMetricasParte(dniPac, bodyPartId, javaToken)`: GET `/api/pacientes/{dniPac}/progreso/partes-cuerpo/{bodyPartId}/metricas?limit=30`.

**Mocks** in `apiClient.js`:
- `/api/pacientes/*/progreso/partes-cuerpo` → returns all 15 body parts, 3 `hasTreatment=true` (`RIGHT_ARM`, `RIGHT_SHOULDER`, `RIGHT_LEG`), rest false. Active ones have `progressPct` 45–80, `improvementPct` 10–25, `periodLabel: '4 semanas'`.
- `/api/pacientes/*/progreso/partes-cuerpo/*/metricas` → returns 8 data points, dates descending weekly for 8 weeks, scores ascending 40→78, `metricType: 'ACCURACY'`.

---

## 9. Java API contract (Agent 1)

- `GET /api/pacientes/{dniPac}/progreso/partes-cuerpo` — new. For each body part in `parte_cuerpo`: compute `hasTreatment` (join `tratamiento_parte_cuerpo` ← `paciente_tratamiento` for this patient), compute aggregated progress from `metrica_sesion` via `sesion_juego.dni_pac`. Return one row per body part (15 rows).
- `GET /api/pacientes/{dniPac}/progreso/partes-cuerpo/{bodyPartId}/metricas?limit=30` — new. Returns recent session metrics weighted to this body part.

Both endpoints require SPECIALIST or the patient themselves (self-service). RBAC check in Java.

---

## 10. DB gaps (Agent 1)

- **`parte_cuerpo`** — seed table:
  - `id_parte VARCHAR(30) PK` (enum values match `BodyPartId` type).
  - `nombre_parte VARCHAR(100) NOT NULL`
  - `lado VARCHAR(10)` (LEFT|RIGHT|CENTER)
  - Seed 15 rows at migration time.
- **`tratamiento_parte_cuerpo`** — junction:
  - PK `(cod_trat, id_parte)`
  - `cod_trat VARCHAR(20) FK tratamiento CASCADE`
  - `id_parte VARCHAR(30) FK parte_cuerpo RESTRICT`
  - `primaria BOOLEAN NOT NULL DEFAULT FALSE`
- **`sesion_juego`** — new:
  - `id_sesion BIGSERIAL PK`
  - `dni_pac VARCHAR(20) FK paciente`
  - `id_juego VARCHAR(20) FK juego`
  - `fecha_inicio TIMESTAMP NOT NULL`
  - `fecha_fin TIMESTAMP`
  - `duracion_segundos INT`
  - `puntos NUMERIC(8,2)`
  - `estado VARCHAR(20)` (IN_PROGRESS|COMPLETED|ABANDONED)
- **`metrica_sesion`** — new:
  - `id_metrica BIGSERIAL PK`
  - `id_sesion BIGINT FK sesion_juego CASCADE`
  - `tipo_metrica VARCHAR(30)` (ACCURACY|REACTION_TIME|COMPLETION_RATE|RANGE_OF_MOTION)
  - `valor NUMERIC(10,4) NOT NULL`
  - `unidad VARCHAR(20)`
  - `registrado_en TIMESTAMP DEFAULT NOW()`
  - Index `(id_sesion, tipo_metrica)`.

Envers audit optional for these (likely not required since data is append-only and plentiful). Agent 1 decides.

---

## 11. Error codes

`NETWORK_ERROR`, `INTERNAL_ERROR`, `TOKEN_EXPIRED`, `BODY_PART_NO_DATA`.

---

## 12. Edge cases

- No active body parts → entire tab empty state.
- Active part with no metrics yet → modal opens, shows inline BODY_PART_NO_DATA text + 0% bar, no chart.
- Chart with only 1 data point → render a dot, no line.
- Many points → limit to last 30.
- Orientation change → re-measure SVG viewport.
- Dark mode → swap body fill colours: inactive `#1F2937`, active keeps `primary-600`.

---

## 13. Files touched

Frontend:
- `app/(tabs)/progress.tsx` — full rewrite.
- `src/components/BodyDiagram.tsx`, `bodyPaths.ts`, `ProgressChartModal.tsx`, `ProgressBar.tsx` — new.
- `src/store/progressStore.ts` — new.
- `src/services/graphql/queries/progress.ts` — new.
- `src/types/progress.ts` — new.

Backend:
- `src/graphql/typeDefs/progress.js` — new.
- `src/graphql/resolvers/progress.js` — new.
- `src/graphql/typeDefs/index.js`, `resolvers/index.js` — register new.
- `src/services/progressService.js` — new.
- `src/services/apiClient.js` — mocks.
- `test/graphql.test.js` — add test for `myBodyPartProgress` + `bodyPartMetrics`.

---

## 14. Acceptance

1. Progreso tab renders body diagram, 3 parts coloured.
2. Tap inactive part → no reaction.
3. Tap active part → modal opens, chart renders, bar shows progress %, text shows improvement.
4. Tap part with no metrics → modal shows "Sin datos todavía." + 0% bar.
5. Close modal → returns to diagram.
6. Pull-to-refresh → progressStore refetches.
7. Dark mode flips fills/strokes correctly.
8. BFF tests pass.
