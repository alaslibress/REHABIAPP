# PLAN.md — RehabiAPP Mobile Frontend Implementation Plan

> **Target directory:** `/mobile/frontend/`
> **Agent:** Agent 2 — Doer (Sonnet)
> **Language:** All code comments in Spanish. Document in English.
> **Stack:** React Native, Expo 54, Expo Router 6, NativeWind, Zustand, Apollo Client, React Native Reanimated

---

## Code Conventions (APPLY TO ALL STEPS)

1. **No arrow functions** for function declarations. Use `function name() { }` syntax. The ONLY exception is inline callbacks inside JSX props where arrow functions are unavoidable (e.g., `onPress={() => doSomething()}`).
2. **No inline types** in function parameters. Always define a named `type` in `src/types/` and reference it.
3. **Use `type` keyword**, never `interface`.
4. **All comments in Spanish.**
5. **No `fetch` or `axios`** anywhere in the project. All data fetching through Apollo Client (`useQuery`, `useMutation`, or `client.query`/`client.mutate`).
6. **Minimum touch targets**: 48x48dp (`min-h-12 min-w-12` in NativeWind).

---

## Step 1: Project Restructure and Dependencies

### 1.1 Install new dependencies

```bash
npx expo install @apollo/client graphql expo-secure-store
```

### 1.2 Create folder structure

Create the following directories and files (empty files where noted):

```
frontend/
  app/
    (auth)/
      _layout.tsx          # Auth group layout
      login.tsx            # Login screen
    (tabs)/
      _layout.tsx          # Bottom tab navigator
      index.tsx            # Home screen (Balloon UI)
      profile.tsx          # Placeholder
      settings.tsx         # Placeholder
      appointments.tsx     # Placeholder
      progress.tsx         # Placeholder
      games.tsx            # Placeholder
      treatments.tsx       # Placeholder
    _layout.tsx            # Root layout (providers + auth guard)
    +not-found.tsx         # Keep existing
  src/
    components/
      ErrorPopup.tsx
      FloatingBalloon.tsx
    hooks/
      useGreeting.ts
    services/
      graphql/
        client.ts
        queries/
          auth.ts
          user.ts
          appointments.ts
          games.ts
          treatments.ts
        mutations/
          auth.ts
          appointments.ts
    store/
      authStore.ts
      userStore.ts
      errorStore.ts
    types/
      auth.ts
      user.ts
      errors.ts
      appointments.ts
      games.ts
      treatments.ts
    utils/
      greeting.ts
      errorHandler.ts
  assets/                  # Already exists
```

### 1.3 Remove boilerplate files

Delete these files from the current project:
- `app/details.tsx`
- `app/index.tsx` (will be replaced by `app/(tabs)/index.tsx`)
- `app/+html.tsx`
- `components/Button.tsx`
- `components/Container.tsx`
- `components/EditScreenInfo.tsx`
- `components/ScreenContent.tsx`
- `store/store.ts` (bear example)

### 1.4 Rename logo assets

```bash
mv "assets/RehabiAPPLogo - Editada.png" assets/rehabiapp-logo.png
```

Keep `assets/RehabiAPPLogoNoLetras.png` as-is (may be used later).

### 1.5 Update tailwind.config.js

Update the `content` array to include the new `src/` directory:

```javascript
// tailwind.config.js
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './app/**/*.{js,ts,tsx}',
    './src/**/*.{js,ts,tsx}',
  ],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      // Colores definidos en el Paso 2
    },
  },
  plugins: [],
};
```

---

## Step 2: Theme and Color Palette

### 2.1 Define colors in tailwind.config.js

Add under `theme.extend.colors`:

```javascript
colors: {
  primary: {
    50: '#EFF6FF',
    100: '#DBEAFE',
    200: '#BFDBFE',
    300: '#93C5FD',
    400: '#60A5FA',
    500: '#3B82F6',
    600: '#2563EB',   // Tono principal
    700: '#1D4ED8',
    800: '#1E40AF',
    900: '#1E3A8A',
  },
  surface: '#FFFFFF',
  background: '#F0F4FF',
  'text-primary': '#1E293B',
  'text-secondary': '#64748B',
  error: '#EF4444',
  success: '#22C55E',
},
```

### 2.2 Design tokens reference

| Token | Value | Usage |
|-------|-------|-------|
| `bg-background` | `#F0F4FF` | Screen backgrounds |
| `bg-surface` | `#FFFFFF` | Cards, containers, nav bars |
| `bg-primary-600` | `#2563EB` | Primary buttons, active tab |
| `text-text-primary` | `#1E293B` | Headings, body text |
| `text-text-secondary` | `#64748B` | Subtitles, hints |
| `text-error` | `#EF4444` | Error messages |
| `min-h-12 min-w-12` | 48x48dp | Minimum touch target |
| Body text | 16px (`text-base`) | Minimum readable size |
| Headings | 20-28px (`text-xl` to `text-2xl`) | Screen titles |

