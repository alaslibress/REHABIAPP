# PLAN.md - Correccion del Flujo de Login (Mobile Frontend)

> **Fecha:** 2026-04-05
> **Ambito:** /mobile/frontend (exclusivamente)
> **Backend (BFF):** NO requiere cambios. 16 tests pasan, JWT correcto, errores bien formateados.
> **Objetivo:** Corregir los bugs que causan que el login "no haga nada" (ni navega, ni muestra error).

---

## 1. DIAGNOSTICO

### Resumen del problema

El usuario pulsa "Iniciar Sesion" y no ocurre nada: ni navega a las pestanas, ni muestra un error.
La causa raiz es una **condicion de carrera entre `authStore.login()` y `fetchProfile()`** combinada
con **errores de `fetchProfile()` que no se parsean correctamente**.

### Bug 1: Race condition — `isAuthenticated` se pone a `true` ANTES de que `fetchProfile()` termine

**Archivo:** `/mobile/frontend/src/store/authStore.ts`, linea 34
**Archivo:** `/mobile/frontend/app/(auth)/login.tsx`, lineas 38-41

El flujo actual es:
```
1. login() -> set({ isAuthenticated: true })     [linea 34 de authStore.ts]
2. AuthGuard detecta isAuthenticated=true         [_layout.tsx linea 32]
3. AuthGuard ejecuta router.replace('/(tabs)')    [_layout.tsx linea 33]
4. fetchProfile() se ejecuta DESPUES              [login.tsx linea 40]
```

**Problema:** En el paso 2, AuthGuard reacciona al cambio de `isAuthenticated` y navega a `/(tabs)`
MIENTRAS `login.tsx` todavia esta ejecutando `fetchProfile()`. Hay dos resultados posibles:
- `fetchProfile()` se ejecuta contra un componente ya desmontado (memory leak, warnings).
- La pantalla de inicio (`/(tabs)/index.tsx`) muestra "Paciente" porque el perfil aun no se ha cargado.
- Si `fetchProfile()` falla, el error se pierde porque el componente `login.tsx` ya no existe.

**La solucion es sencilla:** Eliminar `fetchProfile()` del flujo de login. El perfil se debe cargar
en la pantalla de inicio (`/(tabs)/index.tsx`) DESPUES de que la navegacion se haya completado.
Esto elimina la race condition por completo.

### Bug 2: `fetchProfile()` lanza errores crudos de Apollo, no `AppError`

**Archivo:** `/mobile/frontend/src/store/userStore.ts`, lineas 16-18

```typescript
} catch (err) {
  set({ isLoading: false });
  throw err;   // <-- Lanza el error de Apollo sin parsear
}
```

El `catch` en `login.tsx` (linea 46) comprueba si el error tiene `title` y `subtitle`:
```typescript
if (err && typeof err === 'object' && 'title' in err && 'subtitle' in err) {
```

Pero el error de Apollo NO tiene esas propiedades. Es un `ApolloError` con `graphQLErrors` y
`networkError`. Por tanto, el `catch` cae al `else` (linea 49) y ejecuta `showError(parseGraphQLError(err))`.

Esto deberia funcionar EXCEPTO que hay un problema sutil: si el componente ya se ha desmontado
(por el Bug 1), el popup puede no mostrarse porque React no garantiza actualizaciones de estado
en componentes desmontados.

**La solucion:** Parsear el error en `userStore.ts` antes de lanzarlo, para que el error sea
consistente. Pero como la solucion del Bug 1 elimina `fetchProfile()` del login, este bug
solo necesita corregirse para uso futuro (cuando `fetchProfile` se llame desde `/(tabs)/index.tsx`).

### Bug 3: La pantalla de inicio no carga el perfil del paciente

**Archivo:** `/mobile/frontend/app/(tabs)/index.tsx`

Actualmente, `index.tsx` lee `patient` del `userStore` (linea 78) pero NUNCA llama a `fetchProfile()`.
Si se elimina `fetchProfile()` del login (solucion del Bug 1), necesitamos que la pantalla de inicio
lo llame al montarse.

### Resumen de cambios necesarios

