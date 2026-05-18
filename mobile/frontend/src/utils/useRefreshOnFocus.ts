import { useEffect, useRef } from 'react';
import { useFocusEffect } from '@react-navigation/native';
import { AppState, type AppStateStatus } from 'react-native';

/**
 * Refresca los datos de una pestana cada vez que:
 *  - El usuario entra a la pestana (useFocusEffect)
 *  - La app vuelve del background a foreground (AppState 'active')
 *
 * Asi los stores Zustand con persist no se quedan eternamente con
 * datos cacheados — cada vez que el usuario "abre" o "vuelve" se
 * dispara fetch en background.
 *
 * Uso:
 *   const fetchCitas = useAppointmentsStore(s => s.fetch);
 *   useRefreshOnFocus(fetchCitas);
 *
 * Llamadas duplicadas en menos de `minIntervalMs` se ignoran para no
 * martillear el BFF cuando el usuario alterna pestanas rapidamente.
 */
export function useRefreshOnFocus(
  refetch: () => Promise<unknown> | unknown,
  options: { minIntervalMs?: number } = {},
): void {
  const { minIntervalMs = 5000 } = options;
  const lastRefreshRef = useRef<number>(0);

  function refrescarSiToca() {
    const now = Date.now();
    if (now - lastRefreshRef.current < minIntervalMs) return;
    lastRefreshRef.current = now;
    try {
      const result = refetch();
      if (result && typeof (result as Promise<unknown>).then === 'function') {
        (result as Promise<unknown>).catch(function () { /* silencioso */ });
      }
    } catch {
      // silencioso
    }
  }

  // 1) Al enfocar la pestana
  useFocusEffect(
    // eslint-disable-next-line react-hooks/exhaustive-deps
    (() => {
      refrescarSiToca();
      return undefined;
    }) as unknown as () => void,
  );

  // 2) Cuando la app vuelve a foreground
  useEffect(function () {
    const sub = AppState.addEventListener('change', function (state: AppStateStatus) {
      if (state === 'active') refrescarSiToca();
    });
    return function () { sub.remove(); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
}
