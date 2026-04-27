import { View } from 'react-native';

type ProgressBarProps = {
  value: number; // 0..100
};

export function ProgressBar(props: ProgressBarProps) {
  const { value } = props;
  const clampedPct = Math.max(0, Math.min(100, value));

  return (
    <View className="h-2 bg-border dark:bg-border-dark rounded-full overflow-hidden w-full">
      <View
        className="h-full bg-primary-600 rounded-full"
        style={{ width: `${clampedPct}%` }}
      />
    </View>
  );
}
