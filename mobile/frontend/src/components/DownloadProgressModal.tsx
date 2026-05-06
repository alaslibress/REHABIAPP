import { View, ActivityIndicator } from 'react-native';
import { BackdropModal } from './BackdropModal';
import { AppText } from './AppText';

type Props = {
  visible: boolean;
};

export function DownloadProgressModal({ visible }: Props) {
  return (
    <BackdropModal visible={visible} onClose={function () {}}>
      <View className="bg-surface dark:bg-surface-dark rounded-2xl p-6 items-center gap-4">
        <ActivityIndicator size="large" color="#2563EB" />
        <AppText variant="body" weight="medium" className="text-text-primary dark:text-text-primary-dark text-center">
          Descargando documento...
        </AppText>
      </View>
    </BackdropModal>
  );
}
