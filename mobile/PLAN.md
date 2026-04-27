# PLAN.md — Aesthetic & UX pass: Home, Appointments, Progress, Settings, Dark-mode text, Privacy/Terms modals

> Agent: Sonnet (Doer) under Agent 2 (Mobile)
> Domain: /mobile/frontend
> Prerequisites: Read CLAUDE.md of the root, mobile, and mobile/frontend. Previous plan (notifications + `--offline`) is already applied.
> Date: 2026-04-22

> **NOTE (important):** The developer referenced `[Pasted text #1 +11 lines]` in the request but the pasted content did not arrive. That section is therefore NOT covered in this plan. If, after executing every task below, there is still an unresolved issue that was in that pasted snippet, STOP and ask the developer to paste it again. Do NOT invent fixes for unseen errors.

---

## 0. Global rules for the Doer

- Make edits ONLY to the files listed. Do NOT reformat or refactor anything unrelated.
- Do NOT add, remove, or upgrade dependencies. Do NOT run `npm install`.
- Do NOT change any backend/GraphQL file. All work is in `/mobile/frontend`.
- Every Spanish label you ADD must use accents conservatively (the existing codebase omits them — follow the existing pattern: e.g. "Tamano" not "Tamaño", "Politica" not "Política"). If an existing label already has a specific spelling, keep it.
- After ALL tasks, run the app in Expo Go and visually verify each section below.
- Do NOT call TestSprite — these are cosmetic/UX changes, manual verification only.

---

## TASK 1 — Rename two bottom-tab labels (text clipping)

**Problem:** "Configuracion" and "Tratamientos" are too long for the tab bar and get clipped.

**File:** `/mobile/frontend/app/(tabs)/_layout.tsx`

**Edit:** in the `TAB_CONFIG` array:
- Change `title: 'Tratamientos'` → `title: 'Cura'` (for the `treatments` entry).
- Change `title: 'Configuracion'` → `title: 'Ajustes'` (for the `settings` entry).

Do NOT touch anything else in this file.

**Verification:** open the app; both labels fit inside the tab bar cells with no ellipsis.

---

## TASK 2 — Home tab: center greeting and wrap it in a highlighted card

**Problem:** the greeting on `(tabs)/index.tsx` is a bare left-aligned `<Text>` and looks plain.

**File:** `/mobile/frontend/app/(tabs)/index.tsx`

**Edit ONLY the greeting block** (currently lines 104–109). Replace:

```tsx
{/* Mensaje de bienvenida con saludo dinamico segun la hora */}
<View className="px-6 pt-6 pb-4">
  <Text className="text-2xl font-bold text-text-primary leading-8">
    {getGreeting(patientName)}
  </Text>
</View>
```

with:

```tsx
{/* Mensaje de bienvenida con saludo dinamico segun la hora — tarjeta centrada */}
<View className="px-6 pt-6 pb-4">
  <View className="bg-surface dark:bg-surface-dark rounded-2xl px-5 py-4 shadow-md border border-primary-200 dark:border-primary-600">
    <Text className="text-2xl font-bold text-text-primary dark:text-text-primary-dark leading-8 text-center">
      {getGreeting(patientName)}
    </Text>
  </View>
</View>
```

Keep the rest of the file (balloons, imports) untouched. The `Text` import stays as-is.

**Verification:** greeting appears inside a rounded, lightly-bordered card, centered text, visible in both light and dark mode.

---

## TASK 3 — Appointments tab: replace booking form with hospital contact card

**Problem:** the in-app booking form (`AppointmentRequestForm`) should NOT exist. Replace it with a card that shows hospital phone + email + the existing WhatsApp button.

### 3.1 — Create a new component

**File (NEW):** `/mobile/frontend/src/components/HospitalContactCard.tsx`

Write exactly:

