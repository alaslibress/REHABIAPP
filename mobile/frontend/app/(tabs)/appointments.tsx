import { useState, useCallback } from 'react';
import { ScrollView, View, RefreshControl } from 'react-native';
import { useAppointmentsStore } from '../../src/store/appointmentsStore';
import { useRefreshOnFocus } from '../../src/utils/useRefreshOnFocus';
import { AppointmentCard } from '../../src/components/AppointmentCard';
import { HospitalContactCard } from '../../src/components/HospitalContactCard';
import { ConfirmModal } from '../../src/components/ConfirmModal';
import { EmptyState } from '../../src/components/EmptyState';
import { CalendarBlank, Clock } from 'phosphor-react-native';
import { AppText } from '../../src/components/AppText';
import { useTheme } from '../../src/utils/theme';

export default function AppointmentsScreen() {
  const { scheme, theme } = useTheme();
  const items = useAppointmentsStore(function (s) { return s.items; });
  const loading = useAppointmentsStore(function (s) { return s.loading; });
  const pastItems = useAppointmentsStore(function (s) { return s.pastItems; });
  const loadingPast = useAppointmentsStore(function (s) { return s.loadingPast; });
  const fetchCitas = useAppointmentsStore(function (s) { return s.fetch; });
  const fetchPasadas = useAppointmentsStore(function (s) { return s.fetchPast; });
  const cancelCita = useAppointmentsStore(function (s) { return s.cancel; });

  // Refresca citas (proximas + pasadas) al enfocar la pestana o al volver
  // del background. Sin esto los datos cacheados via persist quedaban
  // estancados hasta pull-to-refresh manual.
  useRefreshOnFocus(useCallback(async function () {
    await Promise.all([fetchCitas(), fetchPasadas()]);
  }, [fetchCitas, fetchPasadas]));

  // Estado del modal de confirmacion de cancelacion
  const [cancelandoId, setCancelandoId] = useState<string | null>(null);

  // Pull-to-refresh — actualiza proximas y pasadas en paralelo
  const [refrescando, setRefrescando] = useState(false);

  const handleRefresh = useCallback(async function () {
    setRefrescando(true);
    try {
      await Promise.all([fetchCitas(), fetchPasadas()]);
    } finally {
      setRefrescando(false);
    }
  }, [fetchCitas, fetchPasadas]);

  function pedirCancelar(id: string) {
    setCancelandoId(id);
  }

  async function confirmarCancelar() {
    if (!cancelandoId) return;
    const id = cancelandoId;
    setCancelandoId(null);
    await cancelCita(id);
  }

  const bgClass = scheme === 'dark' ? 'bg-background-dark' : 'bg-background';

  return (
    <ScrollView
      className={`flex-1 ${bgClass}`}
      contentContainerStyle={{ padding: 24, paddingBottom: 40 }}
      refreshControl={
        <RefreshControl
          refreshing={refrescando}
          onRefresh={handleRefresh}
          tintColor={theme.accent}
          colors={[theme.accent]}
        />
      }
    >
      {/* Seccion: Proximas citas */}
      <AppText variant="subtitle" weight="semibold" className="text-text-primary dark:text-text-primary-dark mb-3">
        Proximas citas
      </AppText>

      {items.length === 0 && !loading ? (
        <View className="mb-6">
          <EmptyState
            Icon={CalendarBlank}
            title="No tienes citas proximas."
          />
        </View>
      ) : (
        <View className="mb-6">
          {items.map(function (cita) {
            return (
              <AppointmentCard
                key={cita.id}
                appointment={cita}
                onCancel={pedirCancelar}
              />
            );
          })}
        </View>
      )}

      {/* Seccion: Historial de citas */}
      <AppText variant="subtitle" weight="semibold" className="text-text-primary dark:text-text-primary-dark mb-3">
        Historial de citas
      </AppText>

      {pastItems.length === 0 && !loadingPast ? (
        <View className="mb-6">
          <EmptyState
            Icon={Clock}
            title="Aun no tienes citas pasadas."
          />
        </View>
      ) : (
        <View className="mb-6">
          {pastItems.map(function (cita) {
            return (
              <AppointmentCard
                key={cita.id}
                appointment={cita}
                onCancel={undefined}
                readOnly
              />
            );
          })}
        </View>
      )}

      {/* Seccion: Pedir cita nueva — el paciente contacta por telefono / email / WhatsApp.
          Sin formulario interno: la solicitud queda fuera de la app por requisito clinico. */}
      <HospitalContactCard />

      {/* Modal de confirmacion de cancelacion */}
      <ConfirmModal
        visible={cancelandoId !== null}
        title="¿Cancelar cita?"
        message="Esta accion no se puede deshacer."
        confirmLabel="Si, cancelar"
        cancelLabel="Volver"
        onConfirm={confirmarCancelar}
        onCancel={function () { setCancelandoId(null); }}
        destructive={true}
      />
    </ScrollView>
  );
}
