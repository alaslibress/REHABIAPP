import { View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { AppText } from './AppText';
import { useTheme } from '../utils/theme';

type IoniconName = keyof typeof Ionicons.glyphMap;

type InfoRowProps = {
  label: string;
  value: string | null;
  icon?: IoniconName;
};

// Fila de datos con etiqueta a la izquierda y valor a la derecha
export function InfoRow(props: InfoRowProps) {
  const { label, value, icon } = props;
  const { scheme } = useTheme();
  const iconColor = scheme === 'dark' ? '#94A3B8' : '#64748B';

  return (
    <View className="flex-row justify-between items-center py-3 border-b border-border dark:border-border-dark last:border-b-0">
      <View className="flex-row items-center gap-2 flex-1">
        {icon && <Ionicons name={icon} size={16} color={iconColor} />}
        <AppText variant="label" className="text-text-secondary dark:text-text-secondary-dark">
          {label}
        </AppText>
      </View>
      <AppText variant="label" weight="medium" className="text-text-primary dark:text-text-primary-dark text-right flex-shrink-0 max-w-[55%]" numberOfLines={1}>
        {value ?? '—'}
      </AppText>
    </View>
  );
}