| # | Archivo | Cambio | Motivo |
|---|---------|--------|--------|
| 1 | `app/(auth)/login.tsx` | Eliminar llamada a `fetchProfile()` y simplificar `catch` | Eliminar race condition |
| 2 | `src/store/userStore.ts` | Parsear errores de Apollo con `parseGraphQLError()` | Consistencia de errores |
| 3 | `app/(tabs)/index.tsx` | Cargar perfil al montarse con `useEffect` + manejar errores | Cargar perfil despues de navegar |

---

## 2. CORRECCION POR ARCHIVO

### Archivo 1: `/mobile/frontend/app/(auth)/login.tsx`

**Objetivo:** Simplificar el `handleLogin` — solo hacer login, sin fetchProfile. AuthGuard se encarga
de la navegacion automaticamente cuando `isAuthenticated` cambia a `true`.

#### ANTES (lineas 1-53, funcion completa del componente):

```typescript
import { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  Pressable,
  Image,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
} from 'react-native';
import { useAuthStore } from '../../src/store/authStore';
import { useUserStore } from '../../src/store/userStore';
import { useErrorStore } from '../../src/store/errorStore';
import { parseGraphQLError } from '../../src/utils/errorHandler';
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
      // login() ya muestra el popup si hay error — si llega aqui, el login fue exitoso
      await fetchProfile();
      // La navegacion se gestiona automaticamente por el AuthGuard en el root layout
    } catch (err: any) {
      // authStore.login() ya mostro el popup para errores de autenticacion.
      // Este catch cubre errores de fetchProfile() (el usuario autenticado pero el perfil no cargo).
      // En ambos casos garantizamos que el popup se muestra con un AppError valido.
      if (err && typeof err === 'object' && 'title' in err && 'subtitle' in err) {
        // Ya es un AppError (lanzado por authStore) — el popup ya se mostro, no hacer nada
      } else {
        // Error inesperado de fetchProfile u otro — mostrarlo como popup
        showError(parseGraphQLError(err));
      }
    }
  }
```

#### DESPUES (reemplazo completo de los imports y la funcion):

```typescript
import { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  Pressable,
  Image,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
} from 'react-native';
import { useAuthStore } from '../../src/store/authStore';
import type { LoginCredentials } from '../../src/types/auth';

// Pantalla de inicio de sesion
export default function LoginScreen() {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');

  const login = useAuthStore(function (s) { return s.login; });
  const isLoading = useAuthStore(function (s) { return s.isLoading; });

  async function handleLogin() {
    if (!identifier.trim() || !password.trim()) return;

    const credentials: LoginCredentials = {
      identifier: identifier.trim(),
      password: password,
    };

    try {
      await login(credentials);
      // Si login() tiene exito, authStore pone isAuthenticated=true.
      // AuthGuard (en _layout.tsx) detecta el cambio y navega a /(tabs) automaticamente.
      // Si login() falla, lanza un AppError y el catch lo gestiona abajo.
    } catch (_err) {
      // authStore.login() ya mostro el popup de error via useErrorStore.
      // No necesitamos hacer nada aqui — el popup ya es visible.
      // El catch existe solo para evitar un "unhandled promise rejection".
    }
  }
```

**Explicacion del cambio:**
- Se eliminan los imports de `useUserStore`, `useErrorStore` y `parseGraphQLError` (ya no se usan aqui).
- Se elimina la referencia a `fetchProfile` y `showError` en los hooks.
- El `handleLogin` solo llama a `login()`. Si falla, `authStore.login()` ya muestra el popup.
- El `catch` esta vacio porque `authStore.login()` ya gestiona la visualizacion del error.
- AuthGuard se encarga de la navegacion cuando `isAuthenticated` cambia a `true`.

**IMPORTANTE:** El JSX (return) del componente NO cambia. Solo cambian los imports, hooks y `handleLogin`.

---

### Archivo 2: `/mobile/frontend/src/store/userStore.ts`

**Objetivo:** Parsear errores de Apollo con `parseGraphQLError` antes de relanzarlos, para que
cualquier componente que llame a `fetchProfile()` reciba un `AppError` consistente.

#### ANTES (archivo completo):

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

#### DESPUES (archivo completo):

