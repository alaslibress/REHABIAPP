import { View } from 'react-native';
import { AppText } from './AppText';

type PathologyPillProps = {
  name: string;
  level: number;
};

// Pill de patologia con nombre y nivel de progresion
export function PathologyPill(props: PathologyPillProps) {
  const { name, level } = props;

  return (
    <View className="flex-row items-center gap-2 bg-primary-100 dark:bg-primary-900 px-3 py-2 rounded-full">
      <AppText variant="label" weight="medium" className="text-primary-700 dark:text-primary-300">
        {name}
      </AppText>
      <View className="bg-primary-600 rounded-full px-2 py-0.5">
        <AppText variant="caption" weight="semibold" className="text-white">
          {`Nv. ${level}`}
        </AppText>
      </View>
    </View>
  );
}
