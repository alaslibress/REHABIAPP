import { create } from 'zustand';
import { useUserStore } from './userStore';
import { useAppointmentsStore } from './appointmentsStore';
import { useGamesStore } from './gamesStore';
import { useTreatmentsStore } from './treatmentsStore';
import { useProgressStore } from './progressStore';
import { scheduleAppointmentReminder } from '../utils/notifications';
import type { Appointment } from '../types/appointments';

type BootstrapState = {
  hydrated: boolean;
  hydrating: boolean;
  refreshing: boolean;
  lastHydratedAt: number | null;
  hydrate: () => Promise<void>;
  reset: () => void;
};

export const useBootstrapStore = create<BootstrapState>(function (set) {
  return {
    hydrated: false,
    hydrating: false,
    refreshing: false,
    lastHydratedAt: null,

    // Hidrata todos los stores en paralelo una sola vez al iniciar sesion
    hydrate: async function () {
      set({ hydrating: true, refreshing: true });

      try {
      const resultados = await Promise.allSettled([
        useUserStore.getState().fetchProfile(),
        useAppointmentsStore.getState().fetch(),
        useGamesStore.getState().fetch(),
        useTreatmentsStore.getState().fetch(),
        useProgressStore.getState().fetch(),
      ]);

      // Registrar errores parciales sin bloquear el resto
      resultados.forEach(function (resultado, indice) {
        if (resultado.status === 'rejected') {
          if (__DEV__) {
            console.warn(`[Bootstrap] Fallo el store ${indice}:`, resultado.reason);
          }
        }
      });

      // Programar recordatorios locales para las citas proximas
      try {
        const citas = useAppointmentsStore.getState().items as Appointment[];
        const hoy = Date.now();
        for (const cita of citas) {
          if (cita.status === 'SCHEDULED') {
            const [anio, mes, dia] = cita.date.split('-').map(Number);
            const [hora, min] = cita.time.split(':').map(Number);
            const fechaCita = new Date(anio, mes - 1, dia, hora, min).getTime();
            if (fechaCita > hoy) {
              await scheduleAppointmentReminder(cita);
            }
          }
        }
      } catch {
        // No bloquear la hidratacion si fallan las notificaciones
      }

      set({ hydrated: true, hydrating: false, lastHydratedAt: Date.now() });
      } finally {
        set({ refreshing: false });
      }
    },

    // Limpia todos los stores al cerrar sesion
    reset: function () {
      useUserStore.getState().clearProfile();
      useAppointmentsStore.getState().reset();
      useGamesStore.getState().reset();
      useTreatmentsStore.getState().reset();
      useProgressStore.getState().reset();
      set({ hydrated: false, hydrating: false, refreshing: false, lastHydratedAt: null });
    },
  };
});
