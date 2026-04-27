import { View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { AppText } from './AppText';

type IoniconName = keyof typeof Ionicons.glyphMap;

type EmptyStateProps = {
  icon: IoniconName;
  title: string;
  message?: string;
};

// Estado vacio centrado con icono, titulo y mensaje opcional
export function EmptyState(props: EmptyStateProps) {
  const { icon, title, message } = props;

  return (
    <View className="flex-1 justify-center items-center px-8 gap-4">
      <Ionicons name={icon} size={56} color="#64748B" />

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
