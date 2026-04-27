import { useWindowDimensions } from 'react-native';
import Svg, { Path } from 'react-native-svg';
import { BODY_PART_PATHS, RENDER_ORDER } from './bodyPaths';
import type { BodyPartProgress } from '../types/progress';
import { useTheme } from '../utils/theme';

// Color activo (tratamiento en curso)
const COLOR_ACTIVO = '#2563EB';
// Colores inactivos — mas opacos para que la silueta humana se perciba claramente
const COLOR_INACTIVO_LIGHT = '#CBD5E1';
const COLOR_INACTIVO_DARK = '#334155';
// Borde de cada parte — da sensacion de contorno anatomico
const STROKE_LIGHT = '#94A3B8';
const STROKE_DARK = '#0F172A';

type BodyDiagramProps = {
  parts: BodyPartProgress[];
  onPressPart: (part: BodyPartProgress) => void;
};

export function BodyDiagram(props: BodyDiagramProps) {
  const { parts, onPressPart } = props;
  const { scheme } = useTheme();
  const { width } = useWindowDimensions();

  // Escalar el diagrama al ancho disponible manteniendo proporcion 300:600
  const svgWidth = Math.min(width - 32, 300);
  const svgHeight = svgWidth * 2;

  const partsMap = new Map(parts.map(function (p) { return [p.id, p]; }));
  const colorInactivo = scheme === 'dark' ? COLOR_INACTIVO_DARK : COLOR_INACTIVO_LIGHT;

  return (
    <Svg
      width={svgWidth}
      height={svgHeight}
      viewBox="0 0 300 600"
    >
      {RENDER_ORDER.map(function (partId) {
        const part = partsMap.get(partId);
        const activa = part?.hasTreatment ?? false;
        const fill = activa ? COLOR_ACTIVO : colorInactivo;
        const stroke = scheme === 'dark' ? STROKE_DARK : STROKE_LIGHT;

        return (
          <Path
            key={partId}
            d={BODY_PART_PATHS[partId]}
            fill={fill}
            stroke={stroke}
            strokeWidth={1.5}
            opacity={activa ? 1 : 0.85}
            onPress={activa && part ? function () { onPressPart(part); } : undefined}
          />
        );
      })}
    </Svg>
  );
}
