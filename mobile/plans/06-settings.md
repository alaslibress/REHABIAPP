# Plan 06 — Configuración (Settings)

Depends on: Plan 00.

---

## 1. Purpose / UX

Scrollable screen with 4 floating section cards:

### Apariencia
- **Tema**: SegmentedControl 3 options: Claro / Oscuro / Sistema. Value from `settingsStore.themeMode`. Change → persist + propagate via `ThemeContext` → whole app re-renders.
- **Tamaño de texto**: SegmentedControl 4 options: A- / A / A+ / A++ (values 0.9, 1.0, 1.15, 1.3). Change → persist + propagate via `FontScaleContext`.

### Notificaciones
- **Recordatorios de citas** — Switch. On enable: call `requestPermission()`. If granted → `settingsStore.setNotifAppointments(true)` + schedule all upcoming appointment reminders. If denied → popup `NOTIFICATION_PERMISSION_DENIED` and keep switch off.
- **Actualizaciones del médico** — Switch. On enable with no prior permission: same permission flow. On enable: call `registerPushToken()` + BFF `registerDeviceToken` mutation. On disable: BFF `unregisterDeviceToken`.
- Inline caption below switches: "Recibirás un aviso el día antes de cada cita y cuando tu médico actualice tu tratamiento."

### Probar notificación (extra, user-requested)
- Primary button "Enviar notificación de prueba". Calls `scheduleTestNotification()` → local notif fires in 5s. Shows success toast (inline text "Notificación programada para 5 s").
- Disabled if permission not granted → helper text "Activa primero las notificaciones".

### Acerca de
- App version: `expo-application` `Application.nativeApplicationVersion` + build number.
- "Política de privacidad" pressable row (stub — logs TODO).
- "Términos y condiciones" pressable row (stub).

Header title: `Configuración`.

---

## 2. Modals / popups

- **ErrorPopup** for `NOTIFICATION_PERMISSION_DENIED`.
- **ConfirmModal** on "Desactivar recordatorios" when enabling→disabling? → Skip, just toggle silently.
- No other modals.

---

## 3. Components to add

`src/components/SettingsSection.tsx`:
- Props: `{ title: string; children; }`. FloatingCard with section title + divider.

`src/components/SettingsRow.tsx`:
- Props: `{ label: string; description?: string; right: React.ReactNode; onPress?; }`.

`src/components/ToggleRow.tsx`:
- Props: `{ label; description?; value; onChange; disabled?; }`. Wraps SettingsRow with `<Switch>`.

Reuse `SegmentedControl` from shared infra.

---

## 4. Zustand — `settingsStore`

Already defined in Plan 00. Nothing new. Selectors used directly from the screen.

---

## 5. Screen behavior

`app/(tabs)/settings.tsx`:
1. Reads `settingsStore` (subscribed).
2. Reads `Application.nativeApplicationVersion` + `Application.nativeBuildVersion`.
3. Reads permission status once on mount via `Notifications.getPermissionsAsync()` → keeps local `permissionStatus` state.
4. On toggle enable → run permission flow.
5. On push-enable → call `registerPushToken()` (shared util) → GraphQL mutation.

---

## 6. Apollo — mutations

Already in Plan 00: `REGISTER_DEVICE_TOKEN`, `UNREGISTER_DEVICE_TOKEN` (add the unregister variant in shared-infra if missed).

Add in `src/services/graphql/mutations/settings.ts` (if not already):
```ts
export const REGISTER_DEVICE_TOKEN = gql`
  mutation RegisterDeviceToken($token: String!, $platform: String!) {
    registerDeviceToken(token: $token, platform: $platform)
  }
`;
export const UNREGISTER_DEVICE_TOKEN = gql`
  mutation UnregisterDeviceToken($token: String!) {
    unregisterDeviceToken(token: $token)
  }
`;
```

---

## 7. BFF

Already covered in Plan 00. No new resolvers here.

---

## 8. Java API contract (Agent 1)

- `POST /api/pacientes/{dniPac}/device-tokens` — body `{ token, platform }`. Upserts into `token_dispositivo` table. Returns `{ ok: true }`.
- `DELETE /api/pacientes/{dniPac}/device-tokens?token=X` — soft-delete (set activo=false).
- Future: Java emits Expo push notifications when practitioner creates appointment or updates treatment → pulls active tokens from this table → POST to `https://exp.host/--/api/v2/push/send`.

---

## 9. DB gaps (Agent 1)

- **`token_dispositivo`** — new:
  - `id_token BIGSERIAL PK`
  - `dni_pac VARCHAR(20) FK paciente CASCADE`
  - `token TEXT NOT NULL` (Expo push token, starts with `ExponentPushToken[...]`)
  - `platform VARCHAR(10) NOT NULL` (ANDROID|IOS|WEB)
  - `activo BOOLEAN NOT NULL DEFAULT TRUE`
  - `fecha_registro TIMESTAMP NOT NULL DEFAULT NOW()`
  - `fecha_ultimo_uso TIMESTAMP`
  - Unique `(dni_pac, token)`.

Optional future: **`configuracion_app_usuario`** for server-side pref mirror. Not required for MVP.

Optional: **`notificacion`** log table for audit of sent notifications. Not required for MVP (Expo handles delivery).

---

## 10. Error codes

`NOTIFICATION_PERMISSION_DENIED`, `NETWORK_ERROR`, `INTERNAL_ERROR`, `TOKEN_EXPIRED`.

---

## 11. Edge cases

- Permission revoked from system settings while app open → on next enable attempt, request again.
- Push token refresh (device rotates) → re-register on app bootstrap via `registerPushToken()` even if already-enabled.
- `Application` returns null version on web → fallback "desarrollo".
- Dark mode toggle with mode=system then system flips → observed via `Appearance.addChangeListener` in `ThemeProvider`.
- Font scale applied to the entire app including header (via React Navigation options reading from context — doer: set `headerTitleStyle.fontSize = 18 * scale`).

---

## 12. Files touched

Frontend:
- `app/(tabs)/settings.tsx` — full rewrite.
- `src/components/SettingsSection.tsx`, `SettingsRow.tsx`, `ToggleRow.tsx` — new.
- `src/services/graphql/mutations/settings.ts` — ensure REGISTER/UNREGISTER both exported.

Backend:
- `src/graphql/typeDefs/settings.js` — add `unregisterDeviceToken` (if not in Plan 00).
- `src/graphql/resolvers/settings.js` — unregister resolver.
- `src/services/notificationService.js` — unregister function.
- `src/services/apiClient.js` — mock DELETE path.

---

## 13. Acceptance

1. Apariencia: toggle Claro/Oscuro/Sistema — entire app switches instantly and persists after app restart.
2. Tamaño de texto: all screens scale correspondingly and persist.
3. Toggle appt reminders enable → permission prompt → if granted, upcoming appts get local notifications scheduled (verify with `getAllScheduledNotificationsAsync`).
4. Deny permission → error popup + switch stays off.
5. Toggle doctor updates enable → BFF receives `registerDeviceToken` mutation call (verify in logs/mock).
6. Test notification button → local notif fires in 5s.
7. App version shown.
8. BFF tests pass (register + unregister).
9. Dark mode styled cleanly.
