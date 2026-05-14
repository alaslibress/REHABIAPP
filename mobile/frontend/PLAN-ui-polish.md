# PLAN — Mobile Frontend Phase K (UI polish + notifications fix)

> **File:** `/mobile/frontend/PLAN-ui-polish.md`
> **Author:** Agent 2 Thinker (Opus). PRESCRIPTIVE.
> **Domain:** `/mobile/frontend/` (NO backend changes in this iteration).
> **Branch:** `stats-implementation`.
> **Date:** 2026-05-11.
> **Doer model:** Sonnet. Read every section in order. Do NOT skip steps. Do NOT improvise.
> **Pass condition:** With the BFF running on `MOCK_API=true` and the app launched on a physical Android via Expo Go:
> 1. The bottom tab bar shows a clear container with proper padding above the system gesture area (no icons hugging the bottom edge).
> 2. All body and label text in every tab is white-ish in dark mode (`#F1F5F9`), not gray.
> 3. Toggling **Notificaciones de citas** in `Ajustes` requests OS permission, schedules a real reminder for the next appointment, and the **"Enviar notificacion de prueba"** button fires a banner 5 seconds later.
> 4. The phone displayed in the "Pedir cita nueva" card reads `+34 628 67 85 90` and tapping `WhatsApp` opens a chat with the same number.
> 5. App boot shows a splash screen with the RehabiAPP logo centered and the app name displayed on the home screen / task switcher reads **"RehabiAPP"**, not "fronted".

---

## 0. MANDATORY READING (do NOT skip)

Read these files BEFORE editing anything. They contain rules that override your default assumptions:

1. `/CLAUDE.md` §4.5 (code style: Spanish comments, no emojis), §4.6 (healthcare security: never log PII).
2. `/mobile/frontend/CLAUDE.md` (frontend conventions, NativeWind classes, store contracts).
3. `/mobile/frontend/IMPORTANT.md` (any active warnings).
4. `/mobile/frontend/PLAN.md` (Phase 5-bridge — already shipped, leave its files alone unless §K below tells you to touch them).
5. `/mobile/frontend/tailwind.config.js` (color tokens — `text-text-primary`, `text-text-primary-dark`, etc. ALWAYS use these tokens, never raw hex in className strings).
6. `/mobile/frontend/src/utils/theme.ts` and `src/utils/fontScale.ts` (consume via `useTheme()` / `useFontScale()` hooks — never read `Appearance` directly inside components).

---

## 1. NON-NEGOTIABLES

| # | Rule |
|---|------|
| 1 | Edit ONLY the files explicitly listed in §K.1–§K.5. Do NOT reformat unrelated files. |
| 2 | Do NOT install new dependencies. `package.json` is frozen — `expo-notifications`, `expo-splash-screen`, `react-native-safe-area-context` are already installed. |
| 3 | Do NOT touch `/mobile/backend/`, `/api/`, `/data/` or any Java/SQL file. This iteration is frontend-only. |
| 4 | Do NOT change GraphQL queries/mutations. Do NOT change stores beyond what §K.3 prescribes. |
| 5 | Use Spanish for code comments. No emojis in code or commits. |
| 6 | Each phase has a verification step. Do NOT mark a phase done until the verification passes. |
| 7 | After EACH phase, run `npx tsc --noEmit` and `npx eslint .` (or `npm run lint` if it exists). Both must be green. |
| 8 | Save a memory entry after the whole PLAN passes via `mem_save` with `topic_key=mobile/phase-k-ui-polish`. |

---

## 2. PHASE K.1 — Bottom tab bar container (safe-area aware)

### Problem (verbatim from developer)

> "La barra de iconos esta muy pegada a la parte inferior. Agrega un contenedor blanco (tema claro) y oscuro (tema oscuro)."

The current `tabBarStyle` in `app/(tabs)/_layout.tsx` uses a fixed `height: 60` and `paddingBottom: 8`. On Android devices with gesture navigation and iPhone X+ devices with a home indicator, the icons get clipped against the system gesture area because the layout does NOT add the bottom safe-area inset.

### Root cause

`expo-router`'s `Tabs` does not auto-pad for the bottom inset when you override `tabBarStyle.height`. The fix is to:
1. Read the bottom inset via `useSafeAreaInsets()` (the provider is already mounted in `app/_layout.tsx`).
2. Apply `height = 60 + insets.bottom` and `paddingBottom = 8 + insets.bottom`.
3. Make the surrounding container's background colour explicit (`#FFFFFF` light, `#111827` dark — already in `tailwind.config.js` as `surface` / `surface-dark`).

