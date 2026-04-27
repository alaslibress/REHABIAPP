/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./app/**/*.{js,ts,tsx}', './src/**/*.{js,ts,tsx}'],
  presets: [require('nativewind/preset')],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#EFF6FF', 100: '#DBEAFE', 200: '#BFDBFE', 300: '#93C5FD',
          400: '#60A5FA', 500: '#3B82F6', 600: '#2563EB', 700: '#1D4ED8',
          800: '#1E40AF', 900: '#1E3A8A',
        },
        surface: '#FFFFFF',
        'surface-dark': '#111827',
        background: '#F0F4FF',
        'background-dark': '#0B1220',
        'text-primary': '#1E293B',
        'text-primary-dark': '#F1F5F9',
        'text-secondary': '#64748B',
        'text-secondary-dark': '#94A3B8',
        error: '#EF4444',
        success: '#22C55E',
        border: '#E2E8F0',
        'border-dark': '#1F2937',
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
