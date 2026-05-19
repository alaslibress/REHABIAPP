import '../global.css';
import { useEffect, useState, useMemo } from 'react';
import { View, Appearance, type ColorSchemeName } from 'react-native';
import * as SplashScreen from 'expo-splash-screen';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ApolloProvider } from '@apollo/client/react';
import { Slot, useRouter, useSegments } from 'expo-router';
import * as SecureStore from 'expo-secure-store';
import {
  useFonts,
  Inter_400Regular,
  Inter_500Medium,
  Inter_600SemiBold,
  Inter_700Bold,
} from '@expo-google-fonts/inter';
import { client } from '../src/services/graphql/client';
import { useAuthStore } from '../src/store/authStore';
import { useBootstrapStore } from '../src/store/bootstrapStore';
import { useSettingsStore } from '../src/store/settingsStore';
import { ErrorPopup } from '../src/components/ErrorPopup';
import { useErrorStore } from '../src/store/errorStore';
import { useColorScheme as useNwColorScheme } from 'nativewind';
import { ThemeContext, resolveTheme, type ThemeMode, type ThemeScheme } from '../src/utils/theme';
import { FontScaleContext, type FontScale } from '../src/utils/fontScale';
import { initNotifications, remotePushDisabled } from '../src/utils/notifications';

// Mantener el splash visible hasta que terminen las fuentes y el bootstrap.
// preventAutoHideAsync se llama en top-level para que se ejecute lo antes posible.
SplashScreen.preventAutoHideAsync().catch(() => {});

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

  if (!currentError) return null;

  return (
    <ErrorPopup
      error={currentError}
      visible={isVisible}
      onAccept={hideError}
      onCancel={hideError}
    />
  );
}

// Proveedor del tema — escucha el modo elegido por el usuario y el esquema del sistema
function ThemeProvider(props: { children: React.ReactNode }) {
  const themeMode = useSettingsStore(function (s) { return s.themeMode; });
  const setThemeMode = useSettingsStore(function (s) { return s.setThemeMode; });

  const [systemScheme, setSystemScheme] = useState<ColorSchemeName>(
    Appearance.getColorScheme()
  );

  useEffect(function () {
    const sub = Appearance.addChangeListener(function (pref) {
      setSystemScheme(pref.colorScheme);
    });
    return function () { sub.remove(); };
  }, []);

  const scheme: ThemeScheme = useMemo(function () {
    if (themeMode === 'dark') return 'dark';
    if (themeMode === 'light') return 'light';
    return systemScheme === 'dark' ? 'dark' : 'light';
  }, [themeMode, systemScheme]);

  // NativeWind v4 resuelve `dark:` SOLO via su propio useColorScheme — NO mira
  // la clase `dark` del View raiz. Hay que sincronizarlo cada vez que el usuario
  // cambia el tema en Ajustes, de lo contrario los componentes con `dark:bg-X`
  // siguen pintando los tokens light pese al provider de tema.
  const nwScheme = useNwColorScheme();
  useEffect(function () {
    nwScheme.setColorScheme(scheme);
  }, [scheme, nwScheme]);

  const value = useMemo(function () {
    return {
      scheme,
      mode: themeMode as ThemeMode,
      setMode: setThemeMode,
      theme: resolveTheme(scheme),
    };
  }, [scheme, themeMode, setThemeMode]);

  // La clase "dark" en el View raiz queda como redundancia (no la usa NW v4),
  // pero ayuda a inspectores y a CSS web.
  return (
    <ThemeContext.Provider value={value}>
      <View className={`flex-1 ${scheme === 'dark' ? 'dark' : ''}`}>
        {props.children}
      </View>
    </ThemeContext.Provider>
  );
}

// Proveedor de escala de fuente
function FontScaleProvider(props: { children: React.ReactNode }) {
  const fontScale = useSettingsStore(function (s) { return s.fontScale; });
  const setFontScale = useSettingsStore(function (s) { return s.setFontScale; });

  const value = useMemo(function () {
    return { scale: fontScale as FontScale, setScale: setFontScale };
  }, [fontScale, setFontScale]);

  return (
    <FontScaleContext.Provider value={value}>
      {props.children}
    </FontScaleContext.Provider>
  );
}

export default function RootLayout() {
  const setAuth = useAuthStore.setState;
  const settingsHydrated = useSettingsStore(function (s) { return s.hydrated; });
  const bootstrapHydrated = useBootstrapStore(function (s) { return s.hydrated; });

  // Cargar fuentes Inter
  const [fontsLoaded] = useFonts({
    Inter_400Regular,
    Inter_500Medium,
    Inter_600SemiBold,
    Inter_700Bold,
  });

  // Cargar ajustes persistidos antes de renderizar
  useEffect(function () {
    useSettingsStore.getState().load();

    // Listener de notificaciones en primer plano — hook para futuras integraciones.
    // Carga perezosa defensiva: evita romper Expo Go si el modulo no esta presente.
    let receivedSub: { remove: () => void } | undefined;
    if (!remotePushDisabled) {
      try {
        // eslint-disable-next-line @typescript-eslint/no-require-imports
        const N = require('expo-notifications') as typeof import('expo-notifications');
        receivedSub = N.addNotificationReceivedListener(function () {
          // Hook para badge counters, analytics, etc. en iteraciones futuras.
        });
      } catch {
        // Ignorar: entorno sin soporte
      }
    }

    initNotifications();

    return function () {
      receivedSub?.remove();
    };
  }, []);

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
            // Hidratar datos al recuperar sesion existente
            useBootstrapStore.getState().hydrate();
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

  // Ocultar el splash cuando fuentes, ajustes y bootstrap esten listos.
  // bootstrapHydrated sera false durante el arranque en frio y true en sesiones
  // sin token valido (donde hydrate() no se llama — el splash se oculta de todos
  // modos en cuanto fontsLoaded y settingsHydrated sean true).
  useEffect(function () {
    const authLoading = useAuthStore.getState().isLoading;
    // Si todavia hay token pendiente de verificar, esperamos al bootstrap.
    // Si no hay sesion (isLoading=false, no token), ocultamos sin esperar bootstrap.
    const listo = fontsLoaded && settingsHydrated && (!authLoading || bootstrapHydrated);
    if (listo) {
      SplashScreen.hideAsync().catch(() => {});
    }
  }, [fontsLoaded, settingsHydrated, bootstrapHydrated]);

  // Esperar fuentes y ajustes antes de renderizar para evitar parpadeo
  if (!fontsLoaded || !settingsHydrated) return null;

  return (
    <ApolloProvider client={client}>
      <SafeAreaProvider>
        <ThemeProvider>
          <FontScaleProvider>
            <AuthGuard />
            <GlobalErrorPopup />
          </FontScaleProvider>
        </ThemeProvider>
      </SafeAreaProvider>
    </ApolloProvider>
  );
}
