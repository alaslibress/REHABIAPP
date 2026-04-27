import { Pressable, ScrollView, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { BackdropModal } from './BackdropModal';
import { AppText } from './AppText';
import { useTheme } from '../utils/theme';

type LegalTextModalProps = {
  visible: boolean;
  title: string;
  onClose: () => void;
};

// Modal generico para mostrar un texto legal de muestra.
// Indica que el proyecto es una prueba no adaptada a ningun hospital concreto.
export function LegalTextModal(props: LegalTextModalProps) {
  const { visible, title, onClose } = props;
  const { scheme } = useTheme();
  const isDark = scheme === 'dark';

  return (
    <BackdropModal visible={visible} onClose={onClose}>
      <View className={`rounded-2xl p-5 ${isDark ? 'bg-surface-dark' : 'bg-surface'}`} style={{ maxHeight: '80%' }}>
        {/* Cabecera */}
        <View className="flex-row justify-between items-center mb-3">
          <AppText
            variant="subtitle"
            weight="semibold"
            className="text-text-primary dark:text-text-primary-dark flex-1"
          >
            {title}
          </AppText>
          <Pressable onPress={onClose} hitSlop={12}>
            <Ionicons name="close-outline" size={24} color={isDark ? '#F1F5F9' : '#1E293B'} />
          </Pressable>
        </View>

        {/* Cuerpo */}
        <ScrollView>
          <AppText
            variant="body"
            className="text-text-secondary dark:text-text-secondary-dark leading-6 mb-3"
          >
            Este documento es una muestra generica. RehabiAPP es actualmente un
            proyecto de prueba y no esta adaptado a ningun hospital ni entidad
            sanitaria concreta.
          </AppText>
          <AppText
            variant="body"
            className="text-text-secondary dark:text-text-secondary-dark leading-6 mb-3"
          >
            En caso de que la aplicacion sea adquirida por una empresa
            contratante, este texto sera sustituido por la politica legal real
            adaptada a los requisitos del cliente (RGPD, LOPDGDD, Ley 41/2002 y
            normativas internas del centro).
          </AppText>
          <AppText
            variant="body"
            className="text-text-secondary dark:text-text-secondary-dark leading-6"
          >
            Hasta entonces, ninguno de los terminos aqui mostrados constituye un
            acuerdo vinculante.
          </AppText>
        </ScrollView>

        {/* Accion de cierre */}
        <Pressable
          onPress={onClose}
          className="mt-4 py-3 rounded-xl bg-primary-600 items-center min-h-12"
        >
          <AppText variant="body" weight="semibold" className="text-white">
            Entendido
          </AppText>
        </Pressable>
      </View>
    </BackdropModal>
  );
}
