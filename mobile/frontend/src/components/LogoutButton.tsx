import { Pressable } from 'react-native';
import { SignOut } from 'phosphor-react-native';
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
      <SignOut size={20} color="#E04848" weight="regular" />
      <AppText variant="body" weight="semibold" className="text-error">
        Cerrar sesion
      </AppText>
    </Pressable>
  );
}
