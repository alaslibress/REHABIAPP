# CLAUDE.md - RehabiAPP Mobile Domain

> **File:** `/mobile/CLAUDE.md`
> **Agent:** Agent 2 (Mobile Developer)
> **Role:** Thinker (Opus) + Doer (Sonnet)

---

## 1. PROJECT DEFINITION

This directory contains the complete mobile domain for RehabiAPP, split into two strictly isolated subdirectories:

| Subdirectory | Description | Stack | Local CLAUDE.md |
|-------------|-------------|-------|-----------------|
| `/mobile/frontend` | Patient-facing React Native app | TypeScript, Expo, Apollo Client, Zustand | `/mobile/frontend/CLAUDE.md` (if exists) |
| `/mobile/backend` | Backend-For-Frontend (BFF) | Node.js 20, Express 5, Apollo Server 4 | `/mobile/backend/CLAUDE.md` |

Patients use the mobile app to view their clinical profile, assigned treatments filtered by progression level, game session history, and to schedule appointments. The app also integrates an AI-powered WhatsApp chatbot for automated appointment booking.

The UI must be highly accessible for all age groups, including elderly patients with reduced mobility or vision.

---

## 2. DOMAIN ARCHITECTURE

```
/mobile/
|
|-- CLAUDE.md                   <-- THIS FILE (domain-level context)
|
|-- /frontend                   <-- Patient mobile app (React Native + Expo)
|   |-- app/                    # Expo Router pages (auth, tabs)
|   |-- src/
|   |   |-- components/         # Reusable UI components
|   |   |-- services/graphql/   # Apollo Client queries and mutations
|   |   |-- store/              # Zustand stores (auth, user, error)
|   |   |-- types/              # TypeScript type definitions
|   |   |-- utils/              # Helpers (error handler, greeting)
|   |-- PLAN.md                 # Frontend implementation plan
|
|-- /backend                    <-- BFF (Node.js + Express + Apollo Server)
|   |-- src/
|   |   |-- graphql/            # TypeDefs + Resolvers
|   |   |-- services/           # API client + domain services
|   |   |-- middleware/         # Auth, greeting, error formatter
|   |   |-- utils/              # Error codes, timezone utilities
|   |   |-- index.js            # Entry point
|   |   |-- config.js           # Configuration + secrets
|   |-- test/                   # Tests
|   |-- CLAUDE.md               # Backend-specific context
|   |-- PLAN.md                 # Backend implementation plan
|   |-- Dockerfile              # Multi-stage container image
```

### Communication Flow (BFF Pattern)

```
Mobile App (frontend)
    |
    | GraphQL (Apollo Client -> Apollo Server)
    | Port 3000 (BFF)
    | JWT BFF token in Authorization header
    | X-Timezone header for greeting context
    v
BFF Node.js (backend)
    |
    | REST / HTTP (fetch native)
    | Internal K8s network
    | JWT Java token in Authorization header
    v
Java API (Spring Boot, /api)
    |
    v
PostgreSQL / MongoDB (via /data)
```

**CRITICAL RULE:** The frontend NEVER communicates with the Java API (`/api`) directly. ALL traffic routes through the BFF (`/mobile/backend`). This is enforced by K8s NetworkPolicy.

---

## 3. OPERATING RULES

1. **Global context:** Read and respect the root `/CLAUDE.md` before any cross-domain decision. This file takes precedence for mobile-domain decisions. Subdirectory CLAUDE.md files take precedence for their specific scope.

2. **Read local CLAUDE.md first:** Before working in `/mobile/frontend` or `/mobile/backend`, read the corresponding local CLAUDE.md and PLAN.md.

3. **Maintain this file:** When you complete a task, change `[ ]` to `[x]`. Remove resolved items that no longer provide useful context.

4. **No direct database access:** Neither the frontend nor the backend connects to PostgreSQL or MongoDB directly. All data flows through the Java API.

5. **Strict directory isolation:** Frontend code NEVER imports from backend. Backend code NEVER imports from frontend. They communicate exclusively through the GraphQL API.

6. **Accessibility first:** Large touch targets (minimum 48x48dp), clear color contrast (WCAG AA minimum), readable font sizes, simple navigation. The interface must be usable by patients of all ages without training.

---

## 4. SUBDOMAIN STACKS

### Frontend (`/mobile/frontend`)

- React Native, Expo, TypeScript
- Apollo Client (GraphQL communication with BFF)
- Zustand (state management)
- Expo Router (navigation)
- NativeWind (styling)

