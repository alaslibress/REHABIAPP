# Plan 02 — Juegos (Games)

Depends on: Plan 00.

---

## 1. Purpose / UX

Tab shows videogames assigned to the patient. FlatList `numColumns={2}`, each cell is a floating card with:
- Thumbnail (image from `thumbnailUrl` or BYTEA fallback). If missing → gradient placeholder with `game-controller-outline` icon.
- Game name (AppText variant=title weight=semibold, numberOfLines=2).
- Short description (AppText variant=caption, numberOfLines=3).
- Difficulty badge (Fácil/Media/Alta, pill, colour-coded: success/primary/error light backgrounds).
- "Jugar" primary pill button (full-width of card bottom). Stub onPress: `// TODO: abrir WebView con webglUrl`.

Header title: `Juegos`.

Empty state (centered, full tab): `<EmptyState icon="heart-circle-outline" title="¡Qué sano estás!" message="No tienes juegos de rehabilitación pendientes." />`

Pull-to-refresh → `gamesStore.fetch()`.

---

## 2. Modals / popups

- `ErrorPopup` on fetch fail → `NETWORK_ERROR` or `INTERNAL_ERROR`.
- No other modals. "Jugar" shows nothing yet (stub).

---

## 3. Components to add

`src/components/GameCard.tsx`:
- Props: `{ game: AssignedGame; onPlay: (id: string) => void; }`
- Width: `(screenWidth - 48) / 2` (accounting for `p-6` screen + `gap-4` between cards).
- Uses `FloatingCard`. Height auto, image height 120dp, object-fit cover.

`src/components/DifficultyBadge.tsx`:
- Props: `{ level: 'EASY' | 'MEDIUM' | 'HARD' }`
- Maps to colour + Spanish label.

---

## 4. Zustand slice — `src/store/gamesStore.ts`

```ts
type GamesState = {
  items: AssignedGame[];
  loading: boolean;
  hydrated: boolean;
  fetch: () => Promise<void>;
  reset: () => void;
};
```

`fetch()` → Apollo query `GET_MY_ASSIGNED_GAMES`. Errors via `errorStore`. No mutations.

---

## 5. Types — `src/types/games.ts`

Extend existing file:

```ts
export type GameDifficulty = 'EASY' | 'MEDIUM' | 'HARD';

export type AssignedGame = {
  id: string;
  name: string;
  description: string;
  thumbnailUrl: string | null;
  webglUrl: string | null;
  difficulty: GameDifficulty;
  assignedAt: string;
};
```

---

## 6. Apollo — `src/services/graphql/queries/games.ts`

Add:
```ts
export const GET_MY_ASSIGNED_GAMES = gql`
  query GetMyAssignedGames {
    myAssignedGames {
      id
      name
      description
      thumbnailUrl
      webglUrl
      difficulty
      assignedAt
    }
  }
`;
```

Keep existing `GET_MY_GAME_SESSIONS` (used by Progreso plan).

---

## 7. BFF — typeDefs

`src/graphql/typeDefs/game.js` — extend:

```graphql
enum GameDifficulty { EASY MEDIUM HARD }

type AssignedGame {
  id: ID!
  name: String!
  description: String!
  thumbnailUrl: String
  webglUrl: String
  difficulty: GameDifficulty!
  assignedAt: String!
}

extend type Query {
  myAssignedGames: [AssignedGame!]!
}
```

---

## 8. BFF — resolver + service

`src/graphql/resolvers/game.js` — add `myAssignedGames`: `requireAuth`, delegate to `gameService.obtenerJuegosAsignados(dniPac, javaToken)`.

`src/services/gameService.js` — add:

```js
async function obtenerJuegosAsignados(dniPac, javaToken) {
  const res = await apiClient.get(`/api/pacientes/${dniPac}/juegos`, javaToken);
  return res.map(mapAssignedGame);
}
```

Map Java fields `{ idJuego, nombreJuego, descripcion, urlThumbnail, urlWebgl, dificultad, fechaAsignacion }` → GraphQL shape.

