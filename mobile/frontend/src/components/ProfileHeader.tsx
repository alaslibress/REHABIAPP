import { Image, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { AppText } from './AppText';
import { calcularEdad } from '../utils/mask';
import type { Patient } from '../types/user';
import { useTheme } from '../utils/theme';

type ProfileHeaderProps = {
  patient: Patient;
  avatarUri: string | null;
};

const SEXO_LABEL: Record<string, string> = {
  MASCULINO: 'Masculino',
  FEMENINO: 'Femenino',
  OTRO: 'Otro',
};

// Tarjeta flotante con avatar, nombre completo y badges de edad/sexo
export function ProfileHeader(props: ProfileHeaderProps) {
  const { patient, avatarUri } = props;
  const { scheme } = useTheme();
  const isDark = scheme === 'dark';

  const edad = calcularEdad(patient.birthDate);
  const sexoLabel = patient.sexo ? (SEXO_LABEL[patient.sexo] ?? patient.sexo) : null;

  return (
    <View className={`rounded-2xl p-6 items-center gap-4 ${isDark ? 'bg-surface-dark' : 'bg-surface'}`}>
      {/* Avatar circular */}
      <View className="w-[120px] h-[120px] rounded-full overflow-hidden bg-border dark:bg-border-dark items-center justify-center">
        {avatarUri ? (
          <Image
            source={{ uri: avatarUri }}
            className="w-full h-full"
            resizeMode="cover"
          />
        ) : (
          <Ionicons
            name="person-circle-outline"
            size={80}
            color={isDark ? '#94A3B8' : '#64748B'}
          />
        )}
      </View>

      {/* Nombre completo */}
      <AppText variant="title" weight="bold" className="text-text-primary dark:text-text-primary-dark text-center">
        {patient.name} {patient.surname}
      </AppText>

      {/* Badges edad + sexo */}
      <View className="flex-row gap-3">
        {edad !== null && (
          <View className="bg-primary-100 dark:bg-primary-900 px-3 py-1 rounded-full">
            <AppText variant="caption" weight="medium" className="text-primary-700 dark:text-primary-300">
              {`${edad} anos`}
            </AppText>
          </View>
        )}
        {sexoLabel && (
          <View className="bg-primary-100 dark:bg-primary-900 px-3 py-1 rounded-full">
            <AppText variant="caption" weight="medium" className="text-primary-700 dark:text-primary-300">
              {sexoLabel}
            </AppText>
          </View>
        )}
      </View>
    </View>
  );
}
