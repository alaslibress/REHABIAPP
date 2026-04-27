import { View } from 'react-native';
import { AppText } from './AppText';
import { LevelBadge } from './LevelBadge';
import { TreatmentCard } from './TreatmentCard';
import type { Disability, Treatment } from '../types/treatments';

type Props = {
  disability: Disability;
  treatments: Treatment[];
  onDownloadPdf: (codTrat: string) => Promise<void>;
};

export function DisabilitySection({ disability, treatments, onDownloadPdf }: Props) {
  if (treatments.length === 0) return null;

  return (
    <View className="mb-6">
      {/* Cabecera de seccion */}
      <View className="flex-row items-center gap-3 mb-3">
        <AppText
          variant="subtitle"
          weight="semibold"
          className="text-text-primary dark:text-text-primary-dark flex-1"
        >
          {disability.name}
        </AppText>
        <LevelBadge level={disability.currentLevel} />
      </View>

      {/* Tarjetas de tratamiento */}
      {treatments.map(function (t) {
        return (
          <TreatmentCard
            key={t.id}
            treatment={t}
            onDownloadPdf={onDownloadPdf}
          />
        );
      })}
    </View>
  );
}
