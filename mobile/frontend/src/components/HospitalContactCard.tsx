import { Linking, Pressable, View } from 'react-native';
import { Phone, Envelope } from 'phosphor-react-native';
import { FloatingCard } from './FloatingCard';
import { useTheme } from '../utils/theme';
import { AppText } from './AppText';
import { WhatsAppButton } from './WhatsAppButton';

// Datos de contacto del hospital — constantes visibles al paciente
const HOSPITAL_PHONE = '+34 628 67 85 90';
const HOSPITAL_EMAIL = 'cita@rehabiapp.com';

// Tarjeta de contacto: telefono + email + WhatsApp. Sustituye al formulario de cita.
export function HospitalContactCard() {
  const { theme } = useTheme();
  function handleLlamar() {
    Linking.openURL(`tel:${HOSPITAL_PHONE.replace(/\s+/g, '')}`).catch(function () {});
  }

  function handleEmail() {
    Linking.openURL(`mailto:${HOSPITAL_EMAIL}`).catch(function () {});
  }

  return (
    <FloatingCard className="mb-4">
      <AppText
        variant="subtitle"
        weight="semibold"
        className="text-text-primary dark:text-text-primary-dark mb-2"
      >
        Pedir cita nueva
      </AppText>

      <AppText
        variant="body"
        className="text-text-secondary dark:text-text-secondary-dark mb-4 leading-6"
      >
        Para solicitar una cita, contacta con el hospital por telefono o email.
        Tambien puedes escribirnos por WhatsApp.
      </AppText>

      {/* Telefono */}
      <Pressable
        onPress={handleLlamar}
        className="flex-row items-center gap-3 py-3 px-3 rounded-xl bg-background dark:bg-background-dark border border-border dark:border-border-dark mb-3 min-h-12"
      >
        <Phone size={22} color={theme.accent} weight="regular" />
        <View className="flex-1">
          <AppText variant="caption" className="text-text-secondary dark:text-text-secondary-dark">
            Telefono
          </AppText>
          <AppText
            variant="body"
            weight="medium"
            className="text-text-primary dark:text-text-primary-dark"
          >
            {HOSPITAL_PHONE}
          </AppText>
        </View>
      </Pressable>

      {/* Email */}
      <Pressable
        onPress={handleEmail}
        className="flex-row items-center gap-3 py-3 px-3 rounded-xl bg-background dark:bg-background-dark border border-border dark:border-border-dark mb-4 min-h-12"
      >
        <Envelope size={22} color={theme.accent} weight="regular" />
        <View className="flex-1">
          <AppText variant="caption" className="text-text-secondary dark:text-text-secondary-dark">
            Email
          </AppText>
          <AppText
            variant="body"
            weight="medium"
            className="text-text-primary dark:text-text-primary-dark"
          >
            {HOSPITAL_EMAIL}
          </AppText>
        </View>
      </Pressable>

      {/* Divisor */}
      <View className="border-t border-border dark:border-border-dark mb-4" />

      {/* WhatsApp */}
      <WhatsAppButton />
    </FloatingCard>
  );
}
