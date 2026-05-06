import { Pressable, View } from 'react-native';
import { AppText } from './AppText';

type SettingsRowProps = {
  label: string;
  description?: string;
  right: React.ReactNode;
  onPress?: () => void;
};

// Fila de ajuste con etiqueta + descripcion opcional a la izquierda y elemento derecho
export function SettingsRow(props: SettingsRowProps) {
  const { label, description, right, onPress } = props;

  return (
    <Pressable
      onPress={onPress}
      disabled={!onPress}
      className="flex-row items-center justify-between px-4 py-4 border-b border-border dark:border-border-dark last:border-b-0"
      style={function ({ pressed }) { return (pressed && onPress) ? { opacity: 0.7 } : {}; }}
    >
      <View className="flex-1 pr-4">
        <AppText variant="body" weight="medium" className="text-text-primary dark:text-text-primary-dark">
          {label}
        </AppText>
        {description && (
          <AppText variant="caption" className="text-text-secondary dark:text-text-secondary-dark mt-0.5 leading-5">
            {description}
          </AppText>
        )}
      </View>
      <View className="flex-shrink-0">
        {right}
      </View>
    </Pressable>
  );
}
