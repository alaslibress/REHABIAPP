import { useEffect } from 'react';
import { Pressable } from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withSequence,
  withTiming,
  withDelay,
  Easing,
} from 'react-native-reanimated';
import { Ionicons } from '@expo/vector-icons';

// Propiedades del componente globo flotante
type FloatingBalloonProps = {
  iconName: keyof typeof Ionicons.glyphMap;
  size: number;
  positionX: number;         // Porcentaje horizontal (0-100)
  positionY: number;         // Porcentaje vertical (0-100)
  animationDelay: number;    // Milisegundos de retardo para efecto organico
  animationDuration: number; // Duracion del ciclo de oscilacion en milisegundos
  onPress: () => void;
};

const AnimatedPressable = Animated.createAnimatedComponent(Pressable);

// Globo flotante con animacion continua de oscilacion vertical
export function FloatingBalloon(props: FloatingBalloonProps) {
  const {
    iconName,
    size,
    positionX,
    positionY,
    animationDelay,
    animationDuration,
    onPress,
  } = props;

  const translateY = useSharedValue(0);

  useEffect(function () {
    // Oscilacion continua arriba y abajo con retardo inicial para efecto organico
    translateY.value = withDelay(
      animationDelay,
      withRepeat(
        withSequence(
          withTiming(-12, { duration: animationDuration, easing: Easing.inOut(Easing.ease) }),
          withTiming(12, { duration: animationDuration, easing: Easing.inOut(Easing.ease) }),
        ),
        -1,  // Repetir indefinidamente
        true // Invertir en cada repeticion
      )
    );
  }, []);

  const animatedStyle = useAnimatedStyle(function () {
    return {
      transform: [{ translateY: translateY.value }],
    };
  });

  return (
    <AnimatedPressable
      onPress={onPress}
      style={[
        {
          position: 'absolute',
          left: `${positionX}%`,
          top: `${positionY}%`,
        },
        animatedStyle,
      ]}
      className="min-h-12 min-w-12 w-20 h-20 rounded-full bg-surface items-center justify-center shadow-lg"
    >
      <Ionicons name={iconName} size={size} color="#2563EB" />
    </AnimatedPressable>
  );
}
