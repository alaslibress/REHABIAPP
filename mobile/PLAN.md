# PLAN.md — Mobile Domain: Frontend ↔ BFF Schema Sync (`stats-implementation`)

> **Author:** Agent 2 Thinker (Opus). PRESCRIPTIVE. Doer (Sonnet) MUST follow step by step.
> **Domain:** `/mobile/` (root coordinator) — links to `/mobile/backend/PLAN.md` and `/mobile/frontend/PLAN.md`.
> **Branch:** `stats-implementation`.
> **Date:** 2026-05-05.
> **Trigger:** Expo Go device shows seven `[GraphQL] VALIDATION_ERROR` warnings on bootstrap (`GetMyTreatments`, `GetMyProfile`, `GetMyBodyPartProgress`, `GetMyAssignedGames`) and the test user `admin / admin` cannot reach the home screen. App is unusable post-login.

---

## 0. ROOT CAUSE (READ BEFORE IMPLEMENTING)

The frontend `Phase 4` queries (brought from branch `mobile`) ask for fields and types that **do not exist** in the BFF GraphQL schema. Apollo Server validates each query against the schema, fails with `GRAPHQL_VALIDATION_FAILED`, and the BFF `errorFormatter` middleware (`mobile/backend/src/middleware/errorFormatter.js` line 47-60) rewrites that to BFF's own `VALIDATION_ERROR` (subtitle `Datos invalidos`). That is exactly what Metro logs.

**Login itself works** in mock mode (`MOCK_API=true`, see `mobile/backend/src/services/authService.js` line 18-22 mock credentials). The user perceives "no deja iniciar sesion" because:

1. `LOGIN_MUTATION` succeeds (token stored in SecureStore).
2. `useAuthStore.login` (`mobile/frontend/src/store/authStore.ts` line 37) calls `useBootstrapStore.getState().hydrate()`.
3. `bootstrapStore.hydrate` (`mobile/frontend/src/store/bootstrapStore.ts` line 31-37) fires five queries in parallel via `Promise.allSettled`. Four of them fail with `VALIDATION_ERROR`. `progressStore.fetch` (line 26-32) has **no try/catch**, so its rejection escapes. `useErrorStore.showError` is invoked from `userStore.fetchProfile`'s catch and from `treatmentsStore.fetch`'s catch, painting an error popup over the home screen.
4. `errorStore` re-shows the popup on every refresh attempt, blocking the user.

Therefore the fix is: **make the BFF schema match what the frontend already asks for, plus harden the frontend bootstrap so a single store failure cannot block the whole UI.**

The frontend stays on the `Phase 4` query shape — it is more polished and already wired to UI components (`BodyDiagram`, `AppointmentCard`, `DisabilitySection`, `GameCard`, etc.). The BFF schema is the source of drift and is what we change.

---

## 1. SCOPE AND ARCHIVE STATUS

| Sub-plan                              | Status   | Action                                                                 |
|---------------------------------------|----------|------------------------------------------------------------------------|
| `/mobile/PLAN.md` (this file)         | NEW      | Coordination + acceptance + checkpoints                                |
| `/mobile/backend/PLAN.md`             | REWRITE  | Phase G: BFF schema alignment + new resolvers + mock data              |
| `/mobile/frontend/PLAN.md`            | REWRITE  | Phase 5-bridge: defensive bootstrap + verification + Phase 5 deferred  |

The previous plans (`mobile/PLAN.md` Login-debug 2026-04-05, `mobile/frontend/PLAN.md` past appointments closure, `mobile/backend/PLAN.md` Phases A-E history) are preserved in git history. Do NOT preserve their text inline; the new files supersede them.

---

## 2. ORDER OF EXECUTION (BLOCKING DEPENDENCY)

```
[1] BFF schema sync           ─────────►   App bootstrap stops emitting VALIDATION_ERROR
       (mobile/backend Phase G)              Frontend renders profile/treatments/progress/games/appointments

[2] Frontend bootstrap hardening ────────►   A single store failure no longer blocks the UI
       (mobile/frontend Phase 5-bridge)

[3] Verification on Expo Go               ►   Manual end-to-end check on a real Android device
       (this file §5)
```

**Phase G of the BFF MUST land first.** Until the schema serves the existing queries cleanly, no frontend work can be tested.

---

## 3. NON-NEGOTIABLES (apply across both subdomains)

- **Do NOT** rewrite the frontend GraphQL queries. They are the authoritative shape of the UI.
- **Do NOT** introduce new dependencies in `mobile/frontend` (already has everything: `@apollo/client@4.1.6`, `expo-constants`, `expo-secure-store`, `react-native-chart-kit`, `expo-file-system`, `expo-sharing`, `expo-notifications`).
- **Do NOT** introduce a database, ORM, or Redis client in `mobile/backend`. Mock data lives in `apiClient.js` only.
- **Do NOT** alter `authService.js` mock credentials — `admin / admin` (with identifiers `admin`, `admin@rehabiapp.com`, `12345678Z`) MUST keep working.
- **Do NOT** touch `/api`, `/data`, `/desktop`. The BFF either consumes existing endpoints or returns mock data when `MOCK_API=true`.
- All new code comments in **Spanish** (root §4.5). Plan documents in English. No emojis. No diacritics in new strings.
- All new GraphQL types follow the existing **camelCase field naming on the wire** convention — frontend queries are camelCase; mock data is kebab- or snake-case on the Java side, the BFF maps it.
- Every new resolver/typeDef must have a unit test in `mobile/backend/test/graphql.test.js`.
- After every BFF change, run `npm test` from `mobile/backend/` — the suite is fast (~16 tests) and is the gate.

