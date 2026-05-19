import { create } from 'zustand';
import type { ErrorState, AppError } from '../types/errors';

export const useErrorStore = create<ErrorState>(function (set, get) {
  return {
    currentError: null,
    isVisible: false,
    // En modo silencioso los errores se suprimen — usado durante el bootstrap inicial
    silent: false,

    showError: function (error: AppError): void {
      if (get().silent) {
        // Suprimir popup durante bootstrap — cada tab muestra su propio empty state
        if (__DEV__) {
          console.warn('[errorStore] Error suprimido en modo silencioso:', error.code, error.subtitle);
        }
        return;
      }
      set({ currentError: error, isVisible: true });
    },

    hideError: function (): void {
      set({ currentError: null, isVisible: false });
    },

    setSilent: function (value: boolean): void {
      set({ silent: value });
    },
  };
});
