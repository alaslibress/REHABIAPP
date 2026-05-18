import { Platform } from 'react-native';
import Constants, { ExecutionEnvironment } from 'expo-constants';
import type { Appointment } from '../types/appointments';

// Detecta si la app corre dentro de Expo Go (sandbox), donde las push remotas
// en Android fueron retiradas a partir de SDK 53. Usamos esta senal para
// desactivar las rutas que dependen del bridge nativo de push remoto.
const isExpoGo =
  Constants.executionEnvironment === ExecutionEnvironment.StoreClient;

// Android + Expo Go = sin soporte de push remotas ni de algunas APIs nativas.
// En ese caso el modulo `expo-notifications` puede lanzar al importarse,
// asi que lo cargamos de forma perezosa y protegida.
export const remotePushDisabled = isExpoGo && Platform.OS === 'android';

// Carga perezosa y defensiva del modulo nativo. Si falla por cualquier motivo
// (entorno no soportado, modulo ausente), devolvemos null y las funciones
// publicas actuan como no-op en lugar de romper la app.
function loadNotifications(): typeof import('expo-notifications') | null {
  if (remotePushDisabled) return null;
  try {
    // require dinamico: evita que el bundler evalue el modulo en import-time
    // en entornos donde lanzaria durante la carga.
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    return require('expo-notifications');
  } catch {
    return null;
  }
}

const Notifications = loadNotifications();

// Configura el manejador de notificaciones — llamar una vez al iniciar la app.
// No-op si el modulo no esta disponible en este entorno.
export function initNotifications() {
  if (!Notifications) return;
  try {
    Notifications.setNotificationHandler({
      handleNotification: async () => ({
        shouldShowAlert: true,
        shouldShowBanner: true,
        shouldShowList: true,
        shouldPlaySound: true,
        shouldSetBadge: false,
      }),
    });

    // En Android los canales son obligatorios desde API 26+. Sin canal,
    // scheduleNotificationAsync no muestra el banner aunque el permiso este concedido.
    if (Platform.OS === 'android') {
      Notifications.setNotificationChannelAsync('default', {
        name: 'Notificaciones',
        importance: Notifications.AndroidImportance.HIGH,
        vibrationPattern: [0, 250, 250, 250],
        lightColor: '#2563EB',
        sound: 'default',
      }).catch(() => {});
    }
  } catch {
    // Ignorar: entorno sin soporte
  }
}

// Devuelve true si el permiso de notificaciones ya esta concedido.
// No-op (false) si el modulo no esta disponible.
export async function isPermissionGranted(): Promise<boolean> {
  if (!Notifications) return false;
  try {
    const { status } = await Notifications.getPermissionsAsync();
    return status === 'granted';
  } catch {
    return false;
  }
}

// Solicita permiso de notificaciones al usuario.
// Devuelve false si el modulo no esta disponible.
export async function requestPermission(): Promise<boolean> {
  if (!Notifications) return false;
  try {
    const { status: existingStatus } = await Notifications.getPermissionsAsync();
    if (existingStatus === 'granted') return true;
    const { status } = await Notifications.requestPermissionsAsync();
    return status === 'granted';
  } catch {
    return false;
  }
}

// Obtiene el token Expo Push y lo devuelve (o null si no esta disponible).
// En Expo Go Android devuelve null sin tocar el modulo nativo.
export async function getExpoPushToken(): Promise<string | null> {
  if (!Notifications) return null;
  if (Platform.OS === 'web') return null;
  if (remotePushDisabled) return null;
  try {
    // SDK 53+ obliga a pasar projectId explicitamente (lo lee de
    // app.json -> extra.eas.projectId). Si no hay projectId real, devolvemos
    // null en lugar de romper: las notificaciones LOCALES no lo necesitan
    // y el flujo de reminders de cita sigue funcionando.
    const projectId =
      (Constants.expoConfig?.extra as { eas?: { projectId?: string } } | undefined)?.eas?.projectId ??
      (Constants as unknown as { easConfig?: { projectId?: string } }).easConfig?.projectId;
    if (!projectId || projectId === '00000000-0000-0000-0000-000000000000') {
      return null;
    }
    const token = await Notifications.getExpoPushTokenAsync({ projectId });
    return token.data;
  } catch {
    return null;
  }
}

