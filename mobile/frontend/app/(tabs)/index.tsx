import { useEffect } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, View, Text } from 'react-native';
import { useRouter } from 'expo-router';
import { useUserStore } from '../../src/store/userStore';
import { useErrorStore } from '../../src/store/errorStore';
import { useBootstrapStore } from '../../src/store/bootstrapStore';
import { FloatingBalloon } from '../../src/components/FloatingBalloon';
import { getGreeting } from '../../src/utils/greeting';

// Configuracion de cada globo: icono, ruta y parametros de animacion
// Las posiciones simulan un desorden organico pero visualmente equilibrado
type BalloonConfig = {
  id: string;
  iconName: string;
  route: string;
  positionX: number;
  positionY: number;
  animationDelay: number;
  animationDuration: number;
};

const BALLOON_CONFIG: BalloonConfig[] = [
  {
    id: 'profile',
    iconName: 'person-circle-outline',
    route: '/(tabs)/profile',
    positionX: 38,
    positionY: 35,
    animationDelay: 0,
    animationDuration: 2800,
  },
  {
    id: 'settings',
    iconName: 'settings-outline',
    route: '/(tabs)/settings',
    positionX: 12,
    positionY: 18,
    animationDelay: 400,
    animationDuration: 3200,
  },
  {
    id: 'appointments',
    iconName: 'calendar-outline',
    route: '/(tabs)/appointments',
    positionX: 65,
    positionY: 15,
    animationDelay: 200,
    animationDuration: 2600,
  },
  {
    id: 'progress',
    iconName: 'bar-chart-outline',
    route: '/(tabs)/progress',
    positionX: 8,
    positionY: 55,
    animationDelay: 600,
    animationDuration: 3000,
  },
  {
    id: 'games',
    iconName: 'game-controller-outline',
    route: '/(tabs)/games',
    positionX: 62,
    positionY: 58,
    animationDelay: 300,
    animationDuration: 2400,
  },
  {
    id: 'treatments',
    iconName: 'medkit-outline',
    route: '/(tabs)/treatments',
    positionX: 35,
    positionY: 72,
    animationDelay: 500,
    animationDuration: 3400,
  },
];

export default function HomeScreen() {
  const router = useRouter();
  const patient = useUserStore(function (s) { return s.patient; });
  const fetchProfile = useUserStore(function (s) { return s.fetchProfile; });
  const showError = useErrorStore(function (s) { return s.showError; });
  const refrescando = useBootstrapStore(function (s) { return s.refreshing; });
  const refrescar = useBootstrapStore(function (s) { return s.hydrate; });
  const patientName = patient?.name ?? 'Paciente';

  // Cargar el perfil del paciente al montar la pantalla de inicio.
  // Se ejecuta una sola vez despues de que AuthGuard haya navegado aqui
  // (lo que garantiza que el token JWT ya esta almacenado en SecureStore).
  useEffect(function () {
    // Solo cargar si no tenemos el perfil aun (evita llamadas duplicadas)
    if (!patient) {
      fetchProfile().catch(function (err) {
        // fetchProfile ya parsea el error a AppError (via parseGraphQLError)
        showError(err);
      });
    }
  }, []);

  function handleBalloonPress(route: string) {
    router.push(route as any);
  }

  return (
    <View className="flex-1 bg-background">
      {/* Mensaje de bienvenida con saludo dinamico segun la hora — tarjeta centrada */}
      <View className="px-6 pt-6 pb-4">
        <View className="bg-surface dark:bg-surface-dark rounded-2xl px-5 py-4 shadow-md border border-primary-200 dark:border-primary-600">
          <Text className="text-2xl font-bold text-text-primary dark:text-text-primary-dark leading-8 text-center">
            {getGreeting(patientName)}
          </Text>
        </View>
      </View>

      {/* Spinner mientras el paciente no se ha cargado aun */}
      {patient == null && refrescando ? (
        <View className="py-3 items-center">
          <ActivityIndicator size="small" />
        </View>
      ) : null}

      {/* Area de globos flotantes con posiciones organicas */}
      <ScrollView
        contentContainerStyle={{ flexGrow: 1 }}
        refreshControl={
          <RefreshControl refreshing={refrescando} onRefresh={refrescar} />
        }
      >
        <View className="flex-1 relative mx-4 mb-4">
          {BALLOON_CONFIG.map(function (balloon) {
            return (
              <FloatingBalloon
                key={balloon.id}
                iconName={balloon.iconName as any}
                size={32}
                positionX={balloon.positionX}
                positionY={balloon.positionY}
                animationDelay={balloon.animationDelay}
                animationDuration={balloon.animationDuration}
                onPress={function () { handleBalloonPress(balloon.route); }}
              />
            );
          })}
        </View>
      </ScrollView>
    </View>
  );
}