---

## Step 3: TypeScript Type Definitions

Create these files with the following content. Remember: use `type`, not `interface`.

### `src/types/auth.ts`

```typescript
type LoginCredentials = {
  identifier: string;   // DNI o correo electronico
  password: string;
};

type AuthToken = {
  accessToken: string;
  refreshToken: string;
  expiresAt: number;     // Unix timestamp en segundos
};

type AuthState = {
  token: AuthToken | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: AppError | null;
  login: (credentials: LoginCredentials) => Promise<void>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<void>;
  clearError: () => void;
};

export type { LoginCredentials, AuthToken, AuthState };
```

### `src/types/user.ts`

```typescript
type Patient = {
  id: string;
  name: string;
  surname: string;
  email: string;
  dni: string;
  phone: string | null;
  birthDate: string | null;
  address: string | null;
  active: boolean;
};

type UserState = {
  patient: Patient | null;
  isLoading: boolean;
  fetchProfile: () => Promise<void>;
  clearProfile: () => void;
};

export type { Patient, UserState };
```

### `src/types/errors.ts`

```typescript
type AppError = {
  title: string;       // Siempre "Error"
  subtitle: string;    // Nombre especifico del problema
  message: string;     // Descripcion detallada
  code: ErrorCode;
};

type ErrorCode =
  | 'INVALID_CREDENTIALS'
  | 'ACCOUNT_DEACTIVATED'
  | 'TOKEN_EXPIRED'
  | 'TOKEN_INVALID'
  | 'NETWORK_ERROR'
  | 'PATIENT_NOT_FOUND'
  | 'APPOINTMENT_CONFLICT'
  | 'APPOINTMENT_NOT_FOUND'
  | 'VALIDATION_ERROR'
  | 'INTERNAL_ERROR';

type ErrorPopupProps = {
  error: AppError;
  visible: boolean;
  onAccept: () => void;
  onCancel: () => void;
};

type ErrorState = {
  currentError: AppError | null;
  isVisible: boolean;
  showError: (error: AppError) => void;
  hideError: () => void;
};

export type { AppError, ErrorCode, ErrorPopupProps, ErrorState };
```

### `src/types/appointments.ts`

```typescript
type AppointmentStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';

type Appointment = {
  id: string;
  date: string;
  time: string;
  practitionerName: string;
  practitionerSpecialty: string | null;
  status: AppointmentStatus;
  notes: string | null;
};

export type { Appointment, AppointmentStatus };
```

### `src/types/games.ts`

```typescript
type GameMetrics = {
  accuracy: number | null;
  reactionTime: number | null;
  completionRate: number | null;
};

type GameSession = {
  id: string;
  gameName: string;
  playedAt: string;
  score: number | null;
  duration: number | null;
  metrics: GameMetrics | null;
};

type ProgressSummary = {
  totalSessions: number;
  averageScore: number | null;
  improvementRate: number | null;
  lastSessionDate: string | null;
};

export type { GameMetrics, GameSession, ProgressSummary };
```

### `src/types/treatments.ts`

```typescript
type TreatmentType = 'MEDICATION' | 'EXERCISE' | 'GAMIFICATION' | 'TEXT_INSTRUCTION';

type Treatment = {
  id: string;
  name: string;
  description: string | null;
  type: TreatmentType;
  visible: boolean;
  progressionLevel: number;
};

type Disability = {
  id: string;
  name: string;
  description: string | null;
  currentLevel: number;
};

export type { Treatment, TreatmentType, Disability };
```

---

## Step 4: Zustand Stores

### `src/store/authStore.ts`

```typescript
import { create } from 'zustand';
import * as SecureStore from 'expo-secure-store';
import type { AuthState, AuthToken, LoginCredentials } from '../types/auth';
import type { AppError } from '../types/errors';
import { client } from '../services/graphql/client';
import { LOGIN_MUTATION } from '../services/graphql/mutations/auth';
import { parseGraphQLError } from '../utils/errorHandler';

// Clave para almacenamiento seguro de tokens
const TOKEN_KEY = 'auth_token';

export const useAuthStore = create<AuthState>(function (set, get) {
  return {
    token: null,
    isAuthenticated: false,
    isLoading: true,
    error: null,

    login: async function (credentials: LoginCredentials): Promise<void> {
      set({ isLoading: true, error: null });
      try {
        const { data } = await client.mutate({
          mutation: LOGIN_MUTATION,
          variables: {
            identifier: credentials.identifier,
            password: credentials.password,
          },
        });
        const token: AuthToken = data.login;
        await SecureStore.setItemAsync(TOKEN_KEY, JSON.stringify(token));
        set({ token, isAuthenticated: true, isLoading: false });
      } catch (err) {
        const appError: AppError = parseGraphQLError(err);
        set({ error: appError, isLoading: false });
        throw appError;
      }
    },

    logout: async function (): Promise<void> {
      await SecureStore.deleteItemAsync(TOKEN_KEY);
      set({ token: null, isAuthenticated: false, error: null });
      client.clearStore();
    },

    refreshSession: async function (): Promise<void> {
      // Implementar con REFRESH_TOKEN_MUTATION
      // Si falla, llamar a logout()
    },

    clearError: function (): void {
      set({ error: null });
    },
  };
});
```

