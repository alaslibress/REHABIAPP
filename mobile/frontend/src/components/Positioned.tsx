import React from 'react';
import { View, type DimensionValue } from 'react-native';

// Helper de posicionamiento absoluto para las burbujas del Inicio.
// Si centerX=true aplica translateX(-50%) para centrar el hijo en el eje X.
type PositionedProps = {
  top?: DimensionValue;
  left?: DimensionValue;
  right?: DimensionValue;
  bottom?: DimensionValue;
  centerX?: boolean;
  children: React.ReactNode;
};

export function Positioned(props: PositionedProps) {
  const { top, left, right, bottom, centerX, children } = props;

  return (
    <View
      style={{
        position: 'absolute',
        top,
        left,
        right,
        bottom,
        transform: centerX ? [{ translateX: -46 }] : undefined,
      }}
    >
      {children}
    </View>
  );
}
