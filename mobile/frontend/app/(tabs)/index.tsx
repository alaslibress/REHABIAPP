import { useEffect } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, View, Text } from 'react-native';
import { useRouter, type Href } from 'expo-router';
import {
  GearSix,
  CalendarBlank,
  UserCircle,
  ChartBar,
  GameController,
  FirstAidKit,
} from 'phosphor-react-native';
import { useUserStore } from '../../src/store/userStore';
import { useErrorStore } from '../../src/store/errorStore';
import { useBootstrapStore } from '../../src/store/bootstrapStore';
import { Bubble } from '../../src/components/Bubble';
import { Positioned } from '../../src/components/Positioned';
import { useTheme } from '../../src/utils/theme';
import { getGreeting } from '../../src/utils/greeting';

// Pantalla Inicio rediseñada (rebrand 2026-05): 6 burbujas flotantes
// con animaciones asincronas (delay + duration unicos por burbuja).
// La burbuja central "Perfil" usa variante big con fondo accent.
export default function HomeScreen() {
  const router = useRouter();
  const { theme } = useTheme();
  const patient = useUserStore(function (s) { return s.patient; });
  const fetchProfile = useUserStore(function (s) { return s.fetchProfile; });
  const showError = useErrorStore(function (s) { return s.showError; });
  const refrescando = useBootstrapStore(function (s) { return s.refreshing; });
  const refrescar = useBootstrapStore(function (s) { return s.hydrate; });
  const patientName = patient?.name ?? 'Paciente';

  // Cargar el perfil del paciente al montar la pantalla de inicio una sola vez
  useEffect(function () {
    if (!patient) {
      fetchProfile().catch(function (err) {
        showError(err);
      });
    }
  }, []);

  function go(route: string) {
    router.push(route as Href);
  }

  return (
    <View style={{ flex: 1, backgroundColor: theme.bg }}>
      <ScrollView
        contentContainerStyle={{ flexGrow: 1, padding: 16 }}
        refreshControl={<RefreshControl refreshing={refrescando} onRefresh={refrescar} />}
      >
        {/* Tarjeta de saludo */}
        <View
          style={{
            backgroundColor: theme.elev1,
            borderColor: theme.border,
            borderWidth: 1,
            borderRadius: 14,
            padding: 16,
            marginBottom: 16,
          }}
        >
          <Text
            style={{
              color: theme.text,
              fontWeight: '700',
              fontSize: 15,
              textAlign: 'center',
            }}
          >
            {getGreeting(patientName)}
          </Text>
        </View>

        {/* Spinner inicial mientras no haya datos del paciente */}
        {patient == null && refrescando ? (
          <View style={{ paddingVertical: 12, alignItems: 'center' }}>
            <ActivityIndicator size="small" color={theme.accent} />
          </View>
        ) : null}

        {/* Area de burbujas flotantes */}
        <View style={{ flex: 1, position: 'relative', minHeight: 480 }}>
          <Positioned top="6%" left="12%">
            <Bubble Icon={GearSix} label="Ajustes" delay={-200} duration={5600} onPress={function () { go('/(tabs)/settings'); }} />
          </Positioned>
          <Positioned top="6%" right="12%">
            <Bubble Icon={CalendarBlank} label="Citas" delay={-1400} duration={6200} onPress={function () { go('/(tabs)/appointments'); }} />
          </Positioned>
          <Positioned top="28%" left="50%" centerX>
            <Bubble big Icon={UserCircle} label="Perfil" delay={-800} duration={7000} onPress={function () { go('/(tabs)/profile'); }} />
          </Positioned>
          <Positioned top="48%" left="6%">
            <Bubble Icon={ChartBar} label="Progreso" delay={-2000} duration={6500} onPress={function () { go('/(tabs)/progress'); }} />
          </Positioned>
          <Positioned top="50%" right="8%">
            <Bubble Icon={GameController} label="Juegos" delay={-2600} duration={5900} onPress={function () { go('/(tabs)/games'); }} />
          </Positioned>
          <Positioned top="72%" left="50%" centerX>
            <Bubble Icon={FirstAidKit} label="Cura" delay={-3200} duration={6800} onPress={function () { go('/(tabs)/treatments'); }} />
          </Positioned>
        </View>
      </ScrollView>
    </View>
  );
}