**Implementation notes:**
- On app startup (in root `_layout.tsx`), check `SecureStore` for an existing token. If found and not expired, set `isAuthenticated: true`. If expired, attempt `refreshSession`. If refresh fails, `logout`.
- The `login` function calls the GraphQL `LOGIN_MUTATION`, stores the token, and updates state.
- Use conventional `function` syntax throughout (not arrow functions).

### `src/store/userStore.ts`

```typescript
import { create } from 'zustand';
import type { UserState } from '../types/user';
import { client } from '../services/graphql/client';
import { GET_MY_PROFILE } from '../services/graphql/queries/user';

export const useUserStore = create<UserState>(function (set) {
  return {
    patient: null,
    isLoading: false,

    fetchProfile: async function (): Promise<void> {
      set({ isLoading: true });
      try {
        const { data } = await client.query({ query: GET_MY_PROFILE });
        set({ patient: data.me, isLoading: false });
      } catch (err) {
        set({ isLoading: false });
        throw err;
      }
    },

    clearProfile: function (): void {
      set({ patient: null });
    },
  };
});
```

### `src/store/errorStore.ts`

```typescript
import { create } from 'zustand';
import type { ErrorState, AppError } from '../types/errors';

export const useErrorStore = create<ErrorState>(function (set) {
  return {
    currentError: null,
    isVisible: false,

    showError: function (error: AppError): void {
      set({ currentError: error, isVisible: true });
    },

    hideError: function (): void {
      set({ currentError: null, isVisible: false });
    },
  };
});
```

Delete the old `store/store.ts` file.

---

## Step 5: GraphQL Service Layer

### 5.1 Apollo Client — `src/services/graphql/client.ts`

```typescript
import { ApolloClient, InMemoryCache, createHttpLink, from } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import { onError } from '@apollo/client/link/error';
import * as SecureStore from 'expo-secure-store';
import type { AuthToken } from '../../types/auth';

const TOKEN_KEY = 'auth_token';

// URL base del backend BFF — cambiar segun entorno
const GRAPHQL_URI = 'http://localhost:4000/graphql';

const httpLink = createHttpLink({
  uri: GRAPHQL_URI,
});

// Link de autenticacion: adjunta el JWT a cada peticion
const authLink = setContext(async function (_request, previousContext) {
  const tokenJson = await SecureStore.getItemAsync(TOKEN_KEY);
  if (!tokenJson) {
    return previousContext;
  }
  const token: AuthToken = JSON.parse(tokenJson);
  return {
    ...previousContext,
    headers: {
      ...previousContext.headers,
      Authorization: `Bearer ${token.accessToken}`,
    },
  };
});

// Link de errores globales
const errorLink = onError(function ({ graphQLErrors, networkError }) {
  if (graphQLErrors) {
    for (const err of graphQLErrors) {
      console.error(`[GraphQL Error]: ${err.message}`);
    }
  }
  if (networkError) {
    console.error(`[Network Error]: ${networkError.message}`);
  }
});

export const client = new ApolloClient({
  link: from([errorLink, authLink, httpLink]),
  cache: new InMemoryCache(),
});
```

**Note:** `@apollo/client/link/context` and `@apollo/client/link/error` are included in the `@apollo/client` package. No extra install needed.

### 5.2 Queries

#### `src/services/graphql/queries/auth.ts`

```typescript
// No hay queries de auth, solo mutations
// Este archivo existe por consistencia de estructura
```

#### `src/services/graphql/queries/user.ts`

```typescript
import { gql } from '@apollo/client';

export const GET_MY_PROFILE = gql`
  query GetMyProfile {
    me {
      id
      name
      surname
      email
      dni
      phone
      birthDate
      address
      active
    }
  }
`;

export const GET_MY_DISABILITIES = gql`
  query GetMyDisabilities {
    myDisabilities {
      id
      name
      description
      currentLevel
    }
  }
`;

export const GET_MY_PROGRESS = gql`
  query GetMyProgress {
    myProgress {
      totalSessions
      averageScore
      improvementRate
      lastSessionDate
    }
  }
`;
```

#### `src/services/graphql/queries/appointments.ts`

```typescript
import { gql } from '@apollo/client';

export const GET_MY_APPOINTMENTS = gql`
  query GetMyAppointments($status: AppointmentStatus, $upcoming: Boolean) {
    myAppointments(status: $status, upcoming: $upcoming) {
      id
      date
      time
      practitionerName
      practitionerSpecialty
      status
      notes
    }
  }
`;
```

#### `src/services/graphql/queries/games.ts`

