/** @type {import('tailwindcss').Config} */
// Paleta sincronizada con src/theme/tokens.ts (rebrand Claude Design 2026-05).
// Variantes oscuras viven bajo colors.dark.* y se usan con className `dark:bg-dark-elev1`.
module.exports = {
  content: ['./app/**/*.{js,ts,tsx}', './src/**/*.{js,ts,tsx}'],
  presets: [require('nativewind/preset')],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // Brand (escala completa)
        primary: {
          50: '#EEF3FF',
          100: '#DCE5FF',
          200: '#B8C9FF',
          300: '#8BA5FF',
          400: '#5B7FFF',
          500: '#3D63F2',
          600: '#2952E8',
          700: '#1E3FBE',
          800: '#18348F',
          900: '#18348F',
          DEFAULT: '#3D63F2',
          dark: '#2952E8',
          darker: '#1E3FBE',
        },
        // Superficies (light)
        surface: '#FFFFFF',
        'surface-dark': '#161E2C',
        bg: '#EEF3FA',
        'bg-dark': '#0E141F',
        background: '#EEF3FA',
        'background-dark': '#0E141F',
        // Texto (light)
        'text-primary': '#0F1B33',
        'text-primary-dark': '#E8ECF3',
        'text-secondary': '#3D4A63',
        'text-secondary-dark': '#A8B3C7',
        // Bordes
        border: '#DEE5EE',
        'border-dark': '#243149',
        // Semanticos
        success: '#2EA66A',
        warn: '#E8A33D',
        danger: '#E04848',
        error: '#E04848',
        info: '#2D8BE0',
        // Variantes dark agrupadas (uso opcional: dark.elev1, dark.text, etc.)
        dark: {
          bg: '#0E141F',
          elev1: '#161E2C',
          elev2: '#1C2536',
          sunken: '#0A0F18',
          border: '#243149',
          border2: '#2E3D58',
          divider: '#1B2434',
          text: '#E8ECF3',
          text2: '#A8B3C7',
          text3: '#7B8AA3',
          accent: '#5B7FFF',
          accent2: '#3D63F2',
        },
      },
      fontFamily: {
        sans: ['Inter_400Regular'],
        medium: ['Inter_500Medium'],
        semibold: ['Inter_600SemiBold'],
        bold: ['Inter_700Bold'],
      },
      borderRadius: { '2xl': '16px', '3xl': '24px' },
    },
  },
  plugins: [],
};