### Files to edit

- `app/(tabs)/_layout.tsx` — single file.

### Exact change

At the top of the file, ADD:

```tsx
import { useSafeAreaInsets } from 'react-native-safe-area-context';
```

Inside `TabsLayout()`, AFTER `const scale = useFontScale();`, ADD:

```tsx
const insets = useSafeAreaInsets();
// En dispositivos con gesture nav (Android) o home indicator (iOS) sumamos el inset
// para que los iconos no queden tapados por la zona de gestos del sistema.
const bottomInset = insets.bottom;
```

Then REPLACE the existing `tabBarStyle` block with:

```tsx
tabBarStyle: {
  backgroundColor: tabBarBg,
  borderTopWidth: 1,
  borderTopColor: tabBarBorder,
  // Altura base 60 + el inset del sistema para no chocar con la zona de gestos.
  height: 60 + bottomInset,
  // Padding inferior identico: deja respiro entre los iconos y el borde inferior.
  paddingBottom: 8 + bottomInset,
  paddingTop: 4,
  // Sombra elevada que enfatiza el contenedor blanco/oscuro de la barra.
  elevation: 8,
  shadowColor: '#000',
  shadowOffset: { width: 0, height: -2 },
  shadowOpacity: isDark ? 0.4 : 0.08,
  shadowRadius: 4,
},
```

Do NOT add a wrapping `<View>` around `<Tabs>` — the tab bar already paints its own background. The `backgroundColor` plus the inset-aware padding IS the "container" the developer asked for.

### Verification

1. Run `npx tsc --noEmit` → 0 errors.
2. Launch Expo Go on a real Android device.
3. In light mode the tab bar is a white block with shadow above it; icons sit comfortably above the gesture pill.
4. Switch system theme to dark — the tab bar becomes `#111827` (deep navy), same vertical breathing room.

### Acceptance criteria

- [ ] No icon is within 16dp of the bottom of the physical screen.
- [ ] No text label is clipped.
- [ ] The tab bar background is `#FFFFFF` in light and `#111827` in dark — verified by inspecting via React DevTools or by visual diff.

---

## 3. PHASE K.2 — Dark-mode text colour audit

### Problem

> "Los textos deberan de ser blancos en el tema oscuro, actualmente son gris oscuro."

`AppText` has NO default colour — it inherits from the parent `<Text>` which inherits from React Native default `#000`. Most screens DO apply `text-text-primary dark:text-text-primary-dark`, but several do not, so in dark mode those texts stay near-black against `#0B1220` background, which is the bug the developer is seeing.

### Strategy

Add a **default colour** to `AppText` itself so that any text without an explicit `text-*` class still gets the right colour in both themes. This is a single-point fix and prevents regressions in future screens.

### Files to edit

- `src/components/AppText.tsx` — single file.
- Optional sweep: any screen that uses raw `<Text>` instead of `<AppText>` (`grep -rn "<Text" app src/components | grep -v "AppText\|TextInput"`). For each hit, swap `<Text>` → `<AppText>` IF and ONLY IF the surrounding context is plain copy (not styled buttons or special widgets).

### Exact change to `AppText.tsx`

REPLACE the component body with:

```tsx
import { Text } from 'react-native';
import type { TextProps } from 'react-native';
import { useFontScale } from '../utils/fontScale';
import { useTheme } from '../utils/theme';

const VARIANT_SIZES = {
  body: 16,
  title: 24,
  subtitle: 18,
  label: 14,
  caption: 12,
};

const WEIGHT_FAMILIES = {
  regular: 'Inter_400Regular',
  medium: 'Inter_500Medium',
  semibold: 'Inter_600SemiBold',
  bold: 'Inter_700Bold',
};

type AppTextProps = TextProps & {
  variant?: keyof typeof VARIANT_SIZES;
  weight?: keyof typeof WEIGHT_FAMILIES;
  className?: string;
  children: React.ReactNode;
};

// Texto de la app con escala global, fuente Inter y color por defecto segun tema.
// Si el caller pasa una className con text-* o un style.color, esos ganan.
export function AppText(props: AppTextProps) {
  const { variant = 'body', weight = 'regular', style, children, ...rest } = props;
  const scale = useFontScale();
  const { scheme } = useTheme();

  const baseFontSize = VARIANT_SIZES[variant];
  const fontFamily = WEIGHT_FAMILIES[weight];

  // Color por defecto: blanco roto en oscuro, gris pizarra en claro.
  // Tailwind colors: text-primary (#1E293B) y text-primary-dark (#F1F5F9).
  const defaultColor = scheme === 'dark' ? '#F1F5F9' : '#1E293B';

  return (
    <Text
      style={[
        { fontFamily, fontSize: baseFontSize * scale, color: defaultColor },
        style,
      ]}
      {...rest}
    >
      {children}
    </Text>
  );
}
```

