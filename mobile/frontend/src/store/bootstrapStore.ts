import { create } from 'zustand';
import { useUserStore } from './userStore';
import { useAppointmentsStore } from './appointmentsStore';
import { useGamesStore } from './gamesStore';
import { useTreatmentsStore } from './treatmentsStore';
import { useProgressStore } from './progressStore';
import { useErrorStore } from './errorStore';
import { scheduleAppointmentReminder, ensureNotificationsEnabled } from '../utils/notifications';
import { client } from '../services/graphql/client';
import { REGISTER_DEVICE_TOKEN } from '../services/graphql/mutations/settings';
import { useSettingsStore } from './settingsStore';
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
      // Suprimir popups durante el bootstrap — cada tab muestra su propio empty state.
      // Los errores siguen registrados via console.warn para depuracion.
      useErrorStore.getState().setSilent(true);

      try {
        const resultados = await Promise.allSettled([
          useUserStore.getState().fetchProfile(),
          useAppointmentsStore.getState().fetch(),
          useAppointmentsStore.getState().fetchPast(),
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

        // Notificaciones — un unico bloque defensivo que cubre permiso, token push
        // y reminders locales. Cualquier fallo se atrapa y la hidratacion sigue.
        try {
          const settings = useSettingsStore.getState();
          // Solo intentar si el usuario no ha apagado AMBAS opciones en Settings.
          // notifAppointments y notifDoctorUpdates son true por defecto.
          if (settings.notifAppointments || settings.notifDoctorUpdates) {
            const { permissionGranted } = await ensureNotificationsEnabled({
              alsoRegisterPushToken: settings.notifDoctorUpdates,
              registerToken: async function (token, platform) {
                await client.mutate({
                  mutation: REGISTER_DEVICE_TOKEN,
                  variables: { token, platform },
                });
              },
            });

            // Reminders locales solo si tenemos permiso y la opcion sigue activa.
            if (permissionGranted && settings.notifAppointments) {
              const citas = useAppointmentsStore.getState().items as Appointment[];
              const hoy = Date.now();
              for (const cita of citas) {
                if (cita.status !== 'SCHEDULED') continue;
                const [anio, mes, dia] = cita.date.split('-').map(Number);
                const [hora, min] = cita.time.split(':').map(Number);
                const fechaCita = new Date(anio, mes - 1, dia, hora, min).getTime();
                if (fechaCita > hoy) {
                  await scheduleAppointmentReminder(cita).catch(function () {});
                }
              }
            }
          }
        } catch (err) {
          if (__DEV__) {
            console.warn('[bootstrap] Fallo en bloque de notificaciones:', (err as Error)?.message);
          }
        }

        set({ hydrated: true, hydrating: false, lastHydratedAt: Date.now() });
      } finally {
        // Reactivar popups para errores futuros (acciones del usuario)
        useErrorStore.getState().setSilent(false);
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
