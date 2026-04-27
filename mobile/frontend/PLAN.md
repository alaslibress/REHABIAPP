# PLAN.md — Mobile Frontend: Phase 4 closure (past appointments)

> **Agent:** Sonnet (Doer) under Agent 2 (Mobile)
> **Domain:** `/mobile/frontend/`
> **Prerequisites:** Read `/CLAUDE.md`, `/mobile/CLAUDE.md`, `app/(tabs)/appointments.tsx`, `src/store/appointmentsStore.ts`, `src/services/graphql/queries/appointments.ts`.
> **Scope:** Close the only remaining Phase-4 item in `mobile/CLAUDE.md`: render past appointments alongside upcoming ones. The other four Phase-4 items (profile, disabilities, treatment list, progress charts) are already implemented.

---

## Status summary

| # | Phase 4 item | Status | Evidence |
|---|---|---|---|
| 1 | Patient profile screen | **DONE** | `profile.tsx` + `GET_MY_PROFILE` + `userStore.patient` + `InfoRow` rows |
| 2 | Assigned disabilities w/ current progression level | **DONE** | `GET_MY_DISABILITIES` returns `currentLevel`; rendered in profile + `treatments.tsx` |
| 3 | Treatment list filtered by disability and level | **DONE** | `treatments.tsx` groups by `disabilityCode` + `DisabilitySection` per disability |
| 4 | Game session history with progress charts | **DONE** | `progress.tsx` + `BodyDiagram` + `ProgressChartModal` (LineChart) |
| 5 | Appointment list — upcoming **and past** | **PARTIAL** | Only upcoming is fetched (`fetch` calls with `upcoming:true`); UI only renders one section. → Steps below |

Only the past-appointments work needs implementation. Do NOT refactor the four DONE items.

---

## Step 0 — Global rules for the Doer

- Edit only the files listed below. No reformatting elsewhere.
- Do NOT add, remove, or upgrade dependencies. Do NOT run `npm install`.
- Do NOT touch `/mobile/backend`.
- Spanish labels follow existing convention (e.g. `Politica`, `Tamano`, no diacritics on new strings).
- Do NOT modify the `RequestAppointment` mutation flow or the `HospitalContactCard`.
- Manual UX verification in Expo Go after the change. No TestSprite for this UI delta.

---

## Step 1 — Extend `appointmentsStore` with past list

### 1.1 File — `src/store/appointmentsStore.ts`

#### State delta

In `AppointmentsState`, add the following members (do NOT remove anything):

```ts
pastItems: Appointment[];
loadingPast: boolean;
hydratedPast: boolean;
fetchPast: () => Promise<void>;
```

In the initial state object inside the store factory, set:

```ts
pastItems: [],
loadingPast: false,
hydratedPast: false,
```

#### New action `fetchPast`

Add a method that mirrors `fetch` but with `upcoming: false`. It does NOT schedule local notifications (those only make sense for upcoming appointments). Place it directly under `fetch`:

```ts
// Carga el historial de citas pasadas/finalizadas/canceladas del paciente
fetchPast: async function () {
  set({ loadingPast: true });
  try {
    const { data } = await client.query({
      query: GET_MY_APPOINTMENTS,
      variables: { upcoming: false },
      fetchPolicy: 'network-only',
    }) as { data: { myAppointments: Appointment[] } };

    const citas: Appointment[] = data.myAppointments ?? [];
    set({ pastItems: citas, loadingPast: false, hydratedPast: true });
  } catch (err) {
    set({ loadingPast: false, hydratedPast: true });
    const appError = parseGraphQLError(err);
    useErrorStore.getState().showError(appError);
  }
},
```

#### Update `reset`

Inside the existing `reset` function, ALSO clear the new fields:

```ts
reset: function () {
  const citas = get().items;
  for (const cita of citas) {
    cancelAppointmentReminder(cita.id).catch(() => {});
  }
  set({
    items: [],
    loading: false,
    hydrated: false,
    pastItems: [],
    loadingPast: false,
    hydratedPast: false,
  });
},
```

Do NOT touch `cancel`, `requestAppointment`, or any of the imports.

### 1.2 Hydrate the past list at boot

**File:** `src/store/bootstrapStore.ts`

Read the file first. Inside the existing `hydrate` action, find where the appointments store's `fetch()` is invoked and add a sibling call to `fetchPast()` so the home/profile-cold-start sequence loads both. If the store fans out via `Promise.all`, append `useAppointmentsStore.getState().fetchPast()` to the array. If it calls the actions sequentially, append a similar line.

Do NOT change any other store hydration order.

---

## Step 2 — Render the past section in `appointments.tsx`

### 2.1 File — `app/(tabs)/appointments.tsx`

The existing screen has one `Proximas citas` block followed by `<HospitalContactCard />`. Add a `Historial de citas` block between them.

Concrete edits inside `AppointmentsScreen`:

1. Read the new state from the store (next to the existing selectors):

   ```tsx
   const pastItems    = useAppointmentsStore(function (s) { return s.pastItems; });
   const loadingPast  = useAppointmentsStore(function (s) { return s.loadingPast; });
   const fetchPasadas = useAppointmentsStore(function (s) { return s.fetchPast; });
   ```

2. Update `handleRefresh` to refresh both lists in parallel (replace the existing implementation):

   ```tsx
   const handleRefresh = useCallback(async function () {
     setRefrescando(true);
     try {
       await Promise.all([fetchCitas(), fetchPasadas()]);
     } finally {
       setRefrescando(false);
     }
   }, [fetchCitas, fetchPasadas]);
   ```

