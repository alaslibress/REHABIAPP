import { useEffect } from 'react';
import { View, Text } from 'react-native';
import { useRouter } from 'expo-router';
import { useUserStore } from '../../src/store/userStore';
import { useErrorStore } from '../../src/store/errorStore';
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
      {/* Mensaje de bienvenida con saludo dinamico segun la hora */}
      <View className="px-6 pt-6 pb-4">
        <Text className="text-2xl font-bold text-text-primary leading-8">
          {getGreeting(patientName)}
        </Text>
      </View>

      {/* Area de globos flotantes con posiciones organicas */}
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
    </View>
  );
}
