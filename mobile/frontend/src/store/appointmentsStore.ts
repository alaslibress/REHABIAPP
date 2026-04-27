import { create } from 'zustand';
import { client } from '../services/graphql/client';
import { GET_MY_APPOINTMENTS } from '../services/graphql/queries/appointments';
import { CANCEL_APPOINTMENT, REQUEST_APPOINTMENT } from '../services/graphql/mutations/appointments';
import { parseGraphQLError } from '../utils/errorHandler';
import { useErrorStore } from './errorStore';
import { scheduleAppointmentReminder, cancelAppointmentReminder } from '../utils/notifications';
import type { Appointment, RequestAppointmentInput, AppointmentRequest } from '../types/appointments';

type AppointmentsState = {
  items: Appointment[];
  loading: boolean;
  hydrated: boolean;
  fetch: () => Promise<void>;
  requestAppointment: (input: RequestAppointmentInput) => Promise<AppointmentRequest>;
  cancel: (id: string) => Promise<void>;
  reset: () => void;
};

export const useAppointmentsStore = create<AppointmentsState>(function (set, get) {
  return {
    items: [],
    loading: false,
    hydrated: false,

    // Carga las citas proximas del paciente y programa sus recordatorios locales
    fetch: async function () {
      set({ loading: true });
      try {
        const { data } = await client.query({
          query: GET_MY_APPOINTMENTS,
          variables: { upcoming: true },
          fetchPolicy: 'network-only',
        }) as { data: { myAppointments: Appointment[] } };

        const citas: Appointment[] = data.myAppointments ?? [];
        set({ items: citas, loading: false, hydrated: true });

        // Programar recordatorio local para cada cita proxima
        for (const cita of citas) {
          if (cita.status === 'SCHEDULED') {
            await scheduleAppointmentReminder(cita).catch(() => {});
          }
        }
      } catch (err) {
        set({ loading: false, hydrated: true });
        const appError = parseGraphQLError(err);
        useErrorStore.getState().showError(appError);
      }
    },

    // Envia una solicitud de cita nueva al BFF
    requestAppointment: async function (input: RequestAppointmentInput): Promise<AppointmentRequest> {
      const { data } = await client.mutate({
        mutation: REQUEST_APPOINTMENT,
        variables: {
          fechaPreferida: input.fechaPreferida,
          horaPreferida: input.horaPreferida,
          motivo: input.motivo,
          telefono: input.telefono ?? null,
          email: input.email ?? null,
        },
      }) as { data: { requestAppointment: AppointmentRequest } };

      return data.requestAppointment;
    },

    // Cancela una cita existente y elimina su recordatorio local
    cancel: async function (id: string) {
      try {
        await client.mutate({
          mutation: CANCEL_APPOINTMENT,
          variables: { appointmentId: id },
        });

        // Eliminar de la lista local sin esperar a refetch
        set({ items: get().items.filter(function (c) { return c.id !== id; }) });

        // Cancelar el recordatorio local
        await cancelAppointmentReminder(id);
      } catch (err) {
        const appError = parseGraphQLError(err);
        useErrorStore.getState().showError(appError);
        throw appError;
      }
    },

    // Limpia el store y cancela todos los recordatorios de citas
    reset: function () {
      const citas = get().items;
      for (const cita of citas) {
        cancelAppointmentReminder(cita.id).catch(() => {});
      }
      set({ items: [], loading: false, hydrated: false });
    },
  };
});