```typescript
import { create } from 'zustand';
import type { UserState } from '../types/user';
import { client } from '../services/graphql/client';
import { GET_MY_PROFILE } from '../services/graphql/queries/user';
import { parseGraphQLError } from '../utils/errorHandler';

export const useUserStore = create<UserState>(function (set) {
  return {
    patient: null,
    isLoading: false,

    fetchProfile: async function (): Promise<void> {
      set({ isLoading: true });
      try {
        const { data } = await client.query({
          query: GET_MY_PROFILE,
          fetchPolicy: 'network-only',
        });
        set({ patient: data.me, isLoading: false });
      } catch (err) {
        set({ isLoading: false });
        // Parsear el error de Apollo a un AppError consistente
        // para que los componentes que llamen a fetchProfile reciban un error estructurado
        throw parseGraphQLError(err);
      }
    },

    clearProfile: function (): void {
      set({ patient: null });
    },
  };
});
```

**Explicacion del cambio:**
- Se anade `import { parseGraphQLError }` para parsear errores de Apollo.
- En el `catch`, se lanza `parseGraphQLError(err)` en vez del error crudo.
- Se anade `fetchPolicy: 'network-only'` para forzar una peticion fresca al servidor
  (evita que Apollo devuelva datos cacheados de una sesion anterior).

---

### Archivo 3: `/mobile/frontend/app/(tabs)/index.tsx`

**Objetivo:** Cargar el perfil del paciente al montar la pantalla de inicio. Si falla, mostrar
el error en el popup global.

#### ANTES (lineas 1-6, imports):

```typescript
import { View, Text } from 'react-native';
import { useRouter } from 'expo-router';
import { useUserStore } from '../../src/store/userStore';
import { FloatingBalloon } from '../../src/components/FloatingBalloon';
import { getGreeting } from '../../src/utils/greeting';
```

#### DESPUES (imports ampliados):

```typescript
import { useEffect } from 'react';
import { View, Text } from 'react-native';
import { useRouter } from 'expo-router';
import { useUserStore } from '../../src/store/userStore';
import { useErrorStore } from '../../src/store/errorStore';
import { FloatingBalloon } from '../../src/components/FloatingBalloon';
import { getGreeting } from '../../src/utils/greeting';
```

#### ANTES (lineas 76-79, inicio del componente HomeScreen):

```typescript
export default function HomeScreen() {
  const router = useRouter();
  const patient = useUserStore(function (s) { return s.patient; });
  const patientName = patient?.name ?? 'Paciente';
```

#### DESPUES (inicio del componente con carga de perfil):

```typescript
export default function HomeScreen() {
  const router = useRouter();
  const patient = useUserStore(function (s) { return s.patient; });
  const fetchProfile = useUserStore(function (s) { return s.fetchProfile; });
  const showError = useErrorStore(function (s) { return s.showError; });
  const patientName = patient?.name ?? 'Paciente';

  // Cargar el perfil del paciente al montar la pantalla de inicio.
  // Se ejecuta una sola vez despues de que AuthGuard haya navegado aqui
  // (lo que garantiza que el token JWT ya esta almacenado en SecureStore).
  useEffect(function () {
    // Solo cargar si no tenemos el perfil aun (evita llamadas duplicadas)
    if (!patient) {
      fetchProfile().catch(function (err) {
        // fetchProfile ya parsea el error a AppError (via parseGraphQLError)
        showError(err);
      });
    }
  }, []);
```

**Explicacion del cambio:**
- Se anade `useEffect` en los imports de React.
- Se anade `useErrorStore` para mostrar errores en el popup global.
- Se obtienen `fetchProfile` y `showError` de sus respectivos stores.
- El `useEffect` llama a `fetchProfile()` solo si `patient` es `null`.
- Si falla, muestra el error en el popup global.
- El array de dependencias esta vacio `[]` para ejecutarse solo al montar el componente.
  Esto es intencional: queremos cargar el perfil una vez al entrar en la pantalla.

---

## 3. ORDEN DE IMPLEMENTACION

Aplicar los cambios en este orden exacto:

### Paso 1: `src/store/userStore.ts`
Primero corregir el store para que parsee errores correctamente. Esto no rompe nada existente.

