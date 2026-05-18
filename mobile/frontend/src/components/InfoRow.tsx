import React from 'react';
import { View } from 'react-native';
import type { IconProps } from 'phosphor-react-native';
import { AppText } from './AppText';
import { useTheme } from '../utils/theme';

type InfoRowProps = {
  label: string;
  value: string | null;
  // Componente icono Phosphor (ej. <IdentificationCard />) — opcional
  Icon?: React.ComponentType<IconProps>;
};

// Fila de datos con etiqueta a la izquierda y valor a la derecha
export function InfoRow(props: InfoRowProps) {
  const { label, value, Icon } = props;
  const { theme } = useTheme();

  return (
    <View className="flex-row justify-between items-center py-3 border-b border-border dark:border-border-dark last:border-b-0">
      <View className="flex-row items-center gap-2 flex-1">
        {Icon && <Icon size={16} color={theme.text3} weight="regular" />}
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