```tsx
import { Linking, Pressable, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { FloatingCard } from './FloatingCard';
import { AppText } from './AppText';
import { WhatsAppButton } from './WhatsAppButton';

// Datos de contacto del hospital — constantes visibles al paciente
const HOSPITAL_PHONE = '+34 628 67 88 88';
const HOSPITAL_EMAIL = 'cita@rehabiapp.com';

// Tarjeta de contacto: telefono + email + WhatsApp. Sustituye al formulario de cita.
export function HospitalContactCard() {
  function handleLlamar() {
    Linking.openURL(`tel:${HOSPITAL_PHONE.replace(/\s+/g, '')}`).catch(function () {});
  }

  function handleEmail() {
    Linking.openURL(`mailto:${HOSPITAL_EMAIL}`).catch(function () {});
  }

  return (
    <FloatingCard className="mb-4">
      <AppText
        variant="subtitle"
        weight="semibold"
        className="text-text-primary dark:text-text-primary-dark mb-2"
      >
        Pedir cita nueva
      </AppText>

      <AppText
        variant="body"
        className="text-text-secondary dark:text-text-secondary-dark mb-4 leading-6"
      >
        Para solicitar una cita, contacta con el hospital por telefono o email.
        Tambien puedes escribirnos por WhatsApp.
      </AppText>

      {/* Telefono */}
      <Pressable
        onPress={handleLlamar}
        className="flex-row items-center gap-3 py-3 px-3 rounded-xl bg-background dark:bg-background-dark border border-border dark:border-border-dark mb-3 min-h-12"
      >
        <Ionicons name="call-outline" size={22} color="#2563EB" />
        <View className="flex-1">
          <AppText variant="caption" className="text-text-secondary dark:text-text-secondary-dark">
            Telefono
          </AppText>
          <AppText
            variant="body"
            weight="medium"
            className="text-text-primary dark:text-text-primary-dark"
          >
            {HOSPITAL_PHONE}
          </AppText>
        </View>
      </Pressable>

      {/* Email */}
      <Pressable
        onPress={handleEmail}
        className="flex-row items-center gap-3 py-3 px-3 rounded-xl bg-background dark:bg-background-dark border border-border dark:border-border-dark mb-4 min-h-12"
      >
        <Ionicons name="mail-outline" size={22} color="#2563EB" />
        <View className="flex-1">
          <AppText variant="caption" className="text-text-secondary dark:text-text-secondary-dark">
            Email
          </AppText>
          <AppText
            variant="body"
            weight="medium"
            className="text-text-primary dark:text-text-primary-dark"
          >
            {HOSPITAL_EMAIL}
          </AppText>
        </View>
      </Pressable>

      {/* Divisor */}
      <View className="border-t border-border dark:border-border-dark mb-4" />

      {/* WhatsApp */}
      <WhatsAppButton />
    </FloatingCard>
  );
}
```

### 3.2 — Swap the component in the Appointments screen

**File:** `/mobile/frontend/app/(tabs)/appointments.tsx`

- Remove the import `import { AppointmentRequestForm } from '../../src/components/AppointmentRequestForm';`.
- Add the import `import { HospitalContactCard } from '../../src/components/HospitalContactCard';`.
- Replace the JSX `<AppointmentRequestForm />` (line 83) with `<HospitalContactCard />`.

Do NOT touch anything else (cancel modal, cards list, RefreshControl all stay).

### 3.3 — Do NOT delete the old file

Leave `src/components/AppointmentRequestForm.tsx` on disk. It will simply no longer be imported. Skip it — deleting it is out of scope.

**Verification:** Appointments tab shows the existing upcoming-appointments list followed by a card with Phone, Email, and WhatsApp. Tapping Phone opens the dialer; tapping Email opens the mail composer.

---

## TASK 4 — Progress tab: human-shaped body diagram + readable chart X-axis

### 4.1 — Rewrite body paths to resemble a human silhouette

**File:** `/mobile/frontend/src/components/bodyPaths.ts`

Overwrite the file entirely with:

