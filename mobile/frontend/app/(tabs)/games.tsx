import { useCallback, useState } from 'react';
import { FlatList, View, RefreshControl } from 'react-native';
import { useGamesStore } from '../../src/store/gamesStore';
import { GameCard } from '../../src/components/GameCard';
import { EmptyState } from '../../src/components/EmptyState';
import { useTheme } from '../../src/utils/theme';
import type { AssignedGame } from '../../src/types/games';

export default function GamesScreen() {
  const { scheme } = useTheme();
  const items = useGamesStore(function (s) { return s.items; });
  const loading = useGamesStore(function (s) { return s.loading; });
  const fetchJuegos = useGamesStore(function (s) { return s.fetch; });

  const [refrescando, setRefrescando] = useState(false);

  const handleRefresh = useCallback(async function () {
    setRefrescando(true);
    await fetchJuegos();
    setRefrescando(false);
  }, [fetchJuegos]);

  function handlePlay(id: string) {
    // TODO: abrir WebView con webglUrl del juego
    console.log('Jugar:', id);
  }

  const bgClass = scheme === 'dark' ? 'bg-background-dark' : 'bg-background';

  if (items.length === 0 && !loading) {
    return (
      <View className={`flex-1 ${bgClass} justify-center items-center p-6`}>
        <EmptyState
          icon="heart-circle-outline"
          title="¡Que sano estas!"
          message="No tienes juegos de rehabilitacion pendientes."
        />
      </View>
    );
  }

  return (
    <FlatList<AssignedGame>
      className={`flex-1 ${bgClass}`}
      contentContainerStyle={{ padding: 24, paddingBottom: 40 }}
      data={items}
      keyExtractor={function (item) { return item.id; }}
      numColumns={2}
      columnWrapperStyle={{ gap: 16 }}
      renderItem={function ({ item }) {
        return <GameCard game={item} onPlay={handlePlay} />;
      }}
      refreshControl={
        <RefreshControl
          refreshing={refrescando}
          onRefresh={handleRefresh}
          tintColor="#2563EB"
          colors={['#2563EB']}
        />
      }
    />
  );
}