```
npx expo start            # Start development server
npx expo start --android  # Start on Android
npx expo start --ios      # Start on iOS
npm test                  # Run tests
```

### Backend (`/mobile/backend`)

- Node.js 20, Express 5, JavaScript (CommonJS)
- Apollo Server 4 (GraphQL API)
- jsonwebtoken (JWT generation/validation)
- fetch native (HTTP client for Java API)
- pino (structured JSON logging)

```
npm start                 # Start production server
npm run dev               # Start with watch mode
npm test                  # Run tests
```

---

## 5. IMPLEMENTATION CHECKLIST

### Phase 1: Frontend project setup

- [x] Initialize Expo project with TypeScript — `expo@^54.0.0`, `typescript@~5.9.2`, `tsconfig.json`, `expo-env.d.ts`, `expo-router/entry` main.
- [x] Define folder structure — `src/components`, `src/services/graphql`, `src/store`, `src/types`, `src/utils`, `src/hooks` populated.
- [x] Configure navigation — Expo Router file-based; `app/(auth)/_layout.tsx` + `app/(tabs)/_layout.tsx` with bottom-tab `Tabs` (7 tabs: Inicio, Citas, Juegos, Cura, Progreso, Perfil, Ajustes).
- [x] Theming light/dark + accessible palette — `tailwind.config.js` (NativeWind preset, `darkMode: 'class'`, full `primary` ramp + `surface`/`background`/`text-*`/`border` light+dark + `error`/`success`); `src/utils/theme.ts` exposes `ThemeContext` + `useTheme()`; `settings.tsx` toggles light/dark; Inter font family with regular/medium/semibold weights.


### Phase 3: Authentication and shell

- [x] Login screen (DNI/email + password via GraphQL mutation) — `app/(auth)/login.tsx` + `LOGIN_MUTATION` in `src/services/graphql/mutations/auth.ts`.
- [x] Secure token storage (Expo SecureStore) — `expo-secure-store@15.0.8`; `authStore.ts` persists token under `auth_token`.
- [x] Auto-logout on token expiration — `errorLink` en `client.ts` detecta `UNAUTHENTICATED`/`TOKEN_EXPIRED`/`TOKEN_INVALID` y llama `cerrarSesionPorExpiracion()`.
- [x] Main navigation shell — `app/(tabs)/_layout.tsx` with 7 tabs (Inicio, Citas, Juegos, Cura, Progreso, Perfil, Ajustes).
- [x] Pull-to-refresh & loading states across all screens — `bootstrapStore.refreshing` + `RefreshControl` añadidos a `index.tsx` y `profile.tsx`; `profile.tsx` muestra `ActivityIndicator` mientras `patient == null`.

### Phase 4: Patient features

- [x] Patient profile screen — `app/(tabs)/profile.tsx` reads `userStore.patient` populated from `GET_MY_PROFILE` (`me` query); `InfoRow` for DNI, NSS, birth date, address, phone, email + `ProfileHeader`.
- [x] Assigned disabilities with current progression level — `GET_MY_DISABILITIES` returns `currentLevel`; rendered in profile (pathology pills) and in `treatments.tsx` via `DisabilitySection`.
- [x] Treatment list filtered by disability and level — `treatments.tsx` groups by `disabilityCode` and renders `DisabilitySection` per disability with the corresponding level.
- [x] Game session history with progress charts — `app/(tabs)/progress.tsx` + `BodyDiagram` + `ProgressChartModal` (LineChart) using `GET_MY_BODY_PART_PROGRESS` and `GET_BODY_PART_METRICS`; assigned-game listing in `games.tsx`.
- [ ] Appointment list (upcoming **and past**) — only upcoming list rendered; `appointmentsStore.fetch` queries with `upcoming:true` only; no past section. Pending.

### Phase 5: Advanced features

- [ ] Appointment booking screen (date/time picker, practitioner selection).
- [ ] AI WhatsApp chatbot integration for automated appointment booking.
- [ ] Push notifications for appointment reminders.
- [ ] Offline-first caching strategy for critical patient data.

---

*This file is the single source of truth for the mobile domain. Each subdomain has its own CLAUDE.md with specific implementation details. Update this file for cross-cutting concerns.*

## Memory

You have access to Engram persistent memory via MCP tools (mem_save, mem_search, mem_session_summary, etc.).
- Save proactively after significant work — don't wait to be asked.
- After any compaction or context reset, call `mem_context` to recover session state before continuing.