```ts
import type { BodyPartId } from '../types/progress';

// Paths SVG para viewBox "0 0 300 600".
// Formas redondeadas y ligeramente curvas para evocar una silueta humana
// frontal, en lugar de rectangulos. Cada path sigue siendo un area clicable.
export const BODY_PART_PATHS: Record<BodyPartId, string> = {
  // Cabeza — ovalo
  HEAD: 'M 150,40 C 125,40 108,62 108,88 C 108,114 125,132 150,132 C 175,132 192,114 192,88 C 192,62 175,40 150,40 Z',

  // Cuello — trapecio estrecho
  NECK: 'M 136,128 L 164,128 L 170,152 L 130,152 Z',

  // Torso — forma acampanada, hombros anchos a cintura estrecha
  TORSO: 'M 110,150 C 102,170 100,195 104,230 C 106,255 112,280 120,305 L 180,305 C 188,280 194,255 196,230 C 200,195 198,170 190,150 Z',

  // Hombros — esferas curvas a cada lado
  LEFT_SHOULDER:
    'M 108,150 C 88,148 70,160 62,180 C 56,195 58,208 68,215 L 100,215 C 102,200 103,180 108,150 Z',
  RIGHT_SHOULDER:
    'M 192,150 C 212,148 230,160 238,180 C 244,195 242,208 232,215 L 200,215 C 198,200 197,180 192,150 Z',

  // Brazos — formas curvas descendentes
  LEFT_ARM:
    'M 62,215 C 55,240 52,265 56,295 C 60,320 68,340 76,350 L 96,345 C 94,320 96,290 100,260 C 102,240 102,225 100,215 Z',
  RIGHT_ARM:
    'M 238,215 C 245,240 248,265 244,295 C 240,320 232,340 224,350 L 204,345 C 206,320 204,290 200,260 C 198,240 198,225 200,215 Z',

  // Manos — ovalos
  LEFT_HAND:
    'M 60,350 C 50,355 46,370 52,385 C 58,398 72,400 82,395 C 90,390 94,378 90,365 C 86,352 72,348 60,350 Z',
  RIGHT_HAND:
    'M 240,350 C 250,355 254,370 248,385 C 242,398 228,400 218,395 C 210,390 206,378 210,365 C 214,352 228,348 240,350 Z',

  // Caderas — areas trapezoidales suaves
  LEFT_HIP:
    'M 120,305 L 150,305 L 152,360 C 150,365 142,368 134,368 C 124,368 118,360 116,350 Z',
  RIGHT_HIP:
    'M 150,305 L 180,305 L 184,350 C 182,360 176,368 166,368 C 158,368 150,365 148,360 Z',

  // Piernas — formas alargadas curvas
  LEFT_LEG:
    'M 116,365 C 112,400 110,440 114,485 C 116,510 122,525 130,530 L 150,528 C 152,490 152,445 150,400 C 150,385 150,375 152,365 Z',
  RIGHT_LEG:
    'M 184,365 C 188,400 190,440 186,485 C 184,510 178,525 170,530 L 150,528 C 148,490 148,445 150,400 C 150,385 150,375 148,365 Z',

  // Pies — elipses achatadas
  LEFT_FOOT:
    'M 112,528 C 100,532 92,545 96,558 C 100,568 114,572 130,568 C 144,565 150,555 150,548 C 148,535 130,526 112,528 Z',
  RIGHT_FOOT:
    'M 188,528 C 200,532 208,545 204,558 C 200,568 186,572 170,568 C 156,565 150,555 150,548 C 152,535 170,526 188,528 Z',
};

// Orden de renderizado — torso primero, extremidades encima
export const RENDER_ORDER: BodyPartId[] = [
  'TORSO',
  'LEFT_HIP', 'RIGHT_HIP',
  'HEAD', 'NECK',
  'LEFT_SHOULDER', 'RIGHT_SHOULDER',
  'LEFT_ARM', 'RIGHT_ARM',
  'LEFT_HAND', 'RIGHT_HAND',
  'LEFT_LEG', 'RIGHT_LEG',
  'LEFT_FOOT', 'RIGHT_FOOT',
];
```

### 4.2 — Improve contrast of the BodyDiagram (make inactive parts visible as silhouette)

**File:** `/mobile/frontend/src/components/BodyDiagram.tsx`

Inside the function body, apply these edits (replace the two lines that set inactive color constants and the opacity logic):

Replace:

```tsx
const COLOR_ACTIVO = '#2563EB';
const COLOR_INACTIVO_LIGHT = '#E2E8F0';
const COLOR_INACTIVO_DARK = '#1F2937';
```

with:

```tsx
// Color activo (tratamiento en curso)
const COLOR_ACTIVO = '#2563EB';
// Colores inactivos — mas opacos para que la silueta humana se perciba claramente
const COLOR_INACTIVO_LIGHT = '#CBD5E1';
const COLOR_INACTIVO_DARK = '#334155';
// Borde de cada parte — da sensacion de contorno anatomico
const STROKE_LIGHT = '#94A3B8';
const STROKE_DARK = '#0F172A';
```

Replace the existing Path mapping block:

```tsx
{RENDER_ORDER.map(function (partId) {
  const part = partsMap.get(partId);
  const activa = part?.hasTreatment ?? false;
  const fill = activa ? COLOR_ACTIVO : colorInactivo;
  const opacity = activa ? 1 : 0.5;

  return (
    <Path
      key={partId}
      d={BODY_PART_PATHS[partId]}
      fill={fill}
      opacity={opacity}
      onPress={activa && part ? function () { onPressPart(part); } : undefined}
    />
  );
})}
```

with:

```tsx
{RENDER_ORDER.map(function (partId) {
  const part = partsMap.get(partId);
  const activa = part?.hasTreatment ?? false;
  const fill = activa ? COLOR_ACTIVO : colorInactivo;
  const stroke = scheme === 'dark' ? STROKE_DARK : STROKE_LIGHT;

  return (
    <Path
      key={partId}
      d={BODY_PART_PATHS[partId]}
      fill={fill}
      stroke={stroke}
      strokeWidth={1.5}
      opacity={activa ? 1 : 0.85}
      onPress={activa && part ? function () { onPressPart(part); } : undefined}
    />
  );
})}
```

Everything else (imports, `svgWidth`, `viewBox="0 0 300 600"`) stays the same.

### 4.3 — Fix overlapping X-axis labels in the progress chart

**File:** `/mobile/frontend/src/components/ProgressChartModal.tsx`

Inside the component, replace the `chartData` block and the `<LineChart .../>` props.

Replace:

```tsx
const chartData = metrics.length > 0
  ? { labels: metrics.map(function (m) { return m.date.slice(5); }), datasets: [{ data: metrics.map(function (m) { return m.score; }) }] }
  : { labels: [''], datasets: [{ data: [0] }] };
```

with:

```tsx
// Reduce densidad de etiquetas: muestra la fecha solo en puntos pares
// para evitar solapamiento cuando hay muchos registros.
const chartLabels = metrics.length > 0
  ? metrics.map(function (m, idx) {
      if (metrics.length > 4 && idx % 2 !== 0) return '';
      return m.date.slice(5);
    })
  : [''];

const chartData = metrics.length > 0
  ? { labels: chartLabels, datasets: [{ data: metrics.map(function (m) { return m.score; }) }] }
  : { labels: [''], datasets: [{ data: [0] }] };
```

Replace the existing `<LineChart ... />` (currently the 11-line JSX block) with:

```tsx
<LineChart
  data={chartData}
  width={chartWidth}
  height={180}
  chartConfig={{
    backgroundGradientFrom: isDark ? '#111827' : '#FFFFFF',
    backgroundGradientTo: isDark ? '#111827' : '#FFFFFF',
    color: function () { return '#2563EB'; },
    labelColor: function () { return isDark ? '#94A3B8' : '#64748B'; },
    propsForDots: { r: '4', strokeWidth: '2', stroke: '#2563EB' },
    propsForLabels: { fontSize: 10 },
    decimalPlaces: 0,
  }}
  bezier
  style={{ borderRadius: 8, paddingRight: 24 }}
  withInnerLines={false}
  verticalLabelRotation={-30}
  xLabelsOffset={-4}
/>
```

Do NOT touch the loading / sinDatos branches or the rest of the modal.

**Verification:** open the progress tab — silhouette is recognizably human; open modal for an active body part — chart X labels are angled and non-overlapping.

---

## TASK 5 — Dark mode text visibility

**Problem:** several screens use `text-text-primary` / `text-text-secondary` without their `dark:` counterparts, so text stays dark-on-dark in dark mode.

### 5.1 — Home greeting (covered by Task 2 already).

### 5.2 — Appointments screen heading

**File:** `/mobile/frontend/app/(tabs)/appointments.tsx`

No change needed — the `Proximas citas` heading already has `dark:text-text-primary-dark`. Skip.

### 5.3 — Audit the following files and, for every `AppText` or `Text` that has `text-text-primary` WITHOUT `dark:text-text-primary-dark`, append ` dark:text-text-primary-dark`. Same for `text-text-secondary` → append ` dark:text-text-secondary-dark`.

Files to audit (in this exact order):