```typescript
import { gql } from '@apollo/client';

export const GET_MY_GAME_SESSIONS = gql`
  query GetMyGameSessions($limit: Int, $offset: Int) {
    myGameSessions(limit: $limit, offset: $offset) {
      id
      gameName
      playedAt
      score
      duration
      metrics {
        accuracy
        reactionTime
        completionRate
      }
    }
  }
`;
```

#### `src/services/graphql/queries/treatments.ts`

```typescript
import { gql } from '@apollo/client';

export const GET_MY_TREATMENTS = gql`
  query GetMyTreatments($disabilityId: ID, $level: Int) {
    myTreatments(disabilityId: $disabilityId, level: $level) {
      id
      name
      description
      type
      visible
      progressionLevel
    }
  }
`;
```

### 5.3 Mutations

#### `src/services/graphql/mutations/auth.ts`

```typescript
import { gql } from '@apollo/client';

export const LOGIN_MUTATION = gql`
  mutation Login($identifier: String!, $password: String!) {
    login(identifier: $identifier, password: $password) {
      accessToken
      refreshToken
      expiresAt
    }
  }
`;

export const REFRESH_TOKEN_MUTATION = gql`
  mutation RefreshToken($refreshToken: String!) {
    refreshToken(refreshToken: $refreshToken) {
      accessToken
      refreshToken
      expiresAt
    }
  }
`;
```

#### `src/services/graphql/mutations/appointments.ts`

```typescript
import { gql } from '@apollo/client';

export const BOOK_APPOINTMENT = gql`
  mutation BookAppointment($date: String!, $time: String!, $practitionerId: ID!) {
    bookAppointment(date: $date, time: $time, practitionerId: $practitionerId) {
      id
      date
      time
      practitionerName
      practitionerSpecialty
      status
      notes
    }
  }
`;

export const CANCEL_APPOINTMENT = gql`
  mutation CancelAppointment($appointmentId: ID!) {
    cancelAppointment(appointmentId: $appointmentId) {
      id
      status
    }
  }
`;
```

---

## Step 6: Error Handling System

### `src/utils/errorHandler.ts`

```typescript
import type { AppError, ErrorCode } from '../types/errors';

// Mapa de codigos de error a mensajes legibles en castellano
const ERROR_MESSAGES: Record<ErrorCode, { subtitle: string; message: string }> = {
  INVALID_CREDENTIALS: {
    subtitle: 'Credenciales invalidas',
    message: 'El DNI/correo o la contrasena introducidos no son correctos. Por favor, intentelo de nuevo.',
  },
  ACCOUNT_DEACTIVATED: {
    subtitle: 'Cuenta desactivada',
    message: 'Su cuenta ha sido desactivada. Contacte con su centro de rehabilitacion para mas informacion.',
  },
  TOKEN_EXPIRED: {
    subtitle: 'Sesion expirada',
    message: 'Su sesion ha expirado. Por favor, inicie sesion de nuevo.',
  },
  TOKEN_INVALID: {
    subtitle: 'Token invalido',
    message: 'Se ha producido un error de autenticacion. Por favor, inicie sesion de nuevo.',
  },
  NETWORK_ERROR: {
    subtitle: 'Error de conexion',
    message: 'No se ha podido conectar con el servidor. Compruebe su conexion a internet e intentelo de nuevo.',
  },
  PATIENT_NOT_FOUND: {
    subtitle: 'Paciente no encontrado',
    message: 'No se ha encontrado el perfil del paciente. Contacte con su centro de rehabilitacion.',
  },
  APPOINTMENT_CONFLICT: {
    subtitle: 'Conflicto de cita',
    message: 'El horario seleccionado ya esta ocupado. Por favor, elija otro horario.',
  },
  APPOINTMENT_NOT_FOUND: {
    subtitle: 'Cita no encontrada',
    message: 'La cita solicitada no existe o ha sido eliminada.',
  },
  VALIDATION_ERROR: {
    subtitle: 'Datos invalidos',
    message: 'Los datos introducidos no son validos. Por favor, revise los campos e intentelo de nuevo.',
  },
  INTERNAL_ERROR: {
    subtitle: 'Error interno',
    message: 'Se ha producido un error inesperado. Por favor, intentelo de nuevo mas tarde.',
  },
};

// Convierte un error GraphQL en un AppError estructurado
export function parseGraphQLError(error: unknown): AppError {
  // Intentar extraer el codigo del error GraphQL
  if (error && typeof error === 'object' && 'graphQLErrors' in error) {
    const gqlError = (error as any).graphQLErrors?.[0];
    if (gqlError?.extensions?.code) {
      const code = gqlError.extensions.code as ErrorCode;
      const mapped = ERROR_MESSAGES[code];
      if (mapped) {
        return {
          title: 'Error',
          subtitle: mapped.subtitle,
          message: mapped.message,
          code: code,
        };
      }
    }
  }

  // Error de red
  if (error && typeof error === 'object' && 'networkError' in error) {
    return {
      title: 'Error',
      ...ERROR_MESSAGES.NETWORK_ERROR,
      code: 'NETWORK_ERROR',
    };
  }

  // Error generico
  return {
    title: 'Error',
    ...ERROR_MESSAGES.INTERNAL_ERROR,
    code: 'INTERNAL_ERROR',
  };
}
```

