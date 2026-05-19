import { Linking, Pressable } from 'react-native';
import { WhatsappLogo } from 'phosphor-react-native';
import { AppText } from './AppText';

// Numero de WhatsApp del centro de rehabilitacion. Formato E.164 sin '+'
// porque la URL whatsapp://send?phone= NO acepta el simbolo '+' (lo reescribe).
// Coincide con HOSPITAL_PHONE de HospitalContactCard pero sin espacios ni '+'.
const WHATSAPP_PHONE = '34641710369';
const WHATSAPP_MESSAGE = 'Hola, soy paciente de RehabiAPP y quiero pedir una cita.';

// Construye la URL whatsapp:// con el mensaje URL-encoded
function buildWhatsappUrl(): string {
  return `whatsapp://send?phone=${WHATSAPP_PHONE}&text=${encodeURIComponent(WHATSAPP_MESSAGE)}`;
}

// Fallback web si la app de WhatsApp no esta instalada en el dispositivo.
// wa.me redirecciona a la web oficial de WhatsApp con el mismo mensaje.
function buildWhatsappWebUrl(): string {
  return `https://wa.me/${WHATSAPP_PHONE}?text=${encodeURIComponent(WHATSAPP_MESSAGE)}`;
}

// Boton de acceso a WhatsApp — abre la app nativa o la web si no esta instalada
export function WhatsAppButton() {
  async function handlePress() {
    const deepLink = buildWhatsappUrl();
    try {
      const soportado = await Linking.canOpenURL(deepLink);
      if (soportado) {
        await Linking.openURL(deepLink);
        return;
      }
    } catch {
      // Algunos dispositivos / Expo Go bloquean canOpenURL para deep links
      // no listados en LSApplicationQueriesSchemes. Caemos al fallback web.
    }
    try {
      await Linking.openURL(buildWhatsappWebUrl());
    } catch {
      // Sin navegador disponible — fallback silencioso. El usuario tiene
      // el numero visible en la tarjeta de contacto justo encima del boton.
    }
  }

  return (
    <Pressable
      onPress={handlePress}
      className="flex-row items-center justify-center gap-2 border border-success rounded-full py-3 px-5 min-h-12"
    >
      <WhatsappLogo size={20} color="#2EA66A" weight="regular" />
      <AppText variant="body" weight="medium" className="text-success">
        Abrir WhatsApp
      </AppText>
    </Pressable>
  );
}
