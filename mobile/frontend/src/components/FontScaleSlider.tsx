import { useRef, useState } from 'react';
import {
  LayoutChangeEvent,
  PanResponder,
  View,
} from 'react-native';
import { AppText } from './AppText';
import type { FontScale } from '../utils/fontScale';

const SCALES: FontScale[] = [0.9, 1.0, 1.15, 1.3];
const SCALE_LABELS = ['A-', 'A', 'A+', 'A++'];

type FontScaleSliderProps = {
  value: FontScale;
  onChange: (v: FontScale) => void;
};

// Slider discreto: linea horizontal con cuatro puntos (A-, A, A+, A++).
// El usuario arrastra el pulgar y el valor se ajusta al punto mas cercano.
export function FontScaleSlider(props: FontScaleSliderProps) {
  const { value, onChange } = props;
  const [trackWidth, setTrackWidth] = useState(0);
  const trackWidthRef = useRef(0);

  function handleLayout(e: LayoutChangeEvent) {
    const w = e.nativeEvent.layout.width;
    trackWidthRef.current = w;
    setTrackWidth(w);
  }

  // Convierte posicion X (pixels) al indice mas cercano [0..3]
  function xToIndex(x: number): number {
    const w = trackWidthRef.current;
    if (w <= 0) return 0;
    const clamped = Math.max(0, Math.min(w, x));
    const step = w / (SCALES.length - 1);
    return Math.round(clamped / step);
  }

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      onPanResponderGrant: function (_evt, gesture) {
        const idx = xToIndex(gesture.x0 - gesture.moveX + gesture.moveX);
        onChange(SCALES[idx]);
      },
      onPanResponderMove: function (evt) {
        const idx = xToIndex(evt.nativeEvent.locationX);
        onChange(SCALES[idx]);
      },
      onPanResponderRelease: function (evt) {
        const idx = xToIndex(evt.nativeEvent.locationX);
        onChange(SCALES[idx]);
      },
    }),
  ).current;

  const currentIndex = SCALES.indexOf(value);
  const thumbX =
    trackWidth > 0 && currentIndex >= 0
      ? (trackWidth / (SCALES.length - 1)) * currentIndex
      : 0;

  return (
    <View>
      {/* Track + thumb */}
      <View
        className="h-10 justify-center"
        onLayout={handleLayout}
        {...panResponder.panHandlers}
      >
        {/* Linea horizontal */}
        <View className="h-1 rounded-full bg-border dark:bg-border-dark" />
        {/* Puntos de paso */}
        <View
          className="absolute left-0 right-0 flex-row justify-between"
          pointerEvents="none"
        >
          {SCALES.map(function (s, i) {
            const active = i <= currentIndex;
            return (
              <View
                key={s}
                className={`w-3 h-3 rounded-full ${active ? 'bg-primary-600' : 'bg-border dark:bg-border-dark'}`}
              />
            );
          })}
        </View>
        {/* Thumb */}
        <View
          className="absolute w-6 h-6 rounded-full bg-primary-600 border-2 border-white dark:border-surface-dark"
          style={{ left: Math.max(0, thumbX - 12) }}
          pointerEvents="none"
        />
      </View>
      {/* Etiquetas */}
      <View className="flex-row justify-between mt-2">
        {SCALE_LABELS.map(function (lbl) {
          return (
            <AppText
              key={lbl}
              variant="caption"
              className="text-text-secondary dark:text-text-secondary-dark"
            >
              {lbl}
            </AppText>
          );
        })}
      </View>
    </View>
  );
}
