import { create } from 'zustand';
import * as FileSystem from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import { client } from '../services/graphql/client';
import { GET_MY_TREATMENTS, GET_TREATMENT_DOCUMENT } from '../services/graphql/queries/treatments';
import { GET_MY_DISABILITIES } from '../services/graphql/queries/user';
import { parseGraphQLError } from '../utils/errorHandler';
import { useErrorStore } from './errorStore';
import type { Treatment, Disability } from '../types/treatments';

type TreatmentsState = {
  items: Treatment[];
  disabilities: Disability[];
  loading: boolean;
  hydrated: boolean;
  fetch: () => Promise<void>;
  downloadPdf: (codTrat: string) => Promise<string>;
  reset: () => void;
};

export const useTreatmentsStore = create<TreatmentsState>(function (set, get) {
  return {
    items: [],
    disabilities: [],
    loading: false,
    hydrated: false,

    fetch: async function () {
      set({ loading: true });
      try {
        const [treatRes, disRes] = await Promise.all([
          client.query({ query: GET_MY_TREATMENTS, fetchPolicy: 'network-only' }),
          client.query({ query: GET_MY_DISABILITIES, fetchPolicy: 'network-only' }),
        ]);

        const rawDisabilities: Disability[] = (disRes.data.myDisabilities ?? []).map(
          function (d: { id: string; name: string; description: string | null; currentLevel: number }) {
            return { ...d, codDis: d.id };
          }
        );

        set({
          items: treatRes.data.myTreatments ?? [],
          disabilities: rawDisabilities,
          hydrated: true,
        });
      } catch (err) {
        const appError = parseGraphQLError(err);
        useErrorStore.getState().showError(appError);
      } finally {
        set({ loading: false });
      }
    },

    downloadPdf: async function (codTrat: string): Promise<string> {
      try {
        const { data } = await client.query({
          query: GET_TREATMENT_DOCUMENT,
          variables: { codTrat },
          fetchPolicy: 'network-only',
        });

        const doc = data.treatmentDocument;
        const fileUri = `${FileSystem.cacheDirectory}${doc.fileName}`;

        if (doc.base64) {
          await FileSystem.writeAsStringAsync(fileUri, doc.base64, {
            encoding: FileSystem.EncodingType.Base64,
          });
        } else if (doc.url) {
          await FileSystem.downloadAsync(doc.url, fileUri);
        } else {
          throw new Error('No base64 ni url disponible');
        }

        await Sharing.shareAsync(fileUri, { mimeType: doc.mimeType, UTI: 'com.adobe.pdf' });
        return fileUri;
      } catch (err) {
        const appError = parseGraphQLError(err);
        if (appError.code !== 'DOCUMENT_DOWNLOAD_FAILED') {
          appError.code = 'DOCUMENT_DOWNLOAD_FAILED';
          appError.subtitle = 'Error al descargar';
          appError.message = 'No se pudo descargar el documento. Intentalo mas tarde.';
        }
        throw appError;
      }
    },

    reset: function () {
      set({ items: [], disabilities: [], loading: false, hydrated: false });
    },
  };
});