### `src/components/ErrorPopup.tsx`

```typescript
import { Modal, View, Text, Pressable } from 'react-native';
import type { ErrorPopupProps } from '../types/errors';

// Ventana emergente de error con titulo, subtitulo, descripcion y botones
export function ErrorPopup(props: ErrorPopupProps) {
  const { error, visible, onAccept, onCancel } = props;

  if (!error) return null;

  return (
    <Modal
      transparent={true}
      visible={visible}
      animationType="fade"
      onRequestClose={onCancel}
    >
      <View className="flex-1 justify-center items-center bg-black/50 px-6">
        <View className="bg-surface rounded-2xl p-6 w-full max-w-sm shadow-lg">
          {/* Titulo */}
          <Text className="text-xl font-bold text-text-primary mb-1">
            {error.title}
          </Text>

          {/* Subtitulo */}
          <Text className="text-base font-medium text-error mb-3">
            {error.subtitle}
          </Text>

          {/* Descripcion */}
          <Text className="text-base text-text-secondary mb-6 leading-6">
            {error.message}
          </Text>

          {/* Botones alineados a la derecha */}
          <View className="flex-row justify-end gap-3">
            <Pressable
              onPress={onCancel}
              className="min-h-12 min-w-12 px-5 py-3 rounded-lg border border-primary-600 justify-center items-center"
            >
              <Text className="text-primary-600 font-medium text-base">
                Cancelar
              </Text>
            </Pressable>

            <Pressable
              onPress={onAccept}
              className="min-h-12 min-w-12 px-5 py-3 rounded-lg bg-primary-600 justify-center items-center"
            >
              <Text className="text-white font-medium text-base">
                Aceptar
              </Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}
```

**Layout specification:**
- Modal overlay: semi-transparent black background (`bg-black/50`)
- White card centered with rounded corners and padding
- Title: bold, large, dark text
- Subtitle: medium weight, error color (red)
- Description: regular weight, secondary color
- Buttons: aligned `flex-row justify-end`, with gap between them
- "Cancelar" is outlined (border only), "Aceptar" is filled (primary blue)
- Both buttons have min 48x48dp touch targets

---

## Step 7: Root Layout and Auth Flow

### `app/_layout.tsx` — Root Layout

```typescript
import '../global.css';
import { useEffect } from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ApolloProvider } from '@apollo/client';
import { Slot, useRouter, useSegments } from 'expo-router';
import { client } from '../src/services/graphql/client';
import { useAuthStore } from '../src/store/authStore';
import { ErrorPopup } from '../src/components/ErrorPopup';
import { useErrorStore } from '../src/store/errorStore';
import * as SecureStore from 'expo-secure-store';

const TOKEN_KEY = 'auth_token';

// Componente que gestiona la redireccion segun el estado de autenticacion
function AuthGuard() {
  const segments = useSegments();
  const router = useRouter();
  const isAuthenticated = useAuthStore(function (state) {
    return state.isAuthenticated;
  });
  const isLoading = useAuthStore(function (state) {
    return state.isLoading;
  });

  useEffect(function () {
    if (isLoading) return;

    const inAuthGroup = segments[0] === '(auth)';

    if (!isAuthenticated && !inAuthGroup) {
      router.replace('/(auth)/login');
    } else if (isAuthenticated && inAuthGroup) {
      router.replace('/(tabs)');
    }
  }, [isAuthenticated, isLoading, segments]);

  return <Slot />;
}

// Componente de error global
function GlobalErrorPopup() {
  const currentError = useErrorStore(function (s) { return s.currentError; });
  const isVisible = useErrorStore(function (s) { return s.isVisible; });
  const hideError = useErrorStore(function (s) { return s.hideError; });

  return (
    <ErrorPopup
      error={currentError!}
      visible={isVisible}
      onAccept={hideError}
      onCancel={hideError}
    />
  );
}

export default function RootLayout() {
  const setAuth = useAuthStore.setState;

  // Comprobar token almacenado al iniciar la app
  useEffect(function () {
    async function checkStoredToken() {
      try {
        const tokenJson = await SecureStore.getItemAsync(TOKEN_KEY);
        if (tokenJson) {
          const token = JSON.parse(tokenJson);
          const now = Math.floor(Date.now() / 1000);
          if (token.expiresAt > now) {
            setAuth({ token, isAuthenticated: true, isLoading: false });
            return;
          }
        }
      } catch (err) {
        console.error('Error al recuperar el token almacenado:', err);
      }
      setAuth({ token: null, isAuthenticated: false, isLoading: false });
    }
    checkStoredToken();
  }, []);

  return (
    <ApolloProvider client={client}>
      <SafeAreaProvider>
        <AuthGuard />
        <GlobalErrorPopup />
      </SafeAreaProvider>
    </ApolloProvider>
  );
}
```