1. `/mobile/frontend/app/(tabs)/index.tsx`
2. `/mobile/frontend/app/(tabs)/games.tsx`
3. `/mobile/frontend/app/(tabs)/treatments.tsx`
4. `/mobile/frontend/app/(tabs)/progress.tsx`
5. `/mobile/frontend/app/(tabs)/profile.tsx`
6. `/mobile/frontend/src/components/EmptyState.tsx`
7. `/mobile/frontend/src/components/AppointmentCard.tsx`
8. `/mobile/frontend/src/components/DifficultyBadge.tsx`
9. `/mobile/frontend/src/components/PathologyPill.tsx`
10. `/mobile/frontend/src/components/InfoRow.tsx`
11. `/mobile/frontend/src/components/ProfileHeader.tsx`
12. `/mobile/frontend/src/components/FloatingCard.tsx` (if it wraps a title)

**How to apply mechanically (use `grep`):**

```bash
# From /mobile/frontend, find every candidate line:
grep -rn "text-text-primary\"" app src/components/ | grep -v "dark:text-text-primary-dark"
grep -rn "text-text-secondary\"" app src/components/ | grep -v "dark:text-text-secondary-dark"
# Also variants where the class list has more classes after:
grep -rn "text-text-primary " app src/components/ | grep -v "dark:text-text-primary-dark"
grep -rn "text-text-secondary " app src/components/ | grep -v "dark:text-text-secondary-dark"
```

For each hit, open the file and append the missing `dark:` class. If the hit appears inside a literal className string that already ends with `"`, insert ` dark:text-text-primary-dark` before the closing quote. Keep existing tailwind classes in place. Do NOT reformat JSX.

**Do NOT add** `dark:` variants to:
- `text-white`, `text-error`, `text-success`, `text-primary-600` — they already look correct in dark mode.
- Classes inside backend / non-frontend files.

**Verification:** toggle the app into dark mode (after Task 6 is done) and confirm every tab is legible. Text should switch to a light tone on dark background.

---

## TASK 6 — Settings: theme toggle + font-scale slider

**Problem:**
- Theme uses a three-way segmented control including "Sistema", which is visually the odd one out.
- Font size is also a segmented control; the user wants a draggable horizontal slider.

### 6.1 — Rewrite the SettingsScreen Appearance section

**File:** `/mobile/frontend/app/(tabs)/settings.tsx`

- REMOVE the `THEME_OPTIONS` and `SCALE_OPTIONS` constants.
- REMOVE the import `import { SegmentedControl } ...`.
- ADD the imports:
  ```tsx
  import { ToggleRow } from '../../src/components/ToggleRow';
  import { FontScaleSlider } from '../../src/components/FontScaleSlider';
  import { LegalTextModal } from '../../src/components/LegalTextModal';
  ```
  (`ToggleRow` is already imported — do not duplicate.)
- ADD `useState` hook import if not already present.
- Inside the component, replace the ENTIRE `{/* Apariencia */}` `<SettingsSection>` block with:

```tsx
{/* Apariencia */}
<SettingsSection title="Apariencia">
  <ToggleRow
    label="Tema oscuro"
    value={themeMode === 'dark'}
    onChange={function (v) { setThemeMode(v ? 'dark' : 'light'); }}
  />
  <View className="px-4 py-4">
    <AppText
      variant="caption"
      weight="medium"
      className="text-text-secondary dark:text-text-secondary-dark mb-2"
    >
      Tamano de texto
    </AppText>
    <FontScaleSlider
      value={fontScale}
      onChange={function (v) { setFontScale(v); }}
    />
  </View>
</SettingsSection>
```

- Add two `useState` values for legal modals near the other `useState` calls:
  ```tsx
  const [privacyOpen, setPrivacyOpen] = useState(false);
  const [termsOpen, setTermsOpen] = useState(false);
  ```

- In the `{/* Acerca de */}` section, replace the two `/* TODO */` `onPress` handlers:
  ```tsx
  onPress={function () { setPrivacyOpen(true); }}   // Politica de privacidad
  onPress={function () { setTermsOpen(true); }}     // Terminos y condiciones
  ```

- Just before the final `</ScrollView>`, add:

```tsx
<LegalTextModal
  visible={privacyOpen}
  title="Politica de privacidad"
  onClose={function () { setPrivacyOpen(false); }}
/>
<LegalTextModal
  visible={termsOpen}
  title="Terminos y condiciones"
  onClose={function () { setTermsOpen(false); }}
/>
```

Keep everything else in the file (Notifications section, Probar notificacion, Version row) untouched.

### 6.2 — Create `FontScaleSlider`

**File (NEW):** `/mobile/frontend/src/components/FontScaleSlider.tsx`

Do NOT install any package. Build a tap-and-drag horizontal slider with `PanResponder` from `react-native`. It snaps to the 4 discrete `FontScale` values.

