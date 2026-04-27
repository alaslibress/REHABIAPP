import { View, Pressable } from 'react-native';
import { BackdropModal } from './BackdropModal';
import { AppText } from './AppText';
import type { ErrorPopupProps } from '../types/errors';

// Ventana emergente de error con titulo, subtitulo, descripcion y botones
export function ErrorPopup(props: ErrorPopupProps) {
  const { error, visible, onAccept, onCancel } = props;

  if (!error) return null;

  return (
    <BackdropModal visible={visible} onClose={onCancel}>
      <View className="bg-surface dark:bg-surface-dark rounded-2xl p-6 w-full shadow-lg">
        {/* Titulo */}
        <AppText variant="subtitle" weight="bold" className="text-text-primary dark:text-text-primary-dark mb-1">
          {error.title}
        </AppText>

        {/* Subtitulo */}
        <AppText variant="body" weight="medium" className="text-error mb-3">
          {error.subtitle}
        </AppText>

        {/* Descripcion */}
        <AppText variant="body" className="text-text-secondary dark:text-text-secondary-dark mb-6 leading-6">
          {error.message}
        </AppText>

        {/* Botones alineados a la derecha */}
        <View className="flex-row justify-end gap-3">
          <Pressable
            onPress={onCancel}
            className="min-h-12 min-w-12 px-5 py-3 rounded-lg border border-primary-600 justify-center items-center"
          >
            <AppText variant="body" weight="medium" className="text-primary-600">
              Cancelar
            </AppText>
          </Pressable>

          <Pressable
            onPress={onAccept}
            className="min-h-12 min-w-12 px-5 py-3 rounded-lg bg-primary-600 justify-center items-center"
          >
            <AppText variant="body" weight="medium" className="text-white">
              Aceptar
            </AppText>
          </Pressable>
        </View>
      </View>
    </BackdropModal>
  );
}
