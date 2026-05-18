import React, { useEffect } from 'react';
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
import type { IconProps } from 'phosphor-react-native';
import { useTheme } from '../utils/theme';

// Propiedades del componente globo flotante (rebrand Phosphor + tokens)
type FloatingBalloonProps = {
  Icon: React.ComponentType<IconProps>;
  size: number;
  positionX: number;
  positionY: number;
  animationDelay: number;
  animationDuration: number;
  onPress: () => void;
};

const AnimatedPressable = Animated.createAnimatedComponent(Pressable);

// Globo flotante con animacion continua de oscilacion vertical
export function FloatingBalloon(props: FloatingBalloonProps) {
  const {
    Icon,
    size,
    positionX,
    positionY,
    animationDelay,
    animationDuration,
    onPress,
  } = props;

  const { theme } = useTheme();
  const translateY = useSharedValue(0);

  useEffect(function () {
    translateY.value = withDelay(
      animationDelay,
      withRepeat(
        withSequence(
          withTiming(-12, { duration: animationDuration, easing: Easing.inOut(Easing.ease) }),
          withTiming(12, { duration: animationDuration, easing: Easing.inOut(Easing.ease) }),
        ),
        -1,
        true,
      ),
    );
  }, []);

  const animatedStyle = useAnimatedStyle(function () {
    return { transform: [{ translateY: translateY.value }] };
  });

  return (
    <AnimatedPressable
      onPress={onPress}
      style={[
        {
          position: 'absolute',
          left: `${positionX}%`,
          top: `${positionY}%`,
          backgroundColor: theme.bubble,
          borderColor: theme.border,
          borderWidth: 1,
        },
        animatedStyle,
      ]}
      className="min-h-12 min-w-12 w-20 h-20 rounded-full items-center justify-center shadow-lg"
    >
      <Icon size={size} color={theme.accent} weight="regular" />
    </AnimatedPressable>
  );
}