3. Insert the historical section AFTER the upcoming `<View className="mb-6">` block (i.e. after the upcoming list/empty-state) and BEFORE `<HospitalContactCard />`:

   ```tsx
   {/* Seccion: Historial de citas */}
   <AppText
     variant="subtitle"
     weight="semibold"
     className="text-text-primary dark:text-text-primary-dark mb-3"
   >
     Historial de citas
   </AppText>

   {pastItems.length === 0 && !loadingPast ? (
     <View className="mb-6">
       <EmptyState
         icon="time-outline"
         title="Aun no tienes citas pasadas."
       />
     </View>
   ) : (
     <View className="mb-6">
       {pastItems.map(function (cita) {
         return (
           <AppointmentCard
             key={cita.id}
             appointment={cita}
             onCancel={undefined}
             readOnly
           />
         );
       })}
     </View>
   )}
   ```

   Notes:
   - Past appointments are NOT cancelable, so pass `onCancel={undefined}` and a new `readOnly` prop. See Step 3 for the prop wiring.
   - Use the same `mb-6` spacing as the upcoming block to keep visual rhythm.

4. Do NOT touch `ConfirmModal` or the cancel logic — it only applies to upcoming items.

### 2.2 Sort order

Past appointments come from the BFF in whatever order `myAppointments` returns. The desired order is most-recent-first. Add a one-liner sort right after reading from the store (in the JSX render or via a `useMemo` near the top of the component):

```tsx
const pastSorted = [...pastItems].sort(function (a, b) {
  // Orden descendente por fecha + hora (cita mas reciente primero)
  return (b.date + b.time).localeCompare(a.date + a.time);
});
```

Use `pastSorted` instead of `pastItems` in the `.map(...)` rendering. The string concat sort works because the BFF returns ISO `YYYY-MM-DD` and `HH:mm` and lexicographic order matches chronological order.

---

## Step 3 — `AppointmentCard` `readOnly` prop

### 3.1 File — `src/components/AppointmentCard.tsx`

The component currently accepts `appointment` + optional `onCancel`. Add an optional `readOnly?: boolean` prop and, when truthy, hide any cancel button / cancel CTA so past cards do not show a non-functional control.

Concrete edits:

1. Extend the prop type:
   ```ts
   type AppointmentCardProps = {
     appointment: Appointment;
     onCancel?: (id: string) => void;
     readOnly?: boolean;
   };
   ```
2. In the component body, gate the existing cancel CTA on `!readOnly && onCancel`. Example:
   ```tsx
   {!readOnly && onCancel ? (
     <Pressable onPress={function () { onCancel(appointment.id); }} /* existing classes */>
       {/* existing label */}
     </Pressable>
   ) : null}
   ```
3. Optional: visually mute past cards by adding `opacity-80` to the outer card wrapper when `readOnly` is true. Tailwind only — no inline styles.

Do NOT change the card's date/practitioner layout, icons, or the `status` badge logic. Past cards should still show whatever status the BFF returns (`COMPLETED`, `CANCELLED`, etc.).

---

## Step 4 — Manual verification

1. Launch the app in Expo Go after a fresh login.
2. Open **Citas**. Expected:
   - `Proximas citas` block renders as before.
   - `Historial de citas` block appears below, listing past appointments newest-first. Each card shows status (e.g. `Completada`, `Cancelada`). No cancel button.
   - When the patient has no past appointments, the empty-state `Aun no tienes citas pasadas.` is shown.
3. Pull to refresh — both sections update simultaneously; `refrescando` returns to `false` once both queries resolve.
4. Logout / login as another patient — `pastItems` is reset on `reset()`, then refilled on the next bootstrap.
5. Toggle dark mode — heading and empty-state text remain legible (existing `dark:` variants apply).

---

## Step 5 — Final checklist (run before reporting Phase 4 closed)

- [ ] Step 1.1: `pastItems`/`loadingPast`/`hydratedPast`/`fetchPast` added to `appointmentsStore`; `reset` clears them.
- [ ] Step 1.2: `bootstrapStore.hydrate` invokes `fetchPast()` alongside `fetch()`.
- [ ] Step 2.1: `appointments.tsx` renders `Historial de citas` section + empty state.
- [ ] Step 2.1: `handleRefresh` refreshes both lists in parallel.
- [ ] Step 2.2: past list sorted newest-first.
- [ ] Step 3.1: `AppointmentCard` accepts `readOnly` and hides cancel CTA.
- [ ] No new dependencies in `package.json` / `package-lock.json`.
- [ ] No file under `/mobile/backend` modified.
- [ ] Expo Go Android still launches cleanly.
- [ ] `mobile/CLAUDE.md` Phase 4 marked entirely `[x]`.

---

## Out of scope (do NOT implement here)

- Pagination of historical appointments — the BFF currently returns a flat list; if perf becomes an issue, raise it as a separate proposal.
- Filters (by status, by practitioner) on the history list.
- A separate "history" tab. The product wants both lists on the same screen.
- Phase 5 work (booking screen, WhatsApp chatbot, push, offline-first).
- Any `/mobile/backend` GraphQL change. The existing `myAppointments(upcoming: Boolean)` already returns past records when `upcoming:false`. If the BFF turns out NOT to honor that flag (verify in `mobile/backend/src/graphql`), STOP and notify the developer — do NOT improvise a fix on the frontend side.

---

## Nota Sprint Progreso (2026-04-27)

Treatment PDF y progress charts disponibles cuando /api Phase 8 cierre. Sin trabajo en este sprint.
