import { create } from 'zustand';
import { client } from '../services/graphql/client';
import { GET_MY_ASSIGNED_GAMES } from '../services/graphql/queries/games';
import { parseGraphQLError } from '../utils/errorHandler';
import { useErrorStore } from './errorStore';
import type { AssignedGame } from '../types/games';

type GamesState = {
  items: AssignedGame[];
  loading: boolean;
  hydrated: boolean;
  fetch: () => Promise<void>;
  reset: () => void;
};

export const useGamesStore = create<GamesState>(function (set) {
  return {
    items: [],
    loading: false,
    hydrated: false,

    fetch: async function () {
      set({ loading: true });
      try {
        const { data } = await client.query({
          query: GET_MY_ASSIGNED_GAMES,
          fetchPolicy: 'network-only',
        });
        set({ items: data.myAssignedGames ?? [], hydrated: true });
      } catch (err) {
        const appError = parseGraphQLError(err);
        useErrorStore.getState().showError(appError);
      } finally {
        set({ loading: false });
      }
    },

    reset: function () {
      set({ items: [], loading: false, hydrated: false });
    },
  };
});
