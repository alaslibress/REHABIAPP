import { useEffect, useState } from 'react';
import { Pressable, ScrollView, View, Platform } from 'react-native';
import * as Application from 'expo-application';
import * as Notifications from 'expo-notifications';
import { Ionicons } from '@expo/vector-icons';
import { useSettingsStore } from '../../src/store/settingsStore';
import { useAppointmentsStore } from '../../src/store/appointmentsStore';
import { useErrorStore } from '../../src/store/errorStore';
import { SettingsSection } from '../../src/components/SettingsSection';
import { SettingsRow } from '../../src/components/SettingsRow';
import { ToggleRow } from '../../src/components/ToggleRow';
import { FontScaleSlider } from '../../src/components/FontScaleSlider';
import { LegalTextModal } from '../../src/components/LegalTextModal';
import { AppText } from '../../src/components/AppText';
import { useTheme } from '../../src/utils/theme';
import { parseGraphQLError } from '../../src/utils/errorHandler';
import {
  requestPermission,
  getExpoPushToken,
  scheduleAppointmentReminder,
  scheduleTestNotification,
} from '../../src/utils/notifications';
import { client } from '../../src/services/graphql/client';
import {
  REGISTER_DEVICE_TOKEN,
  UNREGISTER_DEVICE_TOKEN,
} from '../../src/services/graphql/mutations/settings';
import type { ThemeMode } from '../../src/utils/theme';
import type { FontScale } from '../../src/utils/fontScale';

