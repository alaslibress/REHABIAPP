import { useState } from 'react';
import { View, Pressable } from 'react-native';
import { CaretDown, CaretUp, FilePdf } from 'phosphor-react-native';
import { FloatingCard } from './FloatingCard';
import { useTheme } from '../utils/theme';
import { AppText } from './AppText';
import { LevelBadge } from './LevelBadge';
import type { Treatment } from '../types/treatments';

type Props = {
  treatment: Treatment;
  onDownloadPdf: (codTrat: string) => Promise<void>;
};

export function TreatmentCard({ treatment, onDownloadPdf }: Props) {
  const { theme } = useTheme();
  const [expandido, setExpandido] = useState(false);
  const [descargando, setDescargando] = useState(false);
  const ChevronIcon = expandido ? CaretUp : CaretDown;

  async function handleDescarga() {
    setDescargando(true);
    try {
      await onDownloadPdf(treatment.codTrat);
    } finally {
      setDescargando(false);
    }
  }

  return (
    <FloatingCard className="mb-3">
      {/* Cabecera con nombre + chevron */}
      <Pressable
        onPress={function () { setExpandido(function (p) { return !p; }); }}
        className="flex-row justify-between items-start"
      >
        <View className="flex-1 mr-3">
          <AppText
            variant="label"
            weight="semibold"
            className="text-text-primary dark:text-text-primary-dark mb-1"
          >
            {treatment.name}
          </AppText>
          <LevelBadge level={treatment.progressionLevel} />
        </View>
        <ChevronIcon size={20} color={theme.text3} weight="regular" />
      </Pressable>

      {/* Cuerpo expandible */}
      {expandido && (
        <View className="mt-4 gap-3">
          {/* Resumen */}
          {treatment.summary && (
            <View>
              <AppText variant="caption" weight="semibold" className="text-text-secondary dark:text-text-secondary-dark mb-1 uppercase tracking-wide">
                Resumen
              </AppText>
              <AppText variant="body" className="text-text-primary dark:text-text-primary-dark">
                {treatment.summary}
              </AppText>
            </View>
          )}

          {/* Materiales */}
          {treatment.materials.length > 0 && (
            <View>
              <AppText variant="caption" weight="semibold" className="text-text-secondary dark:text-text-secondary-dark mb-1 uppercase tracking-wide">
                Materiales necesarios
              </AppText>
              {treatment.materials.map(function (m, i) {
                return (
                  <AppText key={i} variant="body" className="text-text-primary dark:text-text-primary-dark">
                    {'• '}{m}
                  </AppText>
                );
              })}
            </View>
          )}

          {/* Medicacion */}
          {treatment.medication.length > 0 && (
            <View>
              <AppText variant="caption" weight="semibold" className="text-text-secondary dark:text-text-secondary-dark mb-1 uppercase tracking-wide">
                Medicacion
              </AppText>
              {treatment.medication.map(function (m, i) {
                return (
                  <AppText key={i} variant="body" className="text-text-primary dark:text-text-primary-dark">
                    {'• '}{m}
                  </AppText>
                );
              })}
            </View>
          )}

          {/* Boton PDF */}
          {treatment.hasDocument ? (
            <Pressable
              onPress={handleDescarga}
              disabled={descargando}
              className={`flex-row items-center justify-center gap-2 py-3 rounded-xl min-h-12 ${descargando ? 'bg-primary-300' : 'bg-primary-600'}`}
            >
              <FilePdf size={18} color="#FFFFFF" weight="regular" />
              <AppText variant="label" weight="semibold" className="text-white">
                {descargando ? 'Descargando...' : 'Descargar PDF'}
              </AppText>
            </Pressable>
          ) : (
            <View className="flex-row items-center justify-center gap-2 py-3 rounded-xl min-h-12 bg-border dark:bg-border-dark">
              <FilePdf size={18} color={theme.textMuted} weight="regular" />
              <AppText variant="label" className="text-text-secondary dark:text-text-secondary-dark">
                PDF pendiente
              </AppText>
            </View>
          )}
        </View>
      )}
    </FloatingCard>
  );
}
