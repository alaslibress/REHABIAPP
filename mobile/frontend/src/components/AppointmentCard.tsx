import { View, Pressable } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { FloatingCard } from './FloatingCard';
import { AppText } from './AppText';
import type { Appointment } from '../types/appointments';

type AppointmentCardProps = {
  appointment: Appointment;
  onCancel: (id: string) => void;
};

// Formatea una fecha YYYY-MM-DD a "DD MMM YYYY" en castellano
function formatearFecha(fechaStr: string): string {
  const meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
  const [anio, mes, dia] = fechaStr.split('-').map(Number);
  return `${dia} ${meses[mes - 1]} ${anio}`;
}

export function AppointmentCard(props: AppointmentCardProps) {
  const { appointment, onCancel } = props;
  const horaFormateada = appointment.time.substring(0, 5);

  return (
    <FloatingCard className="mb-3">
      <View className="flex-row items-center gap-3">
        {/* Icono izquierdo */}
        <View className="w-10 h-10 rounded-full bg-primary-100 dark:bg-primary-900 justify-center items-center">
          <Ionicons name="calendar-outline" size={20} color="#2563EB" />
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
        </View>

        {/* Boton Cancelar */}
        <Pressable
          onPress={function () { onCancel(appointment.id); }}
          className="px-3 py-1.5 rounded-full border border-error"
        >
          <AppText variant="caption" weight="medium" className="text-error">
            Cancelar
          </AppText>
        </Pressable>
      </View>
    </FloatingCard>
  );
}
