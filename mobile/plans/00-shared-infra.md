# Plan 00 — Shared Infrastructure

Scope: theme (dark mode), typography (Inter), font scaling, floating-card primitives, header titles, bootstrap data hydration, notifications init, error popup extensions. MUST be completed BEFORE any per-tab plan. All plans 01–06 depend on primitives created here.

---

## 1. Dependencies to install

In `/mobile/frontend`:

```
npx expo install expo-font expo-notifications expo-application expo-file-system expo-sharing react-native-svg @react-native-async-storage/async-storage
npm i react-native-chart-kit @expo-google-fonts/inter
```

Also ensure already present: `@apollo/client`, `zustand`, `expo-router`, `expo-secure-store`, `nativewind`.

---

## 2. tailwind.config.js

Full replacement content:

```js
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./app/**/*.{js,ts,tsx}', './src/**/*.{js,ts,tsx}'],
  presets: [require('nativewind/preset')],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#EFF6FF', 100: '#DBEAFE', 200: '#BFDBFE', 300: '#93C5FD',
          400: '#60A5FA', 500: '#3B82F6', 600: '#2563EB', 700: '#1D4ED8',
          800: '#1E40AF', 900: '#1E3A8A',
        },
        surface: '#FFFFFF',
        'surface-dark': '#111827',
        background: '#F0F4FF',
        'background-dark': '#0B1220',
        'text-primary': '#1E293B',
        'text-primary-dark': '#F1F5F9',
        'text-secondary': '#64748B',
        'text-secondary-dark': '#94A3B8',
        error: '#EF4444',
        success: '#22C55E',
        border: '#E2E8F0',
        'border-dark': '#1F2937',
      },
      fontFamily: {
        sans: ['Inter_400Regular'],
        medium: ['Inter_500Medium'],
        semibold: ['Inter_600SemiBold'],
        bold: ['Inter_700Bold'],
      },
      borderRadius: { '2xl': '16px', '3xl': '24px' },
    },
  },
  plugins: [],
};
```

Dark tokens used via `dark:` prefix. Example: `className="bg-surface dark:bg-surface-dark"`.

---

## 3. Theme + FontScale context

Create `src/utils/theme.ts`:

```ts
import { createContext, useContext } from 'react';
export type ThemeMode = 'light' | 'dark' | 'system';
export type ThemeScheme = 'light' | 'dark';
export const ThemeContext = createContext<{ scheme: ThemeScheme; mode: ThemeMode; setMode: (m: ThemeMode) => void }>({
  scheme: 'light', mode: 'system', setMode: () => {},
});
export function useTheme() { return useContext(ThemeContext); }
```

Create `src/utils/fontScale.ts`:

```ts
import { createContext, useContext } from 'react';
export type FontScale = 0.9 | 1.0 | 1.15 | 1.3;
export const FontScaleContext = createContext<{ scale: FontScale; setScale: (s: FontScale) => void }>({
  scale: 1.0, setScale: () => {},
});
export function useFontScale() { return useContext(FontScaleContext).scale; }
```

---

## 4. New reusable components

All components in `src/components/`. Spanish comments. Use `type` only.

### AppText.tsx
Wrap `<Text>`. Multiplies `style.fontSize` by `useFontScale()`. Default family: `Inter_400Regular`. Props: `{ variant?: 'body'|'title'|'subtitle'|'label'|'caption'; weight?: 'regular'|'medium'|'semibold'|'bold'; className?: string; children }`.

Variants map to sizes: body=16, title=24, subtitle=18, label=14, caption=12.

### FloatingCard.tsx
`<View>` wrapper. Default classes: `bg-surface dark:bg-surface-dark rounded-2xl p-4 shadow-sm`. Adds `elevation: 2` on Android via style prop. Props: `{ className?: string; children; onPress?; }`. If `onPress` → wrap in `<Pressable>` with active opacity.

### ConfirmModal.tsx
Generic confirm dialog. Props: `{ visible; title; message; confirmLabel?='Confirmar'; cancelLabel?='Cancelar'; onConfirm; onCancel; destructive?=false }`. Same modal pattern as `ErrorPopup.tsx`. Destructive flag → confirm button uses `bg-error`.

### EmptyState.tsx
Props: `{ icon: IoniconName; title: string; message?: string; }`. Centred, `text-text-secondary dark:text-text-secondary-dark`, icon 56dp.

### SegmentedControl.tsx
Props: `{ options: {label: string; value: string}[]; value: string; onChange: (v: string) => void; }`. Row of pills, active pill `bg-primary-600 text-white`, inactive `bg-surface text-text-primary`. Rounded full.

