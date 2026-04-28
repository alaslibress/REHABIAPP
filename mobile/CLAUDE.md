# CLAUDE.md - RehabiAPP Mobile Domain

> **File:** `/mobile/CLAUDE.md`
> **Agent:** Agent 2 (Mobile Developer)
> **Role:** Thinker (Opus) + Doer (Sonnet)

---

## 1. PROJECT DEFINITION

Dominio mobile de RehabiAPP. Dos subdirectorios estrictamente aislados:

| Subdirectory | Description | Stack |
|-------------|-------------|-------|
| `/mobile/frontend` | App paciente React Native | TypeScript, Expo, Apollo Client, Zustand |
| `/mobile/backend` | BFF Backend-For-Frontend | Node.js 20, Express 5, Apollo Server 4 |

Pacientes usan la app para ver perfil clinico, tratamientos por nivel de progresion, descargar PDFs de tratamientos, lanzar videojuegos terapeuticos desbloqueados, ver historial de sesiones, agendar citas, e interactuar con un chatbot IA via WhatsApp.

UI altamente accesible (mayores con movilidad/vision reducida).

---

## 2. DOMAIN ARCHITECTURE

```
/mobile/
|-- CLAUDE.md
|-- /frontend
|   |-- app/                    # Expo Router (auth, tabs)
|   |-- src/
|   |   |-- components/
|   |   |-- services/graphql/
|   |   |-- store/              # Zustand
|   |   |-- types/
|   |   |-- utils/
|   |-- PLAN.md
|-- /backend
|   |-- src/
|   |   |-- graphql/            # TypeDefs + Resolvers
|   |   |-- services/           # apiClient + dominio
|   |   |-- middleware/
|   |   |-- utils/
|   |-- test/
|   |-- CLAUDE.md
|   |-- PLAN.md
|   |-- Dockerfile
```

### Communication Flow (BFF Pattern)

```
Mobile App (frontend)
    | GraphQL (Apollo Client -> Apollo Server)
    | Port 3000 (BFF) | JWT BFF en Authorization | X-Timezone
    v
BFF Node.js (backend)
    | REST / HTTP (fetch native) | JWT Java en Authorization
    v
Java API (Spring Boot, /api)
    v
PostgreSQL / MongoDB (via /data)
```

**REGLA CRITICA:** Frontend NUNCA habla con `/api` directo. SIEMPRE via BFF. K8s NetworkPolicy lo enfuerza.

---

## 3. OPERATING RULES

1. **Global context:** Leer raiz `/CLAUDE.md` antes de decisiones cross-domain. Local files precedente para mobile.
2. **Subdir CLAUDE.md primero:** Antes de tocar `/frontend` o `/backend`, leer su CLAUDE.md y PLAN.md locales.
3. **Maintain this file:** `[x]` al completar. Eliminar resueltos.
4. **No DB direct:** Ni frontend ni backend acceden a PostgreSQL/MongoDB. Todo via API.
5. **Strict isolation:** Frontend NUNCA importa de backend ni viceversa. Solo GraphQL.
6. **Accessibility first:** Touch targets >=48dp, contraste WCAG AA, fuentes legibles, navegacion simple.

---

## 4. SUBDOMAIN STACKS

### Frontend

- React Native, Expo, TypeScript
- Apollo Client (GraphQL <- BFF)
- Zustand (state)
- Expo Router (navegacion)
- NativeWind (styling)
- expo-file-system + expo-sharing (descarga PDFs de tratamientos)
- WebView de Expo (lanzar Unity WebGL games)

```
npx expo start
npx expo start --android | --ios
npm test
```

### Backend (BFF)

- Node.js 20, Express 5, JavaScript (CommonJS)
- Apollo Server 4
- jsonwebtoken
- fetch native
- pino (logs JSON)

```
npm start
npm run dev
npm test
```

---

## 5. IMPLEMENTATION CHECKLIST

> Phase 1-3 (project setup, auth, navigation shell) y Phase debug-login (2026-04-05) YA completados o trackeados en PLAN locales. Items de bug login ya cerrados eliminados de aqui.

### Phase 4 — Patient features (current iteration)

- [ ] 4.1 Patient profile screen (`me` query — datos personales).
- [ ] 4.2 Discapacidades asignadas con nivel actual.
- [ ] 4.3 Lista de tratamientos filtrada por discapacidad y nivel.
- [ ] 4.4 **NUEVO** Boton "Descargar PDF" por tratamiento — descarga el PDF asociado al tratamiento desde el BFF (cache local con expo-file-system, share via expo-sharing).
- [ ] 4.5 **NUEVO** Tab "Juegos" con lista de videojuegos desbloqueados (filtrados por tratamientos asignados + nivel del paciente). Por cada juego, boton "Jugar" que abre WebView con `url_unity` + JWT inyectado.
- [ ] 4.6 Historial de sesiones con grafico de progreso (consume el endpoint `/api/pacientes/{dni}/progreso` via BFF).
- [ ] 4.7 Lista de citas (proximas y pasadas).

### Phase 5 — Advanced features (current iteration)

- [ ] 5.1 Agenda de citas (date/time picker + practitioner selection).
- [ ] 5.2 AI WhatsApp chatbot (booking automatico — pendiente integracion).
- [ ] 5.3 Push notifications (recordatorios de citas).
- [ ] 5.4 Offline-first cache para datos criticos del paciente.

### Phase 6 — BFF endpoints for new features (current iteration)

> Detalles en `/mobile/backend/PLAN.md`.

- [ ] 6.1 GraphQL query `treatmentPdf(codTrat: String!): TreatmentPdfPayload` — proxies a `GET /api/tratamientos/{cod}/pdf`. Devuelve `{ filename, sizeBytes, base64Content }`.
- [ ] 6.2 GraphQL query `availableGames: [Game!]!` — proxies a `GET /api/pacientes/{dni}/dashboard` y devuelve `juegosDesbloqueados`.
- [ ] 6.3 GraphQL mutation `startGame(idVideojuego: ID!): GameSessionLaunch` — devuelve URL de Unity con JWT corto efimero (5 min) en query param.
- [ ] 6.4 GraphQL query `myProgress: PatientProgress` — proxies a `GET /api/pacientes/{dni}/progreso`. Devuelve estructura compatible con grafico react-native-chart-kit.
- [ ] 6.5 GraphQL query `myDashboard: Dashboard` — proxies a `GET /api/pacientes/{dni}/dashboard`.

---

## Memory

You have access to Engram persistent memory via MCP tools (mem_save, mem_search, mem_session_summary, etc.).
- Save proactively after significant work — don't wait to be asked.
- After any compaction or context reset, call `mem_context` to recover session state before continuing.
