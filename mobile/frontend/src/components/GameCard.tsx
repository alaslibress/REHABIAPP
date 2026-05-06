import { View, Image, Pressable, Dimensions } from 'react-native';
import Ionicons from '@expo/vector-icons/Ionicons';
import { FloatingCard } from './FloatingCard';
import { AppText } from './AppText';
import { DifficultyBadge } from './DifficultyBadge';
import type { AssignedGame } from '../types/games';

type Props = {
  game: AssignedGame;
  onPlay: (id: string) => void;
};

const CARD_WIDTH = (Dimensions.get('window').width - 48) / 2;

export function GameCard({ game, onPlay }: Props) {
  return (
    <FloatingCard style={{ width: CARD_WIDTH, marginBottom: 16 }}>
      {/* Thumbnail o placeholder */}
      {game.thumbnailUrl ? (
        <Image
          source={{ uri: game.thumbnailUrl }}
          style={{ width: '100%', height: 120, borderRadius: 12 }}
          resizeMode="cover"
        />
      ) : (
        <View
          className="w-full rounded-xl justify-center items-center bg-primary-600/10"
          style={{ height: 120 }}
        >
          <Ionicons name="game-controller-outline" size={40} color="#2563EB" />
        </View>
      )}

      <View className="mt-3 flex-1">
        {/* Nombre */}
        <AppText
          variant="label"
          weight="semibold"
          numberOfLines={2}
          className="text-text-primary dark:text-text-primary-dark mb-1"
        >
          {game.name}
        </AppText>

        {/* Descripcion */}
        <AppText
          variant="caption"
          numberOfLines={3}
          className="text-text-secondary dark:text-text-secondary-dark mb-2"
        >
          {game.description}
        </AppText>

        {/* Dificultad */}
        <DifficultyBadge level={game.difficulty} />
      </View>

      {/* Boton Jugar */}
      <Pressable
        onPress={function () {
          // TODO: abrir WebView con game.webglUrl
          onPlay(game.id);
        }}
        className="mt-3 bg-primary-600 rounded-xl py-2 justify-center items-center min-h-[40px]"
      >
        <AppText variant="label" weight="semibold" className="text-white">
          Jugar
        </AppText>
      </Pressable>
    </FloatingCard>
  );
}
