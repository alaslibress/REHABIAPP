import { Pressable, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { AppText } from './AppText';

// Boton de acceso a WhatsApp — funcionalidad pendiente de implementar
export function WhatsAppButton() {
  function handlePress() {
    // TODO: abrir WhatsApp con Linking.openURL('whatsapp://send?phone=...')
  }

  return (
    <Pressable
      onPress={handlePress}
      className="flex-row items-center justify-center gap-2 border border-success rounded-full py-3 px-5 min-h-12"
    >
      <Ionicons name="logo-whatsapp" size={20} color="#22C55E" />
      <AppText variant="body" weight="medium" className="text-success">
        Abrir WhatsApp
      </AppText>
    </Pressable>
  );
}
