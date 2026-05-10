# PLAN.md — Mobile Domain: Stabilization Sprint (`stats-implementation`)

> **Author:** Agent 2 Thinker (Opus). PRESCRIPTIVE. Doer (Sonnet) MUST follow step by step. No improvisation.
> **Domain:** `/mobile/` (root coordinator) — supersedes the previous Phase G / 5-bridge plan (now archived in git history).
> **Branch:** `stats-implementation`.
> **Date:** 2026-05-07.
> **Trigger:** End-to-end smoke test on Pixel 8 (Expo Go) over the BFF mock surfaced six concrete defects:
>
> 1. `WARN [parseGraphQLError] Codigo de error recibido: Cannot assign to read-only property` (×2 during bootstrap).
> 2. `WARN [parseGraphQLError] Codigo de error recibido: Cannot read property 'Base64' of undefined` (treatment PDF download).
> 3. `WARN [parseGraphQLError] Codigo de error recibido: No se pudo descargar el documento. Intentalo mas tarde.` (cascaded from #2).
> 4. `WARN [GraphQL] VALIDATION_ERROR | operacion="GetBodyPartMetrics" | variables={"bodyPartId":"RIGHT_HIP" | "TORSO"}` (×2).
> 5. `WARN [parseGraphQLError] Codigo de error recibido: desconocido` (×2 — cascaded from #1 and #2 — disappear once #1 and #2 are fixed).
> 6. Push notifications never request permission and never register the Expo push token unless the user toggles them on manually in Settings.
>
> Plus three UX deltas requested by the developer:
>
> - Replace the appointments form with a contact card (phone + email) and keep the WhatsApp button. NO booking form.
> - Make the appointments tab text fully readable in dark mode (no near-invisible greys on the contact card).
> - Make the home screen surface (FloatingBalloon container) honor dark mode.

---

## 0. ROOT CAUSE TABLE (READ BEFORE TOUCHING CODE)

| # | Symptom (Metro log)                                                | Root cause                                                                                                                                                                                                                                                                                                       | File:line                                                              | Verdict                              |
|---|--------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------|--------------------------------------|
| 1 | `Cannot assign to read-only property`                              | Apollo Client v4 freezes query results with `Object.freeze`. `appointmentsStore.fetchPast` calls `citas.sort(...)` directly on `data.myAppointments`. `Array.prototype.sort` mutates in place → `TypeError: Cannot assign to read-only property '0' of object '[object Array]'`.                                  | `mobile/frontend/src/store/appointmentsStore.ts:75`                    | One-line fix: spread before sort.    |
| 2 | `Cannot read property 'Base64' of undefined`                       | Expo SDK 54 ships `expo-file-system@~19.0.21`. The new modular API does NOT export `EncodingType` from the package root anymore — it lives in `expo-file-system/legacy`. The PDF download path reads `FileSystem.EncodingType.Base64` on a module where `EncodingType` is `undefined` → property access throws. | `mobile/frontend/src/store/treatmentsStore.ts:4,72`                    | Switch import to `/legacy`.          |
| 3 | `No se pudo descargar el documento. Intentalo mas tarde.`          | `treatmentsStore.downloadPdf` catches any error from #2 and rebrands it as `DOCUMENT_DOWNLOAD_FAILED`. Dies once #2 is fixed.                                                                                                                                                                                    | `mobile/frontend/src/store/treatmentsStore.ts:82-88`                   | Resolved by #2.                      |
| 4 | `VALIDATION_ERROR \| operacion="GetBodyPartMetrics"`                | The frontend declares `query GetBodyPartMetrics($bodyPartId: ID!)` but the BFF resolver argument is typed as `bodyPartMetrics(bodyPartId: BodyPartId!)`. Apollo Server's strict variable-vs-argument typing fails the operation BEFORE entering any resolver. The BFF formats it as `VALIDATION_ERROR`.           | `mobile/frontend/src/services/graphql/queries/progress.ts:17`          | Change variable type to `BodyPartId!`. |
| 5 | `Codigo de error recibido: desconocido` → `INTERNAL_ERROR`         | `parseGraphQLError` is given a plain `Error` (from #1 / #2) without `extensions.code` and without `graphQLErrors`. It logs `desconocido` and falls through to `INTERNAL_ERROR`.                                                                                                                                  | `mobile/frontend/src/utils/errorHandler.ts:121-193`                    | Resolved by #1 + #2.                 |
| 6 | Notifications never fire spontaneously                             | `bootstrapStore.hydrate` schedules local reminders for every upcoming appointment, but `requestPermission` is NEVER called outside the Settings tab. With permission denied, `Notifications.scheduleNotificationAsync` silently no-ops. Push token registration is also gated behind a Settings toggle.        | `mobile/frontend/src/store/bootstrapStore.ts` + `_layout.tsx:142`      | Wire ensureNotificationsEnabled().  |
| 7 | Home screen background does NOT switch to dark mode                | The root view of `(tabs)/index.tsx` uses `bg-background` only — no `dark:bg-background-dark`. Likewise the FloatingBalloon circles use `bg-surface` only.                                                                                                                                                       | `mobile/frontend/app/(tabs)/index.tsx:106`, `FloatingBalloon.tsx:73`   | Add `dark:` variants.                |
| 8 | Appointments tab still renders the request form                     | Line 5 of `(tabs)/appointments.tsx` imports `AppointmentRequestForm` and line 116 renders it. The developer wants only contact info (phone + email) plus the WhatsApp button.                                                                                                                                  | `mobile/frontend/app/(tabs)/appointments.tsx:5,116`                    | Swap component for `HospitalContactCard`. |
| 9 | WhatsApp button is a TODO                                          | `WhatsAppButton.handlePress` is an empty function with a `// TODO` comment.                                                                                                                                                                                                                                       | `mobile/frontend/src/components/WhatsAppButton.tsx:7-9`                | Wire `Linking.openURL`.              |

**There is NO change required in `/api`, `/data`, `/desktop`. The BFF needs only one schema flag for `bodyPartMetrics` (kept as enum — frontend changes instead) and zero resolver edits. All work lives in `/mobile/frontend` with one micro-touch in `/mobile/backend` for tests.**

---

## 1. SCOPE AND ARCHIVE STATUS

| Sub-plan                              | Status                | Action                                                                                          |
|---------------------------------------|-----------------------|-------------------------------------------------------------------------------------------------|
| `/mobile/PLAN.md` (this file)         | REWRITE (this commit) | Single source of truth for the stabilization sprint. Both subdomains delegate to this file.     |
| `/mobile/backend/PLAN.md`             | TOUCH                 | Add Phase I-bff: one new test asserting `bodyPartMetrics` resolver still works against frontend's renamed variable. No resolver code changes.   |
| `/mobile/frontend/PLAN.md`            | REWRITE               | Phase I-fe carries every line item below from §3 to §9. Doer reads this file (PLAN.md root) and keeps the frontend file synchronized as a checklist mirror only.   |

The previous "Phase G / 5-bridge" plans are FROZEN. Do not delete their files; just append the new Phase I sections at the bottom and mark the Phase G items `[x] (archived 2026-05-05)`.

---

## 2. ORDER OF EXECUTION (BLOCKING DEPENDENCY)

```
[J.1]  Apollo frozen-array fix (appointmentsStore.fetchPast)
       → eliminates "Cannot assign to read-only property"
       → eliminates two of the four "desconocido" warnings
       → 1 file, ~1 line, ~5 minutes

[J.2]  expo-file-system SDK 54 migration (treatmentsStore.downloadPdf)
       → eliminates "Cannot read property 'Base64' of undefined"
       → eliminates the cascaded "No se pudo descargar el documento"
       → 1 file, ~2 lines, ~5 minutes

[J.3]  GraphQL variable type fix (GetBodyPartMetrics)
       → eliminates VALIDATION_ERROR for both RIGHT_HIP and TORSO and any future body part
       → 1 file, ~1 line. BFF unchanged.
       → ~5 minutes

[J.4]  Replace AppointmentRequestForm with HospitalContactCard
       → 1 import swap + 1 JSX swap in (tabs)/appointments.tsx
       → ~5 minutes

[J.5]  Wire WhatsAppButton
       → 1 file, ~10 lines (Linking.openURL with phone + prefilled message + fallback)
       → ~10 minutes

[J.6]  Dark mode on home screen + FloatingBalloon
       → 2 files, ~4 lines total
       → ~5 minutes

[J.7]  Dark mode polish on the appointments tab
       → after J.4 the only remaining surface is HospitalContactCard which is already dark-aware. Sanity check + AppText overrides if found.
       → 0–1 files

[J.8]  Notifications bootstrap
       → 1 new helper in utils/notifications.ts + 1 call site in bootstrapStore.hydrate
       → ~20 minutes

[J.9]  BFF test — single new test asserting bodyPartMetrics resolver still accepts BodyPartId enum input from a query whose variable is typed BodyPartId! (regression guard for J.3).
       → ~10 minutes

[J.10] Verification on Pixel 8 over Expo Go (Checkpoint Z below).
```

**STRICT order.** J.1, J.2 and J.3 are independent of each other but together unblock the entire bootstrap. J.4 → J.7 are UI deltas. J.8 is functional. Do them in the listed order so that each Metro reload validates one bucket of fixes at a time.

---

## 3. NON-NEGOTIABLES

- **No new packages.** Everything required ships with the current `package.json`. Do NOT install `expo-linking` (already on the deps list as transitive of `expo`) or anything else.
- **Do NOT rewrite `parseGraphQLError`.** It is correct. The fix is upstream — once #1 and #2 stop throwing native `TypeError`s, the catch chain naturally classifies real GraphQL errors.
- **Do NOT change BFF schema.** The enum `BodyPartId` stays as is. The frontend variable type is what matches the schema.
- **Do NOT remove `AppointmentRequestForm.tsx` from disk.** Stop importing it. The component is dead code after this commit but kept in the tree for git history readability and as a fallback.
- **Do NOT add a global notification permission request on app launch (RootLayout).** Keep the request scoped to `bootstrapStore.hydrate`, which only runs after a successful login. Asking for OS permissions before the user has even logged in is bad UX.
- **Do NOT modify the BFF `registerDeviceToken` resolver.** It is already a stub that returns `true` (per `mobile/backend/src/graphql/resolvers/settings.js`). Real APNs/FCM dispatch is out of scope.
- **All new code comments in Spanish, no diacritics, no emojis.** Plan documents in English (this file).
- **Run `npm test` from `mobile/backend/` after J.9.** Must stay 25/25 (or 26/26 if the new test landed).
- **No formatting-only churn.** Touch only the lines listed below.

---

## 4. ENVIRONMENT EXPECTATIONS

| Surface               | Command                                                  | Expected                                                          |
|-----------------------|----------------------------------------------------------|-------------------------------------------------------------------|
| BFF (mock)            | `cd mobile/backend && npm run dev`                       | `BFF mobile-backend iniciado port=3000 mockApi=true`              |
| BFF health            | `curl http://localhost:3000/health`                      | `{"status":"UP"}`                                                 |
| Frontend dev          | `cd mobile/frontend && npm start`                        | `[Apollo] URL del BFF resuelta: http://<IP>:3000/graphql`         |
| Frontend on device    | Expo Go scan QR                                          | Login screen renders                                              |
| Login                 | `admin / admin` (mock)                                   | Navigates to home tab in <3 s                                     |

The Java API at `:8080` is **NOT REQUIRED**. `MOCK_API=true` is the assumed mode for this sprint.

---

## 5. PHASE I-FE — FRONTEND IMPLEMENTATION (PRESCRIPTIVE)

Each task lists: file, exact diff anchor, prescription. The Doer applies the change verbatim.

---

### J.1 — Apollo frozen-array fix

**File:** `mobile/frontend/src/store/appointmentsStore.ts`

**Anchor (current code, lines 73–78):**

```ts
            const citas: Appointment[] = data.myAppointments ?? [];
            // Orden descendente: cita mas reciente primero
            citas.sort(function (a, b) {
              return (b.date + b.time).localeCompare(a.date + a.time);
            });
            set({ pastItems: citas, loadingPast: false, hydratedPast: true });
```

**Replace with:**

```ts
            const citas: Appointment[] = data.myAppointments ?? [];
            // Orden descendente: cita mas reciente primero.
            // Apollo v4 congela los resultados (Object.freeze), por lo que
            // sort() in-place lanzaria "Cannot assign to read-only property '0'".
            // Copiamos a un array mutable antes de ordenar.
            const citasOrdenadas = [...citas].sort(function (a, b) {
              return (b.date + b.time).localeCompare(a.date + a.time);
            });
            set({ pastItems: citasOrdenadas, loadingPast: false, hydratedPast: true });
```

**Why this and not Immer / produce:** the BFF already sorts past appointments server-side (when ready). This local sort is a defensive UI guarantee that survives even if the BFF responds in arbitrary order. A single spread is the cheapest robust fix.

**Side check:** also audit the rest of the store for further mutations. As of 2026-05-07 there are NONE — `cancel` uses `.filter` (returns new array), `reset` re-creates state. Confirm by `grep -n "\.sort\|\.reverse\|\.splice\|\.push(\|\.pop(\|\.shift(\|\.unshift(" src/store/appointmentsStore.ts` and assert only the `sort` you just patched appears.

---

### J.2 — expo-file-system SDK 54 migration

**File:** `mobile/frontend/src/store/treatmentsStore.ts`

**Anchor 1 (line 4):**

```ts
import * as FileSystem from 'expo-file-system';
```

**Replace with:**

```ts
// Expo SDK 54 reorganizo expo-file-system en una nueva API basada en File/Directory.
// La API antigua (cacheDirectory, EncodingType, writeAsStringAsync, downloadAsync)
// vive en el subpath '/legacy' por compatibilidad. Nuestro flujo de descarga de PDF
// sigue siendo procedural (escribir base64 + abrir en Sharing), por lo que /legacy
// es el camino mas directo y no requiere refactor.
import * as FileSystem from 'expo-file-system/legacy';
```

**Anchor 2 (lines 67–73 — the Base64 write):**

The `FileSystem.EncodingType.Base64` reference at line 72 will resolve correctly once the import is `/legacy`. Do NOT touch this line.

**Anchor 3 (lines 75 — `FileSystem.downloadAsync`):**

`downloadAsync` exists in `/legacy` with the same signature. Do NOT touch.

**Anchor 4 (line 68 — `FileSystem.cacheDirectory`):**

`cacheDirectory` is exported from `/legacy`. Do NOT touch.

**Side check:** after the edit, run from the project root:

```bash
grep -rn "from 'expo-file-system'" mobile/frontend/src mobile/frontend/app
```

The only result must be the `treatmentsStore.ts` line you just patched (using `/legacy`). If any other file imports from `expo-file-system` without `/legacy`, patch it identically — but as of 2026-05-07 no other consumer exists.

---

### J.3 — GraphQL variable type fix for `GetBodyPartMetrics`

**File:** `mobile/frontend/src/services/graphql/queries/progress.ts`

**Anchor (current, lines 16–24):**

```ts
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

**Replace with:**

```ts
export const GET_BODY_PART_METRICS = gql`
  query GetBodyPartMetrics($bodyPartId: BodyPartId!) {
    bodyPartMetrics(bodyPartId: $bodyPartId) {
      date
      score
      metricType
    }
  }
`;
```

**Why:** the BFF schema declares `bodyPartMetrics(bodyPartId: BodyPartId!): [BodyPartMetric!]!` (see `mobile/backend/src/graphql/typeDefs/bodyProgress.js` line 58). Apollo Server's strict variable-coercion check rejects an `ID!` variable in a position expecting `BodyPartId!`. Renaming the variable type fixes both reported failures (`RIGHT_HIP`, `TORSO`) and any future body part.

**`progressStore.loadMetrics` is unchanged.** It already passes `bodyPartId` as a string (e.g. `"RIGHT_HIP"`), and the GraphQL enum `BodyPartId` is wire-compatible with the same string literal — Apollo serializes enums as their name on the wire. No code rewrite needed in the store.

**Type assertion:** after the fix, run

```bash
npx tsc --noEmit -p mobile/frontend/tsconfig.json
```

Must report 0 errors. The string literal type stays `string` on the JS/TS side; the GraphQL coercion happens at runtime.

---

### J.4 — Replace AppointmentRequestForm with HospitalContactCard

**File:** `mobile/frontend/app/(tabs)/appointments.tsx`

**Anchor 1 (line 5):**

```ts
import { AppointmentRequestForm } from '../../src/components/AppointmentRequestForm';
```

**Replace with:**

```ts
import { HospitalContactCard } from '../../src/components/HospitalContactCard';
```

**Anchor 2 (line 116):**

```tsx
      {/* Seccion: Pedir cita nueva */}
      <AppointmentRequestForm />
```

**Replace with:**

```tsx
      {/* Seccion: Pedir cita nueva — el paciente contacta por telefono / email / WhatsApp.
          Sin formulario interno: la solicitud queda fuera de la app por requisito clinico. */}
      <HospitalContactCard />
```

**Do NOT delete `AppointmentRequestForm.tsx` from disk.** It stays as dead code in this commit (kept for git readability). A future cleanup change can remove it once a release window passes.

**Verification:**

- `grep -n "AppointmentRequestForm" mobile/frontend/app mobile/frontend/src` must return ONLY the file's own definition.
- `grep -n "HospitalContactCard" mobile/frontend/app mobile/frontend/src` must include the new import in `appointments.tsx`.

---

### J.5 — Wire WhatsAppButton

**File:** `mobile/frontend/src/components/WhatsAppButton.tsx`

**Anchor (full current file, 23 lines):**

```tsx
import { Pressable, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { AppText } from './AppText';

// Boton de acceso a WhatsApp — funcionalidad pendiente de implementar
export function WhatsAppButton() {
  function handlePress() {
    // TODO: abrir WhatsApp con Linking.openURL('whatsapp://send?phone=...')
  }

  return (
    <Pressable
      onPress={handlePress}
      className="flex-row items-center justify-center gap-2 border border-success rounded-full py-3 px-5 min-h-12"
    >
      <Ionicons name="logo-whatsapp" size={20} color="#22C55E" />
      <AppText variant="body" weight="medium" className="text-success">
        Abrir WhatsApp
      </AppText>
    </Pressable>
  );
}
```

**Replace ENTIRELY with:**

```tsx
import { Linking, Pressable } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { AppText } from './AppText';

// Numero de WhatsApp del centro de rehabilitacion. Formato E.164 sin '+'
// porque la URL whatsapp://send?phone= NO acepta el simbolo '+' (lo reescribe).
// Coincide con HOSPITAL_PHONE de HospitalContactCard pero sin espacios ni '+'.
const WHATSAPP_PHONE = '34628678888';
const WHATSAPP_MESSAGE = 'Hola, soy paciente de RehabiAPP y quiero pedir una cita.';

// Construye la URL whatsapp:// con el mensaje URL-encoded
function buildWhatsappUrl(): string {
  return `whatsapp://send?phone=${WHATSAPP_PHONE}&text=${encodeURIComponent(WHATSAPP_MESSAGE)}`;
}

// Fallback web si la app de WhatsApp no esta instalada en el dispositivo.
// wa.me redirecciona a la web oficial de WhatsApp con el mismo mensaje.
function buildWhatsappWebUrl(): string {
  return `https://wa.me/${WHATSAPP_PHONE}?text=${encodeURIComponent(WHATSAPP_MESSAGE)}`;
}

// Boton de acceso a WhatsApp — abre la app nativa o la web si no esta instalada
export function WhatsAppButton() {
  async function handlePress() {
    const deepLink = buildWhatsappUrl();
    try {
      const soportado = await Linking.canOpenURL(deepLink);
      if (soportado) {
        await Linking.openURL(deepLink);
        return;
      }
    } catch {
      // Algunos dispositivos / Expo Go bloquean canOpenURL para deep links
      // no listados en LSApplicationQueriesSchemes. Caemos al fallback web.
    }
    try {
      await Linking.openURL(buildWhatsappWebUrl());
    } catch {
      // Sin navegador disponible — fallback silencioso. El usuario tiene
      // el numero visible en la tarjeta de contacto justo encima del boton.
    }
  }

  return (
    <Pressable
      onPress={handlePress}
      className="flex-row items-center justify-center gap-2 border border-success rounded-full py-3 px-5 min-h-12"
    >
      <Ionicons name="logo-whatsapp" size={20} color="#22C55E" />
      <AppText variant="body" weight="medium" className="text-success">
        Abrir WhatsApp
      </AppText>
    </Pressable>
  );
}
```

**Why this design:**

- Deep link first (`whatsapp://send?phone=...&text=...`) — opens the app instantly when installed.
- `wa.me` fallback for devices without WhatsApp installed and for Expo Go's permissive linking sandbox.
- `Linking.canOpenURL` may throw on Android when the URL scheme is not declared in the manifest; the inner `try/catch` swallows it and falls through to web.
- The user already sees the phone number in `HospitalContactCard` directly above this button, so a silent failure is acceptable (not optimal — but acceptable for this sprint, no toast machinery exists today).

**Numbers and message:**

- Phone is hardcoded to match `HOSPITAL_PHONE = '+34 628 67 88 88'` of `HospitalContactCard.tsx` (line 8). If the developer changes the number elsewhere, change it here too. **Do NOT factor it out into a shared constant in this sprint** — out of scope, single source of truth in two files is acceptable for two callers and avoids touching unrelated files.

> **COORDINATION WITH PHASE L (chatbot).** The WhatsApp number wired here is the same number that the `/chatbot/` service authenticates with via QR scan (see Phase L §L.4). All three places — `HospitalContactCard.tsx:8`, `WhatsAppButton.tsx` (this task), and `/chatbot/.env`'s `HOSPITAL_PHONE_E164` — MUST hold the same number. The Doer treats the J.5 number as the canonical reference and copies it into the chatbot env file at L.4 verbatim.

---

### J.6 — Dark mode on home screen + FloatingBalloon

**File 1:** `mobile/frontend/app/(tabs)/index.tsx`

**Anchor (line 106):**

```tsx
    <View className="flex-1 bg-background">
```

**Replace with:**

```tsx
    <View className="flex-1 bg-background dark:bg-background-dark">
```

**File 2:** `mobile/frontend/src/components/FloatingBalloon.tsx`

**Anchor (lines 73–76):**

```tsx
      className="min-h-12 min-w-12 w-20 h-20 rounded-full bg-surface items-center justify-center shadow-lg"
    >
      <Ionicons name={iconName} size={size} color="#2563EB" />
    </AnimatedPressable>
```

**Replace with:**

```tsx
      className="min-h-12 min-w-12 w-20 h-20 rounded-full bg-surface dark:bg-surface-dark items-center justify-center shadow-lg border border-transparent dark:border-primary-700"
    >
      <Ionicons name={iconName} size={size} color="#60A5FA" />
    </AnimatedPressable>
```

**Why the icon color change to `#60A5FA` (primary-400):** the previous color `#2563EB` (primary-600) renders fine on a white surface but is hard to read on `surface-dark` `#111827`. `#60A5FA` is the accessible (WCAG AA) primary tone for dark-on-dark. The light scheme also benefits — `#60A5FA` keeps brand identity and contrast.

**`dark:border-primary-700`** adds a subtle ring around the balloon in dark mode so it does not visually merge with the dark background. In light mode the border stays transparent.

**Side check on the greeting card:** line 109 uses `bg-surface dark:bg-surface-dark rounded-2xl ... border border-primary-200 dark:border-primary-600` — already correct. Do NOT touch.

---

### J.7 — Dark mode polish on the appointments tab

After J.4, the only mutable surface is the section titles (already dark-aware), `AppointmentCard` (already dark-aware) and `HospitalContactCard` (already dark-aware). Audit:

```bash
grep -n "className=" mobile/frontend/app/\(tabs\)/appointments.tsx
```

Every `text-*` literal in that file MUST contain a paired `dark:text-*` variant. As of 2026-05-07 the file already conforms (lines 63 and 89 both pair `text-text-primary` with `dark:text-text-primary-dark`).

**No code change in this task** unless the audit finds a regression introduced by J.4. If it does, follow this rule:

| Light token              | Dark token                                              |
|--------------------------|---------------------------------------------------------|
| `text-text-primary`      | `dark:text-text-primary-dark`                           |
| `text-text-secondary`    | `dark:text-text-secondary-dark`                         |
| `bg-background`          | `dark:bg-background-dark`                               |
| `bg-surface`             | `dark:bg-surface-dark`                                  |
| `border-border`          | `dark:border-border-dark`                               |
| `bg-primary-100`         | `dark:bg-primary-900` (already used in `AppointmentCard`) |

Definition of "white text" the developer asked for: `text-primary-dark` resolves to `#F1F5F9` per `tailwind.config.js` line 14 — that is the project-wide near-white. Do NOT promote `text-secondary-dark` (`#94A3B8`) to white — secondary text staying lighter grey is a deliberate hierarchy and is WCAG AA on `#0B1220`. The complaint about "inconsistent visibility" originated from `AppointmentRequestForm` (now removed), not from cards or section titles.

---

### J.8 — Notifications bootstrap integration

**File 1:** `mobile/frontend/src/utils/notifications.ts`

**Append a new exported helper at the end of the file (after `scheduleTestNotification`):**

```ts
// Permiso + token push: solicita permiso del SO si todavia no esta concedido,
// recupera el Expo Push Token y lo registra en el BFF si el usuario tiene
// activadas las "Actualizaciones del medico". Idempotente: llamar mas de una
// vez no produce efectos colaterales (el SO solo prompteo una vez en su vida).
//
// Devuelve un objeto con el estado para que el caller pueda decidir si
// programar reminders locales o no.
export async function ensureNotificationsEnabled(opts?: {
  alsoRegisterPushToken?: boolean;
  registerToken?: (token: string, platform: 'IOS' | 'ANDROID' | 'WEB') => Promise<void>;
}): Promise<{ permissionGranted: boolean; pushToken: string | null }> {
  if (!Notifications) return { permissionGranted: false, pushToken: null };

  // 1) Permiso. requestPermission ya es idempotente: si ya esta concedido
  //    devuelve true sin abrir dialogo.
  const granted = await requestPermission();
  if (!granted) return { permissionGranted: false, pushToken: null };

  // 2) Push token (solo si el caller lo pidio). En Expo Go Android el token
  //    sera null por limitacion del SDK 53+; el flujo NO debe romperse.
  let pushToken: string | null = null;
  if (opts?.alsoRegisterPushToken) {
    pushToken = await getExpoPushToken();
    if (pushToken && opts.registerToken) {
      const platform: 'IOS' | 'ANDROID' | 'WEB' =
        Platform.OS === 'ios' ? 'IOS' : Platform.OS === 'android' ? 'ANDROID' : 'WEB';
      await opts.registerToken(pushToken, platform).catch(function () {
        // Fallo silencioso — el log queda en el BFF; el frontend continua.
      });
    }
  }

  return { permissionGranted: true, pushToken };
}
```

**File 2:** `mobile/frontend/src/store/bootstrapStore.ts`

**Anchor 1 (line 8 — imports):**

```ts
import { scheduleAppointmentReminder } from '../utils/notifications';
```

**Replace with:**

```ts
import { scheduleAppointmentReminder, ensureNotificationsEnabled } from '../utils/notifications';
import { Platform } from 'react-native';
import { client } from '../services/graphql/client';
import { REGISTER_DEVICE_TOKEN } from '../services/graphql/mutations/settings';
import { useSettingsStore } from './settingsStore';
```

**Anchor 2 (lines 53–69 — current notification block):**

```ts
        // Programar recordatorios locales para las citas proximas
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
```

**Replace with:**

```ts
        // Notificaciones — un unico bloque defensivo que cubre permiso, token push
        // y reminders locales. Cualquier fallo se atrapa y la hidratacion sigue.
        try {
          const settings = useSettingsStore.getState();
          // Solo intentar si el usuario no ha apagado AMBAS opciones en Settings.
          // notifAppointments y notifDoctorUpdates son true por defecto.
          if (settings.notifAppointments || settings.notifDoctorUpdates) {
            const { permissionGranted } = await ensureNotificationsEnabled({
              alsoRegisterPushToken: settings.notifDoctorUpdates,
              registerToken: async function (token, platform) {
                await client.mutate({
                  mutation: REGISTER_DEVICE_TOKEN,
                  variables: { token, platform },
                });
              },
            });

            // Reminders locales solo si tenemos permiso y la opcion sigue activa.
            if (permissionGranted && settings.notifAppointments) {
              const citas = useAppointmentsStore.getState().items as Appointment[];
              const hoy = Date.now();
              for (const cita of citas) {
                if (cita.status !== 'SCHEDULED') continue;
                const [anio, mes, dia] = cita.date.split('-').map(Number);
                const [hora, min] = cita.time.split(':').map(Number);
                const fechaCita = new Date(anio, mes - 1, dia, hora, min).getTime();
                if (fechaCita > hoy) {
                  await scheduleAppointmentReminder(cita).catch(function () {});
                }
              }
            }
          }
        } catch (err) {
          if (__DEV__) {
            console.warn('[bootstrap] Fallo en bloque de notificaciones:', (err as Error)?.message);
          }
        }
```

**Important:** the `Platform` import in Anchor 1 is currently unused inside this file but kept because `ensureNotificationsEnabled` builds the platform inside the helper itself, not here. Remove the `Platform` import if your linter complains — it's safe to drop.

Actually, `Platform` is computed inside the helper. **Drop the `import { Platform } from 'react-native';` line** in Anchor 1 and keep the rest of the new imports. Final imports block:

```ts
import { scheduleAppointmentReminder, ensureNotificationsEnabled } from '../utils/notifications';
import { client } from '../services/graphql/client';
import { REGISTER_DEVICE_TOKEN } from '../services/graphql/mutations/settings';
import { useSettingsStore } from './settingsStore';
```

**Why this design:**

- Single permission prompt at the moment the user logs in (post-login) — no surprise prompt before login.
- Idempotent: `requestPermission` is a no-op when already granted.
- Defaults to ON: the user opted in by accepting the app on the store / Expo Go. They can opt out from Settings (already wired).
- Push token registration is a side effect of the same prompt; in Expo Go Android the token will be `null`, the BFF mutation is skipped, no error.
- Reminders only scheduled when permission is granted, so failure modes are contained.

**Settings tab is unchanged.** It already requests permission on toggle and registers the push token. The two flows coexist: Settings is for explicit opt-out / re-opt-in; bootstrap is for the implicit default-on path.

---

## 6. PHASE I-BFF — BACKEND VERIFICATION (NO PRODUCTION CODE CHANGE)

The BFF resolver and typedef for `bodyPartMetrics` already accept the enum. We add ONE regression test to lock the contract.

**File:** `mobile/backend/test/graphql.test.js`

**Find the existing `describe('bodyPartMetrics', ...)` block (or the `bodyProgress` test block).** If a test already covers `bodyPartMetrics` with an enum-typed variable, mark task J.9 as DONE — no edit needed. If it covers it only with a `String!` or `ID!` variable (or only via a hardcoded inline argument), add this test next to it:

```js
test('bodyPartMetrics acepta variable tipada como BodyPartId! (regresion progress.ts:17)', async () => {
  const { server } = await crearTestServer();
  const QUERY = `
    query GetBodyPartMetrics($bodyPartId: BodyPartId!) {
      bodyPartMetrics(bodyPartId: $bodyPartId) {
        date
        score
        metricType
      }
    }
  `;
  const resp = await server.executeOperation({
    query: QUERY,
    variables: { bodyPartId: 'RIGHT_HIP' },
  }, { contextValue: contextoAutenticado() });

  assert.strictEqual(resp.body.kind, 'single');
  assert.strictEqual(resp.body.singleResult.errors, undefined,
    `Esperaba sin errores; recibido: ${JSON.stringify(resp.body.singleResult.errors)}`);
  assert.ok(Array.isArray(resp.body.singleResult.data?.bodyPartMetrics));
});
```

> Adjust `crearTestServer`, `contextoAutenticado` and the assertion namespace (`assert` vs `expect`) to match the existing patterns in `graphql.test.js`. The Doer reads the file once, picks the conventions, and writes the test in that style. Keep the test name in Spanish; everything else stylistically aligned with neighbors.

Run after edit:

```bash
cd mobile/backend && npm test
```

Pass condition: previous count + 1 (or 0 if test already existed). Zero failures.

---

## 7. CHECKPOINT Z — END-TO-END VERIFICATION ON DEVICE

Two terminals, one device.

```bash
# T1
cd /home/alaslibres/DAM/RehabiAPP/mobile/backend && npm run dev

# T2
cd /home/alaslibres/DAM/RehabiAPP/mobile/frontend && npm start
```

In Expo Go on Pixel 8 (same WiFi):

| Step | Action                                                                 | Pass condition                                                                                                                    |
|------|------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| Z.1  | Open the app                                                           | Login screen renders. No popup.                                                                                                   |
| Z.2  | Log in with `admin / admin`                                            | Home tab in <3 s. **Metro shows ZERO `[parseGraphQLError] Codigo de error recibido: …` lines for any of the six previous codes.** |
| Z.3  | Toggle device theme to dark from system settings                       | Home background is `#0B1220`. Floating balloons are `#111827` with primary-blue border. Greeting card is `#111827`.               |
| Z.4  | Tap the calendar balloon → appointments tab                            | Past + upcoming sections render. Below them: HospitalContactCard with phone, email, WhatsApp button. NO form fields.              |
| Z.5  | Tap the phone row in HospitalContactCard                               | Native dialer opens with `+34628678888` pre-filled.                                                                               |
| Z.6  | Tap the email row                                                      | Native mail composer opens with `cita@rehabiapp.com` pre-filled.                                                                  |
| Z.7  | Tap "Abrir WhatsApp"                                                   | WhatsApp app opens to a chat with the hospital number and prefilled message. If WhatsApp not installed, browser opens `wa.me/...`. |
| Z.8  | Pull-to-refresh appointments tab                                       | Refresh completes silently. NO red popup. NO `Cannot assign to read-only property` warning in Metro.                              |
| Z.9  | Open progress tab → tap on the right hip on the body diagram           | Modal opens with chart. **NO `VALIDATION_ERROR \| operacion="GetBodyPartMetrics"` warning in Metro.** Repeat with TORSO, head, etc. |
| Z.10 | Open treatments tab → tap "Descargar PDF" on any treatment with a doc  | Sharing sheet opens with the PDF. **NO `Cannot read property 'Base64' of undefined` warning. NO `No se pudo descargar...` popup.** |
| Z.11 | Settings tab → confirm "Recordatorios de citas" and "Actualizaciones del medico" toggles still work                            | Both toggles flip without errors. Test notification arrives in 5 s.                                                              |
| Z.12 | Restart Expo Go (kill app), log back in                                | On second launch, no second permission prompt (permission already granted). Bootstrap completes silently.                         |

**STOP and report if any step fails.** Do NOT proceed to acceptance.

---

## 8. ACCEPTANCE CRITERIA

The Doer reports "Phase I done" only when ALL are simultaneously true:

- [ ] J.1 patched, `appointmentsStore.fetchPast` no longer mutates a frozen array.
- [ ] J.2 patched, `expo-file-system` imported from `/legacy`. PDF download works.
- [ ] J.3 patched, `GET_BODY_PART_METRICS` declares `$bodyPartId: BodyPartId!`.
- [ ] J.4 patched, `appointments.tsx` imports `HospitalContactCard` and renders it instead of `AppointmentRequestForm`.
- [ ] J.5 patched, `WhatsAppButton.tsx` opens the WhatsApp app or the `wa.me` fallback.
- [ ] J.6 patched, home screen and FloatingBalloon honor dark mode.
- [ ] J.7 audit performed; no regression introduced.
- [ ] J.8 patched, `bootstrapStore.hydrate` calls `ensureNotificationsEnabled` once and reminders schedule only when permission is granted.
- [ ] J.9 BFF test added (or pre-existing) and the suite runs green: `cd mobile/backend && npm test` reports `26 passing` (or current count + 1 if pre-existing).
- [ ] Z.1 to Z.12 pass on a real Pixel 8 over Expo Go in the LAN.
- [ ] `mobile/CLAUDE.md` Phase 5 / Phase G items remain `[x]`. New Phase I section appended with all sub-items `[x]`.
- [ ] No new entries in `mobile/frontend/package.json` or `mobile/backend/package.json`.
- [ ] Engram saved with `mem_save` titled `mobile Phase I — stabilization sprint DONE` summarizing root causes 1–9 and the file:line for each fix.

---

## 9. FILES MODIFIED — SUMMARY TABLE (REFERENCE FOR THE DOER)

| Task | File                                                                            | Edit type   | Approx. lines |
|------|---------------------------------------------------------------------------------|-------------|---------------|
| J.1  | `mobile/frontend/src/store/appointmentsStore.ts`                                | Surgical    | 5             |
| J.2  | `mobile/frontend/src/store/treatmentsStore.ts`                                  | Surgical    | 2             |
| J.3  | `mobile/frontend/src/services/graphql/queries/progress.ts`                      | Surgical    | 1             |
| J.4  | `mobile/frontend/app/(tabs)/appointments.tsx`                                   | Surgical    | 4             |
| J.5  | `mobile/frontend/src/components/WhatsAppButton.tsx`                             | Full rewrite| ~50           |
| J.6  | `mobile/frontend/app/(tabs)/index.tsx`                                          | Surgical    | 1             |
| J.6  | `mobile/frontend/src/components/FloatingBalloon.tsx`                            | Surgical    | 2             |
| J.7  | `mobile/frontend/app/(tabs)/appointments.tsx` (audit only)                      | None        | 0             |
| J.8  | `mobile/frontend/src/utils/notifications.ts`                                    | Append      | ~30           |
| J.8  | `mobile/frontend/src/store/bootstrapStore.ts`                                   | Surgical    | ~25           |
| J.9  | `mobile/backend/test/graphql.test.js`                                           | Append test | ~20           |
| —    | `mobile/CLAUDE.md`                                                              | Checklist   | ~10           |
| —    | `mobile/frontend/PLAN.md`                                                       | Checklist   | ~25           |
| —    | `mobile/backend/PLAN.md`                                                        | Checklist   | ~10           |

Total: ~14 files touched (1 BFF test, 9 frontend code files, 4 docs). Roughly 150 lines net diff. No new dependencies.

---

## 10. WHAT REMAINS BEYOND THIS PLAN (PARKING LOT)

Once Checkpoint Z passes, these items remain explicitly OUT OF SCOPE — open them later as `/sdd-new` proposals:

- Real APNs/FCM push delivery from the BFF (requires Expo Notifications service or self-hosted push gateway). The current `registerDeviceToken` resolver is a stub.
- Real `/api` integration (replace BFF mocks). Waits for `/api` Phase 14 (Flyway recovery, completed 2026-05-07) to reach `main` and the data path to be smoke-tested end-to-end.
- Booking screen with date/time picker — explicitly removed in J.4 by product decision; if it returns it will be a new Phase J.
- Offline-first cache — Apollo `InMemoryCache` is in place; persistence (AsyncStorage adapter) is a separate proposal.
- Centralizing `HOSPITAL_PHONE` and `HOSPITAL_EMAIL` into a config module — out of scope (low value, two callers).

---

## 11. CONSISTENCY CHECK (THINKER SELF-AUDIT)

Performed by Agent 2 Thinker before publishing this plan. Each row is verified against the current code on `stats-implementation` as of commit HEAD on 2026-05-07.

| Claim | Verified by                                                                                                  | Status |
|-------|--------------------------------------------------------------------------------------------------------------|--------|
| `appointmentsStore.ts:75` is the only in-place mutation of an Apollo result in `mobile/frontend/src/store`. | `grep -rn "\.sort(\|\.reverse(\|\.splice(\|\.push(\|\.pop(\|\.shift(\|\.unshift(" mobile/frontend/src/store` | OK     |
| `expo-file-system` resolves to `~19.0.21` (SDK 54) where `EncodingType` lives in `/legacy`.                   | `mobile/frontend/package.json` line 25                                                                      | OK     |
| BFF schema declares `bodyPartMetrics(bodyPartId: BodyPartId!)`.                                              | `mobile/backend/src/graphql/typeDefs/bodyProgress.js` line 58                                                | OK     |
| `HospitalContactCard` already exists, exports correctly, and uses dark variants.                              | `mobile/frontend/src/components/HospitalContactCard.tsx` lines 12, 26, 33, 42, 62, 80                       | OK     |
| `WhatsAppButton.tsx` is the only WhatsApp surface.                                                            | `grep -rn "logo-whatsapp\|whatsapp://\|wa.me" mobile/frontend`                                              | OK     |
| FloatingBalloon is consumed only by `(tabs)/index.tsx`.                                                        | `grep -rn "FloatingBalloon" mobile/frontend/app mobile/frontend/src`                                        | OK     |
| `bootstrapStore.hydrate` is the bootstrap entry point (called from `_layout.tsx:156` and `authStore.login`). | `grep -n "bootstrapStore\|hydrate(" mobile/frontend/app mobile/frontend/src/store`                          | OK     |
| `REGISTER_DEVICE_TOKEN` mutation already declared in the frontend.                                            | `mobile/frontend/src/services/graphql/mutations/settings.ts` line 4                                          | OK     |
| BFF `registerDeviceToken` resolver returns `true` (stub).                                                      | `mobile/backend/src/graphql/resolvers/settings.js` line 9                                                    | OK     |
| Tailwind tokens `background-dark`, `surface-dark`, `text-primary-dark`, `border-border-dark` exist.            | `mobile/frontend/tailwind.config.js` lines 11–24                                                             | OK     |
| Settings tab already calls `requestPermission` and `getExpoPushToken` on toggle.                               | `mobile/frontend/app/(tabs)/settings.tsx` lines 60–103                                                       | OK     |
| The `Platform` import added in J.8 Anchor 1 is unused — instructed Doer to drop it.                            | This document, end of J.8                                                                                    | OK     |
| There are no other consumers of `expo-file-system` than `treatmentsStore.ts` as of 2026-05-07.                  | `grep -rn "from 'expo-file-system'" mobile/frontend`                                                        | OK     |
| `parseGraphQLError` correctly maps `VALIDATION_ERROR` to "Datos invalidos" copy.                                | `mobile/frontend/src/utils/errorHandler.ts` lines 45–48                                                      | OK     |
| Apollo client is v4.1.6 with `CombinedGraphQLErrors` import.                                                    | `mobile/frontend/src/services/graphql/client.ts` line 1                                                      | OK     |

No internal inconsistencies detected. Plan is self-consistent.

---

---

## 12. PHASE L — WHATSAPP CHATBOT INTEGRATION (`/chatbot`)

> **Scope creep flag:** this phase introduces a NEW top-level domain in the monorepo at `/chatbot/`. It is documented inside `/mobile/PLAN.md` because the entry point is the WhatsApp button wired in J.5, and because the chatbot has zero coupling to the mobile codebase beyond that phone number. After this sprint lands, the Doer SHALL split this section into a standalone `/chatbot/PLAN.md` and a `/chatbot/CLAUDE.md` so future iterations live in their own files.

### L.0 — Architecture decision (confirmed by developer 2026-05-07)

| Question                                          | Choice                                                                                  |
|---------------------------------------------------|------------------------------------------------------------------------------------------|
| WhatsApp library                                  | **whatsapp-web.js** (Puppeteer-based, requires QR scan once)                              |
| LLM                                               | **Qwen 2.5** running locally via **Ollama** (`http://localhost:11434`)                    |
| Database write strategy                           | **Direct INSERT into `cita`** (mirrors desktop's legacy direct-JDBC pattern)              |
| Service host                                      | Same machine as Ollama and the desktop ERP — single-tenant on-prem MVP                    |
| Code language                                     | Node.js 20 (LTS) with CommonJS — same toolchain as `/mobile/backend`                      |
| Audit trail                                       | The chatbot writes to `audit_log` itself (same table desktop and `/api` use)              |
| Cancellation / reschedule via WhatsApp            | OUT OF SCOPE for this iteration. Bot replies "para cancelar, llama al hospital".          |
| Multi-language                                    | Spanish only. The system prompt rejects requests in other languages politely.             |

**Why direct PostgreSQL and not the Java API:** the developer explicitly stated the chatbot must NOT interfere with the desktop development. Going through `/api` would couple the chatbot's release cadence to `/api`'s and would require adding a new service-auth scope in `/api` (a non-trivial new attack surface). The desktop's calendar already polls / refreshes from `cita`, so a row inserted by the chatbot is visible to the desktop on the next refresh — zero desktop change.

**Trade-offs accepted:**

- Validation logic (no double booking, business hours) lives in `/chatbot/src/booking.js` AND in `/api`'s appointment service. Two implementations of the same rule. We accept this duplication because the chatbot's rules are simpler (no cross-practitioner scheduling) and the alternative (calling `/api`) creates worse coupling.
- Audit log entries from the chatbot use `usuario='chatbot'` — a non-existent user ID. The audit table accepts a free-text actor; if it has an FK to `sanitario`, the schema will need a small carve-out OR we use the patient's `dni_san` as actor. **The Doer reads the `audit_log` schema in `/api/src/main/resources/db/migration/` and picks the approach that fits without schema migration.** If neither fits, set `actor = 'CHATBOT_BOT'` as a literal string in a `notas` field (NOT in a FK column).

### L.1 — Directory tree (Doer creates this verbatim)

```
/RehabiAPP/
└── chatbot/
    ├── CLAUDE.md                    # placeholder, written by Agent 0 in next iteration
    ├── PLAN.md                      # placeholder, references this section until split
    ├── package.json
    ├── package-lock.json            # generated by npm install
    ├── .env.example                 # committed
    ├── .env                         # NOT committed (gitignored)
    ├── .gitignore                   # node_modules, .env, .wwebjs_auth/, .wwebjs_cache/, logs/
    ├── README.md                    # one paragraph: "see /mobile/PLAN.md Phase L"
    ├── src/
    │   ├── index.js                 # entry point — boots WA + Express health + bus
    │   ├── config.js                # loads + validates .env
    │   ├── logger.js                # pino instance, masks phone numbers in output
    │   ├── whatsapp.js              # whatsapp-web.js wrapper, message bus
    │   ├── llm.js                   # Ollama client + prompt assembly + JSON parsing
    │   ├── db.js                    # pg pool + audit_log helper
    │   ├── booking.js               # state machine: identify patient, check, insert
    │   ├── sessions.js              # in-memory map<phoneE164, ConversationState>
    │   └── promptTemplates.js       # exported strings: SYSTEM_PROMPT, ERROR_REPLIES
    ├── test/
    │   ├── booking.test.js          # node:test — pure unit, mocks pg + ollama
    │   └── sessions.test.js         # node:test — state machine
    └── .wwebjs_auth/                # whatsapp-web.js session — gitignored
```

The Doer creates these files exactly. `.env`, `node_modules`, `.wwebjs_auth/`, `.wwebjs_cache/`, `logs/` go in `.gitignore`. Nothing else is gitignored.

### L.2 — `package.json` (verbatim — Doer copies into `/chatbot/package.json`)

```json
{
  "name": "rehabiapp-chatbot",
  "version": "0.1.0",
  "description": "Chatbot WhatsApp + Qwen 2.5 local para solicitar citas en RehabiAPP",
  "private": true,
  "main": "src/index.js",
  "engines": { "node": ">=20" },
  "scripts": {
    "start": "node src/index.js",
    "dev": "node --watch src/index.js",
    "test": "node --test test/*.test.js"
  },
  "dependencies": {
    "whatsapp-web.js": "^1.26.0",
    "qrcode-terminal": "^0.12.0",
    "ollama": "^0.5.9",
    "pg": "^8.13.0",
    "pino": "^9.5.0",
    "express": "^5.0.0",
    "dotenv": "^16.4.5"
  },
  "devDependencies": {}
}
```

> Versions are locked to what is current and stable on 2026-05-07. The Doer runs `npm install` once; `package-lock.json` is committed.

### L.3 — `.env.example` (verbatim — Doer copies into `/chatbot/.env.example` and duplicates as `/chatbot/.env` for local)

```dotenv
# ======================== SERVIDOR ========================
PORT=4000
LOG_LEVEL=info

# ======================== POSTGRESQL ========================
# Misma BD que /api y /desktop. Credenciales identicas a infra/docker-compose.yml.
PGHOST=localhost
PGPORT=5432
PGDATABASE=rehabiapp
PGUSER=admin
PGPASSWORD=admin
# En produccion, usar TLS. En local, dejar 'disable'.
PGSSLMODE=disable

# ======================== OLLAMA ========================
OLLAMA_URL=http://localhost:11434
# Nombre exacto del modelo registrado en `ollama list`.
# El usuario tiene Qwen 2.5 instalado. Si el tag no existe en `ollama list`,
# ajustar a la version disponible (p. ej. qwen2.5:7b-instruct).
OLLAMA_MODEL=qwen2.5:latest

# ======================== WHATSAPP ========================
# Numero E.164 SIN '+' que el usuario escaneara con QR la primera vez.
# Debe coincidir con HOSPITAL_PHONE de mobile/HospitalContactCard.tsx
# y con WHATSAPP_PHONE de mobile/WhatsAppButton.tsx (Phase J.5).
HOSPITAL_PHONE_E164=34628678888

# ======================== REGLAS DE NEGOCIO ========================
# Bloque horario en minutos para los slots
SLOT_MINUTES=30
# Hora de apertura y cierre (formato HH:MM, 24h)
OPEN_TIME=09:00
CLOSE_TIME=18:00
# Dias laborables: 1=lunes ... 7=domingo (formato ISO 8601)
WORKING_DAYS=1,2,3,4,5
# Antelacion minima en horas (no se pueden pedir citas con menos de N horas)
MIN_LEAD_HOURS=2

# ======================== AUDIT ========================
# Cadena literal que se escribe en audit_log cuando el chatbot crea una cita
AUDIT_ACTOR=CHATBOT_BOT
```

### L.4 — `src/config.js` (Doer copies verbatim)

```js
'use strict';
require('dotenv').config();

function leer(name, defecto) {
  const valor = process.env[name];
  if (valor === undefined || valor === '') {
    if (defecto === undefined) {
      throw new Error(`Variable de entorno requerida ausente: ${name}`);
    }
    return defecto;
  }
  return valor;
}

const config = Object.freeze({
  port: parseInt(leer('PORT', '4000'), 10),
  logLevel: leer('LOG_LEVEL', 'info'),

  pg: Object.freeze({
    host: leer('PGHOST'),
    port: parseInt(leer('PGPORT', '5432'), 10),
    database: leer('PGDATABASE'),
    user: leer('PGUSER'),
    password: leer('PGPASSWORD'),
    ssl: leer('PGSSLMODE', 'disable') !== 'disable' ? { rejectUnauthorized: false } : false,
  }),

  ollama: Object.freeze({
    url: leer('OLLAMA_URL', 'http://localhost:11434'),
    model: leer('OLLAMA_MODEL', 'qwen2.5:latest'),
  }),

  whatsapp: Object.freeze({
    hospitalPhoneE164: leer('HOSPITAL_PHONE_E164'),
  }),

  reglas: Object.freeze({
    slotMinutes: parseInt(leer('SLOT_MINUTES', '30'), 10),
    openTime: leer('OPEN_TIME', '09:00'),
    closeTime: leer('CLOSE_TIME', '18:00'),
    workingDays: leer('WORKING_DAYS', '1,2,3,4,5').split(',').map(function (s) { return parseInt(s, 10); }),
    minLeadHours: parseInt(leer('MIN_LEAD_HOURS', '2'), 10),
  }),

  auditActor: leer('AUDIT_ACTOR', 'CHATBOT_BOT'),
});

module.exports = config;
```

### L.5 — `src/logger.js`

```js
'use strict';
const pino = require('pino');
const config = require('./config');

// Enmascara numeros de telefono en logs por privacidad (RGPD).
// Sustituye los digitos por '*' excepto los ultimos 3.
function enmascararTelefono(tel) {
  if (typeof tel !== 'string' || tel.length < 4) return tel;
  return '*'.repeat(tel.length - 3) + tel.slice(-3);
}

const logger = pino({
  level: config.logLevel,
  redact: {
    paths: ['req.headers.authorization', '*.password', '*.contrasena'],
    censor: '***',
  },
  formatters: {
    log(obj) {
      const copia = { ...obj };
      if (copia.from) copia.from = enmascararTelefono(copia.from);
      if (copia.to) copia.to = enmascararTelefono(copia.to);
      return copia;
    },
  },
});

module.exports = logger;
module.exports.enmascararTelefono = enmascararTelefono;
```

### L.6 — `src/db.js`

```js
'use strict';
const { Pool } = require('pg');
const config = require('./config');
const logger = require('./logger');

const pool = new Pool(config.pg);

pool.on('error', function (err) {
  logger.error({ err: err.message }, 'Error inesperado en pool PostgreSQL');
});

// Busca un paciente por su numero de telefono (formato libre, igualamos por sufijo).
// telefono_paciente almacena el telefono tal y como el paciente lo registro:
// puede traer prefijo internacional o no. Hacemos match por LIKE sobre los
// ultimos 9 digitos para tolerar variaciones.
async function buscarPacientePorTelefono(telefonoE164) {
  const sufijo = telefonoE164.replace(/\D/g, '').slice(-9);
  const { rows } = await pool.query(
    `SELECT p.dni_pac, p.dni_san, p.nombre_pac, p.apellido1_pac, p.activo
     FROM paciente p
     INNER JOIN telefono_paciente tp ON tp.dni_pac = p.dni_pac
     WHERE regexp_replace(tp.telefono, '\\D', '', 'g') LIKE '%' || $1
       AND p.activo = TRUE
     LIMIT 1`,
    [sufijo]
  );
  return rows[0] || null;
}

// Busca un paciente por DNI exacto.
async function buscarPacientePorDni(dni) {
  const { rows } = await pool.query(
    `SELECT dni_pac, dni_san, nombre_pac, apellido1_pac, activo
     FROM paciente
     WHERE dni_pac = $1 AND activo = TRUE
     LIMIT 1`,
    [dni]
  );
  return rows[0] || null;
}

// Comprueba si el slot (dni_san, fecha, hora) esta libre.
async function slotDisponible(dniSan, fecha, hora) {
  const { rows } = await pool.query(
    `SELECT 1 FROM cita WHERE dni_san = $1 AND fecha_cita = $2 AND hora = $3 LIMIT 1`,
    [dniSan, fecha, hora]
  );
  return rows.length === 0;
}

// Inserta una cita. Devuelve true si OK, false si ya existia.
async function insertarCita(dniPac, dniSan, fecha, hora) {
  try {
    await pool.query(
      `INSERT INTO cita (dni_pac, dni_san, fecha_cita, hora) VALUES ($1, $2, $3, $4)`,
      [dniPac, dniSan, fecha, hora]
    );
    return true;
  } catch (err) {
    if (err.code === '23505') return false; // unique violation
    throw err;
  }
}

// Anade entrada al audit_log. La columna `actor` puede no existir; el Doer
// adapta esta funcion al schema real leyendo audit_log antes de implementar.
// Como fallback seguro, si el INSERT falla, lo registramos en log y NO
// lanzamos — el cita ya esta creado, no queremos rollback.
async function registrarAudit(actor, accion, dniPac, detalles) {
  try {
    await pool.query(
      `INSERT INTO audit_log (accion, tabla, registro_id, detalles, fecha)
       VALUES ($1, $2, $3, $4, NOW())`,
      [accion, 'cita', dniPac, JSON.stringify({ actor, ...detalles })]
    );
  } catch (err) {
    logger.warn({ err: err.message }, 'Audit log fallo — cita ya creada, continuando');
  }
}

module.exports = {
  buscarPacientePorTelefono,
  buscarPacientePorDni,
  slotDisponible,
  insertarCita,
  registrarAudit,
  pool,
};
```

> **DOER NOTE for L.6:** before committing, the Doer reads `/api/src/main/resources/db/migration/V*.sql` looking for the `audit_log` table definition. If columns differ from `(accion, tabla, registro_id, detalles, fecha)`, adjust `registrarAudit` to fit. The chatbot does NOT add a Flyway migration of its own — the audit table belongs to the API/desktop schema.

### L.7 — `src/llm.js`

```js
'use strict';
const { Ollama } = require('ollama');
const config = require('./config');
const logger = require('./logger');
const { SYSTEM_PROMPT } = require('./promptTemplates');

const cliente = new Ollama({ host: config.ollama.url });

// Construye el array de mensajes del chat para Ollama:
// [system, ...historial, user]
function construirMensajes(historial, nuevoMensaje) {
  const hoy = new Date().toISOString().slice(0, 10);
  const sistema = SYSTEM_PROMPT.replace('{{HOY_ISO}}', hoy);
  const turnos = historial.map(function (turno) {
    return { role: turno.role, content: turno.content };
  });
  return [
    { role: 'system', content: sistema },
    ...turnos,
    { role: 'user', content: nuevoMensaje },
  ];
}

// Llama a Qwen y parsea la respuesta JSON. Si el modelo devuelve algo no parseable,
// devolvemos un fallback con intent='other'.
async function extraerIntencion(historial, nuevoMensaje) {
  const mensajes = construirMensajes(historial, nuevoMensaje);
  let raw;
  try {
    const respuesta = await cliente.chat({
      model: config.ollama.model,
      messages: mensajes,
      format: 'json',
      stream: false,
      options: { temperature: 0.2 },
    });
    raw = respuesta.message.content;
  } catch (err) {
    logger.error({ err: err.message }, 'Fallo llamada a Ollama');
    return {
      intent: 'other',
      fecha: null,
      hora: null,
      respuesta_usuario: 'Lo siento, ahora mismo no puedo procesar tu mensaje. Por favor, llama al hospital.',
    };
  }

  try {
    const parsed = JSON.parse(raw);
    return {
      intent: parsed.intent || 'other',
      fecha: typeof parsed.fecha === 'string' ? parsed.fecha : null,
      hora: typeof parsed.hora === 'string' ? parsed.hora : null,
      respuesta_usuario: typeof parsed.respuesta_usuario === 'string' ? parsed.respuesta_usuario : '',
    };
  } catch (err) {
    logger.warn({ raw }, 'Qwen devolvio JSON invalido — fallback');
    return {
      intent: 'other',
      fecha: null,
      hora: null,
      respuesta_usuario: 'No te he entendido bien. Por favor, indicame la fecha (DD/MM/AAAA) y la hora (HH:MM).',
    };
  }
}

module.exports = { extraerIntencion };
```

### L.8 — `src/promptTemplates.js`

```js
'use strict';

// Prompt del sistema. Spanish, sin diacriticos. Fuerza salida JSON.
const SYSTEM_PROMPT = `
Eres el asistente de citas medicas de RehabiAPP, una clinica de rehabilitacion en Espana. Tu unica funcion es ayudar a pacientes a pedir citas via WhatsApp.

REGLAS DE NEGOCIO:
- Las citas son de lunes a viernes, de 09:00 a 18:00.
- Los slots son de 30 minutos.
- No puedes pedir citas con menos de 2 horas de antelacion.
- No puedes pedir citas en el pasado.
- Hoy es {{HOY_ISO}}.

REGLAS DE COMPORTAMIENTO:
- Solo gestionas peticiones de citas. Si el paciente pregunta otra cosa, indicale amablemente que llame al hospital.
- No inventes datos. Si dudas sobre fecha o hora, pidela.
- Si la fecha es ambigua (p.ej. "el martes"), calcula el martes siguiente al dia de hoy.
- Si te falta la fecha o la hora, pide solo el dato que falta.
- Responde siempre en castellano sin diacriticos (sin tildes ni eñes — usa "n" en lugar de "ñ").

FORMATO DE RESPUESTA:
Responde SIEMPRE con un objeto JSON valido, exactamente con esta forma:
{
  "intent": "book" | "info" | "other",
  "fecha": "YYYY-MM-DD" o null,
  "hora": "HH:MM" o null,
  "respuesta_usuario": "<mensaje en castellano para el paciente>"
}

EJEMPLOS:
Paciente: "Hola, quiero pedir una cita"
Tu: {"intent":"book","fecha":null,"hora":null,"respuesta_usuario":"Hola. Para tu cita, indicame la fecha (DD/MM/AAAA) y la hora (HH:MM)."}

Paciente: "El 15 de junio a las 10"
Tu: {"intent":"book","fecha":"2026-06-15","hora":"10:00","respuesta_usuario":"Perfecto, confirmo cita para el 15 de junio a las 10:00. Voy a comprobar disponibilidad."}

Paciente: "manana a las 9.30"
Tu: {"intent":"book","fecha":"<la fecha de manana>","hora":"09:30","respuesta_usuario":"Confirmo cita para manana a las 09:30. Voy a comprobar disponibilidad."}

Paciente: "Quiero cancelar"
Tu: {"intent":"other","fecha":null,"hora":null,"respuesta_usuario":"Para cancelar una cita, por favor llama al hospital al telefono de contacto que aparece en la app."}

Paciente: "Cuanto cuesta?"
Tu: {"intent":"info","fecha":null,"hora":null,"respuesta_usuario":"Para informacion sobre tarifas, llama al hospital. Yo solo puedo gestionar peticiones de cita."}
`.trim();

const ERROR_REPLIES = Object.freeze({
  PACIENTE_NO_ENCONTRADO_TEL: 'No encuentro tu numero entre nuestros pacientes. Por favor, indicame tu DNI (con la letra) para identificarte.',
  PACIENTE_NO_ENCONTRADO_DNI: 'Ese DNI no aparece en nuestro registro. Si eres paciente, por favor registra tu telefono en la app movil de RehabiAPP y vuelve a escribirme.',
  PACIENTE_INACTIVO: 'Tu cuenta de paciente esta dada de baja. Por favor, llama al hospital.',
  FUERA_HORARIO: 'Esa hora esta fuera del horario de atencion (lunes a viernes, 09:00 a 18:00). Por favor, elige otra.',
  EN_PASADO: 'No puedo pedir una cita en el pasado. Por favor, indicame una fecha y hora futura.',
  POCA_ANTELACION: 'Necesito al menos 2 horas de antelacion. Por favor, elige una hora mas tarde.',
  SLOT_OCUPADO: 'Ese hueco ya esta ocupado. Por favor, dime otra hora del mismo dia o de los siguientes.',
  CITA_OK: function (fecha, hora, medico) {
    return `Cita confirmada para el ${fecha} a las ${hora} con ${medico}. Recibiras un recordatorio. Para cancelar, llama al hospital.`;
  },
  ERROR_INTERNO: 'Ha habido un problema de mi lado. Por favor, intentalo de nuevo en unos minutos o llama al hospital.',
});

module.exports = { SYSTEM_PROMPT, ERROR_REPLIES };
```

### L.9 — `src/sessions.js`

```js
'use strict';

// Sesiones en memoria. Un cierre del servicio reinicia toda la conversacion —
// aceptable para MVP. La clave es el numero E.164 sin '+' (igual que from de WA).
//
// ConversationState = {
//   historial: [{role:'user'|'assistant', content:string}],
//   pacienteDni: string|null,
//   pendienteIdentificarDni: boolean,
//   ultimaInteraccion: number  // ms desde epoch
// }

const TTL_MS = 30 * 60 * 1000; // 30 minutos sin actividad → reset

const sesiones = new Map();

function obtener(phoneE164) {
  let s = sesiones.get(phoneE164);
  if (!s || (Date.now() - s.ultimaInteraccion > TTL_MS)) {
    s = {
      historial: [],
      pacienteDni: null,
      pendienteIdentificarDni: false,
      ultimaInteraccion: Date.now(),
    };
    sesiones.set(phoneE164, s);
  }
  return s;
}

function actualizar(phoneE164, parche) {
  const s = obtener(phoneE164);
  Object.assign(s, parche, { ultimaInteraccion: Date.now() });
  sesiones.set(phoneE164, s);
}

function anadirTurno(phoneE164, role, content) {
  const s = obtener(phoneE164);
  s.historial.push({ role, content });
  // Truncar a los ultimos 10 turnos para no inflar el contexto del LLM
  if (s.historial.length > 10) s.historial = s.historial.slice(-10);
  s.ultimaInteraccion = Date.now();
}

function reset(phoneE164) {
  sesiones.delete(phoneE164);
}

module.exports = { obtener, actualizar, anadirTurno, reset };
```

### L.10 — `src/booking.js`

```js
'use strict';
const config = require('./config');
const db = require('./db');
const llm = require('./llm');
const sessions = require('./sessions');
const { ERROR_REPLIES } = require('./promptTemplates');
const logger = require('./logger');

// Comprueba si la fecha es un dia laborable segun WORKING_DAYS (ISO 1=lun..7=dom).
function esDiaLaborable(fechaIso) {
  const d = new Date(fechaIso + 'T00:00:00');
  const iso = d.getDay() === 0 ? 7 : d.getDay(); // JS: 0=dom, ISO: 7=dom
  return config.reglas.workingDays.includes(iso);
}

// Comprueba si la hora cae en horario [open, close).
function enHorario(horaHHMM) {
  return horaHHMM >= config.reglas.openTime && horaHHMM < config.reglas.closeTime;
}

// Comprueba que la cita es futura y con suficiente antelacion.
function tieneAntelacionSuficiente(fechaIso, horaHHMM) {
  const cita = new Date(`${fechaIso}T${horaHHMM}:00`);
  const minimo = Date.now() + config.reglas.minLeadHours * 60 * 60 * 1000;
  return cita.getTime() >= minimo;
}

// Identifica al paciente: primero por telefono, luego por DNI si la sesion lo pidio.
async function identificar(phoneE164, mensaje) {
  const s = sessions.obtener(phoneE164);

  if (s.pacienteDni) {
    return await db.buscarPacientePorDni(s.pacienteDni);
  }

  // Intentar match por telefono
  const porTel = await db.buscarPacientePorTelefono(phoneE164);
  if (porTel) {
    sessions.actualizar(phoneE164, { pacienteDni: porTel.dni_pac });
    return porTel;
  }

  // Si no hay match y el usuario ya escribio un DNI, intentar
  const dniMatch = mensaje.match(/\b(\d{8}[A-Za-z])\b/);
  if (dniMatch) {
    const dni = dniMatch[1].toUpperCase();
    const porDni = await db.buscarPacientePorDni(dni);
    if (porDni) {
      sessions.actualizar(phoneE164, { pacienteDni: porDni.dni_pac, pendienteIdentificarDni: false });
      return porDni;
    }
    return { __noEncontrado: 'dni' };
  }

  // No hay match y no hay DNI en el mensaje — pedirlo
  sessions.actualizar(phoneE164, { pendienteIdentificarDni: true });
  return { __noEncontrado: 'tel' };
}

// Procesa un mensaje entrante. Devuelve la respuesta del bot (string).
async function manejarMensaje(phoneE164, texto) {
  sessions.anadirTurno(phoneE164, 'user', texto);

  // 1) Identificar al paciente
  const paciente = await identificar(phoneE164, texto);
  if (paciente && paciente.__noEncontrado === 'tel') {
    sessions.anadirTurno(phoneE164, 'assistant', ERROR_REPLIES.PACIENTE_NO_ENCONTRADO_TEL);
    return ERROR_REPLIES.PACIENTE_NO_ENCONTRADO_TEL;
  }
  if (paciente && paciente.__noEncontrado === 'dni') {
    sessions.anadirTurno(phoneE164, 'assistant', ERROR_REPLIES.PACIENTE_NO_ENCONTRADO_DNI);
    return ERROR_REPLIES.PACIENTE_NO_ENCONTRADO_DNI;
  }
  if (!paciente) {
    sessions.anadirTurno(phoneE164, 'assistant', ERROR_REPLIES.ERROR_INTERNO);
    return ERROR_REPLIES.ERROR_INTERNO;
  }

  // 2) Pasar el mensaje a Qwen para extraer intent + fecha + hora
  const sesion = sessions.obtener(phoneE164);
  const intencion = await llm.extraerIntencion(sesion.historial.slice(0, -1), texto);

  // 3) Branch por intent
  if (intencion.intent !== 'book') {
    sessions.anadirTurno(phoneE164, 'assistant', intencion.respuesta_usuario);
    return intencion.respuesta_usuario;
  }

  // 4) Si falta fecha u hora, pedirla
  if (!intencion.fecha || !intencion.hora) {
    sessions.anadirTurno(phoneE164, 'assistant', intencion.respuesta_usuario);
    return intencion.respuesta_usuario;
  }

  // 5) Validar reglas de negocio
  if (!esDiaLaborable(intencion.fecha) || !enHorario(intencion.hora)) {
    sessions.anadirTurno(phoneE164, 'assistant', ERROR_REPLIES.FUERA_HORARIO);
    return ERROR_REPLIES.FUERA_HORARIO;
  }
  if (!tieneAntelacionSuficiente(intencion.fecha, intencion.hora)) {
    sessions.anadirTurno(phoneE164, 'assistant', ERROR_REPLIES.POCA_ANTELACION);
    return ERROR_REPLIES.POCA_ANTELACION;
  }

  // 6) Comprobar disponibilidad y reservar
  const horaSql = `${intencion.hora}:00`;
  const libre = await db.slotDisponible(paciente.dni_san, intencion.fecha, horaSql);
  if (!libre) {
    sessions.anadirTurno(phoneE164, 'assistant', ERROR_REPLIES.SLOT_OCUPADO);
    return ERROR_REPLIES.SLOT_OCUPADO;
  }

  const ok = await db.insertarCita(paciente.dni_pac, paciente.dni_san, intencion.fecha, horaSql);
  if (!ok) {
    sessions.anadirTurno(phoneE164, 'assistant', ERROR_REPLIES.SLOT_OCUPADO);
    return ERROR_REPLIES.SLOT_OCUPADO;
  }

  await db.registrarAudit(config.auditActor, 'INSERT', paciente.dni_pac, {
    canal: 'whatsapp',
    fecha: intencion.fecha,
    hora: horaSql,
    dni_san: paciente.dni_san,
  });

  const reply = ERROR_REPLIES.CITA_OK(intencion.fecha, intencion.hora, paciente.dni_san);
  sessions.anadirTurno(phoneE164, 'assistant', reply);
  logger.info({ from: phoneE164, fecha: intencion.fecha, hora: horaSql }, 'Cita creada via chatbot');
  return reply;
}

module.exports = { manejarMensaje };
```

### L.11 — `src/whatsapp.js`

```js
'use strict';
const { Client, LocalAuth } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');
const logger = require('./logger');
const booking = require('./booking');

// Construye un cliente whatsapp-web.js con auth persistente.
// Primera ejecucion: imprime QR en terminal — escanear desde la app de WhatsApp
// del numero del hospital. Despues, .wwebjs_auth/ guarda la sesion.
function crearCliente() {
  const cliente = new Client({
    authStrategy: new LocalAuth({ clientId: 'rehabiapp-chatbot' }),
    puppeteer: {
      args: ['--no-sandbox', '--disable-setuid-sandbox'],
    },
  });

  cliente.on('qr', function (qr) {
    logger.info('Escanea este QR con la app de WhatsApp del numero del hospital:');
    qrcode.generate(qr, { small: true });
  });

  cliente.on('ready', function () {
    logger.info('WhatsApp client listo. Esperando mensajes.');
  });

  cliente.on('auth_failure', function (msg) {
    logger.error({ msg }, 'Fallo de autenticacion WhatsApp — borra .wwebjs_auth/ y reinicia');
  });

  cliente.on('disconnected', function (motivo) {
    logger.warn({ motivo }, 'WhatsApp desconectado — proceso terminara, supervisor lo reinicia');
    process.exit(1);
  });

  cliente.on('message', async function (msg) {
    // Ignorar grupos, broadcasts, mensajes propios y stickers / media sin texto
    if (msg.fromMe) return;
    if (msg.from.endsWith('@g.us')) return;
    if (msg.from === 'status@broadcast') return;
    if (!msg.body || msg.body.trim() === '') return;

    // msg.from formato '34628678888@c.us' — extraer E.164 sin '+'
    const phoneE164 = msg.from.split('@')[0];
    logger.info({ from: phoneE164, len: msg.body.length }, 'Mensaje recibido');

    try {
      const respuesta = await booking.manejarMensaje(phoneE164, msg.body);
      await msg.reply(respuesta);
    } catch (err) {
      logger.error({ err: err.message, from: phoneE164 }, 'Error manejando mensaje');
      await msg.reply('Ha habido un problema. Por favor, intentalo de nuevo en unos minutos.').catch(function () {});
    }
  });

  return cliente;
}

module.exports = { crearCliente };
```

### L.12 — `src/index.js`

```js
'use strict';
const express = require('express');
const config = require('./config');
const logger = require('./logger');
const wa = require('./whatsapp');

async function main() {
  // 1) Servidor HTTP de health para K8s / monitorizacion
  const app = express();
  app.get('/health', function (_req, res) { res.json({ status: 'UP' }); });
  app.listen(config.port, function () {
    logger.info({ port: config.port }, 'HTTP health server escuchando');
  });

  // 2) Cliente WhatsApp (bloquea hasta event 'ready')
  const cliente = wa.crearCliente();
  await cliente.initialize();
}

main().catch(function (err) {
  logger.fatal({ err: err.message, stack: err.stack }, 'Fallo critico de arranque');
  process.exit(1);
});

// Apagado limpio
process.on('SIGTERM', function () { process.exit(0); });
process.on('SIGINT', function () { process.exit(0); });
```

### L.13 — Tests (`/chatbot/test/booking.test.js` skeleton)

The Doer writes ONE smoke test that:

- Stubs `db.buscarPacientePorTelefono` to return a fixed patient.
- Stubs `db.slotDisponible` to return `true`.
- Stubs `db.insertarCita` to return `true`.
- Stubs `db.registrarAudit` to be a no-op.
- Stubs `llm.extraerIntencion` to return `{ intent:'book', fecha:'2026-06-15', hora:'10:00', respuesta_usuario:'...' }`.
- Calls `booking.manejarMensaje('34999111222', 'cita 15 de junio 10:00')`.
- Asserts the reply contains "Cita confirmada".

Use Node's built-in `node:test` and `node:assert/strict`. No Jest, no Mocha. Same convention as `/mobile/backend/test/*.test.js`. Run with `npm test`.

A second test asserts business rule rejection:

- Stub Qwen to return `{ intent:'book', fecha:'2026-06-13', hora:'10:00', ... }` (a Saturday).
- Assert the reply equals `ERROR_REPLIES.FUERA_HORARIO`.

Total test file: ~80 lines. Pass condition: `cd chatbot && npm test` returns 2 passing.

### L.14 — First-run instructions (Doer reads to itself, does not run)

```bash
# 1) Instalar Ollama y descargar Qwen 2.5 si no esta.
ollama pull qwen2.5:latest         # solo si no aparece en `ollama list`

# 2) Instalar dependencias del chatbot
cd /home/alaslibres/DAM/RehabiAPP/chatbot
npm install

# 3) Configurar entorno
cp .env.example .env
# Editar .env si las credenciales de PG difieren de admin/admin@localhost

# 4) Asegurar que el postgres del proyecto esta arriba
docker compose -f /home/alaslibres/DAM/RehabiAPP/infra/docker-compose.yml up -d postgresql

# 5) Asegurar que Ollama esta corriendo
ollama serve &              # solo si no esta ya corriendo como servicio

# 6) Arrancar el chatbot
npm run dev
# La PRIMERA vez imprimira un QR en terminal.
# Abrir WhatsApp -> Ajustes -> Dispositivos vinculados -> Vincular un dispositivo
# y escanear el QR.
# La sesion queda en .wwebjs_auth/ — proximas ejecuciones no piden QR.

# 7) Probar end-to-end
# Desde la app movil (Pixel 8), pulsar "Abrir WhatsApp" en la pestana de citas.
# Escribir: "Hola, quiero cita para manana a las 10".
# El bot debe responder confirmando o pidiendo aclaracion segun reglas.
```

### L.15 — Mobile coordination (already covered by J.5)

The mobile WhatsApp button (J.5) opens `whatsapp://send?phone=34628678888&text=...`. The phone number `34628678888` MUST equal `HOSPITAL_PHONE_E164` in `/chatbot/.env`. The Doer audits both files match before reporting Phase L done. If the developer changes the hospital number, three places update simultaneously:

1. `mobile/frontend/src/components/HospitalContactCard.tsx:8` (display)
2. `mobile/frontend/src/components/WhatsAppButton.tsx` (deep link)
3. `chatbot/.env` (the WhatsApp account that the bot runs on)

### L.16 — Acceptance criteria for Phase L

The Doer reports Phase L done only when ALL are simultaneously true:

- [ ] `/chatbot/` directory created with the structure of §L.1.
- [ ] `npm install` completes with zero vulnerabilities of severity `high` or `critical` (`npm audit --omit=dev`).
- [ ] `npm test` passes with at least the two smoke tests of §L.13.
- [ ] `npm run dev` boots, prints a QR the first time, and reaches the `WhatsApp client listo` log line after a successful scan.
- [ ] `curl http://localhost:4000/health` returns `{"status":"UP"}`.
- [ ] End-to-end manual test: from a real phone with the seed patient's phone (DNI `12345678Z` is seeded in `V14__datos_prueba_desarrollo.sql` — its phone is `600000000` in `telefono_paciente`), send "cita manana 10:00" to the chatbot's WhatsApp number → bot replies confirming a cita → `psql -c "SELECT * FROM cita WHERE dni_pac='12345678Z' ORDER BY fecha_cita DESC LIMIT 1"` shows the new row → the desktop ERP, after refreshing the calendar, displays the new appointment.
- [ ] Negative path: same patient sends "cita el sabado 10:00" → bot replies `FUERA_HORARIO`. No row inserted.
- [ ] Negative path: an unknown phone number sends a message → bot replies `PACIENTE_NO_ENCONTRADO_TEL`. No row inserted.
- [ ] Audit: `psql -c "SELECT * FROM audit_log WHERE detalles::text LIKE '%CHATBOT_BOT%' ORDER BY fecha DESC LIMIT 5"` returns at least one row per successful booking.
- [ ] `mobile/frontend/src/components/WhatsAppButton.tsx`'s phone number matches `chatbot/.env`'s `HOSPITAL_PHONE_E164`.
- [ ] Engram saved via `mem_save` titled `chatbot Phase L — WhatsApp + Qwen DONE` summarizing the new directory and the data path.

### L.17 — Out of scope (explicitly OUT — open as new proposals)

- Cancellation / reschedule via WhatsApp.
- Multi-language (German, English, etc.).
- Webhook from desktop to notify chatbot when a sanitario manually changes a cita (the chatbot does not need to know).
- Voice messages (Whisper transcription).
- Persistent conversation history across restarts (Redis / SQLite).
- Pretty broadcast notifications to the patient when the cita is approaching (overlap with `expo-notifications` local reminders — different channel).
- Rate limiting per phone number — recommended for production but adds 0 value in the on-prem MVP.
- Monitoring dashboard / Grafana panel.

### L.18 — Plan-level consistency check (Thinker self-audit, addendum to §11)

| Claim                                                                                                  | Verification                                                                                                                | Status |
|--------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|--------|
| The seed patient `12345678Z` has phone `600000000` in `telefono_paciente`.                              | `/api/src/main/resources/db/migration/V14__datos_prueba_desarrollo.sql` line 117 (Phase 14 of `/api`)                       | OK     |
| `paciente.dni_san` is the FK to the patient's primary practitioner.                                    | `/api/src/main/resources/db/migration/V1*.sql` paciente schema, confirmed by `\d paciente` in 2026-05-07 audit              | OK     |
| The `cita` PK is `(dni_pac, dni_san, fecha_cita, hora)`.                                                | `\d cita` output during Phase 14 — matches `db.insertarCita` ON CONFLICT path                                              | OK     |
| Ollama's npm client supports `format: 'json'` for guaranteed JSON output.                               | `ollama` npm v0.5.x changelog                                                                                              | OK     |
| `whatsapp-web.js` `LocalAuth` strategy persists session in a directory.                                | `whatsapp-web.js` README                                                                                                    | OK     |
| The desktop reads `cita` from PostgreSQL via JDBC and refreshes its calendar on user action.            | `/CLAUDE.md` §6 "/desktop --> PostgreSQL (legacy direct)" + `/desktop/CLAUDE.md`                                            | OK     |
| The chatbot's PG credentials match `infra/docker-compose.yml` (admin / admin / rehabiapp / port 5432). | `infra/docker-compose.yml` postgresql service                                                                              | OK     |
| The mobile WhatsApp deep link in J.5 already points to `34628678888`, equal to `HOSPITAL_PHONE_E164`.   | `mobile/frontend/src/components/WhatsAppButton.tsx` after J.5 + `chatbot/.env.example` (this section)                       | OK     |
| There is no `/api` change required for Phase L.                                                         | Read of `/api/CLAUDE.md` §5 (Phase 14 closes the API checklist)                                                            | OK     |
| There is no `/desktop` change required for Phase L.                                                     | The desktop already polls `cita` and renders new rows via existing controller                                                | OK     |

No internal inconsistencies detected. Phase L is self-consistent and consistent with the rest of the document.

---

*This file is the single source of truth for the mobile stabilization sprint of 2026-05-07 AND for the chatbot integration (Phase L). The detailed code instructions live here. `mobile/frontend/PLAN.md` and `mobile/backend/PLAN.md` mirror only the mobile checklist; `chatbot/PLAN.md` will mirror Phase L once split out.*
