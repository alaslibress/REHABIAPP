import { View } from 'react-native';
import { AppText } from './AppText';
import { useTheme } from '../utils/theme';

type SettingsSectionProps = {
  title: string;
  children: React.ReactNode;
};

// Tarjeta flotante con titulo de seccion para ajustes
export function SettingsSection(props: SettingsSectionProps) {
  const { title, children } = props;
  const { scheme } = useTheme();
  const cardClass = scheme === 'dark' ? 'bg-surface-dark' : 'bg-surface';

  return (
    <View className={`rounded-2xl overflow-hidden ${cardClass}`}>
      <View className="px-4 pt-4 pb-2">
        <AppText variant="label" weight="semibold" className="text-text-secondary dark:text-text-secondary-dark uppercase tracking-widest">
          {title}
        </AppText>
      </View>
      <View className="border-t border-border dark:border-border-dark">
        {children}
      </View>
    </View>
  );
}
