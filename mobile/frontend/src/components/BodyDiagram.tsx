import { useWindowDimensions } from 'react-native';
import Svg, { Path } from 'react-native-svg';
import { BODY_PART_PATHS, RENDER_ORDER } from './bodyPaths';
import type { BodyPartProgress } from '../types/progress';
import { useTheme } from '../utils/theme';

type BodyDiagramProps = {
  parts: BodyPartProgress[];
  onPressPart: (part: BodyPartProgress) => void;
};

// Diagrama anatomico. Solo las partes con tratamiento activo (hasTreatment=true)
// son clickables y se pintan en accent. Colores leidos del Theme — adapta a dark.
export function BodyDiagram(props: BodyDiagramProps) {
  const { parts, onPressPart } = props;
  const { theme } = useTheme();
  const { width } = useWindowDimensions();

  const svgWidth = Math.min(width - 32, 300);
  const svgHeight = svgWidth * 2;

  const partsMap = new Map(parts.map(function (p) { return [p.id, p]; }));

  return (
    <Svg width={svgWidth} height={svgHeight} viewBox="0 0 300 600">
      {RENDER_ORDER.map(function (partId) {
        const part = partsMap.get(partId);
        const activa = part?.hasTreatment ?? false;
        const fill = activa ? theme.accent2 : theme.bodyFill;
        const stroke = activa ? theme.accent : theme.bodyStroke;

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