// Programa un recordatorio local 24 horas antes de una cita.
// El identificador es idempotente: cancela el anterior si existe.
// No-op si el modulo no esta disponible.
export async function scheduleAppointmentReminder(appointment: Appointment): Promise<void> {
  if (!Notifications) return;
  try {
    const identifier = `appt-${appointment.id}`;

    // Cancelar notificacion previa con el mismo id si existe
    await Notifications.cancelScheduledNotificationAsync(identifier).catch(() => {});

    // Calcular el momento del recordatorio: fecha+hora de la cita - 24 horas
    const [year, month, day] = appointment.date.split('-').map(Number);
    const [hour, minute] = appointment.time.split(':').map(Number);
    const citaMs = new Date(year, month - 1, day, hour, minute).getTime();
    const recordatorioMs = citaMs - 24 * 60 * 60 * 1000;

    // No programar si ya paso el momento del recordatorio
    if (recordatorioMs <= Date.now()) return;

    const nombreMedico = appointment.practitionerName || 'tu medico';

    await Notifications.scheduleNotificationAsync({
      identifier,
      content: {
        title: 'Recordatorio de cita',
        body: `Tienes cita maniana a las ${appointment.time.substring(0, 5)} con ${nombreMedico}.`,
        sound: true,
        // Android: el canal "default" se crea en initNotifications().
        ...(Platform.OS === 'android' ? { channelId: 'default' } : {}),
      },
      trigger: {
        type: Notifications.SchedulableTriggerInputTypes.DATE,
        date: new Date(recordatorioMs),
      },
    });
  } catch {
    // Ignorar: entorno sin soporte
  }
}

// Cancela el recordatorio de una cita por su id. No-op si no hay modulo.
export async function cancelAppointmentReminder(appointmentId: string): Promise<void> {
  if (!Notifications) return;
  await Notifications.cancelScheduledNotificationAsync(`appt-${appointmentId}`).catch(() => {});
}

// Programa una notificacion de prueba que se dispara en 5 segundos.
// No-op si el modulo no esta disponible.
export async function scheduleTestNotification(): Promise<void> {
  if (!Notifications) return;
  try {
    await Notifications.scheduleNotificationAsync({
      content: {
        title: 'Notificacion de prueba',
        body: 'Las notificaciones funcionan correctamente.',
        sound: true,
        // Android: el canal "default" se crea en initNotifications().
        ...(Platform.OS === 'android' ? { channelId: 'default' } : {}),
      },
      trigger: {
        type: Notifications.SchedulableTriggerInputTypes.TIME_INTERVAL,
        seconds: 3,
        repeats: false,
      },
    });
  } catch {
    // Ignorar: entorno sin soporte
  }
}

// Permiso + token push: solicita permiso del SO si todavia no esta concedido,
// recupera el Expo Push Token y lo registra en el BFF si el usuario tiene
// activadas las "Actualizaciones del medico". Idempotente: llamar mas de una
// vez no produce efectos colaterales (el SO solo prompteo una vez en su vida).
//
// Devuelve un objeto con el estado para que el caller pueda decidir si
// programar reminders locales o no.
export async function ensureNotificationsEnabled(opts?: {
  alsoRegisterPushToken?: boolean;
  registerToken?: (token: string, platform: 'IOS' | 'ANDROID' | 'WEB') => Promise<void>;
}): Promise<{ permissionGranted: boolean; pushToken: string | null }> {
  if (!Notifications) return { permissionGranted: false, pushToken: null };

  // 1) Permiso. requestPermission ya es idempotente: si ya esta concedido
  //    devuelve true sin abrir dialogo.
  const granted = await requestPermission();
  if (!granted) return { permissionGranted: false, pushToken: null };

  // 2) Push token (solo si el caller lo pidio). En Expo Go Android el token
  //    sera null por limitacion del SDK 53+; el flujo NO debe romperse.
  let pushToken: string | null = null;
  if (opts?.alsoRegisterPushToken) {
    pushToken = await getExpoPushToken();
    if (pushToken && opts.registerToken) {
      const platform: 'IOS' | 'ANDROID' | 'WEB' =
        Platform.OS === 'ios' ? 'IOS' : Platform.OS === 'android' ? 'ANDROID' : 'WEB';
      await opts.registerToken(pushToken, platform).catch(function () {
        // Fallo silencioso — el log queda en el BFF; el frontend continua.
      });
    }
  }

  return { permissionGranted: true, pushToken };
}
