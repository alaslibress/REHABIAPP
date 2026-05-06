# PLAN.md — Mobile Frontend: Phase 5-bridge (Bootstrap Hardening)

> **Author:** Agent 2 Thinker (Opus). PRESCRIPTIVE.
> **Domain:** `/mobile/frontend/`.
> **Branch:** `stats-implementation`.
> **Date:** 2026-05-05.
> **Coordinator:** `/mobile/PLAN.md` §2 — runs AFTER `/mobile/backend/PLAN.md` Phase G is green.
> **Pass condition:** With BFF Phase G applied and the BFF running on `MOCK_API=true`, login with `admin / admin` reaches the home tab on a physical Android (Expo Go) within 3 seconds, every tab renders mock data, and Metro shows ZERO `VALIDATION_ERROR` warnings.
> **Doer model:** Sonnet. Read every section in order, do NOT skip steps.

---

## 0. CONTEXT

The frontend is **mostly correct**. The seven `VALIDATION_ERROR` warnings the developer reported were caused by the BFF schema not matching the queries — that is fixed in `mobile/backend/PLAN.md` Phase G. This plan addresses three smaller frontend issues that surfaced during that diagnosis:

1. `progressStore.fetch` lacks try/catch — when the underlying query fails, the rejection escapes and is logged as `[Bootstrap] Fallo el store 4: [CombinedGraphQLErrors: ...]`. After Phase G the query will succeed, but defensive handling MUST exist so that any single store failure cannot bring down the bootstrap.
2. `userStore.fetchProfile` re-throws after calling `parseGraphQLError` — that throw bubbles to `bootstrapStore.hydrate` AND is captured by `Promise.allSettled` AND triggers `useErrorStore.showError` indirectly via re-thrown error from `authStore.login`'s call chain. The chain is hard to reason about. We unify so that bootstrap-time errors do NOT pop the global error popup; instead, each tab decides if it has data or shows an empty state.
3. The `errorStore` popup persists across navigation, which is desired for action errors (e.g. "couldn't book appointment"), but undesired for bootstrap errors (we want the screen to show its own empty state instead of a global modal).

This plan is **purely defensive**. It introduces no new GraphQL operations, no new screens, no new dependencies.

---

## 1. NON-NEGOTIABLES (Doer)

- Edit ONLY the files listed in §2 to §4. Do NOT reformat anything else.
- Do NOT install or update dependencies. `package.json` is frozen.
- Do NOT touch `/mobile/backend/`.
- Do NOT change any GraphQL query/mutation file under `src/services/graphql/`. They are correct as-is — the BFF was the broken side.
- Spanish strings in new code (no diacritics for new text). No emojis.
- After every change, save and re-trigger Metro reload (`r` in the Metro terminal). Verify on Expo Go device.

---

## 2. TASK F.1 — Defensive try/catch in `progressStore.fetch`

### 2.1 Edit `src/store/progressStore.ts`

Replace the `fetch` method (lines 26-32) with:

```ts
    fetch: async function () {
      try {
        const { data } = await client.query({
          query: GET_MY_BODY_PART_PROGRESS,
          fetchPolicy: 'network-only',
        });
        set({ bodyParts: data.myBodyPartProgress ?? [], hydrated: true });
      } catch (err) {
        // El bootstrap NO debe bloquearse por un fallo en el progreso.
        // Marcamos hydrated:true con bodyParts vacio — la pantalla mostrara empty state.
        if (__DEV__) {
          console.warn('[progressStore] Fallo al cargar bodyParts:', (err as Error)?.message);
        }
        set({ bodyParts: [], hydrated: true });
      }
    },
```

Rationale: when the query fails (transient network, 5xx, schema regression after a future BFF change) the bootstrap continues with an empty `bodyParts` array; the `progress.tsx` screen already handles empty state via `EmptyState`.

### 2.2 No other changes in this file

`loadMetrics` already has its own `try/finally`; do NOT modify.

---