### Paso 2: `app/(auth)/login.tsx`
Simplificar el login eliminando `fetchProfile()` y los imports no necesarios.

### Paso 3: `app/(tabs)/index.tsx`
Anadir la carga de perfil en la pantalla de inicio.

### Paso 4: Verificacion (ver seccion 4)

---

## 4. VERIFICACION

### Test manual desde el telefono o emulador

1. **Arrancar el BFF en modo mock:**
   ```bash
   cd /home/alaslibres/DAM/RehabiAPP/mobile/backend
   MOCK_API=true npm run dev
   ```

2. **Arrancar el frontend:**
   ```bash
   cd /home/alaslibres/DAM/RehabiAPP/mobile/frontend
   npx expo start
   ```

3. **Caso 1: Login exitoso**
   - Introducir credenciales validas (segun mock del BFF).
   - Esperado: Se navega a `/(tabs)` (pantalla de inicio con globos flotantes).
   - Esperado: El saludo muestra el nombre del paciente (no "Paciente").
   - Esperado: En la consola de Metro, se ve `[Apollo] URL del BFF resuelta: http://IP:3000/graphql`.

4. **Caso 2: Login con credenciales invalidas**
   - Introducir DNI incorrecto o contrasena incorrecta.
   - Esperado: Aparece el popup de error con titulo "Error" y descripcion legible.
   - Esperado: No se navega a ninguna otra pantalla.
   - Esperado: Al pulsar "Aceptar" o "Cancelar", el popup se cierra.

5. **Caso 3: Login con BFF apagado (error de red)**
   - Apagar el BFF y pulsar login.
   - Esperado: Aparece popup con "Error de conexion".

6. **Caso 4: Login exitoso pero fetchProfile falla**
   - Login con mock exitoso pero query `me` devuelve error (modificar mock temporalmente).
   - Esperado: Se navega a `/(tabs)`, aparece popup de error en la pantalla de inicio.
   - Esperado: El saludo muestra "Paciente" (nombre por defecto).

### Verificacion en consola Metro (logs)

Tras login exitoso, la consola debe mostrar:
```
[Apollo] URL del BFF resuelta: http://192.168.X.X:3000/graphql
```

Tras login fallido, debe mostrar:
```
[GraphQL] INVALID_CREDENTIALS | operacion="Login" | ...
[parseGraphQLError] Codigo de error recibido: INVALID_CREDENTIALS
```

---

## 5. ARCHIVOS QUE NO NECESITAN CAMBIOS

| Archivo | Motivo |
|---------|--------|
| `app/_layout.tsx` | AuthGuard funciona correctamente. Reacciona a `isAuthenticated` e `isLoading`. |
| `src/services/graphql/client.ts` | URL dinamica ya corregida en fase anterior. Apollo links correctos. |
| `src/types/errors.ts` | Tipos correctos, no necesitan cambios. |
| `src/utils/errorHandler.ts` | `parseGraphQLError` maneja todos los casos correctamente. |
| `src/store/errorStore.ts` | Store simple de popup, funciona correctamente. |
| `app/(tabs)/_layout.tsx` | Configuracion de tabs correcta, sin relacion con el bug. |
| **Todo `/mobile/backend/`** | **BFF sin bugs. 16 tests pasan. No tocar.** |

---

## 6. RESUMEN EJECUTIVO

El login falla silenciosamente porque:
1. `authStore.login()` pone `isAuthenticated: true` y AuthGuard navega inmediatamente.
2. `fetchProfile()` se ejecuta en `login.tsx` DESPUES de que AuthGuard ya inicio la navegacion.
3. Si `fetchProfile()` falla, el componente `login.tsx` puede estar desmontado y el error se pierde.

La solucion es mover `fetchProfile()` fuera del flujo de login:
- `login.tsx` solo hace login. Si falla, el popup ya lo muestra `authStore`.
- `(tabs)/index.tsx` carga el perfil al montarse (con manejo de errores).
- `userStore.ts` parsea errores de Apollo para consistencia.

Tres archivos modificados, cero funcionalidad nueva, cero cambios en el backend.