```tsx
import { useRef, useState } from 'react';
import {
  LayoutChangeEvent,
  PanResponder,
  View,
} from 'react-native';
import { AppText } from './AppText';
import type { FontScale } from '../utils/fontScale';

const SCALES: FontScale[] = [0.9, 1.0, 1.15, 1.3];
const SCALE_LABELS = ['A-', 'A', 'A+', 'A++'];

type FontScaleSliderProps = {
  value: FontScale;
  onChange: (v: FontScale) => void;
};

// Slider discreto: linea horizontal con cuatro puntos (A-, A, A+, A++).
// El usuario arrastra el pulgar y el valor se ajusta al punto mas cercano.
export function FontScaleSlider(props: FontScaleSliderProps) {
  const { value, onChange } = props;
  const [trackWidth, setTrackWidth] = useState(0);
  const trackWidthRef = useRef(0);

  function handleLayout(e: LayoutChangeEvent) {
    const w = e.nativeEvent.layout.width;
    trackWidthRef.current = w;
    setTrackWidth(w);
  }

  // Convierte posicion X (pixels) al indice mas cercano [0..3]
  function xToIndex(x: number): number {
    const w = trackWidthRef.current;
    if (w <= 0) return 0;
    const clamped = Math.max(0, Math.min(w, x));
    const step = w / (SCALES.length - 1);
    return Math.round(clamped / step);
  }

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      onPanResponderGrant: function (_evt, gesture) {
        const idx = xToIndex(gesture.x0 - gesture.moveX + gesture.moveX);
        // En grant, moveX y x0 coinciden — aproximamos con locationX no disponible aqui,
        // por lo que usamos el desplazamiento respecto al track al soltar.
        onChange(SCALES[idx]);
      },
      onPanResponderMove: function (evt, _gesture) {
        const idx = xToIndex(evt.nativeEvent.locationX);
        onChange(SCALES[idx]);
      },
      onPanResponderRelease: function (evt) {
        const idx = xToIndex(evt.nativeEvent.locationX);
        onChange(SCALES[idx]);
      },
    }),
  ).current;

  const currentIndex = SCALES.indexOf(value);
  const thumbX =
    trackWidth > 0 && currentIndex >= 0
      ? (trackWidth / (SCALES.length - 1)) * currentIndex
      : 0;

  return (
    <View>
      {/* Track + thumb */}
      <View
        className="h-10 justify-center"
        onLayout={handleLayout}
        {...panResponder.panHandlers}
      >
        {/* Linea horizontal */}
        <View className="h-1 rounded-full bg-border dark:bg-border-dark" />
        {/* Puntos de paso */}
        <View
          className="absolute left-0 right-0 flex-row justify-between"
          pointerEvents="none"
        >
          {SCALES.map(function (s, i) {
            const active = i <= currentIndex;
            return (
              <View
                key={s}
                className={`w-3 h-3 rounded-full ${active ? 'bg-primary-600' : 'bg-border dark:bg-border-dark'}`}
              />
            );
          })}
        </View>
        {/* Thumb */}
        <View
          className="absolute w-6 h-6 rounded-full bg-primary-600 border-2 border-white dark:border-surface-dark"
          style={{ left: Math.max(0, thumbX - 12) }}
          pointerEvents="none"
        />
      </View>
      {/* Etiquetas */}
      <View className="flex-row justify-between mt-2">
        {SCALE_LABELS.map(function (lbl) {
          return (
            <AppText
              key={lbl}
              variant="caption"
              className="text-text-secondary dark:text-text-secondary-dark"
            >
              {lbl}
            </AppText>
          );
        })}
      </View>
    </View>
  );
}
```

Notes for the Doer:
- The slider is DELIBERATELY simple: it snaps to the nearest of 4 positions. No animation, no pan gesture library — `PanResponder` is a built-in from `react-native`, no install.
- If `locationX` is `undefined` on any platform, the value will snap to index 0 — that is acceptable.
- Do NOT introduce `@react-native-community/slider`.

### 6.3 — Create `LegalTextModal`

**File (NEW):** `/mobile/frontend/src/components/LegalTextModal.tsx`

