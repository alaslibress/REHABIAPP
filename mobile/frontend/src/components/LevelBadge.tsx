import { View } from 'react-native';
import { AppText } from './AppText';

type Props = {
  level: number;
  name?: string;
};

export function LevelBadge({ level, name }: Props) {
  const label = name ? `Nv. ${level} · ${name}` : `Nivel ${level}`;

  return (
    <View className="self-start px-2 py-0.5 rounded-full bg-primary-600/10">
      <AppText variant="caption" weight="medium" className="text-primary-700 dark:text-primary-300">
        {label}
      </AppText>
    </View>
  );
}
