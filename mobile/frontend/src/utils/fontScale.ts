import { createContext, useContext } from 'react';

export type FontScale = 0.9 | 1.0 | 1.15 | 1.3;

export const FontScaleContext = createContext<{
  scale: FontScale;
  setScale: (s: FontScale) => void;
}>({
  scale: 1.0,
  setScale: () => {},
});

export function useFontScale() {
  return useContext(FontScaleContext).scale;
}