### Sweep checklist (run greps, fix only the listed surface)

Run:

```bash
cd /home/alaslibres/DAM/RehabiAPP/mobile/frontend
grep -rn "<Text\b" app src --include='*.tsx' | grep -v "AppText\|TextInput\|TextProps"
```

For EVERY hit returned, decide:

- If the `<Text>` is wrapping a label or copy → replace with `<AppText>`.
- If the `<Text>` is inside a third-party component config (e.g., a `TextInput` placeholder) → leave it.

Do NOT mass-replace blindly. Confirm each change individually.

### Verification

1. `npx tsc --noEmit` green.
2. `npx eslint .` green.
3. Open every tab in dark mode (Inicio, Citas, Juegos, Cura, Progreso, Perfil, Ajustes). Every paragraph and label reads near-white.
4. Open the same tabs in light mode — text is still slate (`#1E293B`), not stuck on white.

### Acceptance criteria

- [ ] `AppText` shows `#F1F5F9` text by default in dark mode.
- [ ] `AppText` shows `#1E293B` text by default in light mode.
- [ ] All seven tabs verified by eye in both themes.

---

## 4. PHASE K.3 — Notifications: complete the implementation

### Problem (verbatim)

> "Soluciona y termina de implementar el sistema de notificaciones, este no funciona en absoluto."

### Diagnostic — what already exists (do NOT rewrite)

The plumbing is present:
- `src/utils/notifications.ts` exposes `initNotifications`, `requestPermission`, `getExpoPushToken`, `scheduleAppointmentReminder`, `cancelAppointmentReminder`, `scheduleTestNotification`, `ensureNotificationsEnabled`.
- `app/_layout.tsx` calls `initNotifications()` in a `useEffect`.
- `app/(tabs)/settings.tsx` wires the toggles to `setNotifAppointments`, `setNotifDoctorUpdates`, and the "test" button to `scheduleTestNotification`.
- `src/store/bootstrapStore.ts` registers reminders after login.

### Real causes of the breakage (verified by reading the code)

1. **`app.json` is missing the `expo-notifications` plugin and the Android `notification` icon config.** Without that, the native module never registers the channel on Android, so `scheduleNotificationAsync` silently no-ops in production builds (in Expo Go it half-works but the channel is "default" with no icon → many Android skins suppress the banner).
2. **No Android channel is created.** Android 8+ requires a channel BEFORE any local notification fires.
3. **The handler in `initNotifications()` uses the SDK 53 shape but the trigger in `scheduleTestNotification`/`scheduleAppointmentReminder` mixes old + new keys** — that is fine, BUT it should also pre-create the channel.
4. **The settings handler does not await `Notifications.getPermissionsAsync()` before trying to schedule** in some paths — this hides the real reason a notification didn't fire (permission was actually `denied`).

### Step-by-step fix

#### K.3.a — Update `app.json`

Open `app.json` and ADD inside the `expo.plugins` array:

```json
[
  "expo-notifications",
  {
    "icon": "./assets/RehabiAPPLogoNoLetras.png",
    "color": "#2563EB"
  }
]
```

The final `plugins` array MUST look like:

```json
"plugins": [
  "expo-router",
  "expo-secure-store",
  "expo-font",
  "@react-native-community/datetimepicker",
  [
    "expo-notifications",
    {
      "icon": "./assets/RehabiAPPLogoNoLetras.png",
      "color": "#2563EB"
    }
  ]
]
```

Order matters: leave `expo-router` first. Append the notifications block at the end.

ALSO add inside `expo.android` (next to `adaptiveIcon`):

```json
"useNextNotificationsApi": true
```

