import { create } from 'zustand';
import type { ErrorState, AppError } from '../types/errors';

export const useErrorStore = create<ErrorState>(function (set) {
  return {
    currentError: null,
    isVisible: false,

    showError: function (error: AppError): void {
      set({ currentError: error, isVisible: true });
    },

    hideError: function (): void {
      set({ currentError: null, isVisible: false });
    },
  };
});
