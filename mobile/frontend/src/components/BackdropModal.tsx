import { Modal, Pressable, View } from 'react-native';

type BackdropModalProps = {
  visible: boolean;
  onClose: () => void;
  children: React.ReactNode;
};

// Modal base con fondo oscurecido — pulsar el backdrop lo cierra
export function BackdropModal(props: BackdropModalProps) {
  const { visible, onClose, children } = props;

  return (
    <Modal
      transparent={true}
      visible={visible}
      animationType="fade"
      onRequestClose={onClose}
    >
      <Pressable
        className="flex-1 justify-center items-center bg-black/40 px-6"
        onPress={onClose}
      >
        {/* Bloquear propagacion para que pulsar dentro no cierre el modal */}
        <Pressable onPress={() => {}} className="w-full max-w-sm">
          {children}
        </Pressable>
      </Pressable>
    </Modal>
  );
}