### `app/(auth)/_layout.tsx`

```typescript
import { Stack } from 'expo-router';

// Layout simple para las pantallas de autenticacion
export default function AuthLayout() {
  return (
    <Stack screenOptions={{ headerShown: false }} />
  );
}
```

### `app/(tabs)/_layout.tsx`

```typescript
import { Tabs } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';

// Mapa de iconos para cada pestana
type TabIconName = keyof typeof Ionicons.glyphMap;

type TabConfig = {
  name: string;
  title: string;
  icon: TabIconName;
  iconFocused: TabIconName;
};

const TAB_CONFIG: TabConfig[] = [
  { name: 'index', title: 'Inicio', icon: 'home-outline', iconFocused: 'home' },
  { name: 'profile', title: 'Perfil', icon: 'person-outline', iconFocused: 'person' },
  { name: 'settings', title: 'Ajustes', icon: 'settings-outline', iconFocused: 'settings' },
  { name: 'appointments', title: 'Citas', icon: 'calendar-outline', iconFocused: 'calendar' },
  { name: 'progress', title: 'Progreso', icon: 'bar-chart-outline', iconFocused: 'bar-chart' },
  { name: 'games', title: 'Juegos', icon: 'game-controller-outline', iconFocused: 'game-controller' },
  { name: 'treatments', title: 'Tratamientos', icon: 'medkit-outline', iconFocused: 'medkit' },
];

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: '#2563EB',
        tabBarInactiveTintColor: '#64748B',
        tabBarStyle: {
          backgroundColor: '#FFFFFF',
          borderTopWidth: 1,
          borderTopColor: '#E2E8F0',
          height: 60,
          paddingBottom: 8,
          paddingTop: 4,
        },
        tabBarLabelStyle: {
          fontSize: 11,
          fontWeight: '500',
        },
        headerShown: true,
        headerStyle: {
          backgroundColor: '#FFFFFF',
          elevation: 2,
          shadowColor: '#000',
          shadowOffset: { width: 0, height: 1 },
          shadowOpacity: 0.1,
          shadowRadius: 2,
        },
        headerTintColor: '#1E293B',
        headerTitleStyle: {
          fontWeight: '600',
          fontSize: 18,
        },
      }}
    >
      {TAB_CONFIG.map(function (tab) {
        return (
          <Tabs.Screen
            key={tab.name}
            name={tab.name}
            options={{
              title: tab.title,
              tabBarIcon: function ({ focused, color, size }) {
                const iconName = focused ? tab.iconFocused : tab.icon;
                return <Ionicons name={iconName} size={size} color={color} />;
              },
            }}
          />
        );
      })}
    </Tabs>
  );
}
```

**Implementation notes:**
- The `Tabs` component from Expo Router handles the bottom tab bar.
- The header (top nav bar) is built into the tab navigator via `headerShown: true`.
- The header displays the current tab title automatically.
- White background, primary-blue active state, gray inactive state.
- Tab bar height 60dp with padding for comfortable touch targets.

---

## Step 8: Login Screen

### `app/(auth)/login.tsx`

```typescript
import { useState } from 'react';
import { View, Text, TextInput, Pressable, Image, ActivityIndicator, KeyboardAvoidingView, Platform, ScrollView } from 'react-native';
import { useAuthStore } from '../../src/store/authStore';
import { useUserStore } from '../../src/store/userStore';
import { useErrorStore } from '../../src/store/errorStore';
import type { LoginCredentials } from '../../src/types/auth';

// Pantalla de inicio de sesion
export default function LoginScreen() {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const login = useAuthStore(function (s) { return s.login; });
  const isLoading = useAuthStore(function (s) { return s.isLoading; });
  const fetchProfile = useUserStore(function (s) { return s.fetchProfile; });
  const showError = useErrorStore(function (s) { return s.showError; });

  async function handleLogin() {
    if (!identifier.trim() || !password.trim()) return;

    const credentials: LoginCredentials = {
      identifier: identifier.trim(),
      password: password,
    };

    try {
      await login(credentials);
      await fetchProfile();
      // La navegacion se gestiona automaticamente por el AuthGuard
    } catch (err: any) {
      showError(err);
    }
  }

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      className="flex-1 bg-background"
    >
      <ScrollView
        contentContainerStyle={{ flexGrow: 1 }}
        keyboardShouldPersistTaps="handled"
      >
        <View className="flex-1 justify-center items-center px-8">
          {/* Logotipo */}
          <Image
            source={require('../../assets/rehabiapp-logo.png')}
            className="w-48 h-48 mb-10"
            resizeMode="contain"
          />

          {/* Campo: DNI o correo electronico */}
          <View className="w-full mb-4">
            <Text className="text-text-secondary text-sm mb-1 ml-1">
              DNI / Gmail
            </Text>
            <TextInput
              value={identifier}
              onChangeText={setIdentifier}
              placeholder="12345678A o correo@gmail.com"
              autoCapitalize="none"
              keyboardType="email-address"
              className="w-full min-h-12 bg-surface border border-primary-200 rounded-xl px-4 py-3 text-base text-text-primary"
            />
          </View>

          {/* Campo: Contrasena */}
          <View className="w-full mb-8">
            <Text className="text-text-secondary text-sm mb-1 ml-1">
              Contrasena
            </Text>
            <TextInput
              value={password}
              onChangeText={setPassword}
              placeholder="Introduce tu contrasena"
              secureTextEntry={true}
              className="w-full min-h-12 bg-surface border border-primary-200 rounded-xl px-4 py-3 text-base text-text-primary"
            />
          </View>

          {/* Boton de inicio de sesion */}
          <Pressable
            onPress={handleLogin}
            disabled={isLoading}
            className="w-full min-h-12 bg-primary-600 rounded-xl py-4 items-center justify-center"
          >
            {isLoading ? (
              <ActivityIndicator color="#FFFFFF" />
            ) : (
              <Text className="text-white text-base font-semibold">
                Iniciar Sesion
              </Text>
            )}
          </Pressable>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
```

