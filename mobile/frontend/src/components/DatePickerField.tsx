import { useState } from 'react';
import { Pressable, View, Platform } from 'react-native';
import DateTimePicker from '@react-native-community/datetimepicker';
import { Ionicons } from '@expo/vector-icons';
import { AppText } from './AppText';

type DatePickerFieldProps = {
  label: string;
  value: Date;
  onChange: (date: Date) => void;
  mode: 'date' | 'time';
  minimumDate?: Date;
};

// Formatea una fecha a "DD/MM/YYYY" o una hora a "HH:mm"
function formatear(value: Date, mode: 'date' | 'time'): string {
  if (mode === 'time') {
    const h = value.getHours().toString().padStart(2, '0');
    const m = value.getMinutes().toString().padStart(2, '0');
    return `${h}:${m}`;
  }
  const d = value.getDate().toString().padStart(2, '0');
  const mo = (value.getMonth() + 1).toString().padStart(2, '0');
  const y = value.getFullYear();
  return `${d}/${mo}/${y}`;
}

// Campo presionable que muestra el valor formateado y abre el selector nativo
export function DatePickerField(props: DatePickerFieldProps) {
  const { label, value, onChange, mode, minimumDate } = props;
  const [mostrar, setMostrar] = useState(false);

  function handleChange(_event: any, fecha?: Date) {
    // En Android el picker se cierra solo al seleccionar
    if (Platform.OS === 'android') setMostrar(false);
    if (fecha) onChange(fecha);
  }

  return (
    <View>
      <AppText variant="caption" weight="medium" className="text-text-secondary dark:text-text-secondary-dark mb-1">
        {label}
      </AppText>

      <Pressable
        onPress={function () { setMostrar(true); }}
        className="flex-row items-center justify-between bg-background dark:bg-background-dark border border-border dark:border-border-dark rounded-xl px-4 py-3 min-h-12"
      >
        <AppText variant="body" className="text-text-primary dark:text-text-primary-dark">
          {formatear(value, mode)}
        </AppText>
        <Ionicons
          name={mode === 'date' ? 'calendar-outline' : 'time-outline'}
          size={18}
          color="#64748B"
        />
      </Pressable>

      {mostrar && (
        <DateTimePicker
          value={value}
          mode={mode}
          display={Platform.OS === 'ios' ? 'spinner' : 'default'}
          onChange={handleChange}
          minimumDate={minimumDate}
          locale="es-ES"
        />
      )}

      {/* En iOS cerrar el picker al pulsar fuera */}
      {mostrar && Platform.OS === 'ios' && (
        <Pressable
          onPress={function () { setMostrar(false); }}
          className="mt-2 py-2 items-center"
        >
          <AppText variant="label" weight="medium" className="text-primary-600">
            Listo
          </AppText>
        </Pressable>
      )}
    </View>
  );
}