## 3. TASK F.2 — Bootstrap silent-by-default policy

The current `bootstrapStore.hydrate` calls each store's `fetch()` via `Promise.allSettled`. Some stores (`userStore`, `treatmentsStore`, `gamesStore`) call `useErrorStore.showError(appError)` inside their catch, which paints the global popup over the home screen. We invert this: during bootstrap, errors are **silent** (logged only); each tab's empty state covers the visual gap. Action errors (e.g. user-initiated "book appointment") keep the popup behavior.

### 3.1 Add `silentMode` flag to `errorStore`

#### File `src/store/errorStore.ts`

Read the current file first. Locate the state interface (typically `ErrorState`) and the `showError(error: AppError)` action. Add a `silent: boolean` flag plus a setter:

```ts
type ErrorState = {
  // ... existing fields
  silent: boolean;
  setSilent: (value: boolean) => void;
};
```

In the store factory:

```ts
silent: false,
setSilent: function (value: boolean) {
  set({ silent: value });
},
```

Modify `showError` so it skips the popup when `silent` is true (still logs):

```ts
showError: function (error: AppError) {
  if (get().silent) {
    if (__DEV__) {
      console.warn('[errorStore] Error suprimido en modo silencioso:', error.code, error.subtitle);
    }
    return;
  }
  set({ currentError: error, isVisible: true });
},
```

> **Doer:** if the existing `errorStore` does NOT export `get` from `create((set, get) => ...)`, refactor to add it. The shape `(set, get) => ({...})` is supported by Zustand without dependency changes.

### 3.2 Wrap bootstrap in silent mode

#### File `src/store/bootstrapStore.ts`

Replace the body of `hydrate` with:

```ts
hydrate: async function () {
  set({ hydrating: true, refreshing: true });
  // Suprimir popups durante el bootstrap — cada tab muestra su propio empty state.
  // Los errores siguen registrados via console.warn para depuracion.
  useErrorStore.getState().setSilent(true);

  try {
    const resultados = await Promise.allSettled([
      useUserStore.getState().fetchProfile(),
      useAppointmentsStore.getState().fetch(),
      useGamesStore.getState().fetch(),
      useTreatmentsStore.getState().fetch(),
      useProgressStore.getState().fetch(),
    ]);

    resultados.forEach(function (resultado, indice) {
      if (resultado.status === 'rejected') {
        if (__DEV__) {
          console.warn(`[Bootstrap] Fallo el store ${indice}:`, resultado.reason);
        }
      }
    });

    // Recordatorios locales — silencioso si fallan
    try {
      const citas = useAppointmentsStore.getState().items as Appointment[];
      const hoy = Date.now();
      for (const cita of citas) {
        if (cita.status === 'SCHEDULED') {
          const [anio, mes, dia] = cita.date.split('-').map(Number);
          const [hora, min] = cita.time.split(':').map(Number);
          const fechaCita = new Date(anio, mes - 1, dia, hora, min).getTime();
          if (fechaCita > hoy) {
            await scheduleAppointmentReminder(cita);
          }
        }
      }
    } catch {
      // No bloquear la hidratacion si fallan las notificaciones
    }

    set({ hydrated: true, hydrating: false, lastHydratedAt: Date.now() });
  } finally {
    // Reactivar popups para errores futuros (acciones del usuario).
    useErrorStore.getState().setSilent(false);
    set({ refreshing: false });
  }
},
```

Add the `useErrorStore` import at the top of the file:

```ts
import { useErrorStore } from './errorStore';
```

The `reset` function needs no changes.

---

## 4. TASK F.3 — Manual refresh path keeps popups

When the user pulls-to-refresh on a single tab, errors should still show. This is already correct because each store's `fetch` is invoked outside `bootstrapStore.hydrate` and `silent` defaults to `false`. **No code change needed**, just verify in Checkpoint B (§5).

---

## 5. VERIFICATION

After F.1 + F.2 are saved and Metro reloads:

