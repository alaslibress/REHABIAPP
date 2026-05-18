import React, { useEffect } from 'react';
import { Pressable, Text } from 'react-native';
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

// Burbuja flotante del Inicio. Cada instancia animacion asincrona
// (delay + duration distintos) para evitar movimiento sincronizado.
type BubbleProps = {
  Icon: React.ComponentType<IconProps>;
  label: string;
  big?: boolean;
  delay?: number;
  duration?: number;
  onPress?: () => void;
};

export function Bubble(props: BubbleProps) {
  const { Icon, label, big = false, delay = 0, duration = 6000, onPress } = props;
  const { theme } = useTheme();

  const ty = useSharedValue(0);
  const tx = useSharedValue(0);
  const rot = useSharedValue(0);

  useEffect(function () {
    ty.value = withDelay(
      delay,
      withRepeat(
        withSequence(
          withTiming(-14, { duration: duration / 2, easing: Easing.inOut(Easing.quad) }),
          withTiming(0, { duration: duration / 2, easing: Easing.inOut(Easing.quad) }),
        ),
        -1,
      ),
    );
    tx.value = withDelay(
      delay,
      withRepeat(
        withSequence(
          withTiming(6, { duration: duration / 2, easing: Easing.inOut(Easing.quad) }),
          withTiming(-6, { duration: duration / 2, easing: Easing.inOut(Easing.quad) }),
        ),
        -1,
      ),
    );
    rot.value = withDelay(
      delay,
      withRepeat(
        withSequence(
          withTiming(2, { duration: duration / 2, easing: Easing.inOut(Easing.quad) }),
          withTiming(-2, { duration: duration / 2, easing: Easing.inOut(Easing.quad) }),
        ),
        -1,
      ),
    );
  }, [delay, duration]);

  const animStyle = useAnimatedStyle(function () {
    return {
      transform: [
        { translateY: ty.value },
        { translateX: tx.value },
        { rotate: `${rot.value}deg` },
      ],
    };
  });

  const size = big ? 92 : 76;
  const isBig = big;

  return (
    <Animated.View
      style={[
        animStyle,
        {
          width: size,
          height: size,
          borderRadius: size / 2,
          backgroundColor: isBig ? theme.accent2 : theme.bubble,
          borderColor: isBig ? 'transparent' : theme.border,
          borderWidth: 1,
          alignItems: 'center',
          justifyContent: 'center',
          shadowColor: theme.shadow.color,
          shadowOffset: theme.shadow.offset,
          shadowOpacity: isBig ? theme.shadowStrong.opacity : theme.shadow.opacity,
          shadowRadius: isBig ? theme.shadowStrong.radius : theme.shadow.radius,
          elevation: isBig ? 12 : 6,
        },
      ]}
    >
      <Pressable
        onPress={onPress}
        style={{ alignItems: 'center', justifyContent: 'center', width: '100%', height: '100%' }}
      >
        <Icon
          size={isBig ? 38 : 30}
          color={isBig ? '#FFFFFF' : theme.accent}
          weight="regular"
        />
        <Text
          style={{
            color: isBig ? '#FFFFFF' : theme.text2,
            fontSize: 10,
            fontWeight: '700',
            marginTop: 6,
            letterSpacing: 0.5,
          }}
        >
          {label.toUpperCase()}
        </Text>
      </Pressable>
    </Animated.View>
  );
}
