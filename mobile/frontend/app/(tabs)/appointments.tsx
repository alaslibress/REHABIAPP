import { useState, useCallback } from 'react';
import { ScrollView, View, RefreshControl } from 'react-native';
import { useAppointmentsStore } from '../../src/store/appointmentsStore';
import { AppointmentCard } from '../../src/components/AppointmentCard';
import { HospitalContactCard } from '../../src/components/HospitalContactCard';
import { ConfirmModal } from '../../src/components/ConfirmModal';
import { EmptyState } from '../../src/components/EmptyState';
import { AppText } from '../../src/components/AppText';
import { useTheme } from '../../src/utils/theme';

export default function AppointmentsScreen() {
  const { scheme } = useTheme();
  const items = useAppointmentsStore(function (s) { return s.items; });
  const loading = useAppointmentsStore(function (s) { return s.loading; });
  const fetchCitas = useAppointmentsStore(function (s) { return s.fetch; });
  const cancelCita = useAppointmentsStore(function (s) { return s.cancel; });

  // Estado del modal de confirmacion de cancelacion
  const [cancelandoId, setCancelandoId] = useState<string | null>(null);

  // Pull-to-refresh
  const [refrescando, setRefrescando] = useState(false);

  const handleRefresh = useCallback(async function () {
    setRefrescando(true);
    await fetchCitas();
    setRefrescando(false);
  }, [fetchCitas]);

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
          tintColor="#2563EB"
          colors={['#2563EB']}
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
            icon="calendar-outline"
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

      {/* Seccion: Pedir cita nueva */}
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