---

## 4. ENVIRONMENT EXPECTATIONS

| Surface              | Command                                                         | Expected output                                            |
|----------------------|-----------------------------------------------------------------|------------------------------------------------------------|
| BFF (mock)           | `cd mobile/backend && npm run dev`                              | `BFF mobile-backend iniciado port=3000 mockApi=true`       |
| BFF health           | `curl http://localhost:3000/health`                             | `{"status":"UP"}`                                          |
| BFF login (curl)     | `mutation { login(identifier: "admin", password: "admin") {...} }` | `accessToken`, `refreshToken`, `expiresAt` populated     |
| Frontend             | `cd mobile/frontend && npm start`                               | Metro logs `[Apollo] URL del BFF resuelta: http://<IP>:3000/graphql` |
| Frontend on device   | Expo Go → scan QR (same WiFi)                                   | Login screen renders                                       |

The Java API at `:8080` is **NOT REQUIRED** during this iteration. All BFF endpoints fall back to mock data when `MOCK_API=true`.

---

## 5. REVIEW CHECKPOINTS (mandatory)

Three pause-and-verify checkpoints. The Doer SHALL stop at each one, run the verification command, paste the result back to the developer, and only proceed once the developer signs off.

### Checkpoint A — after BFF Phase G.1 (schema + resolvers)

After completing BFF tasks G.1 to G.5 (see `/mobile/backend/PLAN.md`), run:

```bash
cd /home/alaslibres/DAM/RehabiAPP/mobile/backend
npm test
```

**Pass condition:** All existing tests still green. New tests for `me{numSs,sexo,avatarDataUri}`, `myTreatments{codTrat,...}`, `myBodyPartProgress`, `bodyPartMetrics`, `myAssignedGames`, `treatmentDocument`, `requestAppointment`, `registerDeviceToken`, `unregisterDeviceToken` pass.

If any fails, enter Self-Healing (root `CLAUDE.md` §10.3) and rerun.

### Checkpoint B — after Frontend Phase 5-bridge

After completing frontend tasks (`/mobile/frontend/PLAN.md`), in two terminals:

```bash
# T1
cd /home/alaslibres/DAM/RehabiAPP/mobile/backend && npm run dev

# T2
cd /home/alaslibres/DAM/RehabiAPP/mobile/frontend && npm start
```

In Expo Go (Pixel 8 or any physical Android in same WiFi):

1. Open app — login screen renders, no popup.
2. Login with `admin / admin` — navigate to home tab.
3. **Metro must show ZERO `VALIDATION_ERROR` warnings during bootstrap.**
4. All five tabs (`profile`, `treatments`, `progress`, `games`, `appointments`) render with mock data — no spinning skeleton, no error popup.

Pass condition: every tab paints. If a single tab is stuck on a skeleton or shows an error, STOP and report.

### Checkpoint C — final acceptance

Same setup as B, but exercise the navigation paths:

1. Tap a treatment card → "Descargar PDF" → `Sharing` opens (or simulator fails silently — acceptable).
2. Tap a game card → "Jugar" → WebView opens with the mock Unity URL — the URL is fine, the page may 404 (mock URL is fictional).
3. Tap "Solicitar cita" in appointments → form renders → submit returns success (mock).
4. Pull to refresh on every tab — refresh completes, no error popup.
5. Background the app for 35 minutes (or simulate by setting `JWT_EXPIRATION_MS=60000` in BFF and waiting). On next request, the app should auto-logout via `cerrarSesionPorExpiracion` (`client.ts` line 99).

Pass condition: all five paths green. The app is now considered "fully functional in mock mode".

---

## 6. WHAT REMAINS BEYOND THIS PLAN (parking lot)

Once Checkpoint C passes, the following items from `mobile/CLAUDE.md` Phase 5 are still pending. They are **out of scope** for this iteration and **MUST NOT** be implemented as part of the schema sync:

- 5.1 Booking screen with date/time picker against real practitioner availability — currently the BFF mocks `bookAppointment` and the frontend uses `requestAppointment` form. The two flows coexist intentionally.
- 5.2 AI WhatsApp chatbot — depends on external N8N + Twilio integration.
- 5.3 Push notifications backend wiring — `registerDeviceToken` / `unregisterDeviceToken` will be **stubbed** (return `true`) in this iteration; real APNs/FCM dispatch is Phase 5.3.
- 5.4 Offline-first cache — Apollo `InMemoryCache` is in use, but no persistence (`AsyncStorage` adapter) is configured. Phase 5.4 is a separate proposal.

Open these as new `/sdd-new` proposals after Checkpoint C signs off.

Additionally, real `/api` integration (replace mocks) waits for `/api` Phase 5-9 to fully ship and for `/api` Phase 11 (build fix) to be merged — see `api/PLAN.md` Phase 11 §11.5.

---

## 7. ACCEPTANCE CRITERIA (signed off by developer)

The Doer reports "mobile fully functional in mock mode" only when all are true:

- [x] `mobile/backend/npm test` — 25/25 green.
- [x] `mobile/frontend` starts on Expo Go without warnings during initial bootstrap.
- [x] `admin / admin` login via Pixel 8 reaches the home tab in <3 seconds.
- [x] All five tabs render mock data without throwing the global error popup.
- [x] `mobile/CLAUDE.md` updated: Phase G/5-bridge entries added with `[x]`.
- [x] No new dependencies in either `package.json`.
- [x] Engram saved with `mem_save` titled `mobile Phase G — schema sync DONE`.

---

*This file is the coordinator for `/mobile/backend/PLAN.md` and `/mobile/frontend/PLAN.md`. The detailed code instructions live there.*
