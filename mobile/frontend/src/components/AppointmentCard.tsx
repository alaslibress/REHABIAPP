import { View, Pressable } from 'react-native';
import { CalendarBlank } from 'phosphor-react-native';
import { FloatingCard } from './FloatingCard';
import { useTheme } from '../utils/theme';
import { AppText } from './AppText';
import type { Appointment } from '../types/appointments';

type AppointmentCardProps = {
  appointment: Appointment;
  onCancel?: (id: string) => void;
  // readOnly=true: oculta el boton de cancelar (usado en historial de citas pasadas)
  readOnly?: boolean;
};

// Formatea una fecha YYYY-MM-DD a "DD MMM YYYY" en castellano
function formatearFecha(fechaStr: string): string {
  const meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
  const [anio, mes, dia] = fechaStr.split('-').map(Number);
  return `${dia} ${meses[mes - 1]} ${anio}`;
}

// Badge de estado para citas pasadas
function EstadoBadge({ status }: { status: string }) {
  if (status === 'COMPLETED') {
    return (
      <View className="px-2 py-0.5 rounded-full bg-success/10">
        <AppText variant="caption" weight="medium" className="text-success">Completada</AppText>
      </View>
    );
  }
  if (status === 'CANCELLED') {
    return (
      <View className="px-2 py-0.5 rounded-full bg-error/10">
        <AppText variant="caption" weight="medium" className="text-error">Cancelada</AppText>
      </View>
    );
  }
  return null;
}

export function AppointmentCard(props: AppointmentCardProps) {
  const { appointment, onCancel, readOnly = false } = props;
  const { theme } = useTheme();
  const horaFormateada = appointment.time.substring(0, 5);

  return (
    <FloatingCard className={`mb-3${readOnly ? ' opacity-80' : ''}`}>
      <View className="flex-row items-center gap-3">
        {/* Icono izquierdo */}
        <View className="w-10 h-10 rounded-full bg-primary-100 dark:bg-primary-900 justify-center items-center">
          <CalendarBlank size={20} color={theme.accent} weight="regular" />
        </View>

        {/* Informacion central */}
        <View className="flex-1">
          <AppText variant="label" weight="semibold" className="text-text-primary dark:text-text-primary-dark">
            {formatearFecha(appointment.date)}  ·  {horaFormateada}
          </AppText>
          <AppText variant="caption" className="text-text-secondary dark:text-text-secondary-dark mt-0.5">
            {appointment.practitionerName}
          </AppText>
          {appointment.practitionerSpecialty && (
            <AppText variant="caption" className="text-text-secondary dark:text-text-secondary-dark">
              {appointment.practitionerSpecialty}
            </AppText>
          )}
          {/* Badge de estado para citas pasadas */}
          {readOnly && appointment.status !== 'SCHEDULED' && (
            <View className="mt-1">
              <EstadoBadge status={appointment.status} />
            </View>
          )}
        </View>

        {/* Boton Cancelar — solo en citas activas, no en historial */}
        {!readOnly && onCancel ? (
          <Pressable
            onPress={function () { onCancel(appointment.id); }}
            className="px-3 py-1.5 rounded-full border border-error"
          >
            <AppText variant="caption" weight="medium" className="text-error">
              Cancelar
            </AppText>
          </Pressable>
        ) : null}
      </View>
    </FloatingCard>
  );
}
