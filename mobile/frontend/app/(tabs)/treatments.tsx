import { useCallback, useState } from 'react';
import { ScrollView, View, RefreshControl } from 'react-native';
import { useTreatmentsStore } from '../../src/store/treatmentsStore';
import { useErrorStore } from '../../src/store/errorStore';
import { DisabilitySection } from '../../src/components/DisabilitySection';
import { DownloadProgressModal } from '../../src/components/DownloadProgressModal';
import { EmptyState } from '../../src/components/EmptyState';
import { useTheme } from '../../src/utils/theme';
import { parseGraphQLError } from '../../src/utils/errorHandler';

export default function TreatmentsScreen() {
  const { scheme } = useTheme();
  const items = useTreatmentsStore(function (s) { return s.items; });
  const disabilities = useTreatmentsStore(function (s) { return s.disabilities; });
  const loading = useTreatmentsStore(function (s) { return s.loading; });
  const fetchTratamientos = useTreatmentsStore(function (s) { return s.fetch; });
  const downloadPdf = useTreatmentsStore(function (s) { return s.downloadPdf; });
  const showError = useErrorStore(function (s) { return s.showError; });

  const [refrescando, setRefrescando] = useState(false);
  const [descargando, setDescargando] = useState(false);

  const handleRefresh = useCallback(async function () {
    setRefrescando(true);
    await fetchTratamientos();
    setRefrescando(false);
  }, [fetchTratamientos]);

  async function handleDownloadPdf(codTrat: string) {
    setDescargando(true);
    try {
      await downloadPdf(codTrat);
    } catch (err) {
      const appError = parseGraphQLError(err);
      showError(appError);
    } finally {
      setDescargando(false);
    }
  }

  const bgClass = scheme === 'dark' ? 'bg-background-dark' : 'bg-background';

  // Solo tratamientos visibles agrupados por discapacidad
  const visibles = items.filter(function (t) { return t.visible; });

  if (visibles.length === 0 && !loading) {
    return (
      <View className={`flex-1 ${bgClass} justify-center items-center p-6`}>
        <EmptyState
          icon="medkit-outline"
          title="Sin tratamientos"
          message="No tienes tratamientos asignados todavia."
        />
      </View>
    );
  }

  return (
    <>
      <ScrollView
        className={`flex-1 ${bgClass}`}
        contentContainerStyle={{ padding: 24, paddingBottom: 40 }}
        refreshControl={
          <RefreshControl
            refreshing={refrescando}
            onRefresh={handleRefresh}
            tintColor="#2563EB"
            colors={['#2563EB']}
          />
        }
      >
        {disabilities.map(function (dis) {
          const tratsDis = visibles.filter(function (t) { return t.disabilityCode === dis.id; });
          return (
            <DisabilitySection
              key={dis.id}
              disability={dis}
              treatments={tratsDis}
              onDownloadPdf={handleDownloadPdf}
            />
          );
        })}

        {/* Tratamientos sin discapacidad asociada conocida */}
        {(function () {
          const disIds = new Set(disabilities.map(function (d) { return d.id; }));
          const huerfanos = visibles.filter(function (t) { return !disIds.has(t.disabilityCode); });
          if (huerfanos.length === 0) return null;
          return (
            <DisabilitySection
              disability={{ id: '_otros', codDis: '_otros', name: 'Otros tratamientos', description: null, currentLevel: 0 }}
              treatments={huerfanos}
              onDownloadPdf={handleDownloadPdf}
            />
          );
        })()}
      </ScrollView>

      <DownloadProgressModal visible={descargando} />
    </>
  );
}