**Implementation notes:**
- No register button. No "forgot password" link.
- Logo at top center, inputs below, full-width button at bottom.
- KeyboardAvoidingView wraps everything for proper keyboard handling.
- On submit: calls `login()` from auth store, then `fetchProfile()` from user store.
- On error: shows the ErrorPopup via `showError()`.
- Loading state: replaces button text with `ActivityIndicator` while request is in flight.
- The `AuthGuard` in root layout handles navigation to `(tabs)` automatically after `isAuthenticated` becomes `true`.

---

## Step 9: Home Screen (Balloon UI)

### `src/utils/greeting.ts`

```typescript
// Genera el saludo basado en la hora local del dispositivo
export function getGreeting(name: string): string {
  const hours = new Date().getHours();
  const timeGreeting = hours < 12 ? 'Buenos dias' : 'Buenas tardes';
  return `${timeGreeting} ${name}, vamos a empezar la rutina :)`;
}
```

### `src/components/FloatingBalloon.tsx`

```typescript
import { Pressable } from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withSequence,
  withTiming,
  withDelay,
  Easing,
} from 'react-native-reanimated';
import { useEffect } from 'react';
import { Ionicons } from '@expo/vector-icons';

type FloatingBalloonProps = {
  iconName: keyof typeof Ionicons.glyphMap;
  size: number;
  positionX: number;        // Porcentaje horizontal (0-100)
  positionY: number;        // Porcentaje vertical (0-100)
  animationDelay: number;   // Milisegundos de retardo para efecto organico
  animationDuration: number; // Duracion del ciclo en milisegundos
  onPress: () => void;
};

const AnimatedPressable = Animated.createAnimatedComponent(Pressable);

// Globo flotante con animacion continua de oscilacion
export function FloatingBalloon(props: FloatingBalloonProps) {
  const {
    iconName,
    size,
    positionX,
    positionY,
    animationDelay,
    animationDuration,
    onPress,
  } = props;

  const translateY = useSharedValue(0);

  useEffect(function () {
    // Oscilacion continua arriba y abajo
    translateY.value = withDelay(
      animationDelay,
      withRepeat(
        withSequence(
          withTiming(-12, { duration: animationDuration, easing: Easing.inOut(Easing.ease) }),
          withTiming(12, { duration: animationDuration, easing: Easing.inOut(Easing.ease) }),
        ),
        -1,  // Repetir indefinidamente
        true // Invertir en cada repeticion
      )
    );
  }, []);

  const animatedStyle = useAnimatedStyle(function () {
    return {
      transform: [{ translateY: translateY.value }],
    };
  });

  return (
    <AnimatedPressable
      onPress={onPress}
      style={[
        {
          position: 'absolute',
          left: `${positionX}%`,
          top: `${positionY}%`,
        },
        animatedStyle,
      ]}
      className="min-h-12 min-w-12 w-20 h-20 rounded-full bg-surface items-center justify-center shadow-lg"
    >
      <Ionicons name={iconName} size={size} color="#2563EB" />
    </AnimatedPressable>
  );
}
```

### `app/(tabs)/index.tsx` — Home Screen

