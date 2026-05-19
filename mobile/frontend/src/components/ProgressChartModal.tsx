import { ActivityIndicator, Pressable, useWindowDimensions, View } from 'react-native';
import { X } from 'phosphor-react-native';
import { LineChart } from 'react-native-chart-kit';
import { BackdropModal } from './BackdropModal';
import { ProgressBar } from './ProgressBar';
import { AppText } from './AppText';
import { useTheme } from '../utils/theme';
import type { BodyPartProgress, BodyPartMetric } from '../types/progress';

type ProgressChartModalProps = {
  visible: boolean;
  onClose: () => void;
  bodyPart: BodyPartProgress | null;
  metrics: BodyPartMetric[];
  loadingMetrics: boolean;
};

export function ProgressChartModal(props: ProgressChartModalProps) {
  const { visible, onClose, bodyPart, metrics, loadingMetrics } = props;
  const { scheme, theme } = useTheme();
  const { width } = useWindowDimensions();

  const isDark = scheme === 'dark';
  const chartWidth = Math.min(width - 80, 280);

  // Sin datos — estado inline segun plan (no popup)
  const sinDatos = !loadingMetrics && metrics.length === 0;

  // Reduce densidad de etiquetas: muestra la fecha solo en puntos pares
  // para evitar solapamiento cuando hay muchos registros.
  const chartLabels = metrics.length > 0
    ? metrics.map(function (m, idx) {
        if (metrics.length > 4 && idx % 2 !== 0) return '';
        return m.date.slice(5);
      })
    : [''];

  // Preparar datos del grafico con al menos 2 puntos para trazar linea
  const chartData = metrics.length > 0
    ? { labels: chartLabels, datasets: [{ data: metrics.map(function (m) { return m.score; }) }] }
    : { labels: [''], datasets: [{ data: [0] }] };

  if (!bodyPart) return null;

  return (
    <BackdropModal visible={visible} onClose={onClose}>
      <View className={`rounded-2xl p-5 gap-4 ${isDark ? 'bg-surface-dark' : 'bg-surface'}`}>
        {/* Cabecera */}
        <View className="flex-row justify-between items-center">
          <AppText variant="subtitle" weight="semibold" className="text-text-primary dark:text-text-primary-dark flex-1">
            {bodyPart.name}
          </AppText>
          <Pressable onPress={onClose} hitSlop={12}>
            <X size={24} color={isDark ? '#E8ECF3' : '#0F1B33'} weight="regular" />
          </Pressable>
        </View>

        {/* Contenido */}
        {loadingMetrics ? (
          <ActivityIndicator color={theme.accent} />
        ) : sinDatos ? (
          <AppText variant="body" className="text-text-secondary dark:text-text-secondary-dark text-center">
            Sin datos todavia. Juega para ver tu progreso.
          </AppText>
        ) : (
          <LineChart
            data={chartData}
            width={chartWidth}
            height={180}
            chartConfig={{
              backgroundGradientFrom: theme.elev1,
              backgroundGradientTo: theme.elev1,
              color: function () { return theme.accent; },
              labelColor: function () { return theme.text3; },
              propsForDots: { r: '4', strokeWidth: '2', stroke: theme.accent },
              propsForLabels: { fontSize: 10 },
              decimalPlaces: 0,
            }}
            bezier
            style={{ borderRadius: 8, paddingRight: 24 }}
            withInnerLines={false}
            verticalLabelRotation={-30}
            xLabelsOffset={-4}
          />
        )}

        {/* Barra de progreso */}
        <View className="gap-2">
          <AppText variant="label" className="text-text-secondary dark:text-text-secondary-dark">
            {`${Math.round(bodyPart.progressPct)}% completado`}
          </AppText>
          <ProgressBar value={bodyPart.progressPct} />
        </View>

        {/* Texto de mejora */}
        {!sinDatos && (
          <AppText variant="body" className="text-text-secondary dark:text-text-secondary-dark">
            {`Has mejorado un ${Math.round(bodyPart.improvementPct)}% en las ultimas ${bodyPart.periodLabel}.`}
          </AppText>
        )}
      </View>
    </BackdropModal>
  );
}
