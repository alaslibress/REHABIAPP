import { View } from 'react-native';
import { AppText } from './AppText';
import type { GameDifficulty } from '../types/games';

type Props = { level: GameDifficulty };

const CONFIG: Record<GameDifficulty, { label: string; bg: string; text: string }> = {
  EASY:   { label: 'Facil',  bg: 'bg-success/10',  text: 'text-success' },
  MEDIUM: { label: 'Media',  bg: 'bg-primary-600/10', text: 'text-primary-600' },
  HARD:   { label: 'Alta',   bg: 'bg-error/10',    text: 'text-error' },
};

export function DifficultyBadge({ level }: Props) {
  const { label, bg, text } = CONFIG[level] ?? CONFIG.EASY;
  return (
    <View className={`self-start px-2 py-0.5 rounded-full ${bg}`}>
      <AppText variant="caption" weight="medium" className={text}>
        {label}
      </AppText>
    </View>
  );
}
