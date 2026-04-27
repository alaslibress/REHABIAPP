import { create } from 'zustand';
import { client } from '../services/graphql/client';
import {
  GET_MY_BODY_PART_PROGRESS,
  GET_BODY_PART_METRICS,
} from '../services/graphql/queries/progress';
import type { BodyPartProgress, BodyPartMetric } from '../types/progress';

type ProgressState = {
  bodyParts: BodyPartProgress[];
  metricsByPart: Record<string, BodyPartMetric[]>;
  loadingMetrics: boolean;
  hydrated: boolean;
  fetch: () => Promise<void>;
  loadMetrics: (bodyPartId: string) => Promise<void>;
  reset: () => void;
};

export const useProgressStore = create<ProgressState>(function (set, get) {
  return {
    bodyParts: [],
    metricsByPart: {},
    loadingMetrics: false,
    hydrated: false,

    fetch: async function () {
      const { data } = await client.query({
        query: GET_MY_BODY_PART_PROGRESS,
        fetchPolicy: 'network-only',
      });
      set({ bodyParts: data.myBodyPartProgress ?? [], hydrated: true });
    },

    loadMetrics: async function (bodyPartId) {
      // Cache: no refetch si ya existen datos para esta parte
      if (get().metricsByPart[bodyPartId]) return;

      set({ loadingMetrics: true });
      try {
        const { data } = await client.query({
          query: GET_BODY_PART_METRICS,
          variables: { bodyPartId },
          fetchPolicy: 'network-only',
        });
        set(function (state) {
          return {
            metricsByPart: {
              ...state.metricsByPart,
              [bodyPartId]: data.bodyPartMetrics ?? [],
            },
          };
        });
      } finally {
        set({ loadingMetrics: false });
      }
    },

    reset: function () {
      set({ bodyParts: [], metricsByPart: {}, loadingMetrics: false, hydrated: false });
    },
  };
});
