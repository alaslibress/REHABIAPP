import { useCallback, useState } from 'react';
import { RefreshControl, ScrollView, View } from 'react-native';
import { useProgressStore } from '../../src/store/progressStore';
import { useErrorStore } from '../../src/store/errorStore';
import { BodyDiagram } from '../../src/components/BodyDiagram';
import { ProgressChartModal } from '../../src/components/ProgressChartModal';
import { useTheme } from '../../src/utils/theme';
import { useRefreshOnFocus } from '../../src/utils/useRefreshOnFocus';
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

  // Refresca progreso al enfocar la pestana / volver del background.
  useRefreshOnFocus(fetchProgress);

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

  // Siempre pintamos el muneco: si no hay zonas activas, BodyDiagram las pinta
  // en gris claro. Antes mostrabamos un EmptyState que ocultaba el cuerpo y
  // dejaba al paciente sin referencia visual.

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
