import type { AppError } from './errors';

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
