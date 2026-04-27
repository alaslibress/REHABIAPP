# Plan 01 — Citas (Appointments)

Depends on: Plan 00 (shared infra must be done first).

---

## 1. Purpose / UX

Single scrollable screen, two sections inside floating cards:

1. **Próximas citas** — list of upcoming appointments (status `SCHEDULED` and date >= today). Each item: date (formatted `DD MMM YYYY`), time (`HH:mm`), practitioner name + specialty, floating "Cancelar" button (outline, red). Empty → `<EmptyState icon="calendar-outline" title="No tienes citas próximas." />`.
2. **Pedir cita nueva** — form card with:
   - Teléfono (TextInput prefilled from profile `patient.phone`, editable, keyboardType=phone-pad).
   - Email (TextInput prefilled from `patient.email`, editable, keyboardType=email-address).
   - Fecha preferida (DateTimePicker — use `@react-native-community/datetimepicker` already present or add via `expo install`; open on press of a pressable field).
   - Hora preferida (same picker, time mode).
   - Motivo (TextInput multiline, 4 lines).
   - "Enviar solicitud" primary button.
   - Divider.
   - "Abrir WhatsApp" secondary pill button with `logo-whatsapp` icon + text. Stub: `onPress={() => { /* TODO: Linking.openURL with wa.me */ }}`. User explicitly said NOT to implement WhatsApp functionality yet.

Header title: `Citas`.

---

## 2. Modals / popups

- **ConfirmModal** on Cancelar cita → title "¿Cancelar cita?", message "Esta acción no se puede deshacer.", destructive=true.
- **ConfirmModal** on Enviar solicitud success → title "Solicitud enviada", message "Tu médico revisará la petición y recibirás una notificación cuando sea confirmada.", single button "Aceptar" (reuse ConfirmModal with cancelLabel hidden — add prop `singleAction?: boolean`).
- **ErrorPopup** via `errorStore`:
  - `APPOINTMENT_NOT_FOUND` on cancel of missing cita.
  - `APPOINTMENT_REQUEST_INVALID_CONTACT` if phone and email both empty after trim.
  - `APPOINTMENT_REQUEST_CONFLICT` if BFF returns conflict for same slot.
  - `NETWORK_ERROR` on offline.
  - `TOKEN_EXPIRED` → auto-logout via authStore.

---

## 3. Components to add

`src/components/AppointmentCard.tsx`:
- Props: `{ appointment: Appointment; onCancel: (id: string) => void; }`
- Layout: left column icon `calendar-outline`, middle column date+time+practitioner, right Cancelar pill.
- Wraps in `FloatingCard`.

`src/components/AppointmentRequestForm.tsx`:
- Internal state for the 5 fields.
- Validation: phone OR email required; fecha >= today; motivo >= 10 chars.
- Calls `appointmentsStore.requestAppointment(...)`.
- After success, resets fields and triggers the success ConfirmModal.

`src/components/WhatsAppButton.tsx`:
- Pressable with icon + "Abrir WhatsApp" text. `onPress` is a no-op stub with `// TODO: abrir WhatsApp` comment.

`src/components/DatePickerField.tsx` — thin wrapper around DateTimePicker (pressable field showing formatted value, opens picker). Shared with plan 02 if needed.

---

## 4. Zustand slice — `src/store/appointmentsStore.ts`

```ts
type AppointmentsState = {
  items: Appointment[];
  loading: boolean;
  hydrated: boolean;
  fetch: () => Promise<void>;
  requestAppointment: (input: RequestAppointmentInput) => Promise<AppointmentRequest>;
  cancel: (id: string) => Promise<void>;
  reset: () => void;
};
```

- `fetch()`: calls `GET_MY_APPOINTMENTS({ upcoming: true })`, writes `items`, sets `hydrated=true`. Triggers `scheduleAppointmentReminder` for each upcoming (via `src/utils/notifications.ts`).
- `requestAppointment(input)`: calls `REQUEST_APPOINTMENT_MUTATION`. On success optionally refetches.
- `cancel(id)`: calls `CANCEL_APPOINTMENT`, removes from items, cancels local notification `appt-{id}`.
- `reset()`: clears and cancels all scheduled appointment reminders.

All errors go through `parseGraphQLError` + `errorStore.showError`.

---

## 5. Apollo queries / mutations (frontend)

Extend `src/services/graphql/queries/appointments.ts`: existing `GET_MY_APPOINTMENTS` already present.

Extend `src/services/graphql/mutations/appointments.ts`:

```ts
export const CANCEL_APPOINTMENT = gql`...`; // exists

export const REQUEST_APPOINTMENT = gql`
  mutation RequestAppointment(
    $fechaPreferida: String!
    $horaPreferida: String!
    $motivo: String!
    $telefono: String
    $email: String
  ) {
    requestAppointment(
      fechaPreferida: $fechaPreferida
      horaPreferida: $horaPreferida
      motivo: $motivo
      telefono: $telefono
      email: $email
    ) {
      id
      fechaPreferida
      horaPreferida
      motivo
      estado
      createdAt
    }
  }
`;
```

Add type `src/types/appointments.ts`:
```ts
export type RequestAppointmentInput = {
  fechaPreferida: string;  // YYYY-MM-DD
  horaPreferida: string;   // HH:mm
  motivo: string;
  telefono?: string | null;
  email?: string | null;
};
export type AppointmentRequest = {
  id: string;
  fechaPreferida: string;
  horaPreferida: string;
  motivo: string;
  estado: 'PENDING' | 'CONFIRMED' | 'REJECTED';
  createdAt: string;
};
```

---

## 6. BFF — typeDefs

