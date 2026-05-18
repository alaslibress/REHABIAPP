import React from 'react';
import { View } from 'react-native';
import type { IconProps } from 'phosphor-react-native';
import { AppText } from './AppText';
import { useTheme } from '../utils/theme';

type EmptyStateProps = {
  // Componente icono Phosphor (ej. <CalendarBlank />)
  Icon: React.ComponentType<IconProps>;
  title: string;
  message?: string;
};

// Estado vacio centrado con icono, titulo y mensaje opcional
export function EmptyState(props: EmptyStateProps) {
  const { Icon, title, message } = props;
  const { theme } = useTheme();

  return (
    <View className="flex-1 justify-center items-center px-8 gap-4">
      <Icon size={56} color={theme.text3} weight="regular" />

      <AppText
        variant="subtitle"
        weight="semibold"
        className="text-text-secondary dark:text-text-secondary-dark text-center"
      >
        {title}
      </AppText>

      {message && (
        <AppText
          variant="body"
          className="text-text-secondary dark:text-text-secondary-dark text-center leading-6"
        >
          {message}
        </AppText>
      )}
    </View>
  );
}
