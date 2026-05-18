// Tokens de diseño visual extraidos del rebrand Claude Design (Mayo 2026).
// Estructura: palette base + lightTheme + darkTheme + alias Theme.
// Convencion: cualquier componente nuevo debe leer del Theme via useTheme().
// No hardcodear hex en componentes; añadir aqui si falta el token.

export const palette = {
  // Marca — derivada del logotipo
  brand50: '#EEF3FF',
  brand100: '#DCE5FF',
  brand200: '#B8C9FF',
  brand300: '#8BA5FF',
  brand400: '#5B7FFF',
  brand500: '#3D63F2',
  brand600: '#2952E8',
  brand700: '#1E3FBE',
  brand800: '#18348F',

  ok50: '#DCF5E7',
  ok500: '#2EA66A',
  ok700: '#1F7048',
  ok2: '#5DD49C',
  warn50: '#FEF3DC',
  warn500: '#E8A33D',
  warn700: '#A37020',
  warn2: '#F2C470',
  danger50: '#FEEAEA',
  danger500: '#E04848',
  danger700: '#A82828',
  danger2: '#F49494',
  info50: '#DDEFFF',
  info500: '#2D8BE0',
  info2: '#6DB8F0',
};

export const lightTheme = {
  mode: 'light' as 'light' | 'dark',
  // superficies
  bg: '#EEF3FA',
  elev1: '#FFFFFF',
  elev2: '#F4F7FC',
  sunken: '#E4EAF3',
  bubble: '#FFFFFF',
  // bordes / divisores
  border: '#DEE5EE',
  border2: '#C7D1DE',
  divider: '#ECF0F5',
  // texto
  text: '#0F1B33',
  text2: '#3D4A63',
  text3: '#6C7A91',
  textMuted: '#9AA4B5',
  // acentos
  accent: palette.brand500,
  accent2: palette.brand600,
  accent3: palette.brand700,
  accentSoft: '#EEF3FF',
  // diagrama corporal
  bodyFill: '#C7D1DE',
  bodyStroke: '#8C9AAD',
  bodyHover: '#B3BFD0',
  // sombras / overlays
  shadow: { color: '#0F1B33', opacity: 0.08, radius: 16, offset: { width: 0, height: 6 } },
  shadowStrong: { color: '#0F1B33', opacity: 0.18, radius: 36, offset: { width: 0, height: 14 } },
  backdrop: 'rgba(15, 27, 51, 0.32)',
  // semanticos
  ok: palette.ok700,
  warn: palette.warn700,
  danger: palette.danger700,
  info: palette.info500,
};

export const darkTheme = {
  ...lightTheme,
  mode: 'dark' as 'light' | 'dark',
  bg: '#0E141F',
  elev1: '#161E2C',
  elev2: '#1C2536',
  sunken: '#0A0F18',
  bubble: '#161E2C',
  border: '#243149',
  border2: '#2E3D58',
  divider: '#1B2434',
  text: '#E8ECF3',
  text2: '#A8B3C7',
  text3: '#7B8AA3',
  textMuted: '#5C6A82',
  accent: '#5B7FFF',
  accent2: palette.brand500,
  accent3: palette.brand600,
  accentSoft: 'rgba(91, 127, 255, 0.14)',
  bodyFill: '#2A3651',
  bodyStroke: '#4A5876',
  bodyHover: '#3B4868',
  shadow: { color: '#000000', opacity: 0.45, radius: 28, offset: { width: 0, height: 12 } },
  shadowStrong: { color: '#000000', opacity: 0.55, radius: 40, offset: { width: 0, height: 18 } },
  backdrop: 'rgba(8, 14, 26, 0.6)',
  ok: palette.ok2,
  warn: palette.warn2,
  danger: palette.danger2,
  info: palette.info2,
};

export type Theme = typeof lightTheme;
