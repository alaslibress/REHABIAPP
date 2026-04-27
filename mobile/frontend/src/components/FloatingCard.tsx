import { Pressable, View } from 'react-native';
import type { ViewProps } from 'react-native';

type FloatingCardProps = ViewProps & {
  className?: string;
  children: React.ReactNode;
  onPress?: () => void;
};

// Tarjeta flotante con bordes redondeados, sombra suave y soporte dark mode
export function FloatingCard(props: FloatingCardProps) {
  const { className = '', children, onPress, style, ...rest } = props;

  const baseClass = `bg-surface dark:bg-surface-dark rounded-2xl p-4 shadow-sm ${className}`;
  const androidElevation = { elevation: 2 };

  if (onPress) {
    return (
      <Pressable
        onPress={onPress}
        style={[androidElevation, style as any]}
        className={baseClass}
        android_ripple={{ color: '#E2E8F0', borderless: false }}
        {...rest}
      >
        {children}
      </Pressable>
    );
  }

  return (
    <View
      style={[androidElevation, style as any]}
      className={baseClass}
      {...rest}
    >
      {children}
    </View>
  );
}