**Mock** in `apiClient.js` — path `/api/pacientes/*/juegos` → returns 2 sample games:
```js
[
  { idJuego: 'j1', nombreJuego: 'Alcanza la estrella', descripcion: 'Ejercicio de alcance del brazo derecho.', urlThumbnail: null, urlWebgl: 'https://games.rehabiapp.com/star-reach', dificultad: 'EASY', fechaAsignacion: '2026-04-10' },
  { idJuego: 'j2', nombreJuego: 'Ritmo de pasos', descripcion: 'Coordinación de piernas siguiendo el compás.', urlThumbnail: null, urlWebgl: 'https://games.rehabiapp.com/step-rhythm', dificultad: 'MEDIUM', fechaAsignacion: '2026-04-12' },
]
```

---

## 9. Java API contract (for Agent 1)

- `GET /api/pacientes/{dniPac}/juegos` — new. Returns list of assigned games visible to the patient (join `paciente_juego` + `juego`, filter `visible=true`).
- Response item: `{ idJuego, nombreJuego, descripcion, urlThumbnail, urlWebgl, dificultad, fechaAsignacion }`.

---

## 10. DB gaps (Agent 1)

- **`juego`** table:
  - `id_juego VARCHAR(20) PK`
  - `nombre_juego VARCHAR(200) NOT NULL`
  - `descripcion TEXT`
  - `url_thumbnail TEXT` (S3/CloudFront)
  - `url_webgl TEXT NOT NULL` (S3/CloudFront deployment of Unity WebGL build)
  - `dificultad VARCHAR(10) NOT NULL` (check EASY|MEDIUM|HARD)
  - `activo BOOLEAN NOT NULL DEFAULT TRUE`
  - `created_at TIMESTAMP NOT NULL DEFAULT NOW()`
- **`paciente_juego`** junction:
  - PK `(dni_pac, id_juego)`
  - `dni_pac VARCHAR(20) FK paciente CASCADE`
  - `id_juego VARCHAR(20) FK juego RESTRICT`
  - `visible BOOLEAN NOT NULL DEFAULT TRUE`
  - `fecha_asignacion TIMESTAMP NOT NULL DEFAULT NOW()`

Desktop ERP creates games and assigns them → rows flow into DB → mobile reflects.

Envers audit: `juego_audit`, `paciente_juego_audit`.

---

## 11. Error codes

`NETWORK_ERROR`, `INTERNAL_ERROR`, `TOKEN_EXPIRED`, `PATIENT_NOT_FOUND`. No blocking error for empty list — empty is a legitimate UX state.

---

## 12. Edge cases

- Empty list → empty state, no popup.
- Thumbnail URL broken → onError of `<Image>` falls back to gradient + icon.
- Very long game names → `numberOfLines={2}`, ellipsize tail.
- Offline → cached list from bootstrap.
- Pull-to-refresh while offline → error popup.

---

## 13. Files touched

Frontend:
- `app/(tabs)/games.tsx` — full rewrite.
- `src/components/GameCard.tsx`, `DifficultyBadge.tsx` — new.
- `src/store/gamesStore.ts` — new.
- `src/services/graphql/queries/games.ts` — add `GET_MY_ASSIGNED_GAMES`.
- `src/types/games.ts` — extend with `AssignedGame`, `GameDifficulty`.

Backend:
- `src/graphql/typeDefs/game.js` — extend.
- `src/graphql/resolvers/game.js` — add resolver.
- `src/services/gameService.js` — add function.
- `src/services/apiClient.js` — mock.
- `test/graphql.test.js` — test `myAssignedGames`.

---

## 14. Acceptance

1. Login → Juegos tab shows 2 mock game cards in 2 columns.
2. Clear assignments (empty mock) → empty state with exact copy.
3. Tap Jugar → no crash, stub logs TODO.
4. Dark mode renders cards + badges.
5. Pull-to-refresh triggers `gamesStore.fetch()`.
6. BFF test passes.
