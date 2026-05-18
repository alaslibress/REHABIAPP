import { createContext, useContext } from 'react';
import { lightTheme, darkTheme, type Theme } from '../theme/tokens';

export type ThemeMode = 'light' | 'dark' | 'system';
export type ThemeScheme = 'light' | 'dark';

// Contexto extendido: mantiene scheme/mode/setMode (compatibilidad hacia atras)
// y añade theme — objeto Theme resuelto en funcion del scheme actual.
export const ThemeContext = createContext<{
  scheme: ThemeScheme;
  mode: ThemeMode;
  setMode: (m: ThemeMode) => void;
  theme: Theme;
}>({
  scheme: 'light',
  mode: 'system',
  setMode: () => {},
  theme: lightTheme,
});

export function useTheme() {
  return useContext(ThemeContext);
}

// Helper exportado para que el provider de _layout pueda resolver el Theme
// sin duplicar logica de seleccion light/dark.
export function resolveTheme(scheme: ThemeScheme): Theme {
  return scheme === 'dark' ? darkTheme : lightTheme;
}
