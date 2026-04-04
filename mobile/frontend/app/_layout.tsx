import '../global.css';
import { useEffect } from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ApolloProvider } from '@apollo/client/react';
import { Slot, useRouter, useSegments } from 'expo-router';
import * as SecureStore from 'expo-secure-store';
import { client } from '../src/services/graphql/client';
import { useAuthStore } from '../src/store/authStore';
import { ErrorPopup } from '../src/components/ErrorPopup';
import { useErrorStore } from '../src/store/errorStore';

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