(If the SDK version warns this is deprecated, leave it out — it's harmless either way.)

#### K.3.b — Add channel creation to `src/utils/notifications.ts`

In `initNotifications()` REPLACE the body with:

```ts
export function initNotifications() {
  if (!Notifications) return;
  try {
    Notifications.setNotificationHandler({
      handleNotification: async () => ({
        shouldShowAlert: true,
        shouldShowBanner: true,
        shouldShowList: true,
        shouldPlaySound: true,
        shouldSetBadge: false,
      }),
    });

    // En Android los canales son obligatorios desde API 26+. Sin canal,
    // Notifications.scheduleNotificationAsync no muestra el banner.
    if (Platform.OS === 'android') {
      Notifications.setNotificationChannelAsync('default', {
        name: 'Notificaciones',
        importance: Notifications.AndroidImportance.HIGH,
        vibrationPattern: [0, 250, 250, 250],
        lightColor: '#2563EB',
        sound: 'default',
      }).catch(() => {});
    }
  } catch {
    // Ignorar: entorno sin soporte
  }
}
```

In `scheduleAppointmentReminder()` and `scheduleTestNotification()`, ADD `channelId: 'default'` to the `content` object:

```ts
content: {
  title: 'Recordatorio de cita',
  body: `Tienes cita maniana a las ${appointment.time.substring(0, 5)} con ${nombreMedico}.`,
  sound: true,
  // Android: el canal "default" se crea en initNotifications().
  ...(Platform.OS === 'android' ? { channelId: 'default' } : {}),
},
```

And the analogous addition in `scheduleTestNotification`.

#### K.3.c — Wire a foreground-event listener (so the user sees something even with the app open)

In `app/_layout.tsx`, BEFORE `initNotifications();` inside its `useEffect`, ADD:

```ts
let receivedSub: { remove: () => void } | undefined;
try {
  // El import perezoso evita romper Expo Go cuando el modulo no esta presente.
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const N = require('expo-notifications') as typeof import('expo-notifications');
  receivedSub = N.addNotificationReceivedListener(() => {
    // Hook para futuras integraciones (analytics, badge counters, etc.).
  });
} catch {
  // ignorar
}
initNotifications();
```

And in the cleanup function of the same `useEffect`:

```ts
return () => {
  receivedSub?.remove();
};
```

If there is no existing cleanup function in that effect, ADD one.

#### K.3.d — Settings screen: surface the permission state visibly

Open `app/(tabs)/settings.tsx`. Locate the handler `handleToggleAppointments` (or equivalent) and ADD a user-visible toast/log when `requestPermission()` returns false:

```ts
if (!granted) {
  useErrorStore.getState().showError(
    'Permiso de notificaciones denegado',
    'Activa las notificaciones de RehabiAPP en los ajustes del sistema.'
  );
  await setNotifAppointments(false);
  return;
}
```

Import `useErrorStore` at the top if missing.

#### K.3.e — Test scheduling: lower the delay to 3 seconds and reuse the channel

In `scheduleTestNotification()` change `seconds: 5` to `seconds: 3`. Reason: 5s feels broken on slow devices because the developer expects an immediate response.

### Files touched in this phase

- `app.json`
- `src/utils/notifications.ts`
- `app/_layout.tsx`
- `app/(tabs)/settings.tsx`

### Verification

1. `npx tsc --noEmit` green.
2. Fresh install (delete app from device first) → relaunch via Expo Go.
3. Go to Ajustes → tap "Notificaciones de citas" → accept the OS permission dialog. The toggle stays ON.
4. Tap "Enviar notificacion de prueba" → within 3 seconds a banner reads "Las notificaciones funcionan correctamente."
5. Schedule a test appointment for tomorrow and verify (via `console.log` in the appointments store) that `scheduleAppointmentReminder` was called with the right date.

### Acceptance criteria

- [ ] Permission dialog appears the first time the toggle is enabled.
- [ ] Toast/error popup appears if permission is denied.
- [ ] Test banner fires in < 5 seconds on a real Android device.
- [ ] Cancelling an appointment cancels its reminder (verified by deleting then re-enabling notifications: no orphan banner fires).

---

## 5. PHASE K.4 — Phone number → 628 67 85 90

### Problem (verbatim)

> "Cambia el numero de telefono de contacto a 628 67 85 90 tanto el de WhatsApp como el de contacto."

### Current values (verified by grep)

- `src/components/HospitalContactCard.tsx` line 8: `const HOSPITAL_PHONE = '+34 628 67 88 88';`
- `src/components/WhatsAppButton.tsx` line 8: `const WHATSAPP_PHONE = '34628678888';`

### Exact edits

#### `src/components/HospitalContactCard.tsx`

REPLACE:

```ts
const HOSPITAL_PHONE = '+34 628 67 88 88';
```

WITH:

```ts
const HOSPITAL_PHONE = '+34 628 67 85 90';
```

#### `src/components/WhatsAppButton.tsx`

REPLACE:

```ts
const WHATSAPP_PHONE = '34628678888';
```

WITH:

```ts
const WHATSAPP_PHONE = '34628678590';
```

### DO NOT

- Do not extract these into env vars in this iteration. The developer wants a literal swap, not a refactor.
- Do not edit any other phone constants you may find (search anyway to confirm no third copy exists):

```bash
grep -rn "628 67\|62867" app src --include='*.tsx' --include='*.ts'
```

If a third copy exists in the codebase, update it to `+34 628 67 85 90` / `34628678590` following the same display/E.164 convention. Otherwise leave the rest of the file untouched.

### Verification

1. `npx tsc --noEmit` green.
2. Launch the app → tab "Citas" → "Pedir cita nueva" card shows `+34 628 67 85 90`.
3. Tap the WhatsApp button → the WhatsApp app opens a chat with `+34 628 67 85 90` (or the web fallback at `https://wa.me/34628678590`).

### Acceptance criteria

- [ ] Both files updated to the new number.
- [ ] The displayed phone in the contact card is exactly `+34 628 67 85 90`.
- [ ] The WhatsApp deep link points to `34628678590` (E.164 without `+`).

---

## 6. PHASE K.5 — Splash screen with logo + app name

### Problem (verbatim)

> "Agrega un splash screen con el logotipo de la app. El nombre que aparezca no sea 'frontend' sino 'RehabiAPP'."

### Current state (verified)

- `app.json` `expo.name = "fronted"` — typo + wrong product name. Must become `RehabiAPP`.
- `app.json` `expo.slug = "fronted"` — the slug is what shows in Expo Go's project picker. Change to `rehabiapp`.
- `app.json` `expo.splash.image = "./assets/splash.png"` — the existing file is fine for a fallback, but the developer wants the **logo** centered. Use `./assets/rehabiapp-logo.png` (the one WITH letters) so the brand name reads correctly.
- `expo-splash-screen` is installed transitively (Expo SDK 54 includes it). NO new package needed.

### Exact edits

#### K.5.a — Update `app.json`

In `expo.name`:

```diff
- "name": "fronted",
+ "name": "RehabiAPP",
```

In `expo.slug`:

```diff
- "slug": "fronted",
+ "slug": "rehabiapp",
```

In `expo.scheme`:

```diff
- "scheme": "fronted",
+ "scheme": "rehabiapp",
```

In `expo.splash`:

```diff
- "splash": {
-   "image": "./assets/splash.png",
-   "resizeMode": "contain",
-   "backgroundColor": "#ffffff"
- },
+ "splash": {
+   "image": "./assets/rehabiapp-logo.png",
+   "resizeMode": "contain",
+   "backgroundColor": "#F0F4FF",
+   "dark": {
+     "image": "./assets/rehabiapp-logo.png",
+     "backgroundColor": "#0B1220"
+   }
+ },
```

The `#F0F4FF` / `#0B1220` colours match `background` / `background-dark` in `tailwind.config.js`, so the splash visually continues into the app shell.

#### K.5.b — Add Android adaptive icon + iOS icon consistency

Inside `expo.android`:

```diff
- "adaptiveIcon": {
-   "foregroundImage": "./assets/adaptive-icon.png",
-   "backgroundColor": "#ffffff"
- }
+ "adaptiveIcon": {
+   "foregroundImage": "./assets/adaptive-icon.png",
+   "backgroundColor": "#F0F4FF"
+ },
+ "package": "com.rehabiapp.mobile"
```

ADD inside `expo.ios`:

```diff
- "supportsTablet": true
+ "supportsTablet": true,
+ "bundleIdentifier": "com.rehabiapp.mobile"
```

These IDs make the app installable under the right name on a clean device. Without them, Expo prebuild generates package names from the slug (which we just renamed) and previously installed dev builds would not be replaced cleanly.

#### K.5.c — Programmatically hide the splash AFTER bootstrap (not before)

The splash currently hides on first React paint, which means the user sees a flicker between the splash and the empty home tab while the bootstrap store fetches data. Hold the splash until `bootstrapStore.isHydrated` is true.

Edit `app/_layout.tsx`:

ADD imports at the top:

```ts
import * as SplashScreen from 'expo-splash-screen';
```

IMMEDIATELY AFTER the imports (top-level, NOT inside the component):

```ts
// Mantener el splash visible hasta que terminen las fuentes y el bootstrap.
// Si el preventAutoHideAsync falla (entorno no soportado) no rompemos el arranque.
SplashScreen.preventAutoHideAsync().catch(() => {});
```

Inside the root layout component, locate where the app decides it is ready (fonts loaded + auth resolved). Add an effect:

```ts
const isBootstrapped = useBootstrapStore(s => s.isHydrated);
// O la flag equivalente — si no existe, usar (fontsLoaded && !authLoading).

useEffect(() => {
  if (fontsLoaded && isBootstrapped) {
    SplashScreen.hideAsync().catch(() => {});
  }
}, [fontsLoaded, isBootstrapped]);
```

If `bootstrapStore` does NOT expose `isHydrated`, fall back to hiding right after fonts load AND auth check completes — i.e., use the existing conditions in `app/_layout.tsx` that decide when to render the `<Slot />`.

#### K.5.d — DO NOT touch the asset files

Do not regenerate the icons or recompress the PNGs. Use the existing `rehabiapp-logo.png` as-is.

### Verification

1. `npx tsc --noEmit` green.
2. Stop Metro completely, uninstall the dev build / clear Expo Go cache, relaunch with `npx expo start -c`.
3. The Expo Go project list shows `RehabiAPP` (not `fronted`).
4. On cold start: a splash with the RehabiAPP logo appears, stays during font load + bootstrap, and fades to the login screen WITHOUT a white flash in between.
5. Long-press the app icon on the home screen of a stand-alone build → the title reads `RehabiAPP`.

### Acceptance criteria

- [ ] `app.json` `name` / `slug` / `scheme` all updated.
- [ ] Splash logo is the `rehabiapp-logo.png` asset.
- [ ] No white flicker between splash and first screen.
- [ ] Long-press title says `RehabiAPP`.

---

## 7. PHASE K.6 — Final smoke test (run BEFORE marking any item done)

Execute IN ORDER:

```bash
cd /home/alaslibres/DAM/RehabiAPP/mobile/frontend

# 1. Type check
npx tsc --noEmit

# 2. Lint
npx eslint .

# 3. Start with cache clear (so app.json changes take effect)
npx expo start -c
```

Then on a physical Android device with Expo Go:

| Step | Expected outcome |
|------|------------------|
| Cold launch | Splash with RehabiAPP logo, then login screen. No flicker. |
| Login with `admin / admin` (MOCK_API=true) | Home tab renders in < 3s. |
| Toggle every tab | Bottom bar shows clear container, icons NOT clipped, labels readable. |
| Switch system theme to dark | Body text near-white everywhere; tab bar becomes `#111827`. |
| Go to Ajustes → activate "Notificaciones de citas" | OS permission dialog appears. |
| Tap "Enviar notificacion de prueba" | Banner fires within 5s. |
| Go to Citas → "Pedir cita nueva" | Phone reads `+34 628 67 85 90`. |
| Tap "Abrir WhatsApp" | WhatsApp opens chat with `34628678590`. |
| Background the app, foreground it | No crash. State preserved. |

Every row must pass. If a row fails, fix the cause in its origin phase and re-run the FULL smoke test.

---

## 8. OUT OF SCOPE (do NOT do these)

- Rewriting the tab bar with a custom component. The Expo Router default is fine; only the style changes.
- Adding a dark/light toggle in Ajustes (already exists).
- Refactoring the notifications utility into a class/service.
- Adding remote push registration with the BFF — that requires a backend endpoint that is OUT of this iteration.
- Extracting phone numbers into i18n or env config.
- Replacing the splash screen image (any redesign needs a designer review).

---

## 9. CHECKLIST (copy into the iteration commit message)

```
- [ ] K.1 Bottom tab bar: safe-area inset applied, contrasting container background light/dark
- [ ] K.2 AppText default color: white in dark, slate in light
- [ ] K.3 Notifications: plugin + channel + permission UX + test banner under 5s
- [ ] K.4 Phone number: contact card + WhatsApp updated to +34 628 67 85 90 / 34628678590
- [ ] K.5 Splash: rehabiapp-logo.png + name "RehabiAPP" + dark variant + bootstrap-aware hide
- [ ] K.6 Full smoke test green on physical Android (Expo Go)
- [ ] `npx tsc --noEmit` green
- [ ] `npx eslint .` green
- [ ] mem_save called with topic_key=mobile/phase-k-ui-polish
```

End of plan.