export default function SettingsScreen() {
  const { scheme } = useTheme();
  const isDark = scheme === 'dark';

  const themeMode = useSettingsStore(function (s) { return s.themeMode; });
  const fontScale = useSettingsStore(function (s) { return s.fontScale; });
  const notifAppointments = useSettingsStore(function (s) { return s.notifAppointments; });
  const notifDoctorUpdates = useSettingsStore(function (s) { return s.notifDoctorUpdates; });
  const setThemeMode = useSettingsStore(function (s) { return s.setThemeMode; });
  const setFontScale = useSettingsStore(function (s) { return s.setFontScale; });
  const setNotifAppointments = useSettingsStore(function (s) { return s.setNotifAppointments; });
  const setNotifDoctorUpdates = useSettingsStore(function (s) { return s.setNotifDoctorUpdates; });

  const appointments = useAppointmentsStore(function (s) { return s.items; });
  const showError = useErrorStore(function (s) { return s.showError; });

  const [permissionGranted, setPermissionGranted] = useState(false);
  const [testSent, setTestSent] = useState(false);
  const [privacyOpen, setPrivacyOpen] = useState(false);
  const [termsOpen, setTermsOpen] = useState(false);

  // Comprobar permiso de notificaciones al montar
  useEffect(function () {
    Notifications.getPermissionsAsync().then(function (status) {
      setPermissionGranted(status.status === 'granted');
    });
  }, []);

  const bgClass = isDark ? 'bg-background-dark' : 'bg-background';

  async function handleToggleAppointments(newValue: boolean) {
    if (newValue) {
      const granted = await requestPermission();
      if (!granted) {
        showError(parseGraphQLError({ extensions: { code: 'NOTIFICATION_PERMISSION_DENIED' } }));
        return;
      }
      setPermissionGranted(true);
      await setNotifAppointments(true);
      for (const appt of appointments) {
        await scheduleAppointmentReminder(appt).catch(function () {});
      }
    } else {
      await setNotifAppointments(false);
    }
  }

  async function handleToggleDoctorUpdates(newValue: boolean) {
    if (newValue) {
      const granted = await requestPermission();
      if (!granted) {
        showError(parseGraphQLError({ extensions: { code: 'NOTIFICATION_PERMISSION_DENIED' } }));
        return;
      }
      setPermissionGranted(true);
      const pushToken = await getExpoPushToken();
      if (pushToken) {
        const platform = Platform.OS === 'ios' ? 'IOS' : Platform.OS === 'android' ? 'ANDROID' : 'WEB';
        await client.mutate({
          mutation: REGISTER_DEVICE_TOKEN,
          variables: { token: pushToken, platform },
        }).catch(function () {});
      }
      await setNotifDoctorUpdates(true);
    } else {
      const pushToken = await getExpoPushToken();
      if (pushToken) {
        await client.mutate({
          mutation: UNREGISTER_DEVICE_TOKEN,
          variables: { token: pushToken },
        }).catch(function () {});
      }
      await setNotifDoctorUpdates(false);
    }
  }

  async function handleTestNotification() {
    await scheduleTestNotification();
    setTestSent(true);
    setTimeout(function () { setTestSent(false); }, 6000);
  }

  const version = Application.nativeApplicationVersion ?? 'desarrollo';
  const build = Application.nativeBuildVersion ?? '0';

  return (
    <ScrollView
      className={`flex-1 ${bgClass}`}
      contentContainerClassName="px-4 py-6 gap-4"
    >
      {/* Apariencia */}
      <SettingsSection title="Apariencia">
        <ToggleRow
          label="Tema oscuro"
          value={themeMode === 'dark'}
          onChange={function (v) { setThemeMode(v ? 'dark' : 'light'); }}
        />
        <View className="px-4 py-4">
          <AppText
            variant="caption"
            weight="medium"
            className="text-text-secondary dark:text-text-secondary-dark mb-2"
          >
            Tamano de texto
          </AppText>
          <FontScaleSlider
            value={fontScale}
            onChange={function (v) { setFontScale(v); }}
          />
        </View>
      </SettingsSection>

      {/* Notificaciones */}
      <SettingsSection title="Notificaciones">
        <ToggleRow
          label="Recordatorios de citas"
          value={notifAppointments}
          onChange={handleToggleAppointments}
        />
        <ToggleRow
          label="Actualizaciones del medico"
          value={notifDoctorUpdates}
          onChange={handleToggleDoctorUpdates}
        />
        <View className="px-4 pb-4 pt-1">
          <AppText variant="caption" className="text-text-secondary dark:text-text-secondary-dark leading-5">
            Recibiras un aviso el dia antes de cada cita y cuando tu medico actualice tu tratamiento.
          </AppText>
        </View>
      </SettingsSection>

      {/* Probar notificacion */}
      <SettingsSection title="Probar notificacion">
        <View className="px-4 py-4 gap-3">
          <Pressable
            onPress={handleTestNotification}
            disabled={!permissionGranted}
            className={`h-14 rounded-xl items-center justify-center ${permissionGranted ? 'bg-primary-600' : 'bg-border dark:bg-border-dark'}`}
            style={function ({ pressed }) { return pressed ? { opacity: 0.8 } : {}; }}
          >
            <AppText variant="body" weight="semibold" className={permissionGranted ? 'text-white' : 'text-text-secondary dark:text-text-secondary-dark'}>
              Enviar notificacion de prueba
            </AppText>
          </Pressable>
          {!permissionGranted && (
            <AppText variant="caption" className="text-text-secondary dark:text-text-secondary-dark text-center">
              Activa primero las notificaciones.
            </AppText>
          )}
          {testSent && (
            <AppText variant="caption" weight="medium" className="text-success text-center">
              Notificacion programada para 5 s.
            </AppText>
          )}
        </View>
      </SettingsSection>

      {/* Acerca de */}
      <SettingsSection title="Acerca de">
        <SettingsRow
          label="Version"
          right={
            <AppText variant="label" className="text-text-secondary dark:text-text-secondary-dark">
              {`${version} (${build})`}
            </AppText>
          }
        />
        <SettingsRow
          label="Politica de privacidad"
          onPress={function () { setPrivacyOpen(true); }}
          right={<Ionicons name="chevron-forward" size={16} color={isDark ? '#94A3B8' : '#64748B'} />}
        />
        <SettingsRow
          label="Terminos y condiciones"
          onPress={function () { setTermsOpen(true); }}
          right={<Ionicons name="chevron-forward" size={16} color={isDark ? '#94A3B8' : '#64748B'} />}
        />
      </SettingsSection>
      <LegalTextModal
        visible={privacyOpen}
        title="Politica de privacidad"
        onClose={function () { setPrivacyOpen(false); }}
      />
      <LegalTextModal
        visible={termsOpen}
        title="Terminos y condiciones"
        onClose={function () { setTermsOpen(false); }}
      />
    </ScrollView>
  );
}
