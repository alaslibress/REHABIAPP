# Plan 05 — Perfil (Profile)

Depends on: Plan 00.

---

## 1. Purpose / UX

Single scrollable screen. All data sourced from already-hydrated `userStore` + disabilities list — no new GraphQL. Layout top-to-bottom:

1. **Avatar card** (floating, centered):
   - Circular avatar 120dp. Source: image from API `/api/pacientes/{dni}/foto` — fetched as base64 via a helper (see §5) or directly using `<Image source={{ uri: avatarUri }} />` where `avatarUri` is built from the BFF (see §6).
   - Placeholder: `person-circle-outline` icon if no photo.
   - Below avatar: full name (`${name} ${surname1} ${surname2}`), AppText variant=title weight=bold.
   - Small row below: age + sex badge pills.
2. **Datos personales card** (floating):
   - Rows: DNI (masked), Nº Seguridad Social (masked `1234 56 ****` — last 4 shown), Fecha de nacimiento, Dirección, Teléfono, Email.
   - Each row: label (secondary) left, value (primary) right.
3. **Patología / Discapacidades card**:
   - For each disability: pill with name + current level.
   - If none → text "Sin patologías registradas."
4. **Cerrar sesión button** (outline red, full width, bottom of scroll): `log-out-outline` icon + "Cerrar sesión". Height 56dp.

Header title: `Perfil`.

---

## 2. Modals / popups

- **ConfirmModal** on Cerrar sesión → "¿Cerrar sesión?", message "Tendrás que volver a iniciar sesión para acceder.", destructive=true. Confirm → `authStore.logout()`.
- **ErrorPopup** only in unlikely case of fetch fail (data should already be hydrated).

---

## 3. Components to add

`src/components/ProfileHeader.tsx`:
- Props: `{ patient: Patient; avatarUri: string | null; }`.

`src/components/InfoRow.tsx`:
- Props: `{ label: string; value: string | null; icon?: IoniconName; }`.
- Handles null by rendering `—`.

`src/components/PathologyPill.tsx`:
- Props: `{ name: string; level: number; }`. Primary-100 bg.

`src/components/LogoutButton.tsx`:
- Props: `{ onPress: () => void; }`. Outline red.

---

## 4. Zustand — no new slice

Reuse `userStore` (already has `patient` + `fetchProfile`). Add `disabilities: Disability[]` + `fetchDisabilities()` if not already there, or keep disabilities in `treatmentsStore`. Decision: add disabilities to `userStore` (single source for profile data).

Update `src/store/userStore.ts`:
- Add `disabilities: Disability[]`.
- Extend `fetchProfile()` to also `GET_MY_DISABILITIES` in parallel.

---

## 5. Avatar fetch

Two options — pick the simpler:

**Option A (recommended)**: BFF returns a data URI in `Patient.avatarDataUri` (base64). Single round-trip, Apollo caches.

**Option B**: Direct REST call to `/api/pacientes/{dni}/foto` — needs Java token in header, mobile doesn't have it.

→ Go with **Option A**. Extend BFF to inline avatar base64 when `me` is queried.

### BFF changes for avatar
- `src/graphql/typeDefs/patient.js` — add `avatarDataUri: String` to `Patient` type.
- `src/services/patientService.js` — in `obtenerPerfil`, additionally call `GET /api/pacientes/{dniPac}/foto` via apiClient (handle 204 → null), base64-encode the BYTEA response, return `data:image/png;base64,{b64}`. Size cap: 512 KB (reject larger → null).
- Mock: return a small pre-built base64 string for `12345678Z`.

---

## 6. Apollo query

Extend `src/services/graphql/queries/user.ts`:
```ts
export const GET_MY_PROFILE = gql`
  query GetMyProfile {
    me {
      id name surname email dni phone birthDate address
      active numSs sexo avatarDataUri
    }
  }
`;
```

(Add `numSs`, `sexo`, `avatarDataUri` to `Patient` type in BFF + TS type.)

Frontend `src/types/user.ts`:
```ts
export type Patient = {
  id: string;
  name: string;
  surname: string;
  email: string | null;
  dni: string;
  phone: string | null;
  birthDate: string | null;
  address: string | null;
  active: boolean;
  numSs: string | null;
  sexo: 'MASCULINO' | 'FEMENINO' | 'OTRO' | null;
  avatarDataUri: string | null;
};
```

---

## 7. Java API contract (already exists)

- `GET /api/pacientes/{dni}` — already exists. Ensure response includes `numSs`, `sexo`, surname split (`apellido1_pac`, `apellido2_pac`).
- `GET /api/pacientes/{dni}/foto` — already exists.

BFF consumes these — no new Java endpoints.

---

## 8. DB gaps

None. All fields exist in `paciente`.

---

## 9. Error codes

`PATIENT_NOT_FOUND`, `NETWORK_ERROR`, `TOKEN_EXPIRED`.

---

## 10. Edge cases

- Missing photo → placeholder icon.
- Missing phone/email/address → row shows `—`.
- DNI format `12345678X` → mask middle 4: `1234****X`.
- Num SS format may vary (12 digits) → mask middle: `12 3 ** ****** **`. If null → `—`.
- Logout while offline → still clears local state, navigates to login.

---

## 11. Files touched

Frontend:
- `app/(tabs)/profile.tsx` — full rewrite.
- `src/components/ProfileHeader.tsx`, `InfoRow.tsx`, `PathologyPill.tsx`, `LogoutButton.tsx` — new.
- `src/store/userStore.ts` — extend with disabilities.
- `src/services/graphql/queries/user.ts` — extend profile fields.
- `src/types/user.ts` — extend.
- `src/utils/mask.ts` — new helper with `maskDni`, `maskSsn`.

Backend:
- `src/graphql/typeDefs/patient.js` — add `avatarDataUri`, `numSs`, `sexo` fields.
- `src/services/patientService.js` — enrich `obtenerPerfil` with foto call.
- `src/services/apiClient.js` — mock foto (tiny base64 PNG).
- `test/graphql.test.js` — ensure `me` includes new fields.

---

## 12. Acceptance

1. Perfil tab shows avatar (mocked) + full name + badges.
2. All personal rows shown, null fields show `—`.
3. DNI and SSN masked correctly.
4. Disabilities list shows pills; empty → "Sin patologías registradas.".
5. Cerrar sesión button → ConfirmModal → logout → back to login.
6. Dark mode renders correctly.
7. BFF tests pass with new fields in `me` response.
