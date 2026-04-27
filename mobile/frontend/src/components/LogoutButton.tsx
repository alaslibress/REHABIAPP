import { Pressable, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { AppText } from './AppText';

type LogoutButtonProps = {
  onPress: () => void;
};

// Boton de cerrar sesion — outline rojo, ancho completo, altura 56dp
export function LogoutButton(props: LogoutButtonProps) {
  const { onPress } = props;

  return (
    <Pressable
      onPress={onPress}
      className="flex-row items-center justify-center gap-2 border border-error rounded-xl h-14 w-full"
      style={function ({ pressed }) { return pressed ? { opacity: 0.7 } : {}; }}
    >
      <Ionicons name="log-out-outline" size={20} color="#EF4444" />
      <AppText variant="body" weight="semibold" className="text-error">
        Cerrar sesion
      </AppText>
    </Pressable>
  );
}
