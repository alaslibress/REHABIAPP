import { View, Pressable } from 'react-native';
import { BackdropModal } from './BackdropModal';
import { AppText } from './AppText';

type ConfirmModalProps = {
  visible: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  destructive?: boolean;
  singleAction?: boolean;
};

// Modal de confirmacion generico con boton de cancelar y confirmar
export function ConfirmModal(props: ConfirmModalProps) {
  const {
    visible,
    title,
    message,
    confirmLabel = 'Confirmar',
    cancelLabel = 'Cancelar',
    onConfirm,
    onCancel,
    destructive = false,
    singleAction = false,
  } = props;

  const confirmClass = destructive
    ? 'min-h-12 px-5 py-3 rounded-lg bg-error justify-center items-center'
    : 'min-h-12 px-5 py-3 rounded-lg bg-primary-600 justify-center items-center';

  return (
    <BackdropModal visible={visible} onClose={onCancel}>
      <View className="bg-surface dark:bg-surface-dark rounded-2xl p-6 w-full shadow-lg">
        {/* Titulo */}
        <AppText variant="subtitle" weight="bold" className="text-text-primary dark:text-text-primary-dark mb-2">
          {title}
        </AppText>

        {/* Mensaje */}
        <AppText variant="body" className="text-text-secondary dark:text-text-secondary-dark mb-6 leading-6">
          {message}
        </AppText>

        {/* Botones */}
        <View className="flex-row justify-end gap-3">
          {!singleAction && (
            <Pressable
              onPress={onCancel}
              className="min-h-12 px-5 py-3 rounded-lg border border-primary-600 justify-center items-center"
            >
              <AppText variant="body" weight="medium" className="text-primary-600">
                {cancelLabel}
              </AppText>
            </Pressable>
          )}

          <Pressable
            onPress={onConfirm}
            className={confirmClass}
          >
            <AppText variant="body" weight="medium" className="text-white">
              {confirmLabel}
            </AppText>
          </Pressable>
        </View>
      </View>
    </BackdropModal>
  );
}