### BackdropModal.tsx
Low-level base used by all modals. Props: `{ visible; onClose; children; }`. Dimmed backdrop (`bg-black/40`), centered card. Pressing backdrop calls `onClose`. Reused by ConfirmModal, ErrorPopup (refactor), ProgressChartModal.

---

## 5. Font loading (Inter)

In `app/_layout.tsx`, before rendering, use `useFonts` from `expo-font`:

```ts
import { useFonts, Inter_400Regular, Inter_500Medium, Inter_600SemiBold, Inter_700Bold } from '@expo-google-fonts/inter';
const [fontsLoaded] = useFonts({ Inter_400Regular, Inter_500Medium, Inter_600SemiBold, Inter_700Bold });
if (!fontsLoaded) return null; // or <SplashScreen />
```

---

## 6. Root layout changes (`app/_layout.tsx`)

Wrapping order (outer → inner):
1. `ApolloProvider`
2. `SafeAreaProvider`
3. `ThemeProvider` (reads `settingsStore.themeMode`, subscribes to `Appearance.getColorScheme()` when mode='system')
4. `FontScaleProvider` (reads `settingsStore.fontScale`)
5. `AuthGuard`
6. `<Slot />` + `GlobalErrorPopup`

Add effect: when `isAuthenticated` flips to `true`, call `bootstrapStore.getState().hydrate()` once. When flips to `false`, call `bootstrapStore.getState().reset()`.

Root `<View>` gets dynamic className based on theme scheme so NativeWind `dark:` variants resolve.

---

## 7. Tab layout (`app/(tabs)/_layout.tsx`)

- Keep 7 tabs already defined.
- Each `<Tabs.Screen>` explicitly sets `options.title` (header title): Inicio, Citas, Juegos, Tratamientos, Progreso, Perfil, Configuración.
- Header: `headerStyle.backgroundColor` = `scheme==='dark' ? '#111827' : '#FFFFFF'`; `headerTitleStyle` uses `Inter_600SemiBold` 18px; `headerTintColor` follows scheme.
- Tab bar: same dark-aware colours. Active `#2563EB`, inactive `text-secondary` (light) / `text-secondary-dark` (dark).
- Order remains: index, appointments, games, treatments, progress, profile, settings.

---

## 8. bootstrapStore (`src/store/bootstrapStore.ts`)

```ts
type BootstrapState = {
  hydrated: boolean;
  hydrating: boolean;
  lastHydratedAt: number | null;
  hydrate: () => Promise<void>;
  reset: () => void;
};
```

`hydrate()` calls in parallel (`Promise.allSettled`):
- `userStore.getState().fetchProfile()`
- `appointmentsStore.getState().fetch()`
- `gamesStore.getState().fetch()`
- `treatmentsStore.getState().fetch()`
- `progressStore.getState().fetch()`

Also schedules local notifications for upcoming appointments via `src/utils/notifications.ts` after appointments resolve.

`reset()` calls each slice's reset.

---

## 9. settingsStore (`src/store/settingsStore.ts`)

Persisted via AsyncStorage (manual, no middleware — keep bundle small). Key `@rehabiapp/settings`.

```ts
type SettingsState = {
  themeMode: 'light' | 'dark' | 'system';
  fontScale: 0.9 | 1.0 | 1.15 | 1.3;
  notifAppointments: boolean;
  notifDoctorUpdates: boolean;
  hydrated: boolean;
  load: () => Promise<void>;
  setThemeMode: (m) => Promise<void>;
  setFontScale: (s) => Promise<void>;
  setNotifAppointments: (v) => Promise<void>;
  setNotifDoctorUpdates: (v) => Promise<void>;
};
```

Defaults: system, 1.0, true, true. `load()` called before app render in root layout (block render until hydrated).

---

## 10. Notifications util (`src/utils/notifications.ts`)

Functions:
- `initNotifications()`: set handler `setNotificationHandler({ shouldShowAlert: true, shouldPlaySound: true, shouldSetBadge: false })`. Called once in root layout.
- `requestPermission(): Promise<boolean>`.
- `registerPushToken(): Promise<string | null>`: calls `Notifications.getExpoPushTokenAsync()`; POSTs to BFF `registerDeviceToken` mutation; returns token.
- `scheduleAppointmentReminder(appointment: Appointment): Promise<string>`: schedules local notification 24h before `appointment.date + appointment.time`. Identifier = `appt-${appointment.id}`. Cancels prior identifier first. Body: `"Tienes cita mañana a las {hora} con {practitioner}."`
- `cancelAppointmentReminder(id: string)`.
- `scheduleTestNotification()`: fires in 5s. Used by Configuración.

All Spanish copy.

---

## 11. Apollo client adjustments (`src/services/graphql/client.ts`)

Add header `X-Timezone` via context link: `Intl.DateTimeFormat().resolvedOptions().timeZone`. Already used by BFF greeting middleware.

