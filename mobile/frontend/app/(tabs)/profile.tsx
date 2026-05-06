import { useState } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, View } from 'react-native';
import { useUserStore } from '../../src/store/userStore';
import { useAuthStore } from '../../src/store/authStore';
import { useBootstrapStore } from '../../src/store/bootstrapStore';
import { useErrorStore } from '../../src/store/errorStore';
import { ProfileHeader } from '../../src/components/ProfileHeader';
import { InfoRow } from '../../src/components/InfoRow';
import { PathologyPill } from '../../src/components/PathologyPill';
import { LogoutButton } from '../../src/components/LogoutButton';
import { ConfirmModal } from '../../src/components/ConfirmModal';
import { AppText } from '../../src/components/AppText';
import { useTheme } from '../../src/utils/theme';
import { maskDni, maskSsn } from '../../src/utils/mask';
import { parseGraphQLError } from '../../src/utils/errorHandler';

export default function ProfileScreen() {
  const { scheme } = useTheme();
  const patient = useUserStore(function (s) { return s.patient; });
  const disabilities = useUserStore(function (s) { return s.disabilities; });
  const logout = useAuthStore(function (s) { return s.logout; });
  const showError = useErrorStore(function (s) { return s.showError; });

  const refrescando = useBootstrapStore(function (s) { return s.refreshing; });
  const refrescar = useBootstrapStore(function (s) { return s.hydrate; });

  const [confirmVisible, setConfirmVisible] = useState(false);

  const bgClass = scheme === 'dark' ? 'bg-background-dark' : 'bg-background';
  const cardClass = scheme === 'dark' ? 'bg-surface-dark' : 'bg-surface';

  async function handleConfirmLogout() {
    setConfirmVisible(false);
    try {
      await logout();
    } catch (err) {
      showError(parseGraphQLError(err));
    }
  }

  if (!patient) {
    return (
      <View className={`flex-1 items-center justify-center ${bgClass}`}>
        <ActivityIndicator size="large" />
        <AppText
          variant="caption"
          className="text-text-secondary dark:text-text-secondary-dark mt-3"
        >
          Cargando perfil...
        </AppText>
      </View>
    );
  }

  return (
    <>
      <ScrollView
        className={`flex-1 ${bgClass}`}
        contentContainerClassName="px-4 py-6 gap-4"
        refreshControl={
          <RefreshControl refreshing={refrescando} onRefresh={refrescar} />
        }
      >
        {/* Tarjeta de avatar */}
        <ProfileHeader patient={patient} avatarUri={patient.avatarDataUri ?? null} />

        {/* Datos personales */}
        <View className={`rounded-2xl px-4 ${cardClass}`}>
          <InfoRow label="DNI" value={maskDni(patient.dni)} icon="card-outline" />
          <InfoRow label="N. Seguridad Social" value={maskSsn(patient.numSs)} icon="shield-checkmark-outline" />
          <InfoRow label="Fecha de nacimiento" value={patient.birthDate} icon="calendar-outline" />
          <InfoRow label="Direccion" value={patient.address} icon="location-outline" />
          <InfoRow label="Telefono" value={patient.phone} icon="call-outline" />
          <InfoRow label="Email" value={patient.email} icon="mail-outline" />
        </View>

        {/* Patologias */}
        <View className={`rounded-2xl p-4 gap-3 ${cardClass}`}>
          <AppText variant="subtitle" weight="semibold" className="text-text-primary dark:text-text-primary-dark">
            Patologias
          </AppText>
          {disabilities.length === 0 ? (
            <AppText variant="body" className="text-text-secondary dark:text-text-secondary-dark">
              Sin patologias registradas.
            </AppText>
          ) : (
            <View className="flex-row flex-wrap gap-2">
              {disabilities.map(function (d) {
                return (
                  <PathologyPill key={d.id} name={d.name} level={d.currentLevel} />
                );
              })}
            </View>
          )}
        </View>

        {/* Cerrar sesion */}
        <LogoutButton onPress={function () { setConfirmVisible(true); }} />
      </ScrollView>

      <ConfirmModal
        visible={confirmVisible}
        title="Cerrar sesion"
        message="Tendras que volver a iniciar sesion para acceder."
        confirmLabel="Cerrar sesion"
        cancelLabel="Cancelar"
        destructive={true}
        onConfirm={handleConfirmLogout}
        onCancel={function () { setConfirmVisible(false); }}
      />
    </>
  );
}
