import { useCallback, useState } from 'react';
import { RefreshControl, ScrollView, View } from 'react-native';
import { useProgressStore } from '../../src/store/progressStore';
import { useErrorStore } from '../../src/store/errorStore';
import { BodyDiagram } from '../../src/components/BodyDiagram';
import { ProgressChartModal } from '../../src/components/ProgressChartModal';
import { EmptyState } from '../../src/components/EmptyState';
import { useTheme } from '../../src/utils/theme';
import { parseGraphQLError } from '../../src/utils/errorHandler';
import type { BodyPartProgress } from '../../src/types/progress';

export default function ProgressScreen() {
  const { scheme } = useTheme();
  const bodyParts = useProgressStore(function (s) { return s.bodyParts; });
  const metricsByPart = useProgressStore(function (s) { return s.metricsByPart; });
  const loadingMetrics = useProgressStore(function (s) { return s.loadingMetrics; });
  const fetchProgress = useProgressStore(function (s) { return s.fetch; });
  const loadMetrics = useProgressStore(function (s) { return s.loadMetrics; });
  const showError = useErrorStore(function (s) { return s.showError; });

  const [refrescando, setRefrescando] = useState(false);
  const [selectedPart, setSelectedPart] = useState<BodyPartProgress | null>(null);
  const [modalVisible, setModalVisible] = useState(false);

  const handleRefresh = useCallback(async function () {
    setRefrescando(true);
    try {
      await fetchProgress();
    } catch (err) {
      showError(parseGraphQLError(err));
    } finally {
      setRefrescando(false);
    }
  }, [fetchProgress, showError]);

  async function handlePressPart(part: BodyPartProgress) {
    setSelectedPart(part);
    setModalVisible(true);
    try {
      await loadMetrics(part.id);
    } catch (err) {
      showError(parseGraphQLError(err));
    }
  }

  function handleCloseModal() {
    setModalVisible(false);
    setSelectedPart(null);
  }

  const bgClass = scheme === 'dark' ? 'bg-background-dark' : 'bg-background';
  const hayActivas = bodyParts.some(function (p) { return p.hasTreatment; });

  if (!hayActivas && bodyParts.length > 0) {
    return (
      <View className={`flex-1 ${bgClass}`}>
        <EmptyState
          icon="body-outline"
          title="Sin zonas activas"
          message="No tienes tratamientos asignados a zonas del cuerpo."
        />
      </View>
    );
  }

  return (
    <ScrollView
      className={`flex-1 ${bgClass}`}
      contentContainerClassName="items-center py-6 px-4 gap-6"
      refreshControl={
        <RefreshControl refreshing={refrescando} onRefresh={handleRefresh} />
      }
    >
      <BodyDiagram parts={bodyParts} onPressPart={handlePressPart} />

      <ProgressChartModal
        visible={modalVisible}
        onClose={handleCloseModal}
        bodyPart={selectedPart}
        metrics={selectedPart ? (metricsByPart[selectedPart.id] ?? []) : []}
        loadingMetrics={loadingMetrics}
      />
    </ScrollView>
  );
}
