import { create } from 'zustand';
import type { UserState } from '../types/user';
import { client } from '../services/graphql/client';
import { GET_MY_PROFILE, GET_MY_DISABILITIES } from '../services/graphql/queries/user';
import { parseGraphQLError } from '../utils/errorHandler';
import type { Disability } from '../types/treatments';

type UserStateExtended = UserState & {
  disabilities: Disability[];
};

export const useUserStore = create<UserStateExtended>(function (set) {
  return {
    patient: null,
    disabilities: [],
    isLoading: false,

    fetchProfile: async function (): Promise<void> {
      set({ isLoading: true });
      try {
        const [profileRes, disRes] = await Promise.all([
          client.query({ query: GET_MY_PROFILE, fetchPolicy: 'network-only' }),
          client.query({ query: GET_MY_DISABILITIES, fetchPolicy: 'network-only' }),
        ]);

        const rawDisabilities: Disability[] = (disRes.data.myDisabilities ?? []).map(
          function (d: { id: string; name: string; description: string | null; currentLevel: number }) {
            return { ...d, codDis: d.id };
          }
        );

        set({ patient: profileRes.data.me, disabilities: rawDisabilities, isLoading: false });
      } catch (err) {
        set({ isLoading: false });
        throw parseGraphQLError(err);
      }
    },

    clearProfile: function (): void {
      set({ patient: null, disabilities: [] });
    },
  };
});
