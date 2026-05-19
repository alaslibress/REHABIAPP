import { useCallback, useState } from 'react';
import { FlatList, View, RefreshControl, Linking } from 'react-native';
import { useGamesStore } from '../../src/store/gamesStore';
import { useUserStore } from '../../src/store/userStore';
import { useErrorStore } from '../../src/store/errorStore';
import { client } from '../../src/services/graphql/client';
import { START_GAME } from '../../src/services/graphql/queries/games';
import { parseGraphQLError } from '../../src/utils/errorHandler';
import { GameCard } from '../../src/components/GameCard';
import { EmptyState } from '../../src/components/EmptyState';
import { Heart } from 'phosphor-react-native';
import { useTheme } from '../../src/utils/theme';
import { useRefreshOnFocus } from '../../src/utils/useRefreshOnFocus';
import type { AssignedGame } from '../../src/types/games';

export default function GamesScreen() {
  const { scheme, theme } = useTheme();
  const items = useGamesStore(function (s) { return s.items; });
  const loading = useGamesStore(function (s) { return s.loading; });
  const fetchJuegos = useGamesStore(function (s) { return s.fetch; });

  // Refresca juegos al enfocar la pestana / volver del background.
  useRefreshOnFocus(fetchJuegos);

  const [refrescando, setRefrescando] = useState(false);

  const handleRefresh = useCallback(async function () {
    setRefrescando(true);
    await fetchJuegos();
    setRefrescando(false);
  }, [fetchJuegos]);

  const patient = useUserStore(function (s) { return s.patient; });
  const showError = useErrorStore(function (s) { return s.showError; });
  const [lanzandoId, setLanzandoId] = useState<string | null>(null);

  // Lanza el videojuego asignado:
  // 1. Pide al BFF un token efimero (mutation startGame) con JWT corto 5 min.
  // 2. Compone URL con `?api=...&dni=...&token=...` para que Unity envie
  //    telemetria firmada al API a nombre del paciente correcto.
  // 3. Abre la URL en el navegador del sistema (WebGL no funciona embebido en RN).
  async function handlePlay(id: string) {
    if (lanzandoId) return;
    setLanzandoId(id);
    try {
      const result = await client.mutate({
        mutation: START_GAME,
        variables: { idVideojuego: id },
      });
      const launch = (result.data as { startGame?: { urlUnity: string; ephemeralToken: string } } | undefined)?.startGame;
      if (!launch) {
        showError(parseGraphQLError(result.error ?? new Error('Respuesta vacia')));
        return;
      }
      const dniPaciente = patient?.dni ?? '';
      // Construimos la URL con los params que Unity espera (ver TelemetryUploader.cs).
      const separator = launch.urlUnity.includes('?') ? '&' : '?';
      const apiBase = 'https://rehabiapp-api.duckdns.org';
      const url = `${launch.urlUnity}${separator}api=${encodeURIComponent(apiBase)}&dni=${encodeURIComponent(dniPaciente)}&token=${encodeURIComponent(launch.ephemeralToken)}`;
      const supported = await Linking.canOpenURL(url);
      if (!supported) {
        showError(parseGraphQLError(new Error('No se puede abrir el navegador')));
        return;
      }
      await Linking.openURL(url);
    } catch (err) {
      showError(parseGraphQLError(err));
    } finally {
      setLanzandoId(null);
    }
  }

  const bgClass = scheme === 'dark' ? 'bg-background-dark' : 'bg-background';

  if (items.length === 0 && !loading) {
    return (
      <View className={`flex-1 ${bgClass} justify-center items-center p-6`}>
        <EmptyState
          Icon={Heart}
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
          tintColor={theme.accent}
          colors={[theme.accent]}
        />
      }
    />
  );
}
