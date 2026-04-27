import { create } from 'zustand';
import AsyncStorage from '@react-native-async-storage/async-storage';
import type { FontScale } from '../utils/fontScale';
import type { ThemeMode } from '../utils/theme';

const STORAGE_KEY = '@rehabiapp/settings';

type SettingsState = {
  themeMode: ThemeMode;
  fontScale: FontScale;
  notifAppointments: boolean;
  notifDoctorUpdates: boolean;
  hydrated: boolean;
  load: () => Promise<void>;
  setThemeMode: (m: ThemeMode) => Promise<void>;
  setFontScale: (s: FontScale) => Promise<void>;
  setNotifAppointments: (v: boolean) => Promise<void>;
  setNotifDoctorUpdates: (v: boolean) => Promise<void>;
};

// Persiste el estado actual en AsyncStorage
async function guardarEstado(estado: Partial<SettingsState>) {
  try {
    const actual = await AsyncStorage.getItem(STORAGE_KEY);
    const previo = actual ? JSON.parse(actual) : {};
    await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify({ ...previo, ...estado }));
  } catch {
    // Fallo silencioso — la app sigue funcionando con los valores en memoria
  }
}

export const useSettingsStore = create<SettingsState>(function (set, get) {
  return {
    themeMode: 'system',
    fontScale: 1.0,
    notifAppointments: true,
    notifDoctorUpdates: true,
    hydrated: false,

    // Carga los ajustes desde AsyncStorage al iniciar la app
    load: async function () {
      try {
        const json = await AsyncStorage.getItem(STORAGE_KEY);
        if (json) {
          const guardado = JSON.parse(json);
          set({
            themeMode: guardado.themeMode ?? 'system',
            fontScale: guardado.fontScale ?? 1.0,
            notifAppointments: guardado.notifAppointments ?? true,
            notifDoctorUpdates: guardado.notifDoctorUpdates ?? true,
          });
        }
      } catch {
        // Valores por defecto si AsyncStorage falla
      } finally {
        set({ hydrated: true });
      }
    },

    setThemeMode: async function (m: ThemeMode) {
      set({ themeMode: m });
      await guardarEstado({ themeMode: m });
    },

    setFontScale: async function (s: FontScale) {
      set({ fontScale: s });
      await guardarEstado({ fontScale: s });
    },

    setNotifAppointments: async function (v: boolean) {
      set({ notifAppointments: v });
      await guardarEstado({ notifAppointments: v });
    },

    setNotifDoctorUpdates: async function (v: boolean) {
      set({ notifDoctorUpdates: v });
      await guardarEstado({ notifDoctorUpdates: v });
    },
  };
});
