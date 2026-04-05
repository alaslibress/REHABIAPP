import { create } from 'zustand';
import type { UserState } from '../types/user';
import { client } from '../services/graphql/client';
import { GET_MY_PROFILE } from '../services/graphql/queries/user';
import { parseGraphQLError } from '../utils/errorHandler';

export const useUserStore = create<UserState>(function (set) {
  return {
    patient: null,
    isLoading: false,

    fetchProfile: async function (): Promise<void> {
      set({ isLoading: true });
      try {
        const { data } = await client.query({
          query: GET_MY_PROFILE,
          fetchPolicy: 'network-only',
        });
        set({ patient: data.me, isLoading: false });
      } catch (err) {
        set({ isLoading: false });
        // Parsear el error de Apollo a un AppError consistente
        // para que los componentes que llamen a fetchProfile reciban un error estructurado
        throw parseGraphQLError(err);
      }
    },

    clearProfile: function (): void {
      set({ patient: null });
    },
  };
});
