import { View, ActivityIndicator } from 'react-native';
import { BackdropModal } from './BackdropModal';
import { AppText } from './AppText';
import { useTheme } from '../utils/theme';

type Props = {
  visible: boolean;
};

export function DownloadProgressModal({ visible }: Props) {
  const { theme } = useTheme();
  return (
    <BackdropModal visible={visible} onClose={function () {}}>
      <View className="bg-surface dark:bg-surface-dark rounded-2xl p-6 items-center gap-4">
        <ActivityIndicator size="large" color={theme.accent} />
        <AppText variant="body" weight="medium" className="text-text-primary dark:text-text-primary-dark text-center">
          Descargando documento...
        </AppText>
      </View>
    </BackdropModal>
  );
}