`src/graphql/typeDefs/appointment.js` — extend:

```graphql
enum AppointmentRequestStatus { PENDING CONFIRMED REJECTED }

type AppointmentRequest {
  id: ID!
  fechaPreferida: String!
  horaPreferida: String!
  motivo: String!
  estado: AppointmentRequestStatus!
  createdAt: String!
}

extend type Mutation {
  requestAppointment(
    fechaPreferida: String!
    horaPreferida: String!
    motivo: String!
    telefono: String
    email: String
  ): AppointmentRequest!
}
```

---

## 7. BFF — resolvers

`src/graphql/resolvers/appointment.js` — add `requestAppointment`:
- `requireAuth(context)`.
- Validate `telefono || email` (throw `APPOINTMENT_REQUEST_INVALID_CONTACT` else).
- Validate date format (YYYY-MM-DD), time format (HH:mm), fecha >= today, motivo length >= 10.
- Call `appointmentService.solicitarCita(dniPac, input, javaToken)`.

---

## 8. BFF — service

`src/services/appointmentService.js` — add `solicitarCita(dniPac, input, javaToken)`:
- POST `/api/pacientes/{dniPac}/solicitudes-cita` with body `{ fechaPreferida, horaPreferida, motivo, telefono, email }`.
- Map Java response → `AppointmentRequest`.
- On 409 conflict → throw `APPOINTMENT_REQUEST_CONFLICT`.

**Mock** in `src/services/apiClient.js` — when path matches `/api/pacientes/*/solicitudes-cita`, return:
```js
{ id: `req-${Date.now()}`, fechaPreferida, horaPreferida, motivo, estado: 'PENDING', createdAt: new Date().toISOString() }
```

---

## 9. Java API contract (for Agent 1 to implement later)

- `GET /api/pacientes/{dniPac}/citas` — already exists (returns cita[]). Extend response to include `estado: SCHEDULED|COMPLETED|CANCELLED|NO_SHOW` and `notas`.
- `POST /api/pacientes/{dniPac}/solicitudes-cita` — new. Body: `{ fechaPreferida, horaPreferida, motivo, telefono, email }`. Response: `{ id, fechaPreferida, horaPreferida, motivo, estado, createdAt }`.
- `DELETE /api/citas?dniPac&dniSan&fecha&hora` — already exists.

---

## 10. DB gaps (flag to Agent 1)

Required tables/columns missing:

- **`cita.estado`** column: `VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'`, check constraint `SCHEDULED|COMPLETED|CANCELLED|NO_SHOW`.
- **`solicitud_cita`** table — new:
  - `id_solicitud BIGSERIAL PK`
  - `dni_pac VARCHAR(20) NOT NULL FK paciente CASCADE`
  - `fecha_preferida DATE NOT NULL`
  - `hora_preferida TIME NOT NULL`
  - `motivo TEXT NOT NULL`
  - `telefono VARCHAR(20)`
  - `email VARCHAR(200)`
  - `estado VARCHAR(20) NOT NULL DEFAULT 'PENDING'` (check: PENDING|CONFIRMED|REJECTED)
  - `id_cita_creada` — optional composite FK to `cita` when estado=CONFIRMED
  - `created_at TIMESTAMP NOT NULL DEFAULT NOW()`
  - `updated_at TIMESTAMP`
  - Index: `(dni_pac, estado)`.

Audit table `solicitud_cita_audit` via Envers.

---

## 11. Error codes used

`APPOINTMENT_NOT_FOUND`, `APPOINTMENT_REQUEST_INVALID_CONTACT`, `APPOINTMENT_REQUEST_CONFLICT`, `NETWORK_ERROR`, `TOKEN_EXPIRED`, `VALIDATION_ERROR`.

---

## 12. Edge cases

- Offline → cards show cached data from bootstrap; pull-to-refresh shows `NETWORK_ERROR`.
- Profile has no phone/email → form shows both empty, Enviar triggers `APPOINTMENT_REQUEST_INVALID_CONTACT`.
- Date in the past → local validation, inline red helper text "La fecha debe ser futura.".
- Motivo < 10 chars → inline helper "Describe el motivo (mínimo 10 caracteres).".
- Cancel last appointment → list shows empty state.
- Token expires mid-request → BFF returns `TOKEN_EXPIRED`, authStore logs out.

---

## 13. Files touched

Frontend:
- `app/(tabs)/appointments.tsx` — full rewrite.
- `src/components/AppointmentCard.tsx`, `AppointmentRequestForm.tsx`, `WhatsAppButton.tsx`, `DatePickerField.tsx` — new.
- `src/store/appointmentsStore.ts` — new.
- `src/services/graphql/mutations/appointments.ts` — add REQUEST_APPOINTMENT.
- `src/types/appointments.ts` — extend.
- `src/components/ConfirmModal.tsx` — add `singleAction?` prop.

Backend:
- `src/graphql/typeDefs/appointment.js` — extend.
- `src/graphql/resolvers/appointment.js` — extend.
- `src/services/appointmentService.js` — extend.
- `src/services/apiClient.js` — mock response.
- `src/utils/errors.js` — add codes.
- `test/graphql.test.js` — add test for `requestAppointment`.

---

## 14. Acceptance

1. Login with `12345678Z`/`admin` → Citas tab shows mocked upcoming cita.
2. Cancel → ConfirmModal → list shrinks, local notification cancelled.
3. Submit request form with missing phone+email → error popup.
4. Valid submit → success modal, form resets.
5. Pull-to-refresh → list refetches.
6. Dark mode renders properly.
7. BFF `npm test` green with new `requestAppointment` test.
