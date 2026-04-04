import { Modal, View, Text, Pressable } from 'react-native';
import type { ErrorPopupProps } from '../types/errors';

// Ventana emergente de error con titulo, subtitulo, descripcion y botones
export function ErrorPopup(props: ErrorPopupProps) {
  const { error, visible, onAccept, onCancel } = props;

  if (!error) return null;

  return (
    <Modal
      transparent={true}
      visible={visible}
      animationType="fade"
      onRequestClose={onCancel}
    >
      <View className="flex-1 justify-center items-center bg-black/50 px-6">
        <View className="bg-surface rounded-2xl p-6 w-full max-w-sm shadow-lg">
          {/* Titulo */}
          <Text className="text-xl font-bold text-text-primary mb-1">
            {error.title}
          </Text>

          {/* Subtitulo */}
          <Text className="text-base font-medium text-error mb-3">
            {error.subtitle}
          </Text>

          {/* Descripcion */}
          <Text className="text-base text-text-secondary mb-6 leading-6">
            {error.message}
          </Text>

          {/* Botones alineados a la derecha */}
          <View className="flex-row justify-end gap-3">
            <Pressable
              onPress={onCancel}
              className="min-h-12 min-w-12 px-5 py-3 rounded-lg border border-primary-600 justify-center items-center"
            >
              <Text className="text-primary-600 font-medium text-base">
                Cancelar
              </Text>
            </Pressable>

            <Pressable
              onPress={onAccept}
              className="min-h-12 min-w-12 px-5 py-3 rounded-lg bg-primary-600 justify-center items-center"
            >
              <Text className="text-white font-medium text-base">
                Aceptar
              </Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}
