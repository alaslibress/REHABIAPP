import { createContext, useContext } from 'react';

export type ThemeMode = 'light' | 'dark' | 'system';
export type ThemeScheme = 'light' | 'dark';

export const ThemeContext = createContext<{
  scheme: ThemeScheme;
  mode: ThemeMode;
  setMode: (m: ThemeMode) => void;
}>({
  scheme: 'light',
  mode: 'system',
  setMode: () => {},
});

export function useTheme() {
  return useContext(ThemeContext);
}