```tsx
import { Pressable, ScrollView, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { BackdropModal } from './BackdropModal';
import { AppText } from './AppText';
import { useTheme } from '../utils/theme';

type LegalTextModalProps = {
  visible: boolean;
  title: string;
  onClose: () => void;
};

// Modal generico para mostrar un texto legal de muestra.
// Indica que el proyecto es una prueba no adaptada a ningun hospital concreto.
export function LegalTextModal(props: LegalTextModalProps) {
  const { visible, title, onClose } = props;
  const { scheme } = useTheme();
  const isDark = scheme === 'dark';

  return (
    <BackdropModal visible={visible} onClose={onClose}>
      <View className={`rounded-2xl p-5 ${isDark ? 'bg-surface-dark' : 'bg-surface'}`} style={{ maxHeight: '80%' }}>
        {/* Cabecera */}
        <View className="flex-row justify-between items-center mb-3">
          <AppText
            variant="subtitle"
            weight="semibold"
            className="text-text-primary dark:text-text-primary-dark flex-1"
          >
            {title}
          </AppText>
          <Pressable onPress={onClose} hitSlop={12}>
            <Ionicons name="close-outline" size={24} color={isDark ? '#F1F5F9' : '#1E293B'} />
          </Pressable>
        </View>

        {/* Cuerpo */}
        <ScrollView>
          <AppText
            variant="body"
            className="text-text-secondary dark:text-text-secondary-dark leading-6 mb-3"
          >
            Este documento es una muestra generica. RehabiAPP es actualmente un
            proyecto de prueba y no esta adaptado a ningun hospital ni entidad
            sanitaria concreta.
          </AppText>
          <AppText
            variant="body"
            className="text-text-secondary dark:text-text-secondary-dark leading-6 mb-3"
          >
            En caso de que la aplicacion sea adquirida por una empresa
            contratante, este texto sera sustituido por la politica legal real
            adaptada a los requisitos del cliente (RGPD, LOPDGDD, Ley 41/2002 y
            normativas internas del centro).
          </AppText>
          <AppText
            variant="body"
            className="text-text-secondary dark:text-text-secondary-dark leading-6"
          >
            Hasta entonces, ninguno de los terminos aqui mostrados constituye un
            acuerdo vinculante.
          </AppText>
        </ScrollView>

        {/* Accion de cierre */}
        <Pressable
          onPress={onClose}
          className="mt-4 py-3 rounded-xl bg-primary-600 items-center min-h-12"
        >
          <AppText variant="body" weight="semibold" className="text-white">
            Entendido
          </AppText>
        </Pressable>
      </View>
    </BackdropModal>
  );
}
```

**Verification:**
- Settings → Apariencia shows a single toggle "Tema oscuro" and below it the new slider labelled A- … A++.
- Dragging the slider changes the font scale across the app immediately.
- Tapping Privacy / Terms opens the modal with the prototype disclaimer; closing works via X, backdrop, or the "Entendido" button.

---

## TASK 7 — Acceptance checklist

After ALL tasks:

1. Bottom tabs show: Inicio, Citas, Juegos, Cura, Progreso, Perfil, Ajustes — no clipped text.
2. Inicio greeting is inside a centered highlighted card.
3. Citas shows upcoming appointments list + a single contact card (phone, email, WhatsApp). No form fields.
4. Progreso silhouette looks like a human (not rectangles). Inactive parts are clearly visible but muted. Modal chart X-axis has no overlapping labels.
5. Toggling Tema oscuro switches the whole UI to dark mode AND every visible text is legible.
6. Font scale slider is draggable; labels change size live.
7. Tapping Politica de privacidad / Terminos y condiciones opens a disclaimer modal.
8. Expo Go Android still launches cleanly (no `expo-notifications` crash — preserved from earlier fix).
9. `package.json` / `package-lock.json` are NOT modified.

---

## TASK 8 — Guardrails for the Doer (re-read before starting)

- ONE pass per task. Do NOT revisit a file after completing its task unless a later task explicitly tells you to.
- Do NOT add CSS outside tailwind classes (no inline `StyleSheet.create` sections) except where this plan explicitly uses `style={{...}}`.
- Do NOT touch ANY file under `/mobile/backend`.
- Do NOT touch `tailwind.config.js`.
- Do NOT delete `AppointmentRequestForm.tsx` or any other file, even if it becomes unused after Task 3.
- If any step lacks info to complete safely, STOP and ask the developer. Do NOT improvise.
- Treat the missing `[Pasted text #1]` content as out-of-scope. Do NOT fabricate fixes for it.