No other changes.

---

## 12. Error codes extension

Frontend `src/types/errors.ts` — extend `ErrorCode` union with: `NO_GAMES_ASSIGNED`, `NO_TREATMENTS_ASSIGNED`, `DOCUMENT_DOWNLOAD_FAILED`, `APPOINTMENT_REQUEST_INVALID_CONTACT`, `NOTIFICATION_PERMISSION_DENIED`, `BODY_PART_NO_DATA`, `APPOINTMENT_REQUEST_CONFLICT`.

Frontend `src/utils/errorHandler.ts` — add Spanish messages to `ERROR_MESSAGES`:
- NO_GAMES_ASSIGNED → { subtitle: 'Sin juegos', message: 'No tienes juegos asignados.' }
- NO_TREATMENTS_ASSIGNED → { subtitle: 'Sin tratamientos', message: 'No tienes tratamientos asignados todavía.' }
- DOCUMENT_DOWNLOAD_FAILED → { subtitle: 'Descarga fallida', message: 'No se pudo descargar el documento. Inténtalo de nuevo.' }
- APPOINTMENT_REQUEST_INVALID_CONTACT → { subtitle: 'Contacto inválido', message: 'Debes indicar un teléfono o email válido.' }
- NOTIFICATION_PERMISSION_DENIED → { subtitle: 'Permiso denegado', message: 'Activa las notificaciones desde los ajustes del sistema.' }
- BODY_PART_NO_DATA → { subtitle: 'Sin datos', message: 'Aún no hay métricas para esta zona.' }
- APPOINTMENT_REQUEST_CONFLICT → { subtitle: 'Horario ocupado', message: 'Ya hay una cita para esa fecha y hora.' }

Backend `src/utils/errors.js` — add same codes with mirrored Spanish subtitles/messages.

---

## 13. BFF bootstrap additions

Already-present typeDefs unchanged. Add new GraphQL mutation for device token (used in Configuración but created in shared plan since bootstrap registers token):

`src/graphql/typeDefs/settings.js`:
```graphql
extend type Mutation {
  registerDeviceToken(token: String!, platform: String!): Boolean!
  unregisterDeviceToken(token: String!): Boolean!
}
```

Resolver in `src/graphql/resolvers/settings.js` — guards `requireAuth()`, calls `notificationService.registerToken(dniPac, token, platform, javaToken)`.

`src/services/notificationService.js` — POST `/api/pacientes/{dniPac}/device-tokens` (MOCK — returns true). Contract documented here so Agent 1 can add `token_dispositivo` table.

---

## 14. Files touched (shared infra only)

Frontend:
- `tailwind.config.js` (full rewrite)
- `package.json` (deps)
- `app/_layout.tsx` (font + theme + fontScale providers + bootstrap)
- `app/(tabs)/_layout.tsx` (headers + dark mode)
- `src/utils/theme.ts`, `src/utils/fontScale.ts`, `src/utils/notifications.ts` (new)
- `src/components/AppText.tsx`, `FloatingCard.tsx`, `ConfirmModal.tsx`, `EmptyState.tsx`, `SegmentedControl.tsx`, `BackdropModal.tsx` (new)
- `src/components/ErrorPopup.tsx` (refactor to use BackdropModal + AppText)
- `src/store/bootstrapStore.ts`, `settingsStore.ts` (new)
- `src/store/authStore.ts` (call bootstrap hydrate after login success; call reset on logout)
- `src/types/errors.ts` (extend ErrorCode)
- `src/utils/errorHandler.ts` (extend messages)
- `src/services/graphql/mutations/settings.ts` (new, REGISTER_DEVICE_TOKEN)
- `src/services/graphql/client.ts` (X-Timezone header)

Backend:
- `src/graphql/typeDefs/settings.js` (new)
- `src/graphql/resolvers/settings.js` (new)
- `src/graphql/typeDefs/index.js`, `resolvers/index.js` (register new)
- `src/services/notificationService.js` (new)
- `src/services/apiClient.js` (mock `/api/pacientes/{dni}/device-tokens`)
- `src/utils/errors.js` (add codes)

---

## 15. Acceptance

1. App builds and boots with new deps — no red screen.
2. Inter font renders across all screens (inspect via `AppText`).
3. Dark mode toggle from Configuración flips every tab instantly.
4. Font scale toggle changes text size everywhere.
5. After login, `bootstrapStore.hydrated === true` and all slices populated (verify via devtools / console).
6. Logout clears all slices and returns to login.
7. Permission request for notifications shown on first enable. Test notification fires in 5s.
8. BFF `npm test` passes + new resolver test for `registerDeviceToken`.
9. TestSprite verification passes.
