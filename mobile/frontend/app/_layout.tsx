import '../global.css';
import { useEffect, useState, useMemo } from 'react';
import { View, Appearance, type ColorSchemeName } from 'react-native';
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
import { ThemeContext, type ThemeMode, type ThemeScheme } from '../src/utils/theme';
import { FontScaleContext, type FontScale } from '../src/utils/fontScale';
import { initNotifications } from '../src/utils/notifications';

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

  const value = useMemo(function () {
    return {
      scheme,
      mode: themeMode as ThemeMode,
      setMode: setThemeMode,
    };
  }, [scheme, themeMode, setThemeMode]);

  // La clase "dark" en el View raiz activa los tokens dark: de NativeWind
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
    initNotifications();
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