```typescript
import { View, Text } from 'react-native';
import { useRouter } from 'expo-router';
import { useUserStore } from '../../src/store/userStore';
import { FloatingBalloon } from '../../src/components/FloatingBalloon';
import { getGreeting } from '../../src/utils/greeting';

// Configuracion de posiciones y animaciones de los globos
// Las posiciones simulan un desorden organico pero equilibrado
type BalloonConfig = {
  id: string;
  iconName: string;
  route: string;
  positionX: number;
  positionY: number;
  animationDelay: number;
  animationDuration: number;
};

const BALLOON_CONFIG: BalloonConfig[] = [
  {
    id: 'profile',
    iconName: 'person-circle-outline',
    route: '/(tabs)/profile',
    positionX: 38,    // Centro
    positionY: 35,
    animationDelay: 0,
    animationDuration: 2800,
  },
  {
    id: 'settings',
    iconName: 'settings-outline',
    route: '/(tabs)/settings',
    positionX: 12,
    positionY: 18,
    animationDelay: 400,
    animationDuration: 3200,
  },
  {
    id: 'appointments',
    iconName: 'calendar-outline',
    route: '/(tabs)/appointments',
    positionX: 65,
    positionY: 15,
    animationDelay: 200,
    animationDuration: 2600,
  },
  {
    id: 'progress',
    iconName: 'bar-chart-outline',
    route: '/(tabs)/progress',
    positionX: 8,
    positionY: 55,
    animationDelay: 600,
    animationDuration: 3000,
  },
  {
    id: 'games',
    iconName: 'game-controller-outline',
    route: '/(tabs)/games',
    positionX: 62,
    positionY: 58,
    animationDelay: 300,
    animationDuration: 2400,
  },
  {
    id: 'treatments',
    iconName: 'medkit-outline',
    route: '/(tabs)/treatments',
    positionX: 35,
    positionY: 72,
    animationDelay: 500,
    animationDuration: 3400,
  },
];

export default function HomeScreen() {
  const router = useRouter();
  const patient = useUserStore(function (s) { return s.patient; });
  const patientName = patient?.name ?? 'Paciente';

  function handleBalloonPress(route: string) {
    router.push(route as any);
  }

  return (
    <View className="flex-1 bg-background">
      {/* Mensaje de bienvenida */}
      <View className="px-6 pt-6 pb-4">
        <Text className="text-2xl font-bold text-text-primary leading-8">
          {getGreeting(patientName)}
        </Text>
      </View>

      {/* Area de globos flotantes */}
      <View className="flex-1 relative mx-4 mb-4">
        {BALLOON_CONFIG.map(function (balloon) {
          return (
            <FloatingBalloon
              key={balloon.id}
              iconName={balloon.iconName as any}
              size={32}
              positionX={balloon.positionX}
              positionY={balloon.positionY}
              animationDelay={balloon.animationDelay}
              animationDuration={balloon.animationDuration}
              onPress={function () { handleBalloonPress(balloon.route); }}
            />
          );
        })}
      </View>
    </View>
  );
}
```

**Implementation notes:**
- NO text labels on balloons. Icons only.
- Each balloon has different animation timing for organic floating effect.
- Positions are absolute percentages creating a scattered, non-grid layout.
- Profile balloon is centered and most prominent.
- The greeting text checks device local time using `new Date().getHours()`.
- Tapping a balloon navigates to the corresponding tab.

---

## Step 10: Placeholder Screens

All remaining tab screens follow the same minimal structure. Each displays the tab name centered on screen. They will be implemented in future phases.

### Template for all placeholder screens

Use this template for: `profile.tsx`, `settings.tsx`, `appointments.tsx`, `progress.tsx`, `games.tsx`, `treatments.tsx`

```typescript
import { View, Text } from 'react-native';

// Pantalla de [NOMBRE] — pendiente de implementacion
export default function [Name]Screen() {
  return (
    <View className="flex-1 bg-background justify-center items-center">
      <Text className="text-xl text-text-secondary">
        Pantalla de [Nombre]
      </Text>
    </View>
  );
}
```

Replace `[Name]` and `[Nombre]` accordingly:

| File | Function name | Display text |
|------|--------------|--------------|
| `profile.tsx` | `ProfileScreen` | `Pantalla de Perfil` |
| `settings.tsx` | `SettingsScreen` | `Pantalla de Ajustes` |
| `appointments.tsx` | `AppointmentsScreen` | `Pantalla de Citas` |
| `progress.tsx` | `ProgressScreen` | `Pantalla de Progreso` |
| `games.tsx` | `GamesScreen` | `Pantalla de Juegos` |
| `treatments.tsx` | `TreatmentsScreen` | `Pantalla de Tratamientos` |

---

## Verification Checklist (for the implementing agent)

Before marking any step as complete, verify:

- [ ] No arrow functions used for function declarations (only conventional `function`)
- [ ] No inline types in function parameters (all reference named types from `src/types/`)
- [ ] No `fetch` or `axios` calls anywhere (only Apollo Client)
- [ ] All comments written in Spanish
- [ ] All touch targets are at least 48x48dp
- [ ] Error popup follows exact layout: title, subtitle, message, right-aligned buttons
- [ ] Color values match the palette in Step 2
- [ ] All GraphQL queries/mutations match the schema in IMPORTANT.md
