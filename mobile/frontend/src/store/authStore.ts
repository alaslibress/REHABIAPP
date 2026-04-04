import { create } from 'zustand';
import * as SecureStore from 'expo-secure-store';
import type { AuthState, AuthToken, LoginCredentials } from '../types/auth';
import type { AppError } from '../types/errors';
import { client } from '../services/graphql/client';
import { LOGIN_MUTATION } from '../services/graphql/mutations/auth';
import { parseGraphQLError } from '../utils/errorHandler';

// Clave para almacenamiento seguro de tokens
const TOKEN_KEY = 'auth_token';

export const useAuthStore = create<AuthState>(function (set) {
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
      // Implementar con REFRESH_TOKEN_MUTATION cuando el backend este disponible
    },

    clearError: function (): void {
      set({ error: null });
    },
  };
});
