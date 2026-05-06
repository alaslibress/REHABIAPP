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
const remotePushDisabled = isExpoGo && Platform.OS === 'android';

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
  } catch {
    // Ignorar: entorno sin soporte
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
    const token = await Notifications.getExpoPushTokenAsync();
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
        body: `Tienes cita mañana a las ${appointment.time.substring(0, 5)} con ${nombreMedico}.`,
        sound: true,
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
      },
      trigger: {
        type: Notifications.SchedulableTriggerInputTypes.TIME_INTERVAL,
        seconds: 5,
        repeats: false,
      },
    });
  } catch {
    // Ignorar: entorno sin soporte
  }
}