1. Start BFF: `cd /home/alaslibres/DAM/RehabiAPP/mobile/backend && npm run dev`. Confirm `mockApi: true`.
2. Start frontend: `cd /home/alaslibres/DAM/RehabiAPP/mobile/frontend && npm start`.
3. Open Expo Go on Pixel 8 (or any Android in same WiFi). Scan QR.
4. Login with `admin / admin`.

**Pass conditions (all must hold simultaneously):**

- Metro logs `[Apollo] URL del BFF resuelta: http://<LAN-IP>:3000/graphql` — IP matches the developer machine's LAN IP.
- Login completes without popup.
- Home tab renders with mock data (greeting "Buenos dias/tardes/noches, Admin").
- Switching to **Tratamientos** tab renders 3 visible treatments grouped by disability (Coxartrosis + Lumbalgia cronica).
- Switching to **Progreso** tab renders the body diagram with at least Cadera derecha + Cadera izquierda + Espalda y tronco highlighted.
- Switching to **Juegos** tab renders at least one game card (Mover la cadera).
- Switching to **Citas** tab renders 3 upcoming appointments + an empty "Historial de citas" state.
- Metro shows ZERO `VALIDATION_ERROR` warnings.
- Pull-to-refresh on any tab updates the data without popup.

**Failure handling:**

- If a tab is stuck on a skeleton, tap the tab a second time to force re-render. If still stuck, check Metro for `[<store>Store] Fallo` warnings — that points to the affected query. Re-run BFF curl smoke from `/mobile/backend/PLAN.md` §11 for that specific query.
- If a popup appears during bootstrap, F.2 was applied incorrectly — re-read §3.

---

## 6. WHAT IS DELIBERATELY NOT IN SCOPE

These items remain in `mobile/CLAUDE.md` Phase 5 and are NOT to be implemented in this iteration:

- `5.1` Booking calendar with practitioner/slot selection.
- `5.2` AI WhatsApp chatbot.
- `5.3` Real push delivery (we only stub `registerDeviceToken` in BFF Phase G.7).
- `5.4` Offline-first cache with `AsyncStorage` Apollo persistor.

Open them as separate `/sdd-new` proposals after Checkpoint C in `/mobile/PLAN.md` §5.

Additionally, **do NOT** rewrite the GraphQL queries to use shapes like `myProgressSummary` (BFF G.8) — that migration belongs to a future welcome-card sprint.

---

## 7. CHECKLIST

- [x] F.1 `progressStore.fetch` wrapped in try/catch with empty-state fallback.
- [x] F.2 `errorStore` exposes `silent` flag + `setSilent`.
- [x] F.2 `bootstrapStore.hydrate` enables silent mode on entry, restores on exit.
- [x] F.3 Manual refresh on a tab still shows popup on error (no code change needed — silent defaults to false outside hydrate).
- [ ] Expo Go run-through (§5 pass conditions) — 8/8 green. (requires physical device)
- [x] No new dependencies in `package.json` (Phase 5-bridge adds zero deps).
- [x] No backend file modified.
- [x] `mobile/frontend/CLAUDE.md` does NOT need an update (it has no checklist — the parent `mobile/CLAUDE.md` tracks Phase 4-6).
- [x] Phase 5.1 — AppointmentRequestForm + historial de citas pasadas + pull-to-refresh + cancel modal (implementado en esta sesion, extra al scope original).
- [x] Phase 5.4 — persist middleware (zustand + AsyncStorage) en userStore, treatmentsStore, gamesStore, appointmentsStore (implementado en esta sesion, extra al scope original).
- [x] Engram saved with title `mobile frontend Phase 5-bridge — bootstrap hardening DONE`.

---

## 8. POST-IMPLEMENTATION

After §5 passes, advance to Checkpoint B in `/mobile/PLAN.md` §5 and request the developer's sign-off.

---

*Phase 4 closure (past appointments, Treatment PDF + Progress charts) was completed on the `mobile` branch and merged in commit `8bfd80c`. Do NOT redo it.*
